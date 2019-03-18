package qif.importer;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import qif.data.Account;
import qif.data.Common;
import qif.data.QDate;
import qif.data.Security;
import qif.data.SecurityPortfolio;
import qif.data.SecurityPosition;
import qif.data.Statement;

class StatementProcessor {
	private QifDomReader qrdr;

	public StatementProcessor(QifDomReader qrdr) {
		this.qrdr = qrdr;
	}

	public void loadStatements(File file) {
		for (;;) {
			String s = this.qrdr.getFileReader().peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			List<Statement> stmts = loadStatementsSection(this.qrdr.getFileReader());
			for (Statement stmt : stmts) {
				Account.currAccountBeingLoaded.statements.add(stmt);
				Account.currAccountBeingLoaded.statementFile = file;
			}
		}
	}

	private List<Statement> loadStatementsSection(QFileReader qfr) {
		final QFileReader.QLine qline = new QFileReader.QLine();
		final List<Statement> stmts = new ArrayList<Statement>();

		Statement currstmt = null;

		for (;;) {
			qfr.nextStatementsLine(qline);

			switch (qline.type) {
			case EndOfSection:
				return stmts;

			case StmtsAccount: {
				final String aname = qline.value;
				final Account a = Account.findAccount(aname);
				if (a == null) {
					Common.reportError("Can't find account: " + aname);
				}

				Account.currAccountBeingLoaded = a;
				currstmt = null;
				break;
			}

			case StmtsMonthly: {
				final String[] ss = qline.value.split(" ");
				int ssx = 0;

				final String datestr = ss[ssx++];
				final int slash1 = datestr.indexOf('/');
				final int slash2 = (slash1 < 0) ? -1 : datestr.indexOf('/', slash1 + 1);
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

					final BigDecimal bal = new BigDecimal(balStr);
					final QDate d = (day == 0) //
							? QDate.getDateForEndOfMonth(year, month) //
							: new QDate(year, month, day);

					final Statement prevstmt = (stmts.isEmpty() ? null : stmts.get(stmts.size() - 1));

					currstmt = new Statement(Account.currAccountBeingLoaded.acctid, d);
					currstmt.closingBalance = currstmt.cashBalance = bal;
					if ((prevstmt != null) && (prevstmt.acctid == currstmt.acctid)) {
						currstmt.prevStatement = prevstmt;
					}

					stmts.add(currstmt);

					++month;
				}

				break;
			}

			case StmtsCash:
				currstmt.cashBalance = Common.parseDecimal(qline.value);
				break;

			case StmtsSecurity: {
				final String[] ss = qline.value.split(";");
				int ssx = 0;

				// S<SYM>;[<order>;]QTY;VALUE;PRICE
				final String secStr = ss[ssx++];

				String ordStr = ss[ssx];
				if ("qpv".indexOf(ordStr.charAt(0)) < 0) {
					ordStr = "qvp";
				} else {
					++ssx;
				}

				final int qidx = ordStr.indexOf('q');
				final int vidx = ordStr.indexOf('v');
				final int pidx = ordStr.indexOf('p');

				final String qtyStr = ((qidx >= 0) && (qidx + ssx < ss.length)) ? ss[qidx + ssx] : "x";
				final String valStr = ((vidx >= 0) && (vidx + ssx < ss.length)) ? ss[vidx + ssx] : "x";
				final String priceStr = ((pidx >= 0) && (pidx + ssx < ss.length)) ? ss[pidx + ssx] : "x";

				final Security sec = Security.findSecurity(secStr);
				if (sec == null) {
					Common.reportError("Unknown security: " + secStr);
				}

				final SecurityPortfolio h = currstmt.holdings;
				final SecurityPosition p = new SecurityPosition(sec);

				p.value = (valStr.equals("x")) ? null : new BigDecimal(valStr);
				p.endingShares = (qtyStr.equals("x")) ? null : new BigDecimal(qtyStr);
				BigDecimal price = (priceStr.equals("x")) ? null : new BigDecimal(priceStr);
				final BigDecimal price4date = sec.getPriceForDate(currstmt.date).getPrice();

				// We care primarily about the number of shares. If that is not
				// present, the other two must be set for us to calculate the
				// number of shares. If the price is not present, we can use the
				// price on the day of the statement.
				// If we know two of the values, we can calculate the third.
				if (p.endingShares == null) {
					if (p.value != null) {
						if (price == null) {
							price = price4date;
						}

						p.endingShares = p.value.divide(price, RoundingMode.HALF_UP);
					}
				} else if (p.value == null) {
					if (p.endingShares != null) {
						if (price == null) {
							price = price4date;
						}

						p.value = price.multiply(p.endingShares);
					}
				} else if (price == null) {
					price = price4date;
				}

				if (p.endingShares == null) {
					Common.reportError("Missing security info in stmt");
				}

				h.positions.add(p);
				break;
			}

			default:
				Common.reportError("syntax error");
			}
		}
	}

	public void processStatementFiles(File stmtDirectory) {
		if (!stmtDirectory.isDirectory()) {
			return;
		}

		File stmtFiles[] = stmtDirectory.listFiles();

		for (final File f : stmtFiles) {
			if (!f.getName().endsWith(".qif")) {
				continue;
			}

			try {
				qrdr.load(f.getAbsolutePath(), false);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}

		buildStatementChains();
	}

	private void buildStatementChains() {
		for (Account a : Account.getAccounts()) {
			Statement last = null;

			for (Statement s : a.statements) {
				assert (last == null) || (last.date.compareTo(s.date) < 0);
				s.prevStatement = last;
				last = s;
			}
		}
	}
}