package moneymgr.io.mm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
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

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import moneymgr.io.qif.TransactionCleaner;
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
import moneymgr.util.Common;
import moneymgr.util.QDate;

/*
 Splits
 9/16/92 olde options
 6/14/93 olde espp options
 6/26/95 olde espp options
 6/27/95 w&r
 6/13/03 etrade options smithbarney tdiraEtrade
 */
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
			default:
				if ((ch < (char) 32) || (ch > (char) 127)) {
					sub = String.format("&%x;", (int) ch);
				}
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
		return encodeAmount(amt);
	}

	private static String encodeAmount(BigDecimal amt) {
		if (amt == null) {
			amt = BigDecimal.ZERO;
		}

		return encodeString(amt.toString());
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
				default: {
					int endidx = idx;
					while ((endidx < sb.length()) && (sb.charAt(endidx) != ';')) {
						++endidx;
					}
					String v = sb.substring(idx, endidx);
					sb.replace(idx, endidx, "");
					sub = String.format("%c", Integer.parseInt(v, 16));
				}
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
	private MoneyMgrModel model;
	private JSONObject jsonModel;

	public Persistence() {
	}

	private void saveCategories_gson() {
		try {
			JsonWriter writer = new JsonWriter(new FileWriter("/tmp/cat.json"));
			writer.beginObject();
			writer.name("Categories");
			writer.beginArray();

			for (int catid = 1; catid < this.model.nextCategoryID(); ++catid) {
				Category cat = this.model.getCategory(catid);

				writer.beginObject();
				writer.name("id").value(cat.catid);
				writer.name("name").value(cat.name);
				writer.name("desc").value(cat.description);
				writer.name("isexpense").value(cat.isExpense);
				writer.endObject();
			}

			writer.endArray();
			writer.endObject();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void saveCategories() {
		saveCategories_gson();

		// ------------------------------------------------
		wtr.println("");
		wtr.println("\"Categories\": [");
		// ------------------------------------------------

		wtr.print("  [\"id\",\"name\",\"desc\",\"isExpense\"]");
		String sep = ",";
		int id = 1;
		for (int catid = 1; catid < this.model.nextCategoryID(); ++catid) {
			while (id++ < catid) {
				wtr.println(sep);
				wtr.print("  [0]");
			}

			Category cat = this.model.getCategory(catid);

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
		wtr.println("]");
	}

	private void saveBasicInfo() {
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
		wtr.println("]");
	}

	private void saveAccounts() {
		String sep = "";

		// ------------------------------------------------
		wtr.println("");
		wtr.println("\"Accounts\": [");
		// ------------------------------------------------

		wtr.print(
				"  [\"acctid\",\"name\",\"accttypeid\",\"desc\",\"closedate\",\"statfreq\",\"statday\",\"bal\",\"clearbal\"]");
		sep = ",";
		List<Account> accts = this.model.getAccountsById();
		for (int acctid = 1; acctid < accts.size(); ++acctid) {
			Account ac = accts.get(acctid);
			String line;

			if (ac == null) {
				line = "  [0]";
			} else {
				int close = (ac.getCloseDate() != null) ? ac.getCloseDate().getRawValue() : 0;

				line = String.format("  [%d,%s,%d,%s,%d,%d,%d,%s,%s]", //
						ac.acctid, //
						encodeString(ac.name), //
						ac.type.id, //
						// ac.acctCategory.label, //
						encodeString(ac.description), //
						close, //
						ac.getStatementFrequency(), //
						ac.getStatementDay(), //
						encodeAmount(ac.getBalance()), //
						encodeAmount(ac.getClearedBalance()));

				// TODO ac.securities
			}

			wtr.println(sep);
			wtr.print(line);
		}

		wtr.println();
		wtr.println("]");
	}

	void saveSecurities() {
		// ------------------------------------------------
		wtr.println("");
		wtr.println("\"Securities\": [");
		// ------------------------------------------------

		wtr.print("  [\"secid\",\"symbol\",\"[name]\",\"type\",\"goal\",\"[txn]\",\"[split]\",\"[[date,price]]\"]");

		final String sep = ",";
		List<Security> securities = this.model.getSecuritiesById();
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
					txns += String.format("%d", txn.getTxid());
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

					prices += String.format("[%d,%s,%s]", //
							price.date.getRawValue(), //
							encodeAmount3(price.getPrice()), //
							encodeAmount3(price.getSplitAdjustedPrice()));
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
		wtr.println("]");
	}

	void saveLots() {
		// ------------------------------------------------
		wtr.println("");
		wtr.println("\"Lots\": [");
		// ------------------------------------------------

		wtr.print("  [\"lotid\",\"date\",\"acctid\",\"secid\",\"shares\","
				+ "\"basisprice\",\"createTxid\",\"disposeTxid\"," //
				+ "\"srcLotid\",\"[childLotid]\"]");

		final String sep = ",";
		List<Lot> lots = this.model.getLots();
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
						encodeAmount3(lot.shares), //
						encodeAmount3(lot.basisPrice), //
						lot.createTransaction.getTxid(), //
						((lot.getDisposingTransaction() != null) ? lot.getDisposingTransaction().getTxid() : 0), //
						((lot.getSourceLot() != null) ? lot.getSourceLot().lotid : 0), //
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
				+ "  \"cost\",\"origmarketvalue\",\"lifetimemonths\",\"vestfreq\",\"vestcount\",\"txid\",\"canceldate\",\"srcoptid\"" //
				+ "]");

		final String sep = ",";
		List<StockOption> opts = this.model.getStockOptions();
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
						((opt.transaction != null) ? opt.transaction.getTxid() : 0), //
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
				+ "\"optid\",\"[split]\",\"[secxfer]\",\"[lot]\"," //
				+ "\"xacct\",\"xfercash\",\"commission\"" //
				+ "]");

		final String sep = ",";
		List<SimpleTxn> txns = this.model.getAllTransactions();
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
						splits += String.format("[%d,[", split.getTxid());

						String sep2 = "";
						for (SplitTxn ssplit : ((MultiSplitTxn) split).getSplits()) {
							splits += sep2;
							splits += String.format("%d", ssplit.getTxid());
							sep2 = ",";
						}

						splits += "]]";
					} else {
						splits += String.format("%d", split.getTxid());
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

				int sdate = (tx.isCleared()) //
						? tx.getStatementDate().getRawValue() //
						: 0;

				validateTransfers(tx, errcount);

				int secid = tx.getSecurityId();
				String shareaction = "";
				BigDecimal shares = BigDecimal.ZERO;
				BigDecimal shareprice = BigDecimal.ZERO;
				BigDecimal splitratio = BigDecimal.ONE;
				BigDecimal xfercash = BigDecimal.ZERO;
				BigDecimal commission = BigDecimal.ZERO;

				String xacctName = "";
				String securityTransferTxns = "[]";
				String lots = "[]";
				int optid = 0;

				if (tx instanceof InvestmentTxn) {
					InvestmentTxn itx = (InvestmentTxn) tx;

					xacctName = itx.getAccountForTransfer();
					// TODO encode share action
					shareaction = itx.getShareAction().toString();
					shares = itx.getQuantity();
					shareprice = itx.getPrice();
					splitratio = itx.getSplitRatio();
					xfercash = itx.getCashTransferred();
					commission = itx.getCommission();

					securityTransferTxns = "[";
					String sep2 = "";
					for (InvestmentTxn xtx : itx.getSecurityTransferTxns()) {
						securityTransferTxns += sep2 + xtx.getTxid();
						sep2 = ",";
					}
					securityTransferTxns += "]";

					lots = "[";

					lots += "[";
					sep2 = "";
					for (Lot lot : itx.getLots()) {
						lots += sep2 + lot.lotid;
						sep2 = ",";
					}

					lots += "],[";

					sep2 = "";
					for (Lot lot : itx.getCreatedLots()) {
						lots += sep2 + lot.lotid;
						sep2 = ",";
					}

					lots += "],[";

					sep2 = "";
					for (Lot lot : itx.getDisposedLots()) {
						lots += sep2 + lot.lotid;
						sep2 = ",";
					}
					lots += "]";

					lots += "]";

					if (itx.isStockOptionTxn() && (itx.getOption() != null)) {
						optid = itx.getOption().optid;
					}
				}

				line = String.format("  [%d,%d,%d,%d,%d,%s,%s,%d,%s,%s,%d,%d,%s,%s,%s,%s,%d", //
						tx.getTxid(), //
						tx.getDate().getRawValue(), //
						sdate, //
						tx.getAccountID(), //
						((tx.getCashTransferTxn() != null) ? tx.getCashTransferTxn().getTxid() : 0), //
						encodeString(tx.getAction().key), //
						encodeString(tx.getPayee()), //
						tx.getCheckNumber(), //
						encodeString(tx.getMemo()), //
						encodeAmount(tx.getAmount()), //
						tx.getCatid(), // TODO catid or splits, not both

						secid, //
						encodeString(shareaction), //
						encodeAmount3(shares), //
						encodeAmount(shareprice), //
						encodeAmount(splitratio), //
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

				line += sep;
				line += "\n    ";
				line += String.format("%s,%s,%s", //
						encodeString(xacctName), //
						encodeAmount(xfercash), //
						encodeAmount(commission));

				line += "]";
			}

			wtr.println(sep);
			wtr.print(line);
		}

		wtr.println();
		wtr.println("]");
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
		for (Account acct : this.model.getAccountsById()) {
			// TODO Assign statement id and reference/store accordingly
			if (acct == null) {
				continue;
			}

			for (Statement stmt : acct.getStatements()) {
				String txns = "[";
				String sep2 = "";
				for (GenericTxn tx : stmt.transactions) {
					txns += sep2 + tx.getTxid();
					sep2 = ",";
				}
				txns += "]";

				String prevdate = (stmt.prevStatement != null) //
						? Integer.toString(stmt.prevStatement.date.getRawValue()) //
						: "0";

				SecurityPortfolio h = stmt.holdings;

				String holdings = "[";
				sep2 = "";
				for (SecurityPosition p : h.getPositions()) {
					holdings += sep2 + String.format("[%d,%s,%s]", //
							p.security.secid, //
							encodeAmount3(p.getEndingShares()), //
							encodeAmount(p.getEndingValue()));
					sep2 = ",";
				}
				holdings += "]";

				line = String.format("  [%d,%d,%s,%s,%s,%s,\n    %s,\n    %s]", //
						stmt.acctid, //
						stmt.date.getRawValue(), //
						stmt.isBalanced(), //
						prevdate, //
						encodeAmount(stmt.closingBalance), //
						encodeAmount(stmt.getCashBalance()), //
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
		this.model = model;

		try {
			this.wtr = new PrintStream(filename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		wtr.println("{");
		saveCategories();
		wtr.println(",");
		saveBasicInfo();
		wtr.println(",");
		saveAccounts();
		wtr.println(",");
		saveSecurities();
		wtr.println(",");
		saveLots();
		wtr.println(",");
		saveOptions();
		wtr.println(",");
		saveTransactions();
		wtr.println(",");
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

	private JSONObject load(File file) {
		JSONObject obj = null;
		FileReader rdr = null;

		try {
			if (file.canRead()) {
				rdr = new FileReader(file);
				obj = (JSONObject) new JSONParser().parse(rdr);
			}
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		} finally {
			safeClose(rdr);
		}

		return obj;
	}

	public void loadMulti(File file) {
		try {
			Object o = loadMultiJSON(file);

			JSONObject jobj = load(file);
			if (jobj != null) {
				System.out.println("Multi object file:");
				System.out.println(jobj.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void loadMulti(String filename) {
	}

	public MoneyMgrModel loadJSON(String modelName, String filename) {
		File multi = new File("/tmp/multi.json");
		if (multi.canRead()) {
			loadMulti(multi);
		}

		this.model = MoneyMgrModel.changeModel(modelName);

		File file = new File(filename);

		this.jsonModel = load(file);
		if (this.jsonModel == null) {
			return null;
		}

		processCategories();
		processAccounts();
		processSecurities();
		processTransactions();
		processLots();
		processOptions();
		processSecurityTransactions();
		processStatements();

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

		TransactionCleaner.cleanUpTransactionsFromJSON();

		return this.model;
	}

	private void processCategories() {
		int CATID = -1;
		int NAME = -1;
		int DESC = -1;
		int EXP = -1;

		boolean first = true;
		JSONArray cats = (JSONArray) this.jsonModel.get("Categories");

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
				this.model.addCategory(cat);
			}
		}
	}

	private void processAccounts() {
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
		JSONArray accts = (JSONArray) this.jsonModel.get("Accounts");

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
				acct.setCloseDate(closedate);

				this.model.addAccount(acct);
			}
		}
	}

	private void processSecurityTransactions() {
		int SECID = -1;
		int TXNS = -1;
		boolean first = true;

		JSONArray secs = (JSONArray) this.jsonModel.get("Securities");

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
				Security sec = this.model.getSecurity(secid);

				JSONArray jtxns = (JSONArray) tuple.get(TXNS);
				for (Object txidobj : jtxns) {
					int txid = ((Long) txidobj).intValue();
					SimpleTxn txn = this.model.getTransaction(txid);

					sec.addTransaction((InvestmentTxn) txn);
				}
			}
		}

		for (SimpleTxn tx : this.model.getAllTransactions()) {
			if (tx instanceof GenericTxn) {
				GenericTxn gtx = (GenericTxn) tx;

				this.model.portfolio.addTransaction(gtx);
				tx.getAccount().securities.addTransaction(gtx);
			}
		}
	}

	private void processSecurities() {
		int SECID = -1;
		int SYMBOL = -1;
		int NAMES = -1;
		int TYPE = -1;
		int GOAL = -1;
		int SPLITS = -1;
		int PRICES = -1;
		int TXNS = -1;
		// TODO security txns

		boolean first = true;
		JSONArray secs = (JSONArray) this.jsonModel.get("Securities");

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
					BigDecimal saprice = new BigDecimal((String) jpriceinfo.get(2));

					QPrice qprice = new QPrice(date, secid, price, saprice);
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

				// TODO process security transactions
				Object txns = tuple.get(TXNS);

				this.model.addSecurity(sec);
			}
		}
	}

	// TODO fix this
	int SHARES = -1;
	int SHAREPRICE = -1;
	int SPLITRATIO = -1;
	int OPTID = -1;
	int SECXFERS = -1;
	int LOTS = -1;
	int XACCT = -1;
	int XFERCASH = -1;
	int COMMISSION = -1;

	private int[][] getTransactionLotids(JSONObject json, int txid) {
		int[][] ret = new int[3][];

		JSONArray txns = (JSONArray) json.get("Transactions");
		JSONArray txn = (JSONArray) txns.get(txid);
		JSONArray txnlotids = (JSONArray) txn.get(LOTS);

		for (int idx = 0; idx < ret.length; ++idx) {
			JSONArray lotids = (JSONArray) txnlotids.get(idx);

			int ids[] = new int[lotids.size()];
			ret[idx] = ids;

			for (int ii = 0; ii < lotids.size(); ++ii) {
				ids[ii] = ((Long) lotids.get(ii)).intValue();
			}
		}

		return ret;
	}

	private void processTransactions() {
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
		JSONArray txns = (JSONArray) this.jsonModel.get("Transactions");

		for (Object txnobj : txns) {
			JSONArray tuple = (JSONArray) txnobj;

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
					} else if (s.equalsIgnoreCase("xacct")) {
						XACCT = ii;
					} else if (s.equalsIgnoreCase("xfercash")) {
						XFERCASH = ii;
					} else if (s.equalsIgnoreCase("commission")) {
						COMMISSION = ii;
					}
				}

				first = false;
			} else {
				int txid = ((Long) tuple.get(TXID)).intValue();
				if (txid <= 0) {
					continue;
				}

				int acctid = ((Long) tuple.get(ACCTID)).intValue();

				Account acct = this.model.getAccountByID(acctid);
				SimpleTxn stx = this.model.getSimpleTransaction(txid);
				GenericTxn gtx = null;

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
						SimpleTxn xtxn = this.model.getSimpleTransaction(xtxid);
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

						SimpleTxn simptxn = this.model.getSimpleTransaction(splitid);
						if (simptxn instanceof SplitTxn) {
							stxn = (SplitTxn) simptxn;
						} else {
							// TODO this is a reference to a tx that is not loaded yet
							// Fill in the details later
							stxn = new SplitTxn(splitid, stx);
							this.model.addTransaction(stxn);
						}

					} else {
						JSONArray split = (JSONArray) splitobj;

						int splitid = ((Long) split.get(0)).intValue();

						MultiSplitTxn mstxn = new MultiSplitTxn(splitid, stx);
						this.model.addTransaction(mstxn);
						stxn = mstxn;

						for (Object subsplitobj : (JSONArray) split.get(1)) {
							int ssplitid = ((Long) subsplitobj).intValue();

							SplitTxn sstxn;
							SimpleTxn simptxn = this.model.getSimpleTransaction(ssplitid);
							if (simptxn instanceof SplitTxn) {
								sstxn = (SplitTxn) simptxn;
							} else {
								// TODO this is a reference to a tx that is not loaded yet
								// Fill in the details later
								sstxn = new SplitTxn(ssplitid, mstxn);
								this.model.addTransaction(sstxn);
							}

							mstxn.addSplit(sstxn);
						}
					}

					stx.addSplit(stxn);
				}

				if (secid > 0) {
					InvestmentTxn itx = (InvestmentTxn) gtx;

					handleTransactionSecurity(itx, tuple, secid);
				}

				if (gtx != null) {
					acct.addTransaction(gtx);
				}
			}
		}
	}

	private void handleTransactionSecurity(InvestmentTxn itx, JSONArray tuple, int secid) {
		Security sec = this.model.getSecurity(secid);
		itx.setSecurity(sec);

		BigDecimal shares = new BigDecimal((String) tuple.get(SHARES));
		BigDecimal shareprice = new BigDecimal((String) tuple.get(SHAREPRICE));
		BigDecimal splitratio = new BigDecimal((String) tuple.get(SPLITRATIO));

		itx.setAccountForTransfer(decodeString((String) tuple.get(XACCT)));
		itx.setCashTransferred(new BigDecimal((String) tuple.get(XFERCASH)));
		itx.setCommission(new BigDecimal((String) tuple.get(COMMISSION)));

		// TODO option doesn't exist yet?
		int optid = ((Long) tuple.get(OPTID)).intValue();

		itx.setPrice(shareprice);

		if (itx.getAction() == TxAction.STOCKSPLIT) {
			itx.setQuantity(splitratio.multiply(BigDecimal.TEN));
		} else {
			itx.setQuantity(shares);
		}

		JSONArray secxfers = ((JSONArray) tuple.get(SECXFERS));
		for (Object xferobj : secxfers) {
			int xferid = ((Long) xferobj).intValue();

			InvestmentTxn txn = (InvestmentTxn) this.model.getTransaction(xferid);
			if (txn != null) {
				itx.addSecurityTransferTxn(txn);
			}
		}

		JSONArray lots = ((JSONArray) tuple.get(LOTS));
		// TODO there are no lots yet
	}

	private void processLots() {
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
		JSONArray lots = (JSONArray) this.jsonModel.get("Lots");

		for (Object lotobj : lots) {
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
						? (InvestmentTxn) this.model.getTransaction(ctid) //
						: null;
				int dtid = ((Long) tuple.get(DTID)).intValue();
				InvestmentTxn disposingTxn = (dtid > 0) //
						? (InvestmentTxn) this.model.getTransaction(dtid) //
						: null;

				int srcLotId = ((Long) tuple.get(SRCLOTID)).intValue();
				Lot srcLot = (srcLotId > 0) ? this.model.getLot(srcLotId) : null;

				int nextLotid = this.model.nextLotId();
				JSONArray childids = (JSONArray) tuple.get(CHILDLOTIDS);
				for (Object jchildid : childids) {
					int childid = ((Long) jchildid).intValue();

					Lot childlot = this.model.getLot(childid);
					// lot.childLots.add(childlot);
				}

				Security sec = this.model.getSecurity(secid);

				Lot lot = new Lot(lotid, createDate, acctid, secid, //
						shares, basisPrice, createTxn, disposingTxn, srcLot);

				sec.addLot(lot);
			}
		}

		for (SimpleTxn txn : this.model.getAllTransactions()) {
			if (!(txn instanceof InvestmentTxn)) {
				continue;
			}

			InvestmentTxn itxn = (InvestmentTxn) txn;

			int[][] txlotids = getTransactionLotids(this.jsonModel, txn.getTxid());

			for (int idx = 0; idx < txlotids.length; ++idx) {
				int[] lotids = txlotids[idx];

				for (int lotid : lotids) {
					Lot lot = this.model.getLot(lotid);

					switch (idx) {
					case 0:
						itxn.addLot(lot);
						break;
					case 1:
						itxn.addCreatedLot(lot);
						break;
					case 2:
						itxn.addDisposedLot(lot);
						break;
					}
				}
			}
		}
	}

	private void processOptions() {
		int OPTID = -1;
		int NAME = -1;
		int DATE = -1;
		int ACCTID = -1;
		int SECID = -1;
		int SHARES = -1;
		int STRIKEPRICE = -1;
		int MARKETPRICE = -1;
		int COST = -1;
		int ORIGMARKETVALUE = -1;
		int LIFETIMEMONTHS = -1;
		int VESTFREQ = -1;
		int VESTCOUNT = -1;
		int TXID = -1;
		int CANCELDATE = -1;
		int SRCOPTID = -1;

		boolean first = true;
		JSONArray opts = (JSONArray) this.jsonModel.get("Options");

		for (Object optobj : opts) {
			JSONArray tuple = (JSONArray) optobj;

			if (first) {
				for (int ii = 0; ii < tuple.size(); ++ii) {
					String s = (String) tuple.get(ii);

					if (s.equalsIgnoreCase("optid")) {
						OPTID = ii;
					} else if (s.equalsIgnoreCase("name")) {
						NAME = ii;
					} else if (s.equalsIgnoreCase("date")) {
						DATE = ii;
					} else if (s.equalsIgnoreCase("acctid")) {
						ACCTID = ii;
					} else if (s.equalsIgnoreCase("secid")) {
						SECID = ii;
					} else if (s.equalsIgnoreCase("shares")) {
						SHARES = ii;
					} else if (s.equalsIgnoreCase("strikeprice")) {
						STRIKEPRICE = ii;
					} else if (s.equalsIgnoreCase("marketprice")) {
						MARKETPRICE = ii;
					} else if (s.equalsIgnoreCase("cost")) {
						COST = ii;
					} else if (s.equalsIgnoreCase("origmarketvalue")) {
						ORIGMARKETVALUE = ii;
					} else if (s.equalsIgnoreCase("lifetimemonths")) {
						LIFETIMEMONTHS = ii;
					} else if (s.equalsIgnoreCase("vestfreq")) {
						VESTFREQ = ii;
					} else if (s.equalsIgnoreCase("vestcount")) {
						VESTCOUNT = ii;
					} else if (s.equalsIgnoreCase("txid")) {
						TXID = ii;
					} else if (s.equalsIgnoreCase("canceldate")) {
						CANCELDATE = ii;
					} else if (s.equalsIgnoreCase("srcoptid")) {
						SRCOPTID = ii;
					}
				}

				first = false;
			} else {
				int optid = ((Long) tuple.get(OPTID)).intValue();
				int acctid = ((Long) tuple.get(ACCTID)).intValue();
				int secid = ((Long) tuple.get(SECID)).intValue();
				int srcoptid = ((Long) tuple.get(SRCOPTID)).intValue();
				String name = (String) tuple.get(NAME);
				QDate date = QDate.fromRawData(((Long) tuple.get(DATE)).intValue());
				QDate canceldate = QDate.fromRawData(((Long) tuple.get(CANCELDATE)).intValue());

				int lifetimemonths = ((Long) tuple.get(LIFETIMEMONTHS)).intValue();
				int vestfreq = ((Long) tuple.get(VESTFREQ)).intValue();
				int vestcount = ((Long) tuple.get(VESTCOUNT)).intValue();
				BigDecimal shares = new BigDecimal((String) tuple.get(SHARES));
				BigDecimal strikeprice = new BigDecimal((String) tuple.get(STRIKEPRICE));
				BigDecimal marketprice = new BigDecimal((String) tuple.get(MARKETPRICE));
				BigDecimal cost = new BigDecimal((String) tuple.get(COST));
				BigDecimal origmarketvalue = new BigDecimal((String) tuple.get(ORIGMARKETVALUE));

				int txid = ((Long) tuple.get(TXID)).intValue();

				StockOption srcopt = null;
				if (srcoptid > 0) {
					srcopt = this.model.getStockOption(srcoptid);
				}

				StockOption opt = new StockOption( //
						srcopt, optid, name, date, acctid, secid, shares, //
						strikeprice, cost, marketprice, origmarketvalue, //
						lifetimemonths, vestfreq, vestcount);

				opt.cancelDate = canceldate;

				if (txid > 0) {
					opt.transaction = (InvestmentTxn) this.model.getTransaction(txid);
					opt.transaction.setOption(opt);
				}

				this.model.addStockOption(opt);
			}
		}
	}

	private void processStatements() {
		int ACCTID = -1;
		int DATE = -1;
		int ISBAL = -1;
		int PREVDATE = -1;
		int TOTBAL = -1;
		int CASHBAL = -1;
		int TXNS = -1;
		int HOLDINGS = -1;

		boolean first = true;
		JSONArray stats = (JSONArray) this.jsonModel.get("Statements");

		for (Object statobj : stats) {
			JSONArray tuple = (JSONArray) statobj;

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

				Account acct = this.model.getAccountByID(acctid);
				Statement prevstmt = (prevdate != null) ? acct.getStatement(prevdate) : null;

				Statement stmt = new Statement(acctid, date, totbal, cashbal, prevstmt);
				acct.addStatement(stmt);

				stmt.setIsBalanced(isbal);

				JSONArray txnids = (JSONArray) tuple.get(TXNS);
				for (Object txnidobj : txnids) {
					int txid = ((Long) txnidobj).intValue();
					GenericTxn tx = this.model.getTransaction(txid);

					stmt.addTransaction(tx);
				}

				JSONArray holdings = (JSONArray) tuple.get(HOLDINGS);
				for (Object hold_obj : holdings) {
					JSONArray hold = (JSONArray) hold_obj;

					int secid = ((Long) hold.get(0)).intValue();
					BigDecimal qty = new BigDecimal((String) hold.get(1));
					BigDecimal value = new BigDecimal((String) hold.get(2));

					SecurityPosition pos = stmt.holdings.getPosition(secid);
					pos.setExpectedEndingShares(qty);
					pos.setEndingValue(value);
				}
			}
		}
	}

	static class ANOTHERPOJO {
		int id;
		String name;
	}

	static class YOURPOJO {
		String id;
		List<ANOTHERPOJO> children;

		public void setId(String id) {
			this.id = id;
		}

		public void setChildren(List<ANOTHERPOJO> children) {
			this.children = children;
		}
	}

	private List<YOURPOJO> loadMultiJSON(File file) throws IOException {
		Gson gson = new Gson();
		JsonReader jsonReader = null;
		try {
			jsonReader = new JsonReader(new FileReader(file));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		// Handle formatting, e.g. extra spaces
		jsonReader.setLenient(true);

		boolean start = true;
		jsonReader.beginObject();

		List<YOURPOJO> completeList = new ArrayList<YOURPOJO>();

		while (jsonReader.hasNext()) {
			if (!start) {
				if (jsonReader.peek().toString().matches("END_DOCUMENT")) {
					break;
				}

				jsonReader.beginObject();
			}

			start = false;

			YOURPOJO pojo = new YOURPOJO();

			String id = jsonReader.nextName();
			pojo.setId(id);

			List<ANOTHERPOJO> tempList = new ArrayList<ANOTHERPOJO>();

			jsonReader.beginArray();
			while (jsonReader.hasNext()) {
				ANOTHERPOJO t = gson.fromJson(jsonReader, ANOTHERPOJO.class);
				tempList.add(t);
			}

			jsonReader.endArray();

			pojo.setChildren(tempList);

			completeList.add(pojo);

			jsonReader.endObject();
		}

		jsonReader.close();
		return completeList;
	}
}
