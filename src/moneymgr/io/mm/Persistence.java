package moneymgr.io.mm;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import moneymgr.model.Account;
import moneymgr.model.AccountCategory;
import moneymgr.model.AccountType;
import moneymgr.model.Category;
import moneymgr.model.GenericTxn;
import moneymgr.model.InvestmentTxn;
import moneymgr.model.Lot;
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.MultiSplitTxn;
import moneymgr.model.QPrice;
import moneymgr.model.Security;
import moneymgr.model.Security.SplitInfo;
import moneymgr.model.SecurityPortfolio;
import moneymgr.model.SecurityPosition;
import moneymgr.model.SimpleTxn;
import moneymgr.model.SplitTxn;
import moneymgr.model.Statement;
import moneymgr.model.StockOption;
import moneymgr.model.TxAction;
import moneymgr.util.Common;

/** Read/write data in native (JSON) format */
public class Persistence {
	private static String encodeString(String s) {
		if (s == null) {
			return "\"\"";
		}

		StringBuilder sb = new StringBuilder(s);

		int idx = 0;
		String sub = null;

		while (idx < sb.length()) {
			char ch = sb.charAt(idx);

			switch (ch) {
			case '"':
				sub = "&q";
				break;
			case '&':
				sub = "&&";
				break;
			case ',':
				sub = "&c";
				break;
			case ';':
				sub = "&s";
				break;
			case '[':
				sub = "&(";
				break;
			case ']':
				sub = "&)";
				break;
			}

			if (sub != null) {
				sb.replace(idx, idx + 1, sub);
				idx += sub.length();
				sub = null;
			} else {
				++idx;
			}
		}

		return String.format("\"%s\"", sb.toString());
	}

	private static String encodeAmount3(BigDecimal amt) {
		return encodeString(Common.formatAmount3(amt).trim());
	}

	private static String encodeAmount(BigDecimal amt) {
		return encodeString(Common.formatAmount3(amt).trim());
	}

	private static String decodeString(String s) {
		// Strip quotes
		StringBuilder sb = new StringBuilder(s.substring(1, s.length() - 1));

		int idx = 0;
		String sub = null;

		while (idx < sb.length()) {
			char ch = sb.charAt(idx);

			if (ch == '&') {
				sb.delete(idx, idx + 1);

				switch (sb.charAt(idx)) {
				case 'q':
					sub = "\"";
					break;
				case '&':
					sub = "&";
					break;
				case 'c':
					sub = ",";
					break;
				case 's':
					sub = ";";
					break;
				case '(':
					sub = "[";
					break;
				case ')':
					sub = "]";
					break;
				}
			}

			if (sub != null) {
				sb.replace(idx, idx + 1, sub);
				idx += sub.length();
				sub = null;
			} else {
				++idx;
			}
		}

		return sb.toString();
	}

	public static void validateTransfers(GenericTxn tx, int[] counts) {
		boolean isTransfer = tx.getAction().isTransfer //
				|| (tx.getCashTransferAcctid() > 0);

		int xacctid = tx.getCashTransferAcctid();
		if (xacctid == tx.getAccountID()) {
			// TODO Should just get rid of self-transfers!
			xacctid = 0;
			isTransfer = false;
		}

		SimpleTxn xtxn = tx.getCashTransferTxn();
		List<InvestmentTxn> xtxns = tx.getSecurityTransferTxns();
		String err = null;

		if (xacctid > 0 && tx.hasSplits()) {
			// TODO this happens but doesn't make sense, deal with it later
			return;
		}

		if ((xacctid > 0) //
				&& (xtxn == null) && ((xtxns == null) || xtxns.isEmpty())) {
			err = "xacctid set but no xtxn exists";
		} else if ((xtxn != null) && (xtxns != null) && !xtxns.isEmpty()) {
			// TODO this may be fine, if we can transfer shares and cash together
			err = "xtxn and xtxns both set!";
		} else if (isTransfer) {
			if ((tx.getAction() == TxAction.SHRS_IN) //
					|| (tx.getAction() == TxAction.SHRS_OUT)) {
				if ((xtxn != null) || ((xtxns != null) && !xtxns.isEmpty())) {
					err = "shares in/out sometimes involves transfer";
					// TODO Not necessarily a problem; we see both with/without
					err = null;
				}
			} else if ((xtxn == null) && ((xtxns == null) || xtxns.isEmpty())) {
				err = "xtxn/xtxns missing from xfer txn!";
			}
		} else if ((tx instanceof InvestmentTxn) //
				&& ((xtxn != null) || ((xtxns != null) && !xtxns.isEmpty()))) {
			err = "xtxn/xtxns set in non-xfer txn!";
		}

		if (err != null) {
			System.out.println(tx.toString());
			System.out.println("" + ++counts[1] + "/" + counts[0] + ": " + err);
		} else if ((xacctid > 0) //
				|| (xtxn != null) //
				|| ((xtxns != null) && !xtxns.isEmpty()) //
				|| tx.getAction().isTransfer) {
			++counts[0];
		}
	}

	private String filename;
	private LineNumberReader rdr;
	private PrintStream wtr;

	public Persistence(String filename) {
		this.filename = filename;
	}

	private void saveCategories() {
		// ------------------------------------------------
		wtr.println("");
		wtr.println("\"Categories\": [");
		// ------------------------------------------------

		wtr.print("  [\"id\",\"name\",\"desc\",\"isExpense\"]");
		String sep = ",";
		int id = 1;
		for (int catid = 1; catid < MoneyMgrModel.getNextCategoryID(); ++catid) {
			while (id++ < catid) {
				wtr.println(sep);
				wtr.print("  [0]");
			}

			Category cat = MoneyMgrModel.getCategory(catid);

			String line;
			if (cat == null) {
				line = "  [0]";
			} else {
				line = String.format("  [%d,%s,%s,%s]", //
						cat.catid, //
						encodeString(cat.name), //
						encodeString(cat.description), //
						cat.isExpense);
			}

			wtr.println(sep);
			wtr.print(line);
		}

		wtr.println();
		wtr.println("],");
	}

	private void saveAccounts() {
		String sep = "";

		// ------------------------------------------------
		wtr.println("");
		wtr.println("\"AccountTypes\": [");
		// ------------------------------------------------

		wtr.print("  [\"id\",\"name\",\"isAsset\",\"isInvestment\",\"isCash\"]");
		int id = 1;
		sep = ",";
		for (AccountType at : AccountType.values()) {
			while (id++ < at.id) {
				wtr.println(sep);
				wtr.print("  [0]");
			}

			String line = String.format("  [%d,%s,%s,%s,%s]", //
					at.id, //
					encodeString(at.name), //
					at.isAsset, //
					at.isInvestment, //
					at.isCash);

			wtr.println(sep);
			wtr.print(line);
		}

		wtr.println();
		wtr.println("],");

		// ------------------------------------------------
		wtr.println("");
		wtr.println("\"AccountCategories\": [");
		// ------------------------------------------------

		wtr.print("  [\"label\",\"isAsset\",\"[accountType]\"]");
		sep = ",";
		for (AccountCategory ac : AccountCategory.values()) {
			String line = String.format("  [%s,%s,[", //
					encodeString(ac.label), //
					ac.isAsset);

			String sep2 = "";
			for (AccountType at : ac.accountTypes) {
				line += String.format("%s%s", sep2, at.id);
				sep2 = ",";
			}
			line += "]]";

			wtr.println(sep);
			wtr.print(line);
		}

		wtr.println();
		wtr.println("],");

		// ------------------------------------------------
		wtr.println("");
		wtr.println("\"Accounts\": [");
		// ------------------------------------------------

		wtr.print(
				"  [\"acctid\",\"name\",\"acctypeid\",\"desc\",\"closedate\",\"statfreq\",\"statday\",\"bal\",\"clearbal\"]");
		sep = ",";
		List<Account> accts = Account.getAccountsById();
		for (int acctid = 1; acctid < accts.size(); ++acctid) {
			Account ac = accts.get(acctid);
			String line;

			if (ac == null) {
				line = "  [0]";
			} else {
				int close = (ac.closeDate != null) ? ac.closeDate.getRawValue() : 0;

				line = String.format("  [%d,%s,%d,%s,%d,%d,%d,\"%s\",\"%s\"]", //
						ac.acctid, //
						encodeString(ac.name), //
						ac.type.id, //
						// ac.acctCategory.label, //
						encodeString(ac.description), //
						close, //
						ac.statementFrequency, //
						ac.statementDayOfMonth, //
						Common.formatAmount(ac.balance).trim(), //
						Common.formatAmount(ac.clearedBalance).trim());

				// TODO ac.securities
			}

			wtr.println(sep);
			wtr.print(line);
		}

		wtr.println();
		wtr.println("],");
	}

	void saveSecurities() {
		// ------------------------------------------------
		wtr.println("");
		wtr.println("\"Securities\": [");
		// ------------------------------------------------

		wtr.print("  [\"secid\",\"symbol\",\"[name]\",\"type\",\"[split]\",\"[[date,price]]\"]");

		final String sep = ",";
		List<Security> securities = Security.getSecuritiesById();
		for (int secid = 1; secid < securities.size(); ++secid) {
			Security sec = securities.get(secid);
			String line;

			if (sec == null) {
				line = "  [0]";
			} else {
				String secNames = "[";
				String sep2 = "";
				for (String name : sec.names) {
					secNames += sep2;
					secNames += String.format("%s", encodeString(name));
					sep2 = ",";
				}
				secNames += "]";

				String splits = "[";
				sep2 = "";
				for (SplitInfo split : sec.splits) {
					splits += sep2;
					// TODO probably wrong format for ratio
					splits += String.format("[%d,\"%f\"]", //
							split.splitDate.getRawValue(), //
							split.splitRatio);
					sep2 = ",";
				}
				splits += "]";

				String prices = "[\n";
				int count = 0;
				sep2 = "";
				prices += "      ";
				for (QPrice price : sec.prices) {
					prices += sep2;

					if (count++ == 4) {
						count = 1;
						prices += "\n";
						prices += "      ";
					}

					prices += String.format("[%d,\"%s\"]", //
							price.date.getRawValue(), //
							Common.formatAmount3(price).trim());
					sep2 = ",";
				}

				if (count > 0) {
					prices += "\n";
				}
				prices += "    ]";

				line = String.format("  [%d,%s,%s,%s,\n    %s,\n    %s\n  ]", //
						sec.secid, //
						encodeString(sec.symbol), //
						secNames, //
						encodeString(sec.type), //
						splits, //
						prices);
			}

			wtr.println(sep);
			wtr.print(line);
		}

		wtr.println();
		wtr.println("],");
	}

	void saveLots() {
		// ------------------------------------------------
		wtr.println("");
		wtr.println("\"Lots\": [");
		// ------------------------------------------------

		wtr.print("  [\"lotid\",\"date\",\"acctid\",\"secid\",\"shares\",\"basisprice\",\"createTxid\",\"disposeTxid\"," //
				+ "\"srcLotid\",\"[childLotid]\"]");

		final String sep = ",";
		List<Lot> lots = Lot.getLots();
		for (int lotid = 1; lotid < lots.size(); ++lotid) {
			Lot lot = lots.get(lotid);

			String line;
			if (lot == null) {
				line = "  [0]";
			} else {
				String childlots = "[";
				if (lot.childLots != null) {
					String sep2 = "";
					for (Lot child : lot.childLots) {
						childlots += sep2 + child.lotid;
						sep2 = ",";
					}
				}
				childlots += "]";

				line = String.format("  [%d,%s,%d,%d,%s,%s,%d,%d,%d,%s]", //
						lot.lotid, //
						lot.createDate.getRawValue(), //
						lot.acctid, //
						lot.secid, //
						encodeString(Common.formatAmount3(lot.shares).trim()), //
						encodeString(Common.formatAmount3(lot.basisPrice).trim()), //
						lot.createTransaction.txid, //
						((lot.disposingTransaction != null) ? lot.disposingTransaction.txid : 0), //
						((lot.sourceLot != null) ? lot.sourceLot.lotid : 0), //
						childlots);
			}

			wtr.println(sep);
			wtr.print(line);
		}

		wtr.println();
		wtr.println("]");
	}

	void saveOptions() {
		// ------------------------------------------------
		wtr.println("");
		wtr.println("\"Options\": [");
		// ------------------------------------------------

		wtr.print("  [\"optid\",\"name\",\"date\",\"acctid\",\"secid\",\"shares\",\"strikeprice\",\"marketprice\"," //
				+ "  \"cost\",\"origmarketvalue\",\"lifetimemonths\",\"vestcount\",\"txid\",\"canceldate\",\"srcoptid\"" //
				+ "]");

		final String sep = ",";
		List<StockOption> opts = StockOption.options;
		for (int optid = 1; optid < opts.size(); ++optid) {
			StockOption opt = opts.get(optid);

			String line;
			if (opt == null) {
				line = "  [0]";
			} else {
				line = String.format("  [%d,%s,%d,%d,%d,%s,%s,%s,%s,%s,%s,%d,%d,%d,%d,%d]", //
						opt.optid, //
						encodeString(opt.name), //
						opt.date.getRawValue(), //
						opt.acctid, //
						opt.secid, //
						encodeAmount3(opt.grantShares), //
						encodeAmount3(opt.strikePrice), //
						encodeAmount3(opt.marketPrice), //
						encodeAmount(opt.cost), //
						encodeAmount(opt.marketValueAtPurchase), //
						opt.lifetimeMonths, //
						opt.vestFrequencyMonths, //
						opt.vestCount, //
						// opt.vestCurrent, //
						// opt.sharesRemaining, //
						((opt.transaction != null) ? opt.transaction.txid : 0), //
						((opt.cancelDate != null) ? opt.cancelDate.getRawValue() : 0), //
						((opt.srcOption != null) ? opt.srcOption.optid : 0));
			}

			wtr.println(sep);
			wtr.print(line);
		}

		wtr.println();
		wtr.println("]");
	}

	void saveTransactions() {
		int errcount[] = { 0, 0 };
		String line;

		// ------------------------------------------------
		wtr.println("");
		wtr.println("\"Transactions\": [");
		// ------------------------------------------------

		wtr.print(
				"  [\"id\",\"date\",\"statdate\",\"acctid\",\"xtxid\",\"action\",\"payee\",\"cknum\",\"memo\",\"amt\",\"cat\"," //
						+ "\"secid\",\"secaction\",\"shares\",\"shareprice\",\"splitratio\",\"optid\",\"[split]\",\"[secxfer]\",\"[lot]\"]");

		final String sep = ",";
		List<GenericTxn> txns = GenericTxn.getAllTransactions();
		for (int txid = 1; txid < txns.size(); ++txid) {
			GenericTxn tx = txns.get(txid);

			if (tx == null) {
				line = "  [0]";
			} else {
				String splits = "[";

				String sep1 = "";
				for (SplitTxn split : tx.getSplits()) {
					splits += sep1;

					if (split instanceof MultiSplitTxn) {
						splits += "[";

						String sep2 = "";
						for (SplitTxn ssplit : ((MultiSplitTxn) split).subsplits) {
							splits += sep2;
							splits += String.format("[%d,%s,\"%s\"]", //
									ssplit.getCatid(), //
									encodeString(ssplit.getMemo()), //
									Common.formatAmount(ssplit.getAmount()).trim());
							sep2 = ",";
						}

						splits += "]";
					} else {
						splits += String.format("[%d,%s,\"%s\"]", //
								split.getCatid(), //
								encodeString(split.getMemo()), //
								Common.formatAmount(split.getAmount()).trim() //
						);
					}

					sep1 = ",";
				}
				splits += "]";

				// SimpleTxn
				// DERIVED tx.getCashAmount()
				// DERIVED, not implemented tx.getGain()
				// DERIVED tx.isCredit()
				//
				// GenericTxn
				// NonInvestmentTxn
				// InvestmentTxn

				int sdate = (tx.stmtdate != null) ? tx.stmtdate.getRawValue() : 0;

				validateTransfers(tx, errcount);

				int secid = tx.getSecurityId();
				String shareaction = "";
				BigDecimal shares = BigDecimal.ZERO;
				BigDecimal shareprice = BigDecimal.ZERO;
				BigDecimal splitratio = BigDecimal.ONE;

				String securityTransferTxns = "[]";
				String lots = "[]";
				int optid = 0;

				// TODO investment transactions/stocksplit
				if (tx instanceof InvestmentTxn) {
					InvestmentTxn itx = (InvestmentTxn) tx;

					// TODO encode share action
					shareaction = itx.getShareAction().toString();
					shares = itx.getShares();
					shareprice = itx.getShareCost();
					splitratio = itx.getSplitRatio();

					securityTransferTxns = "[";
					String sep2 = "";
					for (InvestmentTxn xtx : itx.getSecurityTransferTxns()) {
						securityTransferTxns += sep2 + xtx.txid;
						sep2 = ",";
					}
					securityTransferTxns += "]";

					itx.getLots();
					lots = "[";
					sep2 = "";
					for (Lot lot : itx.getLots()) {
						lots += sep2 + lot.lotid;
						sep2 = ",";
					}
					lots += "]";

					if (itx.isStockOptionTxn() && (itx.option != null)) {
						// TODO why would option be null
						optid = itx.option.optid;
					}
				}

				line = String.format("  [%d,%d,%d,%d,%d,%s,%s,%d,%s,\"%s\",%d,%d,%s,%s,%s,%s,%d", //
						tx.txid, //
						tx.getDate().getRawValue(), //
						sdate, //
						tx.getAccountID(), //
						((tx.getCashTransferTxn() != null) ? tx.getCashTransferTxn().txid : 0), //
						encodeString(tx.getAction().name()), //
						encodeString(tx.getPayee()), //
						tx.getCheckNumber(), //
						encodeString(tx.getMemo()), //
						Common.formatAmount(tx.getAmount()).trim(), //
						tx.getCatid(), // TODO catid or splits, not both

						secid, //
						encodeString(shareaction), //
						encodeString(Common.formatAmount3(shares).trim()), //
						encodeString(Common.formatAmount(shareprice).trim()), //
						encodeString(Common.formatAmount(splitratio).trim()), //
						optid);

				line += sep;
				if (splits.length() > 2) {
					line += "\n    ";
				}
				line += splits;

				line += sep;
				if (securityTransferTxns.length() > 2) {
					line += "\n    ";
				}
				line += securityTransferTxns;

				line += sep;
				if (lots.length() > 2) {
					line += "\n    ";
				}
				line += lots;

				line += "]";

			}

			wtr.println(sep);
			wtr.print(line);
		}

		wtr.println();
		wtr.println("],");
	}

	void saveStatements() {
		String line;

		// ------------------------------------------------
		wtr.println("");
		wtr.println("\"Statements\": [");
		// ------------------------------------------------

		wtr.print("  [\"acctid\",\"date\",\"isbal\",\"prevdate\",\"totbal\",\"cashbal\",\"txns\"," //
				+ "\"holdings\"]");

		final String sep = ",";
		for (Account acct : Account.getAccountsById()) {
			// TODO Assign statement id and reference/store accordingly
			if (acct == null) {
				continue;
			}

			for (Statement stmt : acct.statements) {
				String txns = "[";
				String sep2 = "";
				for (GenericTxn tx : stmt.transactions) {
					txns += sep2 + tx.txid;
					sep2 = ",";
				}
				txns += "]";

				String prevdate = (stmt.prevStatement != null) //
						? Integer.toString(stmt.prevStatement.date.getRawValue()) //
						: "0";

				SecurityPortfolio h = stmt.holdings;

				String holdings = "[";
				sep2 = "";
				for (SecurityPosition p : h.positions) {
					holdings += sep2 + String.format("[%d,\"%s\",\"%s\"]", //
							p.security.secid, //
							Common.formatAmount3(p.getEndingShares()).trim(), //
							Common.formatAmount(p.endingValue).trim());
					sep2 = ",";
				}
				holdings += "]";

				line = String.format("  [%d,%d,%s,%s,\"%s\",\"%s\",\n    %s,\n    %s]", //
						stmt.acctid, //
						stmt.date.getRawValue(), //
						stmt.isBalanced, //
						prevdate, //
						Common.formatAmount(stmt.closingBalance).trim(), //
						Common.formatAmount(stmt.getCashBalance()).trim(), //
						txns, //
						holdings);

				wtr.println(sep);
				wtr.print(line);
			}
		}

		wtr.println();
		wtr.println("]");
	}

	public void save() {
		try {
			this.wtr = new PrintStream(this.filename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		wtr.println("{");
		saveCategories();
		saveAccounts();
		saveSecurities();
		saveLots();
		saveOptions();
		saveTransactions();
		saveStatements();
		wtr.println("}");

		wtr.close();
	}

	// TODO testing
	public void validate() {
		load();
	}

	public Object load() {
		Object obj;

		try {
			obj = new JSONParser().parse(new FileReader(this.filename));
		} catch (IOException | ParseException e) {
			e.printStackTrace();
			return null;
		}

		JSONObject jo = (JSONObject) obj;

		Object accts = jo.get("Accounts");
		Object cats = jo.get("Categories");
		Object secs = jo.get("Securities");
		Object trans = jo.get("Transactions");
		Object stats = jo.get("Statements");

		/**
		 * To reconstruct the data completely, do the following:<br>
		 * 1. Create categories <br>
		 * 2. Create securities <br>
		 * 3. Create account types<br>
		 * 4. Create account categories<br>
		 * 5. Create accounts<br>
		 * 6. Create transactions<br>
		 * 7. Create lots<br>
		 * 8. Create options<br>
		 * 9. Create statements<br>
		 */
		return jo;
	}
}
