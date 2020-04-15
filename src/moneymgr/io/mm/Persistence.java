package moneymgr.io.mm;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
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
import moneymgr.model.MultiSplitTxn;
import moneymgr.model.QPrice;
import moneymgr.model.Security;
import moneymgr.model.Security.SplitInfo;
import moneymgr.model.SecurityPortfolio;
import moneymgr.model.SecurityPosition;
import moneymgr.model.SimpleTxn;
import moneymgr.model.SplitTxn;
import moneymgr.model.Statement;
import moneymgr.model.TxAction;
import moneymgr.util.Common;

/** TODO JSON format; Read/write data in native format */
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
		wtr.println("");
		wtr.print("\"Categories\": [");

		String sep = "";
		for (int catid = 0; catid < Category.getNextCategoryID(); ++catid) {
			Category cat = Category.getCategory(catid);

			String line;
			if (cat == null) {
				line = "[0,\"\",\"\",\"\"]";
			} else {
				line = String.format("  [%d,%s,%s,%s]", //
						cat.catid, //
						encodeString(cat.name), //
						encodeString(cat.description), //
						cat.isExpense);
			}

			wtr.println(sep);
			wtr.print(line);
			sep = ",";
		}

		wtr.println();
		wtr.println("],");
	}

	private void saveAccounts() {
		String sep = "";

		wtr.println("");
		wtr.print("\"AccountTypes\": [");

		String MISSING = "  [0]";
		int id = 1;
		sep = "";
		for (AccountType at : AccountType.values()) {
			while (id++ < at.id) {
				wtr.println(sep);
				wtr.print(MISSING);
				sep = ",";
			}

			String line = String.format("  [%d,%s,%s,%s,%s]", //
					at.id, //
					encodeString(at.name), //
					at.isAsset, //
					at.isInvestment, //
					at.isCash);

			wtr.println(sep);
			wtr.print(line);
			sep = ",";
		}

		wtr.println();
		wtr.println("],");

		wtr.println("");
		wtr.print("\"AccountCategories\": [");

		sep = "";
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
			sep = ",";
		}

		wtr.println();
		wtr.println("],");

		wtr.println("");
		wtr.print("\"Accounts\": [");

		sep = "";
		List<Account> accts = Account.getAccountsById();
		for (int acctid = 0; acctid < accts.size(); ++acctid) {
			Account ac = accts.get(acctid);

			if (ac == null) {
				wtr.println(sep);
				wtr.print("[0]");
				sep = ",";

				continue;
			}

			int close = (ac.closeDate != null) ? ac.closeDate.getRawValue() : 0;

			while (id++ < ac.acctid) {
				wtr.print(sep);
				wtr.println(MISSING);
				sep = ",";
			}

			String line = String.format("  [%d,%s,%d,%s,%d,%d,%d,%s,%s]", //
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

			wtr.println(sep);
			wtr.print(line);
			sep = ",";
		}

		wtr.println();
		wtr.println("],");
	}

	void saveSecurities() {
		wtr.println("");
		wtr.print("\"Securities\": [");

		String sep = "";
		List<Security> securities = Security.getSecuritiesById();
		for (int secid = 0; secid < securities.size(); ++secid) {
			Security sec = securities.get(secid);

			if (sec == null) {
				wtr.println(sep);
				wtr.print("[0]");
				sep = ",";

				continue;
			}

			String secNames = "[";
			String sep2 = "";
			for (String name : sec.names) {
				secNames += sep2;
				secNames += String.format("%s", encodeString(name));
				sep2 = ",";
			}
			secNames += "]";

			String line = String.format("  [%d,%s,%s,%s,", //
					sec.secid, //
					encodeString(sec.symbol), //
					secNames, //
					encodeString(sec.type));
			wtr.println(sep);
			wtr.println(line);

			wtr.print("    [");
			sep2 = "";
			for (SplitInfo split : sec.splits) {
				// TODO probably wrong format for ratio
				line = String.format("[%d,%f]", //
						split.splitDate.getRawValue(), //
						split.splitRatio);

				wtr.print(sep2);
				wtr.print(line);
				sep2 = ",";
			}
			wtr.println("],");

			wtr.println("    [");
			int count = 0;
			sep2 = "";
			line = "      ";
			for (QPrice price : sec.prices) {
				line += sep2;

				if (count++ == 4) {
					count = 1;
					wtr.println(line);
					line = "      ";
				}

				line += String.format("[%d,%s]", //
						price.date.getRawValue(), //
						Common.formatAmount3(price).trim());

				sep2 = ",";
			}

			if (count > 0) {
				wtr.println(line);
			}
			wtr.println("    ]");

			wtr.print("  ]");
		}

		wtr.println();
		wtr.println("],");
	}

	void saveTransactions() {
		int errcount[] = { 0, 0 };

		wtr.println("");
		wtr.print("\"Transactions\": [");

		String sep = "";
		for (GenericTxn tx : GenericTxn.getAllTransactions()) {
			if (tx == null) {
				wtr.println(sep);
				wtr.print("[0]");
				sep = ",";

				continue;
			}

			String splits = "[";

			String sep1 = "";
			for (SplitTxn split : tx.getSplits()) {
				splits += sep1;

				if (split instanceof MultiSplitTxn) {
					splits += "[";

					String sep2 = "";
					for (SplitTxn ssplit : ((MultiSplitTxn) split).subsplits) {
						splits += sep2;
						splits += String.format("[%d,%s,%s]", //
								ssplit.getCatid(), //
								encodeString(ssplit.getMemo()), //
								Common.formatAmount(ssplit.getAmount()).trim());
						sep2 = ",";
					}

					splits += "]";
				} else {
					splits += String.format("[%d,%s,%s]", //
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

			// TODO investment transactions/stocksplit

			String line = String.format("  [%d,%d,%d,%d,%d,%s,%s,%d,%s,%s,%d,", //
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
					tx.getCatid()); // TODO catid or splits, not both

			if (splits.length() > 2) {
				line += "\n    ";
			}

			line += splits + "]";

			wtr.println(sep);
			wtr.print(line);
			sep = ",";
		}

		wtr.println();
		wtr.println("],");
	}

	void saveStatements() {
		wtr.println("");
		wtr.print("\"Statements\": [");

		String sep = "";
		for (Account acct : Account.getAccounts()) {
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
					holdings += sep2 + String.format("[%d,%s,%s]", //
							p.security.secid, //
							Common.formatAmount3(p.getEndingShares()).trim(), //
							Common.formatAmount(p.endingValue).trim());
					sep2 = ",";
				}
				holdings += "]";

				String line = String.format("  [%d,%d,%s,%s,%s,%s,\n    %s,\n    %s]", //
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
				sep = ",";
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
		saveTransactions();
		saveStatements();
		wtr.println("}");

		wtr.close();

		// TODO testing
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

		return jo;
	}
}
