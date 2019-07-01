package moneymgr.io;

import java.util.ArrayList;
import java.util.List;

import moneymgr.model.SimpleTxn;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/**
 * TransactionInfo is a generic tuple-based representation of transactions.<br>
 * It may be created from Quicken Windows QIF export files or Mac CSV files.
 */
public class TransactionInfo {
	public static int SPLIT_IDX = -1;
	public static int ACCOUNT_IDX = -1;
	public static int DATE_IDX = -1;
	public static int CHECKNUM_IDX = -1;
	public static int PAYEE_IDX = -1;
	public static int CATEGORY_IDX; // >0: CategoryID; <0 AccountID
	public static int AMOUNT_IDX;
	public static int MEMO_IDX;
	public static int DESCRIPTION_IDX = -1;
	public static int TYPE_IDX = -1;
	public static int SECURITY_IDX = -1;
	public static int FEES_IDX = -1;
	public static int SHARES_IDX = -1;

	public static int ACTION_IDX = -1;
	public static int SHARESIN_IDX = -1;
	public static int SHARESOUT_IDX = -1;
	public static int INFLOW_IDX = -1;
	public static int OUTFLOW_IDX = -1;
	// private static int XACCOUNT_IDX;

	/** List of field names for the input CSV file */
	public static String[] fieldnames = null;

	private static int getFieldIndex(String fieldname) {
		for (int idx = 0; idx < fieldnames.length; ++idx) {
			if (fieldnames[idx].equals(fieldname)) {
				return idx;
			}
		}

		return -1;
	}

	public static void setFieldNames(String[] fieldnames) {
		TransactionInfo.fieldnames = fieldnames;

		SPLIT_IDX = getFieldIndex("Split");
		DATE_IDX = getFieldIndex("Date");
		PAYEE_IDX = getFieldIndex("Payee");
		CATEGORY_IDX = getFieldIndex("Category");
		AMOUNT_IDX = getFieldIndex("Amount");
		ACCOUNT_IDX = getFieldIndex("Account");
		CHECKNUM_IDX = getFieldIndex("Check #");
		MEMO_IDX = getFieldIndex("Memo/Notes");
		DESCRIPTION_IDX = getFieldIndex("Description/Category");
		TYPE_IDX = getFieldIndex("Type");
		SECURITY_IDX = getFieldIndex("Security");
		FEES_IDX = getFieldIndex("Comm/Fee");
		SHARES_IDX = getFieldIndex("Shares");
		ACTION_IDX = getFieldIndex("Action");
		SHARESIN_IDX = getFieldIndex("Shares In");
		SHARESOUT_IDX = getFieldIndex("Shares Out");
		INFLOW_IDX = getFieldIndex("Inflow");
		OUTFLOW_IDX = getFieldIndex("Outflow");
	}

	public String[] values;
	public QDate date = null;
	public SimpleTxn macTxn = null;
	public SimpleTxn winTxn = null;
	public final List<SimpleTxn> winTxnMatches = new ArrayList<SimpleTxn>();
	public String inexactMessages = "";
	public String multipleMessages = "";
	public String actionMessages = "";
	public boolean datemismatch = false;
	public boolean fixaction = false;

	public TransactionInfo(List<String> values) {
		while (values.size() < TransactionInfo.fieldnames.length) {
			values.add("");
		}

		this.values = values.toArray(new String[0]);
	}

	public String value(int idx) {
		return ((idx >= 0) && (idx < this.values.length)) ? this.values[idx] : "";
	}

	public QDate getDate() {
		if (this.date == null) {
			try {
				this.date = Common.parseQDate(value(DATE_IDX));
			} catch (Exception e) {
				this.date = QDate.today();
			}
		}

		return this.date;
	}

	public void addInexactMessage(String msg) {
		this.inexactMessages += msg + "\n";

//			System.out.println(msg);
//			SimpleTxn mactxn = this.macTxn;
//			SimpleTxn other = this.winTxn;
//			if (mactxn.getAccount().name.startsWith("Checking")) {
//				int ii = 0;
//				++ii;
//			}
//			this.macTxn.compareWith(this, other);
	}

	public void addMultipleMessage(String msg) {
		this.multipleMessages += msg + "\n";

//			System.out.println(msg);
//			SimpleTxn mactxn = this.macTxn;
//			List<SimpleTxn> txns = this.winTxnMatches;
//			SimpleTxn other = this.winTxnMatches.get(0);
//			this.macTxn.compareWith(this, other);
	}

	public void addActionMessage(String msg) {
		this.actionMessages += msg + "\n";
	}

	public String toString() {
		return "" //
				+ value(DATE_IDX) //
				+ "\n   acct:" + value(ACCOUNT_IDX) + ":" //
				+ "\n   ty:" + value(TYPE_IDX) + ":" //
				+ "\n   actn:" + value(ACTION_IDX) + ":" //
				+ "\n   pay:" + value(PAYEE_IDX) + ":" //
				+ "\n   amt:" + value(AMOUNT_IDX) + ":" //
				+ "\n   spl:" + value(SPLIT_IDX) + ":" //
				+ "\n   cat:" + value(CATEGORY_IDX) + ":" //
				+ "\n   fee:" + value(FEES_IDX) + ":" //
				+ "\n   ck:" + value(CHECKNUM_IDX) + ":" //
				+ "\n   mem:" + value(MEMO_IDX) + ":" //
				+ "\n   dsc:" + value(DESCRIPTION_IDX) + ":" //
				+ "\n   infl:" + value(INFLOW_IDX) + ":" //
				+ "\n   outfl:" + value(OUTFLOW_IDX) + ":" //
				+ "\n   sec:" + value(SECURITY_IDX) + ":" //
				+ "\n   shr:" + value(SHARES_IDX) + ":" //
				+ "\n   shin:" + value(SHARESIN_IDX) + ":" //
				+ "\n   shout:" + value(SHARESOUT_IDX) + ":" //
		;
	}
}