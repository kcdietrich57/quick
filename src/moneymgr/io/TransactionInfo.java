package moneymgr.io;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import moneymgr.model.Account;
import moneymgr.model.Category;
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.Security;
import moneymgr.model.SimpleTxn;
import moneymgr.model.TxAction;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/**
 * TransactionInfo is a generic tuple-based representation of transactions.<br>
 * It may be created from Quicken Windows QIF export files or Mac CSV files, or
 * other formats.
 * 
 * TODO Modify QIF to use TxInfo as intermediate representation<br>
 * QIF - QIF->(TxInfo,Tx,JSON)<br>
 * CSV - (experimental) CSV->TxInfo->Tx<br>
 * JSON - (experimental) JSON->TxInfo->Tx
 */
public class TransactionInfo {
	/**
	 * The csv file specifies the names of the values at each position.<br>
	 * We parse that to determine the column index of each field, stored in these
	 * variables.<br>
	 * TODO Using a map would probably be more straightforward.
	 */
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

	public static int XACCOUNT_IDX = -1;
	public static int XAMOUNT_IDX = -1;
	public static int PRICE_IDX = -1;
	public static int COMMISSION_IDX = -1;

	/** List of field names for the input CSV file */
	public static String[] fieldNames = null;

	/** Divvy up transaction data by account */
	public static Map<Account, List<TransactionInfo>> winTransactionInfoByAccount = new HashMap<>();

	/**
	 * Add a transaction to the list for its account<br>
	 * This is used to capture information about transactions created by QIF import,
	 * which we can compare to MAC CSV import during testing.<br>
	 * Currently, WIN import creates both transaction object and info and calls this
	 * to save it.<br>
	 */
	public static void addWinInfo(TransactionInfo info, SimpleTxn txn) {
		info.processValues();

		Account acct = info.account;

		if (acct != null) {
			List<TransactionInfo> infos = winTransactionInfoByAccount.get(acct);
			if (infos == null) {
				infos = new ArrayList<TransactionInfo>();
				winTransactionInfoByAccount.put(acct, infos);
			}

//			SimpleTxn newtxn = info.createTransaction(acct);
//			info.winTxn = newtxn;

			infos.add(info);
		}
	}

	/** Look up the column index of a field by its name */
	private static int getFieldIndex(String fieldname) {
		for (int idx = 0; idx < fieldNames.length; ++idx) {
			if (fieldNames[idx].equals(fieldname)) {
				return idx;
			}
		}

		return -1;
	}

	/** Default field names by position (e.g. for QIF import) */
	private static final String[] dfltFieldNames = { //
			"Account", "Date", "Action", "Type", //
			"Payee", "Amount", "Split", "Category", //
			"Check #", "Memo/Notes", "Description/Category", //
			"Security", "Comm/Fee", "Shares", "Shares In", "Shares Out", //
			"Inflow", "Outflow", //
			"Transfer Account", "Transfer Amount", "Price", "Commission" //
	};

	static {
		setFieldOrder(dfltFieldNames);
	}

	/** Assign the indexes of each field based on the field names array */
	public static void setFieldOrder(String[] fieldnames) {
		fieldNames = (fieldnames != null) ? fieldnames : dfltFieldNames;

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
		XACCOUNT_IDX = getFieldIndex("Transfer Account");
		XAMOUNT_IDX = getFieldIndex("Transfer Amount");
		PRICE_IDX = getFieldIndex("Price");
		COMMISSION_IDX = getFieldIndex("Commission");
	}

	public boolean isInvestmentTransaction;

	public String[] values;
	public List<TransactionInfo> splits = new ArrayList<>();

	public boolean isSplit;

	public Account account;
	public Account xaccount;
	public QDate date;
	public TxAction action;
	public String type;
	public BigDecimal amount;
	public BigDecimal xamount;
	public String payee;
	public Category category;
	public String memo;
	public String description;
	public String cknum;
	public Security security;
	public BigDecimal shares;
	public BigDecimal price;
	public BigDecimal commission;
	public BigDecimal fees;
	public BigDecimal sharesIn;
	public BigDecimal sharesOut;
	public BigDecimal inflow;
	public BigDecimal outflow;

	// TODO do we have both of these for comparison purposes when testing import?
	public SimpleTxn macTxn = null;
	public SimpleTxn winTxn = null;

	public final List<SimpleTxn> winTxnMatches = new ArrayList<>();
	public String inexactMessages = "";
	public String multipleMessages = "";
	public String actionMessages = "";
	public boolean datemismatch = false;
	public boolean fixaction = false;

	/** Constructor - empty transaction info */
	public TransactionInfo(Account acct) {
		this.isInvestmentTransaction = acct.isInvestmentAccount();

		this.values = new String[fieldNames.length];

		for (int ii = 0; ii < fieldNames.length; ++ii) {
			this.values[ii] = "";
		}

		setValue(ACCOUNT_IDX, acct.name);
	}

	/** Constructor - with values - e.g. from CSV import */
	public TransactionInfo(List<String> values) {
		this.isInvestmentTransaction = false;

		while (values.size() < TransactionInfo.fieldNames.length) {
			values.add("");
		}

		this.values = values.toArray(new String[0]);
	}

	/** Return a value as integer, or zero if not parsable as integer */
	public int intValue(int idx) {
		String s = value(idx);

		if (!s.isEmpty()) {
			try {
				return Integer.parseInt(s);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return 0;
	}

	/** Return a value as decimal, or null if not parsable as decimal */
	public BigDecimal decimalValue(int idx) {
		String s = value(idx);

		if (!s.isEmpty()) {
			try {
				return Common.getDecimal(s);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	/** Extract values from raw info and set member variables accordingly */
	public void processValues() {
		try {
			this.account = MoneyMgrModel.currModel.findAccount(value(ACCOUNT_IDX));
			if (this.account == null) {
				Common.reportWarning("Can't find account '" + value(ACCOUNT_IDX) + "'");
			}
			this.date = Common.parseQDate(value(DATE_IDX));
			this.cknum = value(CHECKNUM_IDX);
			this.payee = value(PAYEE_IDX);
			int catid = Common.parseCategory(value(CATEGORY_IDX));
			if (catid > 0) {
				this.category = MoneyMgrModel.currModel.getCategory(catid);
				this.xaccount = null;
			} else {
				this.category = null;
				this.xaccount = MoneyMgrModel.currModel.getAccountByID(-catid);
			}
			this.amount = decimalValue(AMOUNT_IDX);
			this.memo = value(MEMO_IDX);
			this.description = value(DESCRIPTION_IDX);
			this.type = value(TYPE_IDX);
			this.security = MoneyMgrModel.currModel.findSecurity(value(SECURITY_IDX));
			this.fees = decimalValue(FEES_IDX);
			this.shares = decimalValue(SHARES_IDX);
			this.action = TxAction.parseAction(value(ACTION_IDX));
			this.sharesIn = decimalValue(SHARESIN_IDX);
			this.sharesOut = decimalValue(SHARESOUT_IDX);
			this.inflow = decimalValue(INFLOW_IDX);
			this.outflow = decimalValue(OUTFLOW_IDX);
			if (this.xaccount == null) {
				this.xaccount = MoneyMgrModel.currModel.findAccount(value(XACCOUNT_IDX));
			}
			this.xamount = decimalValue(XAMOUNT_IDX);
			this.price = Common.parsePrice(value(PRICE_IDX));
			this.commission = decimalValue(COMMISSION_IDX);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * TODO UNUSED see CVSImport.createTransaction(TransactionInfo)<br>
	 * Create a transaction object in a given account using the contained info
	 */
	public SimpleTxn createTransaction(Account acct) {
//		processValues();

		SimpleTxn txn = null;
//
//		if (!this.isInvestmentTransaction) {
//			NonInvestmentTxn nitxn = new NonInvestmentTxn(acct.acctid);
//			txn = nitxn;
//		} else {
//			InvestmentTxn itxn = new InvestmentTxn(acct.acctid);
//			txn = itxn;
//
//			itxn.setQuantity(this.shares);
//			itxn.price = this.price;
//			itxn.accountForTransfer = this.xaccount.name;
//			itxn.cashTransferred = this.xamount;
//			itxn.commission = this.commission;
//			itxn.setSecurity(this.security);
//		}
//
//		txn.setDate(this.date);
//		txn.setAction(this.action);
//		txn.setAmount(this.amount);
//		txn.setCheckNumber(this.cknum);
//		txn.setMemo(this.memo);
//		if (this.xaccount != null) {
//			txn.setCatid(-this.xaccount.acctid);
//			txn.setCashTransferAcctid(this.xaccount.acctid);
//		} else {
//			txn.setCatid(this.category.catid);
//		}

		return txn;
	}

	/** Return the value of a field by index */
	public String value(int idx) {
		return ((idx >= 0) && (idx < this.values.length)) ? this.values[idx] : "";
	}

	/** Set the value of a field by name */
	public void setValue(String fieldname, String value) {
		setValue(getFieldIndex(fieldname), value);
	}

	/** Set the value of a field by index */
	public void setValue(int idx, String value) {
		if (idx >= 0 && idx < fieldNames.length) {
			this.values[idx] = value;
		}
	}

	/**
	 * Create a new split line and set its category value. Note that this is the
	 * only way to create split lines and must be done before setting other values
	 * for the split line.
	 */
	public void addSplitCategory(String cat) {
		TransactionInfo splitinfo = new TransactionInfo(MoneyMgrModel.currModel.findAccount(value(ACCOUNT_IDX)));
		splitinfo.setValue(TransactionInfo.SPLIT_IDX, "S");
		splitinfo.setValue(TransactionInfo.CATEGORY_IDX, cat);
		this.splits.add(splitinfo);
	}

	/** Set the amount in the last split line */
	public void addSplitAmount(String amount) {
		if (this.splits.isEmpty()) {
			Common.reportError("Error adding split amount to TransactionInfo");
		}

		TransactionInfo splitinfo = this.splits.get(this.splits.size() - 1);
		splitinfo.setValue(TransactionInfo.AMOUNT_IDX, amount);
	}

	/** Set the memo in the last split line */
	public void addSplitMemo(String memo) {
		if (this.splits.isEmpty()) {
			Common.reportError("Error adding split memo to TransactionInfo");
		}

		TransactionInfo splitinfo = this.splits.get(this.splits.size() - 1);
		splitinfo.setValue(TransactionInfo.MEMO_IDX, memo);
	}

	/** Get the transaction date */
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

	/** Append an inexact match message */
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

	/** Append a message to the multi-messages string */
	public void addMultipleMessage(String msg) {
		this.multipleMessages += msg + "\n";

//			System.out.println(msg);
//			SimpleTxn mactxn = this.macTxn;
//			List<SimpleTxn> txns = this.winTxnMatches;
//			SimpleTxn other = this.winTxnMatches.get(0);
//			this.macTxn.compareWith(this, other);
	}

	/** Append a message to the messages string */
	public void addActionMessage(String msg) {
		this.actionMessages += msg + "\n";
	}

	private String getIndent() {
		return (value(SPLIT_IDX).equals("S")) ? "  " : "";
	}

	public String toString() {
		String ret = "";
		String indent = getIndent();
		boolean first = true;

		for (int ii = 0; ii < fieldNames.length; ++ii) {
			String val = value(ii);
			if (!val.isEmpty()) {
				if (!first) {
					ret += "\n";
				} else {
					first = false;
				}

				ret += indent + fieldNames[ii] + ":" + val;
			}
		}

		for (TransactionInfo sinfo : this.splits) {
			ret += "\n" + sinfo.toString();
		}

		return ret;

//		"" //
//				+ value(DATE_IDX) //
//				+ "\n   acct:" + value(ACCOUNT_IDX) + ":" //
//				+ "\n   ty:" + value(TYPE_IDX) + ":" //
//				+ "\n   actn:" + value(ACTION_IDX) + ":" //
//				+ "\n   pay:" + value(PAYEE_IDX) + ":" //
//				+ "\n   amt:" + value(AMOUNT_IDX) + ":" //
//				+ "\n   spl:" + value(SPLIT_IDX) + ":" //
//				+ "\n   cat:" + value(CATEGORY_IDX) + ":" //
//				+ "\n   fee:" + value(FEES_IDX) + ":" //
//				+ "\n   ck:" + value(CHECKNUM_IDX) + ":" //
//				+ "\n   mem:" + value(MEMO_IDX) + ":" //
//				+ "\n   dsc:" + value(DESCRIPTION_IDX) + ":" //
//				+ "\n   infl:" + value(INFLOW_IDX) + ":" //
//				+ "\n   outfl:" + value(OUTFLOW_IDX) + ":" //
//				+ "\n   sec:" + value(SECURITY_IDX) + ":" //
//				+ "\n   shr:" + value(SHARES_IDX) + ":" //
//				+ "\n   shin:" + value(SHARESIN_IDX) + ":" //
//				+ "\n   shout:" + value(SHARESOUT_IDX) + ":" //
//		;
	}
}