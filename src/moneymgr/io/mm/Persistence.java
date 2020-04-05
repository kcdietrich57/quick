package moneymgr.io.mm;

import java.io.FileNotFoundException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.util.List;

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
import moneymgr.model.SimpleTxn;
import moneymgr.model.SplitTxn;
import moneymgr.model.TxAction;
import moneymgr.util.Common;

/** TODO JSON format; Read/write data in native format */
public class Persistence {
	private static String encodeString(String s) {
		if (s == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder(s);

		int idx = 0;
		String sub = null;

		while (idx < sb.length()) {
			char ch = sb.charAt(idx);

			switch (ch) {
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

		return sb.toString();
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

	private String filename;
	private LineNumberReader rdr;
	private PrintStream wtr;

	public Persistence(String filename) {
		this.filename = filename;
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
	}

	private void saveCategories() {
		wtr.println("Categories:[");

		for (int catid = 1; catid < Category.getNextCategoryID(); ++catid) {
			Category cat = Category.getCategory(catid);

			if (cat != null) {
				String line = String.format("%d,%s,%s,%s;", //
						cat.catid, //
						encodeString(cat.name), //
						encodeString(cat.description), //
						cat.isExpense);
				wtr.println(line);
			}
		}

		wtr.println("],");
	}

	private void saveAccounts() {
		wtr.println("AccountTypes:[");
		for (AccountType at : AccountType.values()) {
			String line = String.format("%d,%s,%s,%s,%s;", //
					at.id, //
					encodeString(at.name), //
					at.isAsset, //
					at.isInvestment, //
					at.isCash);
			wtr.println(line);
		}
		wtr.println("],");

		wtr.println("AccountCategories:[");
		for (AccountCategory ac : AccountCategory.values()) {
			String line = String.format("%s,%s,[", //
					encodeString(ac.label), //
					ac.isAsset);

			String sep = "";
			for (AccountType at : ac.accountTypes) {
				line += String.format("%s%s", sep, at.id);
				sep = ",";
			}
			line += "]";

			wtr.println(line);
		}
		wtr.println("],");

		wtr.println("Accounts:[");
		for (Account ac : Account.getAccounts()) {
			int close = (ac.closeDate != null) ? ac.closeDate.getRawValue() : 0;

			String line = String.format("%s,%d,%s,%d,%d,%d,%s,%s;", //
					ac.name, //
					ac.type.id, //
					// ac.acctCategory.label, //
					encodeString(ac.description), //
					close, //
					ac.statementFrequency, //
					ac.statementDayOfMonth, //
					Common.formatAmount(ac.balance).trim(), //
					Common.formatAmount(ac.clearedBalance).trim());

			// ac.transactions, //
			// ac.statements, //
			// ac.securities //

			wtr.println(line);
		}
		wtr.println("],");
	}

	void saveSecurities() {
		wtr.println("Securities:[");
		for (Security sec : Security.getSecurities()) {
			String secNames = "[";
			String sep = "";
			for (String name : sec.names) {
				secNames += String.format("%s%s", sep, encodeString(name));
				sep = ",";
			}
			secNames += "]";

			String line = String.format("%d,%s,%s,%s;", //
					sec.secid, //
					encodeString(sec.symbol), //
					secNames, //
					sec.type);
			wtr.println();
			wtr.println(line);

			wtr.print("[");
			for (SplitInfo split : sec.splits) {
				// TODO probably wrong format for ration
				line = String.format("%d,%f;", //
						split.splitDate.getRawValue(), //
						split.splitRatio);
				wtr.print(line);
			}

			wtr.println("],[");

			int count = 0;
			for (QPrice price : sec.prices) {
				line = String.format("%d,%s;", //
						price.date.getRawValue(), //
						Common.formatAmount3(price).trim());

				wtr.print(line);
				if (++count == 8) {
					count = 0;
					wtr.println();
				}
			}

			if (count > 0) {
				wtr.println();
			}

			wtr.println("],");
		}
		wtr.println("],");
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

	void saveTransactions() {
		int errcount[] = { 0, 0 };

		wtr.println("Transactions:[");
		for (GenericTxn tx : GenericTxn.getAllTransactions()) {
			if (tx == null) {
				continue;
			}

			String splits = "[";
			for (SplitTxn split : tx.getSplits()) {
				if (split instanceof MultiSplitTxn) {
					splits += "[";
					for (SplitTxn ssplit : ((MultiSplitTxn) split).subsplits) {
						splits += String.format("%d,%s,%s;", //
								ssplit.getCatid(), //
								encodeString(ssplit.getMemo()), //
								Common.formatAmount(ssplit.getAmount()).trim() //
						);
					}
					splits += "]";
				} else {
					splits += String.format("%d,%s,%s;", //
							split.getCatid(), //
							encodeString(split.getMemo()), //
							Common.formatAmount(split.getAmount()).trim() //
					);
				}
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

			String line = String.format("%d,%d,%d,%d,%d,%s,%s,%d,%s,%s,%d,%s;", //
					tx.txid, //
					tx.getDate().getRawValue(), //
					sdate, //
					tx.getAccountID(), //
					((tx.getCashTransferTxn() != null) ? tx.getCashTransferTxn().txid : -1), //
					tx.getAction().name(), //
					encodeString(tx.getPayee()), //
					tx.getCheckNumber(), //
					encodeString(tx.getMemo()), //
					Common.formatAmount(tx.getAmount()).trim(), //
					tx.getCatid(), // TODO catid or splits, not both
					splits);
			wtr.println(line);
		}
		wtr.println("],");

	}

	void saveStatements() {
		wtr.println("Statements:[");
		wtr.println("],");
	}
}
