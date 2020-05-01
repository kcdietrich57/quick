package moneymgr.io.mm;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import moneymgr.io.qif.TransactionCleaner;
import moneymgr.model.Account;
import moneymgr.model.AccountCategory;
import moneymgr.model.AccountType;
import moneymgr.model.Category;
import moneymgr.model.GenericTxn;
import moneymgr.model.InvestmentTxn;
import moneymgr.model.InvestmentTxn.ShareAction;
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

	public static void validateTransfers(SimpleTxn tx, int[] counts) {
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

		if (tx instanceof SplitTxn) {
			SplitTxn stxn = (SplitTxn) tx;

			if (!stxn.getParent().getSplits().contains(tx)) {
				err = "Broken split/parent link";
			}
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

	private PrintStream wtr;

	public Persistence() {
	}

	private void saveCategories(MoneyMgrModel model) {
		// ------------------------------------------------
		wtr.println("");
		wtr.println("\"Categories\": [");
		// ------------------------------------------------

		wtr.print("  [\"id\",\"name\",\"desc\",\"isExpense\"]");
		String sep = ",";
		int id = 1;
		for (int catid = 1; catid < model.nextCategoryID(); ++catid) {
			while (id++ < catid) {
				wtr.println(sep);
				wtr.print("  [0]");
			}

			Category cat = model.getCategory(catid);

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

	private void saveAccounts(MoneyMgrModel model) {
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
		List<Account> accts = model.getAccountsById();
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

	void saveSecurities(MoneyMgrModel model) {
		// ------------------------------------------------
		wtr.println("");
		wtr.println("\"Securities\": [");
		// ------------------------------------------------

		wtr.print("  [\"secid\",\"symbol\",\"[name]\",\"type\",\"goal\",\"[txn]\",\"[split]\",\"[[date,price]]\"]");

		final String sep = ",";
		List<Security> securities = model.getSecuritiesById();
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

				String txns = "[";
				sep2 = "";
				for (InvestmentTxn txn : sec.getTransactions()) {
					txns += sep2;
					txns += String.format("%d", txn.txid);
					sep2 = ",";
				}
				txns += "]";

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

				line = String.format("  [%d,%s,%s,%s,%s,\n    %s,\n    %s,\n    %s\n  ]", //
						sec.secid, //
						encodeString(sec.symbol), //
						secNames, //
						encodeString(sec.type), //
						encodeString(sec.goal), //
						txns, //
						splits, //
						prices);
			}

			wtr.println(sep);
			wtr.print(line);
		}

		wtr.println();
		wtr.println("],");
	}

	void saveLots(MoneyMgrModel model) {
		// ------------------------------------------------
		wtr.println("");
		wtr.println("\"Lots\": [");
		// ------------------------------------------------

		wtr.print("  [\"lotid\",\"date\",\"acctid\",\"secid\",\"shares\","
				+ "\"basisprice\",\"createTxid\",\"disposeTxid\"," //
				+ "\"srcLotid\",\"[childLotid]\"]");

		final String sep = ",";
		List<Lot> lots = model.getLots();
		for (int lotid = 1; lotid < lots.size(); ++lotid) {
			Lot lot = lots.get(lotid);

			String line;
			if (lot == null) {
				line = "  [0]";
			} else {
				String childlots = "[";
				String sep2 = "";
				for (Lot child : lot.getChildLots()) {
					childlots += sep2 + child.lotid;
					sep2 = ",";
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
						((lot.getDisposingTransaction() != null) ? lot.getDisposingTransaction().txid : 0), //
						((lot.getSourceLot() != null) ? lot.getSourceLot().lotid : 0), //
						childlots);
			}

			wtr.println(sep);
			wtr.print(line);
		}

		wtr.println();
		wtr.println("]");
	}

	void saveOptions(MoneyMgrModel model) {
		// ------------------------------------------------
		wtr.println("");
		wtr.println("\"Options\": [");
		// ------------------------------------------------

		wtr.print("  [\"optid\",\"name\",\"date\",\"acctid\",\"secid\",\"shares\",\"strikeprice\",\"marketprice\"," //
				+ "  \"cost\",\"origmarketvalue\",\"lifetimemonths\",\"vestcount\",\"txid\",\"canceldate\",\"srcoptid\"" //
				+ "]");

		final String sep = ",";
		List<StockOption> opts = model.getStockOptions();
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

	void saveTransactions(MoneyMgrModel model) {
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
		List<SimpleTxn> txns = model.getAllTransactions();
		for (int txid = 1; txid < txns.size(); ++txid) {
			SimpleTxn tx = txns.get(txid);

			if (tx == null) {
				line = "  [0]";
			} else {
				String splits = "[";

				String sep1 = "";
				for (SplitTxn split : tx.getSplits()) {
					splits += sep1;

					if (split instanceof MultiSplitTxn) {
						splits += String.format("[%d,[", split.txid);

						String sep2 = "";
						for (SplitTxn ssplit : ((MultiSplitTxn) split).subsplits) {
							splits += sep2;
							splits += String.format("%d", ssplit.txid);
							sep2 = ",";
						}

						splits += "]]";
					} else {
						splits += String.format("%d", split.txid);
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

				int sdate = (tx.getStatementDate() != null) //
						? tx.getStatementDate().getRawValue() //
						: 0;

				validateTransfers(tx, errcount);

				int secid = tx.getSecurityId();
				String shareaction = "";
				BigDecimal shares = BigDecimal.ZERO;
				BigDecimal shareprice = BigDecimal.ZERO;
				BigDecimal splitratio = BigDecimal.ONE;

				String securityTransferTxns = "[]";
				String lots = "[]";
				int optid = 0;

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

					lots = "[";

					String sep3 = "";
					lots += sep3 + "[";
					sep3 = ",";
					sep2 = "";
					for (Lot lot : itx.getLots()) {
						lots += sep2 + lot.lotid;
						sep2 = ",";
					}
					lots += "]";

					sep3 = "";
					lots += sep3 + "[";
					sep3 = ",";
					sep2 = "";
					for (Lot lot : itx.lotsCreated) {
						lots += sep2 + lot.lotid;
						sep2 = ",";
					}
					lots += "]";

					sep3 = "";
					lots += sep3 + "[";
					sep3 = ",";
					sep2 = "";
					for (Lot lot : itx.lotsDisposed) {
						lots += sep2 + lot.lotid;
						sep2 = ",";
					}
					lots += "]";

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

	void saveStatements(MoneyMgrModel model) {
		String line;

		// ------------------------------------------------
		wtr.println("");
		wtr.println("\"Statements\": [");
		// ------------------------------------------------

		wtr.print("  [\"acctid\",\"date\",\"isbal\",\"prevdate\",\"totbal\",\"cashbal\",\"txns\"," //
				+ "\"holdings\"]");

		final String sep = ",";
		for (Account acct : model.getAccountsById()) {
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

	public void saveJSON(MoneyMgrModel model, String filename) {
		try {
			this.wtr = new PrintStream(filename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		wtr.println("{");
		saveCategories(model);
		saveAccounts(model);
		saveSecurities(model);
		saveLots(model);
		saveOptions(model);
		saveTransactions(model);
		saveStatements(model);
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

	private JSONObject load(String filename) {
		JSONObject obj = null;
		FileReader rdr = null;

		try {
			rdr = new FileReader(filename);
			obj = (JSONObject) new JSONParser().parse(rdr);
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		} finally {
			safeClose(rdr);
		}

		return obj;
	}

	public MoneyMgrModel loadJSON(String modelName, String filename) {
		MoneyMgrModel model = MoneyMgrModel.changeModel(modelName);

		JSONObject json = load(filename);
		if (json == null) {
			return null;
		}

		processCategories(model, json);
		processAccounts(model, json);
		processSecurities(model, json);
		processTransactions(model, json);
		processLots(model, json);
		processSecurityTransactions(model, json);
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
		 * Last - replay transactions to populate dynamic info (running totals, etc)
		 * TODO process security transactions/holdings
		 */

		new TransactionCleaner().cleanUpTransactionsFromJSON();

		return model;
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

	private void processSecurityTransactions(MoneyMgrModel model, JSONObject json) {
		int SECID = -1;
		int TXNS = -1;
		boolean first = true;

		JSONArray secs = (JSONArray) json.get("Securities");

		for (Object secobj : secs) {
			JSONArray tuple = (JSONArray) secobj;

			if (first) {
				for (int ii = 0; ii < tuple.size(); ++ii) {
					String s = (String) tuple.get(ii);

					if (s.equalsIgnoreCase("secid")) {
						SECID = ii;
					} else if (s.equalsIgnoreCase("[txn]")) {
						TXNS = ii;
					}
				}

				first = false;
			} else {
				int secid = ((Long) tuple.get(SECID)).intValue();
				Security sec = model.getSecurity(secid);

				JSONArray jtxns = (JSONArray) tuple.get(TXNS);
				for (Object txidobj : jtxns) {
					int txid = ((Long) txidobj).intValue();
					SimpleTxn txn = model.getTransaction(txid);

					sec.addTransaction((InvestmentTxn) txn);
				}
			}
		}
	}

	private void processSecurities(MoneyMgrModel model, JSONObject json) {
		int SECID = -1;
		int SYMBOL = -1;
		int NAMES = -1;
		int TYPE = -1;
		int GOAL = -1;
		int SPLITS = -1;
		int PRICES = -1;
		int TXNS = -1;

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
					} else if (s.equalsIgnoreCase("goal")) {
						GOAL = ii;
					} else if (s.equalsIgnoreCase("[txn]")) {
						TXNS = ii;
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
				String goal = decodeString((String) tuple.get(GOAL));

				JSONArray jnames = (JSONArray) tuple.get(NAMES);
				List<String> names = new ArrayList<String>();
				for (Object jname : jnames) {
					String name = (String) jname;
					names.add(name);
				}

				String name = names.remove(0);

				Security sec = new Security(symbol, name, type, goal);

				sec.names.addAll(names);

				JSONArray jprices = (JSONArray) tuple.get(PRICES);
				List<QPrice> prices = new ArrayList<QPrice>();
				for (Object jprice : jprices) {
					JSONArray jpriceinfo = (JSONArray) jprice;

					int rawdate = ((Long) jpriceinfo.get(0)).intValue();
					QDate date = QDate.fromRawData(rawdate);
					BigDecimal price = new BigDecimal((String) jpriceinfo.get(1));

					// TODO need more price details? (e.g. split adjusted price)
					QPrice qprice = new QPrice(date, secid, price);
					prices.add(qprice);
					sec.addPrice(qprice);
				}

				JSONArray jsplits = (JSONArray) tuple.get(SPLITS);
				for (Object jsplit : jsplits) {
					JSONArray jsplitinfo = (JSONArray) jsplit;

					int rawdate = ((Long) jsplitinfo.get(0)).intValue();
					QDate date = QDate.fromRawData(rawdate);
					BigDecimal ratio = new BigDecimal((String) jsplitinfo.get(1));

					StockSplitInfo split = new StockSplitInfo(date, ratio);
					sec.splits.add(split);
				}

				model.addSecurity(sec);
			}
		}
	}

	// TODO fix this
	int SECACTION = -1;
	int SHARES = -1;
	int SHAREPRICE = -1;
	int SPLITRATIO = -1;
	int OPTID = -1;
	int SECXFERS = -1;
	int LOTS = -1;

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
		int SPLITS = -1;

		Set<Integer> pendingTransfers = new HashSet<Integer>();

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

				GenericTxn gtx = null;

				Account acct = model.getAccountByID(acctid);

				SimpleTxn stx = model.getSimpleTransaction(txid);

				if (stx != null) {
					Common.debugInfo("Filling in split transaction");
				} else if (acct.isInvestmentAccount()) {
					gtx = new InvestmentTxn(txid, acctid);
					stx = gtx;
				} else {
					gtx = new NonInvestmentTxn(txid, acctid);
					stx = gtx;
				}

				QDate date = QDate.fromRawData(((Long) tuple.get(DATE)).intValue());
				TxAction action = TxAction.parseAction((String) tuple.get(ACTION));
				BigDecimal amt = new BigDecimal((String) tuple.get(AMT));
				String payee = decodeString((String) tuple.get(PAYEE));
				String memo = decodeString((String) tuple.get(MEMO));
				int cknum = ((Long) tuple.get(CKNUM)).intValue(); // decodeString((String) tuple.get(CKNUM));
				QDate stmtdate = QDate.fromRawData(((Long) tuple.get(STATDATE)).intValue());

				int xtxid = ((Long) tuple.get(XTXID)).intValue();
				if (xtxid > 0) {
					if (xtxid > txid) {
						pendingTransfers.add(new Integer(txid));
					} else {
						SimpleTxn xtxn = model.getSimpleTransaction(xtxid);
						if (xtxn != null) {
							stx.setCashTransferTxn(xtxn);
							xtxn.setCashTransferTxn(stx);

							pendingTransfers.remove(new Integer(xtxid));
						} else {
							Common.reportWarning( //
									String.format("Can't find xtxn for %d: %d", txid, xtxid));
						}
					}
				}

				int catid = ((Long) tuple.get(CAT)).intValue();
				int secid = ((Long) tuple.get(SECID)).intValue();

				if (!(stx instanceof SplitTxn)) {
					stx.setDate(date);
				}

				stx.setAction(action);
				stx.setCatid(catid);
				stx.setAmount(amt);
				stx.setPayee(payee);
				stx.setMemo(memo);
				stx.setCheckNumber(Integer.toString(cknum));
				stx.setStatementDate(stmtdate);

				JSONArray splits = ((JSONArray) tuple.get(SPLITS));

				int count = splits.size();
				for (Object splitobj : splits) {
					SplitTxn stxn = null;

					if (splitobj instanceof Long) {
						int splitid = ((Long) splitobj).intValue();

						SimpleTxn simptxn = model.getSimpleTransaction(splitid);
						if (simptxn instanceof SplitTxn) {
							stxn = (SplitTxn) simptxn;
						} else {
							// TODO this is a reference to a tx that is not loaded yet
							// Fill in the details later
							stxn = new SplitTxn(splitid, stx);
							model.addTransaction(stxn);
						}

					} else {
						JSONArray split = (JSONArray) splitobj;

						int splitid = ((Long) split.get(0)).intValue();

						MultiSplitTxn mstxn = new MultiSplitTxn(splitid, stx);
						model.addTransaction(mstxn);
						stxn = mstxn;

						for (Object subsplitobj : (JSONArray) split.get(1)) {
							int ssplitid = ((Long) subsplitobj).intValue();

							SplitTxn sstxn;
							SimpleTxn simptxn = model.getSimpleTransaction(ssplitid);
							if (simptxn instanceof SplitTxn) {
								sstxn = (SplitTxn) simptxn;
							} else {
								// TODO this is a reference to a tx that is not loaded yet
								// Fill in the details later
								sstxn = new SplitTxn(ssplitid, mstxn);
								model.addTransaction(sstxn);
							}

							mstxn.addSplit(sstxn);
						}
					}

					stx.addSplit(stxn);
				}

				if (secid > 0) {
					InvestmentTxn itx = (InvestmentTxn) gtx;

					handleTransactionSecurity(model, itx, tuple, secid);
				}

				if (gtx != null) {
					acct.addTransaction(gtx);
				}
			}
		}
	}

	private void handleTransactionSecurity( //
			MoneyMgrModel model, InvestmentTxn itx, JSONArray tuple, int secid) {
		Security sec = model.getSecurity(secid);
		itx.setSecurity(sec);

		// TODO derived - remove
		ShareAction secaction = ShareAction.parseAction((String) tuple.get(SECACTION));
		BigDecimal shares = new BigDecimal((String) tuple.get(SHARES));
		BigDecimal shareprice = new BigDecimal((String) tuple.get(SHAREPRICE));
		BigDecimal splitratio = new BigDecimal((String) tuple.get(SPLITRATIO));
		int optid = ((Long) tuple.get(OPTID)).intValue();

		itx.price = shareprice;

		if (itx.getAction() == TxAction.STOCKSPLIT) {
			itx.setQuantity(splitratio.multiply(BigDecimal.TEN));
		} else {
			itx.setQuantity(shares);
		}

		JSONArray secxfers = ((JSONArray) tuple.get(SECXFERS));
		for (Object xferobj : secxfers) {
			int xferid = ((Long) xferobj).intValue();

			InvestmentTxn txn = (InvestmentTxn) model.getTransaction(xferid);
			if (txn != null) {
				itx.addSecurityTransferTxn(txn);
			}
		}

		JSONArray lots = ((JSONArray) tuple.get(LOTS));
		// TODO there are no lots yet
	}

	private void processLots(MoneyMgrModel model, JSONObject json) {
		int LOTID = -1;
		int DATE = -1;
		int ACCTID = -1;
		int SECID = -1;
		int SHARES = -1;
		int BASIS = -1;
		int CTID = -1;
		int DTID = -1;
		int SRCLOTID = -1;
		int CHILDLOTIDS = -1;

		boolean first = true;
		JSONArray secs = (JSONArray) json.get("Lots");

		for (Object lotobj : secs) {
			JSONArray tuple = (JSONArray) lotobj;

			if (first) {
				for (int ii = 0; ii < tuple.size(); ++ii) {
					String s = (String) tuple.get(ii);

					if (s.equalsIgnoreCase("lotid")) {
						LOTID = ii;
					} else if (s.equalsIgnoreCase("date")) {
						DATE = ii;
					} else if (s.equalsIgnoreCase("acctid")) {
						ACCTID = ii;
					} else if (s.equalsIgnoreCase("secid")) {
						SECID = ii;
					} else if (s.equalsIgnoreCase("shares")) {
						SHARES = ii;
					} else if (s.equalsIgnoreCase("basisprice")) {
						BASIS = ii;
					} else if (s.equalsIgnoreCase("createTxid")) {
						CTID = ii;
					} else if (s.equalsIgnoreCase("disposeTxid")) {
						DTID = ii;
					} else if (s.equalsIgnoreCase("srcLotid")) {
						SRCLOTID = ii;
					} else if (s.equalsIgnoreCase("[childLotid]")) {
						CHILDLOTIDS = ii;
					}
				}

				first = false;
			} else {
				int lotid = ((Long) tuple.get(LOTID)).intValue();
				QDate createDate = QDate.fromRawData(((Long) tuple.get(DATE)).intValue());
				int acctid = ((Long) tuple.get(ACCTID)).intValue();
				int secid = ((Long) tuple.get(SECID)).intValue();
				BigDecimal basisPrice = new BigDecimal((String) tuple.get(BASIS));
				BigDecimal shares = new BigDecimal((String) tuple.get(SHARES));
				// TODO boolean addshares = false;

				int ctid = ((Long) tuple.get(CTID)).intValue();
				InvestmentTxn createTxn = (ctid > 0) //
						? (InvestmentTxn) model.getTransaction(ctid) //
						: null;
				int dtid = ((Long) tuple.get(DTID)).intValue();
				InvestmentTxn disposingTxn = (dtid > 0) //
						? (InvestmentTxn) model.getTransaction(dtid) //
						: null;

				int srcLotId = ((Long) tuple.get(SRCLOTID)).intValue();
				Lot srcLot = (srcLotId > 0) ? model.getLot(srcLotId) : null;

				int nextLotid = model.nextLotId();
				JSONArray childids = (JSONArray) tuple.get(CHILDLOTIDS);
				for (Object jchildid : childids) {
					int childid = ((Long) jchildid).intValue();

					Lot childlot = model.getLot(childid);
					// lot.childLots.add(childlot);
				}

				Security sec = model.getSecurity(secid);

				Lot lot = new Lot(lotid, createDate, acctid, secid, shares, basisPrice, createTxn, disposingTxn, srcLot);

				sec.addLot(lot);
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

				Account acct = model.getAccountByID(acctid);
				Statement prevstmt = (prevdate != null) ? acct.getStatement(prevdate) : null;

				Statement stmt = new Statement(acctid, date, totbal, cashbal, prevstmt);
				acct.addStatement(stmt);

				stmt.isBalanced = isbal;

				JSONArray txnids = (JSONArray) tuple.get(TXNS);
				for (Object txnidobj : txnids) {
					int txid = ((Long) txnidobj).intValue();
					GenericTxn tx = model.getTransaction(txid);

					stmt.addTransaction(tx);
				}

				JSONArray holdings = (JSONArray) tuple.get(HOLDINGS);
			}
		}
	}
}
