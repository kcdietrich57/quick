package moneymgr.io.mm;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
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
import moneymgr.model.NonInvestmentTxn;
import moneymgr.model.QPrice;
import moneymgr.model.Security;
import moneymgr.model.Security.StockSplitInfo;
import moneymgr.model.SecurityPortfolio;
import moneymgr.model.SecurityPosition;
import moneymgr.model.SimpleTxn;
import moneymgr.model.SplitTxn;
import moneymgr.model.Statement;
import moneymgr.model.StockOption;
import moneymgr.model.TxAction;
import moneymgr.model.InvestmentTxn.ShareAction;
import moneymgr.util.Common;
import moneymgr.util.QDate;

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
		StringBuilder sb = new StringBuilder(s);

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
		for (int catid = 1; catid < MoneyMgrModel.currModel.nextCategoryID(); ++catid) {
			while (id++ < catid) {
				wtr.println(sep);
				wtr.print("  [0]");
			}

			Category cat = MoneyMgrModel.currModel.getCategory(catid);

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
				"  [\"acctid\",\"name\",\"accttypeid\",\"desc\",\"closedate\",\"statfreq\",\"statday\",\"bal\",\"clearbal\"]");
		sep = ",";
		List<Account> accts = MoneyMgrModel.currModel.getAccountsById();
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
		List<Security> securities = MoneyMgrModel.currModel.getSecuritiesById();
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
				for (StockSplitInfo split : sec.splits) {
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
		List<Lot> lots = MoneyMgrModel.currModel.getLots();
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
		List<StockOption> opts = MoneyMgrModel.currModel.getStockOptions();
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

		wtr.print("  [\"id\",\"date\",\"statdate\",\"acctid\",\"xtxid\",\"action\"," //
				+ "\"payee\",\"cknum\",\"memo\",\"amt\",\"cat\"," //
				+ "\"secid\",\"secaction\",\"shares\",\"shareprice\",\"splitratio\"," //
				+ "\"optid\",\"[split]\",\"[secxfer]\",\"[lot]\"]");

		final String sep = ",";
		List<GenericTxn> txns = MoneyMgrModel.currModel.getAllTransactions();
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
						encodeString(tx.getAction().key), //
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
		for (Account acct : MoneyMgrModel.currModel.getAccountsById()) {
			// TODO Assign statement id and reference/store accordingly
			if (acct == null) {
				continue;
			}

			for (Statement stmt : acct.getStatements()) {
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

	public void saveJSON() {
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

	public void safeClose(Reader rdr) {
		try {
			if (rdr != null) {
				rdr.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public JSONObject loadJSON() {
		JSONObject obj = null;
		FileReader rdr = null;

		try {
			rdr = new FileReader(this.filename);
			obj = (JSONObject) new JSONParser().parse(rdr);
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		} finally {
			safeClose(rdr);
		}

		return obj;
	}

	// TODO testing
	public void buildModel(String name) {
		MoneyMgrModel model = MoneyMgrModel.changeModel(name);

		JSONObject json = loadJSON();
		if (json == null) {
			return;
		}

		processCategories(model, json);
		processAccounts(model, json);
		processSecurities(model, json);
		processTransactions(model, json);
		processStatements(model, json);

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
	}

	private void processCategories(MoneyMgrModel model, JSONObject json) {
		int CATID = -1;
		int NAME = -1;
		int DESC = -1;
		int EXP = -1;

		boolean first = true;
		JSONArray cats = (JSONArray) json.get("Categories");

		for (Object catobj : cats) {
			JSONArray tuple = (JSONArray) catobj;

			if (first) {
				for (int ii = 0; ii < tuple.size(); ++ii) {
					String s = (String) tuple.get(ii);
					if (s.equalsIgnoreCase("id")) {
						CATID = ii;
					} else if (s.equalsIgnoreCase("name")) {
						NAME = ii;
					} else if (s.equalsIgnoreCase("desc")) {
						DESC = ii;
					} else if (s.equalsIgnoreCase("isexpense")) {
						EXP = ii;
					}
				}

				first = false;
			} else {
				int catid = ((Long) tuple.get(CATID)).intValue();
				String name = decodeString((String) tuple.get(NAME));
				String desc = decodeString((String) tuple.get(DESC));
				boolean isExp = ((Boolean) tuple.get(EXP)).booleanValue();

				Category cat = new Category(catid, name, desc, isExp);
				model.addCategory(cat);
			}
		}
	}

	private void processAccounts(MoneyMgrModel model, JSONObject json) {
		int ACCTID = -1;
		int NAME = -1;
		int ACCTTYPEID = -1;
		int DESC = -1;
		int CLOSEDATE = -1;
		int STATFREQ = -1;
		int STATDAY = -1;
//		int BAL = -1;
//		int CLEARBAL = -1;

		boolean first = true;
		JSONArray accts = (JSONArray) json.get("Accounts");

		for (Object acctobj : accts) {
			JSONArray tuple = (JSONArray) acctobj;
			if (first) {
				for (int ii = 0; ii < tuple.size(); ++ii) {
					String s = (String) tuple.get(ii);

					// "acctid","name","accttypeid","desc","closedate","statfreq","statday","bal","clearbal"
					if (s.equalsIgnoreCase("acctid")) {
						ACCTID = ii;
					} else if (s.equalsIgnoreCase("accttypeid")) {
						ACCTTYPEID = ii;
					} else if (s.equalsIgnoreCase("name")) {
						NAME = ii;
					} else if (s.equalsIgnoreCase("desc")) {
						DESC = ii;
					} else if (s.equalsIgnoreCase("closedate")) {
						CLOSEDATE = ii;
					} else if (s.equalsIgnoreCase("statfreq")) {
						STATFREQ = ii;
					} else if (s.equalsIgnoreCase("statday")) {
						STATDAY = ii;
//					} else if (s.equalsIgnoreCase("bal")) {
//						BAL = ii;
//					} else if (s.equalsIgnoreCase("clearbal")) {
//						CLEARBAL = ii;
					}
				}

				first = false;
			} else {
				int acctid = ((Long) tuple.get(ACCTID)).intValue();
				int typeid = ((Long) tuple.get(ACCTTYPEID)).intValue();
				String name = decodeString((String) tuple.get(NAME));
				String desc = decodeString((String) tuple.get(DESC));
				int statfreq = ((Long) tuple.get(STATFREQ)).intValue();
				int statday = ((Long) tuple.get(STATDAY)).intValue();
				// TODO BigDecimal bal = new BigDecimal((String) tuple.get(BAL));
				// TODO BigDecimal clearbal = new BigDecimal((String) tuple.get(CLEARBAL));

				int rawdate = ((Long) tuple.get(CLOSEDATE)).intValue();
				QDate closedate = QDate.fromRawData(rawdate);

				AccountType atype = AccountType.byId(typeid);

				Account acct = new Account(acctid, name, desc, atype, statfreq, statday);
				acct.closeDate = closedate;

				model.addAccount(acct);
			}
		}
	}

	private void processSecurities(MoneyMgrModel model, JSONObject json) {
		int SECID = -1;
		int SYMBOL = -1;
		int NAMES = -1;
		int TYPE = -1;
		int SPLITS = -1;
		int PRICES = -1;

		boolean first = true;
		JSONArray secs = (JSONArray) json.get("Securities");

		for (Object secobj : secs) {
			JSONArray tuple = (JSONArray) secobj;

			if (first) {
				for (int ii = 0; ii < tuple.size(); ++ii) {
					String s = (String) tuple.get(ii);

					if (s.equalsIgnoreCase("secid")) {
						SECID = ii;
					} else if (s.equalsIgnoreCase("symbol")) {
						SYMBOL = ii;
					} else if (s.equalsIgnoreCase("[name]")) {
						NAMES = ii;
					} else if (s.equalsIgnoreCase("type")) {
						TYPE = ii;
					} else if (s.equalsIgnoreCase("[split]")) {
						SPLITS = ii;
					} else if (s.equalsIgnoreCase("[[date,price]]")) {
						PRICES = ii;
					}
				}

				first = false;
			} else {
				int secid = ((Long) tuple.get(SECID)).intValue();
				String symbol = decodeString((String) tuple.get(SYMBOL));
				String type = decodeString((String) tuple.get(TYPE));

				JSONArray jnames = (JSONArray) tuple.get(NAMES);
				List<String> names = new ArrayList<String>();
				for (Object jname : jnames) {
					String name = (String) jname;
					names.add(name);
				}

				String name = names.get(0);
				String goal = "";

				Security sec = new Security(symbol, name, type, goal);

				JSONArray jprices = (JSONArray) tuple.get(PRICES);
				List<QPrice> prices = new ArrayList<QPrice>();
				for (Object jprice : jprices) {
					JSONArray jpriceinfo = (JSONArray) jprice;

					int rawdate = ((Long) jpriceinfo.get(0)).intValue();
					QDate date = QDate.fromRawData(rawdate);
					BigDecimal price = new BigDecimal((String) jpriceinfo.get(1));

					// TODO need more price details? (e.g. split adjusted price)
					QPrice qprice = new QPrice(date, secid, price, price);
					prices.add(qprice);
					sec.addPrice(qprice);
				}

				// TODO what to do with splits here?
				JSONArray jsplits = (JSONArray) tuple.get(SPLITS);
				List<Object> splits = new ArrayList<Object>();
				for (Object jsplit : jsplits) {
					JSONArray jsplitinfo = (JSONArray) jsplit;

					int rawdate = ((Long) jsplitinfo.get(0)).intValue();
					QDate date = QDate.fromRawData(rawdate);
					BigDecimal ratio = new BigDecimal((String) jsplitinfo.get(1));

					splits.add(jsplit);
				}

				model.addSecurity(sec);
			}
		}
	}

	private void processTransactions(MoneyMgrModel model, JSONObject json) {
		int TXID = -1;
		int DATE = -1;
		int STATDATE = -1;
		int ACCTID = -1;
		int XTXID = -1;
		int ACTION = -1;
		int PAYEE = -1;
		int CKNUM = -1;
		int MEMO = -1;
		int AMT = -1;
		int CAT = -1;
		int SECID = -1;
		int SECACTION = -1;
		int SHARES = -1;
		int SHAREPRICE = -1;
		int SPLITRATIO = -1;
		int OPTID = -1;
		int SPLITS = -1;
		int SECXFERS = -1;
		int LOTS = -1;

		boolean first = true;
		JSONArray secs = (JSONArray) json.get("Transactions");

		for (Object secobj : secs) {
			JSONArray tuple = (JSONArray) secobj;

			if (first) {
				for (int ii = 0; ii < tuple.size(); ++ii) {
					String s = (String) tuple.get(ii);

					if (s.equalsIgnoreCase("id")) {
						TXID = ii;
					} else if (s.equalsIgnoreCase("date")) {
						DATE = ii;
					} else if (s.equalsIgnoreCase("statdate")) {
						STATDATE = ii;
					} else if (s.equalsIgnoreCase("acctid")) {
						ACCTID = ii;
					} else if (s.equalsIgnoreCase("xtxid")) {
						XTXID = ii;
					} else if (s.equalsIgnoreCase("action")) {
						ACTION = ii;
					} else if (s.equalsIgnoreCase("payee")) {
						PAYEE = ii;
					} else if (s.equalsIgnoreCase("cknum")) {
						CKNUM = ii;
					} else if (s.equalsIgnoreCase("memo")) {
						MEMO = ii;
					} else if (s.equalsIgnoreCase("amt")) {
						AMT = ii;
					} else if (s.equalsIgnoreCase("cat")) {
						CAT = ii;
					} else if (s.equalsIgnoreCase("secid")) {
						SECID = ii;
					} else if (s.equalsIgnoreCase("secaction")) {
						SECACTION = ii;
					} else if (s.equalsIgnoreCase("shares")) {
						SHARES = ii;
					} else if (s.equalsIgnoreCase("shareprice")) {
						SHAREPRICE = ii;
					} else if (s.equalsIgnoreCase("splitratio")) {
						SPLITRATIO = ii;
					} else if (s.equalsIgnoreCase("optid")) {
						OPTID = ii;
					} else if (s.equalsIgnoreCase("[split]")) {
						SPLITS = ii;
					} else if (s.equalsIgnoreCase("[secxfer]")) {
						SECXFERS = ii;
					} else if (s.equalsIgnoreCase("[lot]")) {
						LOTS = ii;
					}
				}

				first = false;
			} else {
				int txid = ((Long) tuple.get(TXID)).intValue();
				if (txid <= 0) {
					continue;
				}

				int acctid = ((Long) tuple.get(ACCTID)).intValue();

				GenericTxn tx;
				NonInvestmentTxn ntx = null;
				InvestmentTxn itx = null;

				Account acct = MoneyMgrModel.currModel.getAccountByID(acctid);
				if (acct.isInvestmentAccount()) {
					itx = new InvestmentTxn(txid, acctid);
					tx = itx;
				} else {
					ntx = new NonInvestmentTxn(txid, acctid);
					tx = ntx;
				}

				QDate date = QDate.fromRawData(((Long) tuple.get(DATE)).intValue());
				TxAction action = TxAction.parseAction((String) tuple.get(ACTION));
				BigDecimal amt = new BigDecimal((String) tuple.get(AMT));
				String payee = decodeString((String) tuple.get(PAYEE));
				String memo = (String) tuple.get(MEMO);
				int cknum = ((Long) tuple.get(CKNUM)).intValue(); // decodeString((String) tuple.get(CKNUM));
				QDate stmtdate = QDate.fromRawData(((Long) tuple.get(STATDATE)).intValue());

				int xtxid = ((Long) tuple.get(XTXID)).intValue();
				int catid = ((Long) tuple.get(CAT)).intValue();

				int secid = ((Long) tuple.get(SECID)).intValue();
				ShareAction secaction = ShareAction.parseAction((String) tuple.get(SECACTION));
				BigDecimal shares = new BigDecimal((String) tuple.get(SHARES));
				BigDecimal shareprice = new BigDecimal((String) tuple.get(SHAREPRICE));
				BigDecimal splitratio = new BigDecimal((String) tuple.get(SPLITRATIO));
				int optid = ((Long) tuple.get(OPTID)).intValue();

				tx.setDate(date);
				tx.setAction(action);
				tx.setCatid(catid);
				tx.setAmount(amt);
				tx.setPayee(payee);
				tx.setMemo(memo);
				tx.setCheckNumber(Integer.toString(cknum));
				tx.stmtdate = stmtdate;

				JSONArray splits = ((JSONArray) tuple.get(SPLITS));

				if (secid > 0) {
					itx.setSecurity(MoneyMgrModel.currModel.getSecurity(secid));

					JSONArray secxfers = ((JSONArray) tuple.get(SECXFERS));
					JSONArray lots = ((JSONArray) tuple.get(LOTS));
				}
			}
		}
	}

	private void processStatements(MoneyMgrModel model, JSONObject json) {
		int ACCTID = -1;
		int DATE = -1;
		int ISBAL = -1;
		int PREVDATE = -1;
		int TOTBAL = -1;
		int CASHBAL = -1;
		int TXNS = -1;
		int HOLDINGS = -1;

		boolean first = true;
		JSONArray secs = (JSONArray) json.get("Statements");

		for (Object secobj : secs) {
			JSONArray tuple = (JSONArray) secobj;

			if (first) {
				for (int ii = 0; ii < tuple.size(); ++ii) {
					String s = (String) tuple.get(ii);

					if (s.equalsIgnoreCase("acctid")) {
						ACCTID = ii;
					} else if (s.equalsIgnoreCase("date")) {
						DATE = ii;
					} else if (s.equalsIgnoreCase("isbal")) {
						ISBAL = ii;
					} else if (s.equalsIgnoreCase("prevdate")) {
						PREVDATE = ii;
					} else if (s.equalsIgnoreCase("totbal")) {
						TOTBAL = ii;
					} else if (s.equalsIgnoreCase("cashbal")) {
						CASHBAL = ii;
					} else if (s.equalsIgnoreCase("txns")) {
						TXNS = ii;
					} else if (s.equalsIgnoreCase("holdings")) {
						HOLDINGS = ii;
					}

					first = false;
				}
			} else {
				int acctid = ((Long) tuple.get(ACCTID)).intValue();
				QDate date = QDate.fromRawData(((Long) tuple.get(DATE)).intValue());
				QDate prevdate = QDate.fromRawData(((Long) tuple.get(PREVDATE)).intValue());
				BigDecimal totbal = new BigDecimal((String) tuple.get(TOTBAL));
				BigDecimal cashbal = new BigDecimal((String) tuple.get(CASHBAL));
				boolean isbal = ((Boolean) tuple.get(ISBAL)).booleanValue();

				Account acct = MoneyMgrModel.currModel.getAccountByID(acctid);
				Statement prevstmt = (prevdate != null) ? acct.getStatement(prevdate) : null;

				Statement stmt = new Statement(acctid, date, totbal, cashbal, prevstmt);
				acct.addStatement(stmt);

				stmt.isBalanced = isbal;

				JSONArray txnids = (JSONArray) tuple.get(TXNS);
				for (Object txnidobj : txnids) {
					int txid = ((Long) txnidobj).intValue();
					GenericTxn tx = MoneyMgrModel.currModel.getTransaction(txid);

					stmt.addTransaction(tx);
				}

				JSONArray holdings = (JSONArray) tuple.get(HOLDINGS);
			}
		}
	}
}
