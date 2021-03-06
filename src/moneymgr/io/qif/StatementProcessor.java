package moneymgr.io.qif;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import moneymgr.io.csv.CSVImport;
import moneymgr.model.Account;
import moneymgr.model.Security;
import moneymgr.model.SecurityPortfolio;
import moneymgr.model.SecurityPosition;
import moneymgr.model.Statement;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/** Load statements from extra quasi-QIF input file */
public class StatementProcessor {
	private final QifDomReader qrdr;

	public StatementProcessor(QifDomReader qrdr) {
		this.qrdr = qrdr;
	}

	/** Load each statement file (one per account) in the statements directory */
	public void loadStatments(File stmtDirectory) {
		if (!stmtDirectory.isDirectory()) {
			return;
		}

		File stmtFiles[] = stmtDirectory.listFiles();

		for (File f : stmtFiles) {
			if (!f.getName().endsWith(".qif")) {
				continue;
			}

			try {
				this.qrdr.load(f.getAbsolutePath(), false);
			} catch (Exception e) {
				e.printStackTrace();
				Common.reportError("statement file '" + f.getName() + "' loading failed");
			}
		}
	}

	/** Load statements for an account from the quasi-QIF file */
	public void loadStatements(File file) {
		for (;;) {
			String s = this.qrdr.getFileReader().peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			List<Statement> stmts = loadStatementsSection(this.qrdr.getFileReader());

			for (Statement stmt : stmts) {
				Account a = this.qrdr.model.currAccountBeingLoaded;
				a.addStatement(stmt);
			}
		}
	}

	private List<Statement> loadStatementsSection(QFileReader qfr) {
		QFileReader.QLine qline = new QFileReader.QLine();
		List<Statement> stmts = new ArrayList<Statement>();

		Statement currstmt = null;

		for (;;) {
			qfr.nextStatementsLine(qline);

			switch (qline.type) {
			case EndOfSection:
				return stmts;

			case StmtsAccount: {
				String aname = qline.value;
				Account a = this.qrdr.model.findAccount(aname);
				if (a == null) {
					Common.reportError("Can't find account: " + aname);
				}

				this.qrdr.model.currAccountBeingLoaded = a;
				currstmt = null;
				break;
			}

			case StmtsMonthly: {
				String[] ss = qline.value.split(" ");
				int ssx = 0;

				String datestr = ss[ssx++];
				int slash1 = datestr.indexOf('/');
				int slash2 = (slash1 < 0) ? -1 : datestr.indexOf('/', slash1 + 1);

				int day = 0; // last day of month
				int month = 1;
				int year = 0;

				if (slash2 > 0) {
					month = Integer.parseInt(datestr.substring(0, slash1));
					day = Integer.parseInt(datestr.substring(slash1 + 1, slash2));
					year = Integer.parseInt(datestr.substring(slash2 + 1));
				} else if (slash1 >= 0) {
					month = Integer.parseInt(datestr.substring(0, slash1));
					year = Integer.parseInt(datestr.substring(slash1 + 1));
				} else {
					year = Integer.parseInt(datestr);
				}

				while (ssx < ss.length) {
					if (month > 12) {
						Common.reportError( //
								"Statements month wrapped to next year:\n" //
										+ qline.value);
					}

					String balStr = ss[ssx++];
					if (balStr.equals("x")) {
						balStr = "0.00";
					}

					BigDecimal bal = new BigDecimal(balStr);
					QDate d = (day == 0) //
							? QDate.getDateForEndOfMonth(year, month) //
							: new QDate(year, month, day);

					Account acct = this.qrdr.model.currAccountBeingLoaded;

					Statement prevstmt = acct.getLastStatement();

					// TODO what if there is a stmt in stmts, but not the last one?
					if (!stmts.isEmpty()) {
						Statement s = stmts.get(stmts.size() - 1);
						if (s.acctid == acct.acctid) {
							prevstmt = s;
						}
					}

					currstmt = new Statement(acct.acctid, d, bal, null, prevstmt);

					stmts.add(currstmt);

					++month;
				}

				break;
			}

			case StmtsCash:
				currstmt.setCashBalance(Common.parseDecimal(qline.value));
				break;

			case StmtsSecurity: {
				String[] ss = qline.value.split(";");
				int ssx = 0;

				// S<SYM>;[<order>;]QTY;VALUE;PRICE
				String secStr = ss[ssx++];

				String ordStr = ss[ssx];
				if ("qpv".indexOf(ordStr.charAt(0)) < 0) {
					ordStr = "qvp";
				} else {
					++ssx;
				}

				// Quantity, Price, Value can occur in different order
				int qidx = ordStr.indexOf('q');
				int vidx = ordStr.indexOf('v');
				int pidx = ordStr.indexOf('p');

				String qtyStr = ((qidx >= 0) && (qidx + ssx < ss.length)) ? ss[qidx + ssx] : "x";
				String valStr = ((vidx >= 0) && (vidx + ssx < ss.length)) ? ss[vidx + ssx] : "x";
				String priceStr = ((pidx >= 0) && (pidx + ssx < ss.length)) ? ss[pidx + ssx] : "x";

				Security sec = this.qrdr.model.findSecurity(secStr);
				if (sec == null) {
					String sym;
					String name;

					if (CSVImport.predefinedSecurityMap.containsKey(secStr)) {
						name = secStr;
						sym = CSVImport.predefinedSecurityMap.get(name);
					}
					if (CSVImport.predefinedSymbolMap.containsKey(secStr)) {
						sym = secStr;
						name = CSVImport.predefinedSymbolMap.get(sym);
					} else {
						Common.reportWarning("Unknown security: " + secStr);
						sym = secStr;
						name = secStr;
					}

					Common.reportWarning(String.format( //
							"Creating security: %s / '%s'", sym, name));
					sec = new Security(sym, name);
					this.qrdr.model.addSecurity(sec);
				}

				SecurityPortfolio hold = currstmt.holdings;
				SecurityPosition pos = // new SecurityPosition(hold, sec);
						hold.getPosition(sec);

				pos.setEndingValue((valStr.equals("x")) ? null : new BigDecimal(valStr));
				BigDecimal endingShares = (qtyStr.equals("x")) ? null : new BigDecimal(qtyStr);
				BigDecimal price = (priceStr.equals("x")) ? null : new BigDecimal(priceStr);
				BigDecimal price4date = sec.getPriceForDate(currstmt.date).getPrice();

				// We care primarily about the number of shares. If that is not
				// present, the other two must be set for us to calculate the
				// number of shares. If the price is not present, we can use the
				// price on the day of the statement.
				// If we know two of the values, we can calculate the third.
				if (endingShares == null) {
					if (price == null) {
						price = price4date;
					}

					endingShares = pos.getEndingValue().divide(price, RoundingMode.HALF_UP);
				} else if (pos.getEndingValue() == null) {
					if (price == null) {
						price = price4date;
					}

					pos.setEndingValue(price.multiply(endingShares));
				} else if (price == null) {
					price = price4date;
				}

				pos.setExpectedEndingShares(endingShares);

				break;
			}

			default:
				Common.reportError("syntax error");
			}
		}
	}
}