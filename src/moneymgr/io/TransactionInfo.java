package moneymgr.io;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

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
	public static int REFERENCE_IDX = -1; // TODO bad mac data
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
		info.processValues(null);

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
		REFERENCE_IDX = getFieldIndex("Reference");
		MEMO_IDX = getFieldIndex("Memo/Notes");
		DESCRIPTION_IDX = getFieldIndex("Description/Category");
		TYPE_IDX = getFieldIndex("Type");
		SECURITY_IDX = getFieldIndex("Security");
		COMMISSION_IDX = getFieldIndex("Comm/Fee");
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
	}

	public boolean isInvestmentTransaction;

	public String[] values;

	public TransactionInfo parent = null;
	public List<TransactionInfo> splits = new ArrayList<>();

	public Account account;
	public Account xaccount;
	public QDate date;
	public TxAction action;
	public String type;
	public BigDecimal amount;
	public BigDecimal xamount;
	public String payee;
	public boolean isSplit;
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

	private SimpleTxn macTxn = null;
	private SimpleTxn winTxn = null;

	public SimpleTxn macTxn() {
		return this.macTxn;
	}

	public SimpleTxn winTxn() {
		return this.winTxn;
	}

	public void setMacTransaction(SimpleTxn tx) {
		if (tx != null && this.macTxn != null && this.macTxn != tx) {
			Common.reportWarning("Replacing macTxn in info");
		}

		this.macTxn = tx;
		if (tx != null) {
			tx.info = this;
		}
	}

	public void setWinTransaction(SimpleTxn tx) {
		if (tx != null && this.winTxn != null && this.winTxn != tx) {
			Common.reportWarning("Replacing winTxn in info");
		}

		this.winTxn = tx;
		if (tx != null) {
			tx.info = this;
		}
	}

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

	private Account cloneAccount(MoneyMgrModel sourceModel, String acctName) {
		Account a = sourceModel.findAccount(acctName);
		if (a == null) {
			Common.reportError("Account '" + acctName + "' not found");
		}

		Common.debugInfo( //
				"Cloning source account '" + acctName + "'" + //
						"(" + a.acctid + ")");

		return new Account( //
				a.acctid, acctName, a.description, a.type, //
				a.getStatementFrequency(), a.getStatementDay());
	}

	/** Extract values from raw info and set member variables accordingly */
	public void processValues(MoneyMgrModel sourceModel) {
		try {
			MoneyMgrModel model = MoneyMgrModel.currModel;

			String acctName = value(ACCOUNT_IDX);
			this.account = model.findAccount(acctName);
			if (this.account == null) {
				Common.reportWarning("No account '" + acctName + "'"); // TODO xyzzy
			}

			this.date = getDate();

			if (hasSplits()) {
				BigDecimal totAmount = BigDecimal.ZERO;
				BigDecimal totInflow = BigDecimal.ZERO;
				BigDecimal totOutflow = BigDecimal.ZERO;

				for (TransactionInfo split : this.splits) {
					split.processValues(sourceModel);

					if (split.amount != null) {
						totAmount = totAmount.add(split.amount);
					}
					if (split.inflow != null) {
						totInflow = totInflow.add(split.inflow);
					}
					if (split.outflow != null) {
						totOutflow = totOutflow.add(split.outflow);
					}
				}

				if (this.amount == null) {
					this.amount = totAmount;
				}

				if (this.inflow == null) {
					this.inflow = totInflow;
				}

				if (this.outflow == null) {
					this.outflow = totOutflow;
				}

				return;
			}

			this.cknum = value(CHECKNUM_IDX);
			if (this.cknum == "" && this.account.name.equals("UnionNationalChecking")) {
				// TODO I don't know why the data is different in this case
				this.cknum = value(REFERENCE_IDX);
			}
			this.payee = value(PAYEE_IDX);

			// QIF IntInc CSV "Investments:Interest Income"
			String catstring = value(CATEGORY_IDX);
			if (catstring == null || catstring.isEmpty() //
					|| "Uncategorized".equals(catstring) //
					|| "Investments:Interest Income".equals(catstring) //
					|| "Investments:Dividend Income".equals(catstring) //
					|| "Investments:Dividend Income Tax-Free".equals(catstring) //
					|| "Investments:Reinvest Long-term Capital Gain".equals(catstring) //
					|| "Investments:Reinvest Short-term Capital Gain".equals(catstring) //
					|| "Investments:Add Shares".equals(catstring) //
					|| "Investments:Remove Shares".equals(catstring) //
					|| "Investments:Buy".equals(catstring) //
					|| "Investments:Sell".equals(catstring) //
					|| "Investments:Stock Split".equals(catstring) //
			) {
				this.values[CATEGORY_IDX] = "";
				catstring = "";
			}

			this.isSplit = "S".equals(value(SPLIT_IDX));

			if (catstring != null && !catstring.isEmpty() //
					&& !catstring.equals("Uncategorized")) {
				int catid = Common.parseCategory(catstring);
				if (catid > 0) {
					this.category = model.getCategory(catid);
					this.xaccount = null;
				} else if (catid < 0) {
					this.category = null;
					this.xaccount = model.getAccountByID(-catid);
				} else {
					Common.reportWarning("Creating category from '" + value(CATEGORY_IDX) + "'");
					Common.parseCategory(catstring);
					model.addCategory(new Category(catstring, "mac category", true));
				}
			}

			this.amount = decimalValue(AMOUNT_IDX);
			this.memo = value(MEMO_IDX);
			this.description = value(DESCRIPTION_IDX);
			this.type = value(TYPE_IDX);

			String sname = value(SECURITY_IDX);
			if (sname != null && !sname.isEmpty()) {
				if (sname.equals("Total Bond Market 1")) {
					sname = "Total Bond Market  1";
					setValue(SECURITY_IDX, sname);
				}

				this.security = model.findSecurity(sname);

				if (this.security == null) {
					Common.reportWarning("Creating dummy security for '" + sname + "'");
					this.security = new Security(sname, sname);
					model.addSecurity(this.security);
				}
			}

			this.fees = decimalValue(FEES_IDX);
			this.shares = decimalValue(SHARES_IDX);
			// TODO is this the correct field?
			this.action = TxAction.parseAction(value(TYPE_IDX));
			this.sharesIn = decimalValue(SHARESIN_IDX);
			this.sharesOut = decimalValue(SHARESOUT_IDX);
			this.inflow = decimalValue(INFLOW_IDX);
			this.outflow = decimalValue(OUTFLOW_IDX);

			// TODO work out the kinks in whether amount is positive or negative
			if (this.outflow != null && this.outflow.signum() > 0 //
					&& ( //
					this.type.equals("Buy") //
							|| this.type.equals("WithdrawX") //
					) //
					&& this.amount.signum() < 0) {
				this.amount = this.amount.negate();
			}

			String catOrXfer = value(XACCOUNT_IDX);
			if (catOrXfer != null && !catOrXfer.isEmpty()) {
				if (catOrXfer.startsWith("[")) {
					acctName = catOrXfer.substring(1, catOrXfer.length() - 1);

					this.xaccount = model.findAccount(acctName);

					if (this.xaccount == null) {
						Common.reportWarning("No xfer account from '" + catOrXfer + "'");
					}
				} else {
					this.category = model.findCategory(catOrXfer);

					if (this.category == null) {
						Common.reportWarning("No category from '" + catOrXfer + "'");
					}
				}
			}

			this.xamount = decimalValue(XAMOUNT_IDX);
			this.price = Common.parsePrice(value(PRICE_IDX));
			this.commission = decimalValue(COMMISSION_IDX);

			// Fix amount for CSV with certain TxAction
			if ((this.action == TxAction.REINV_DIV //
					|| this.action == TxAction.REINV_INT //
					|| this.action == TxAction.REINV_LG //
					|| this.action == TxAction.REINV_SH //
					|| this.action == TxAction.SHRS_IN //
			// TODO || this.action == TxAction.SHRS_OUT //
			) //
					&& (this.amount == null || Common.isEffectivelyZero(this.amount))) {
				StringTokenizer toker = new StringTokenizer(this.description, " shares @ ");
				BigDecimal q = (toker.hasMoreTokens()) //
						? Common.getDecimal(toker.nextToken()) //
						: null;
				BigDecimal p = (toker.hasMoreTokens()) //
						? Common.getDecimal(toker.nextToken()) //
						: null;

				if (q != null && p != null) {
					this.amount = q.multiply(p);
				} else if (this.price != null && this.shares != null) {
					this.amount = this.price.multiply(this.shares);
				}

				if (p != null //
						&& (this.price == null || Common.isEffectivelyZero(this.price))) {
					this.price = p;
				}
			}

			if (this.commission != null && !Common.isEffectivelyZero(this.commission)) {
				this.amount = this.amount.add(this.commission);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
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

	public boolean isSplit() {
		return value(SPLIT_IDX).equals("S");
	}

	public boolean hasSplits() {
		return this.splits != null && !this.splits.isEmpty();
	}

	public TransactionInfo addSplit() {
		if (this.splits == null) {
			this.splits = new ArrayList<>();
		}

		Account acct = MoneyMgrModel.currModel.findAccount(value(ACCOUNT_IDX));
		TransactionInfo splitinfo = new TransactionInfo(acct);

		splitinfo.setValue(TransactionInfo.SPLIT_IDX, "S");

		return addSplit(splitinfo);
	}

	public TransactionInfo addSplit(TransactionInfo splitinfo) {
		// This can be more complicated:
		// Tx1 <------------> Tx2
		// somecat 1.23 <-+
		// ...............+-> somecat 5.79
		// somecat 4.56 <-+

		splitinfo.parent = this;
		this.splits.add(splitinfo);

		// TODO adjust parent's amount, inflow, outflow ???

		return splitinfo;
	}

	private TransactionInfo lastSplit() {
		if (hasSplits()) {
			TransactionInfo split = this.splits.get(this.splits.size() - 1);

			if (value(DATE_IDX).equals(split.value(DATE_IDX))) {
				return split;
			}
		}

		return addSplit();
	}

	/** Create a split and set its category */
	public void addSplitCategory(String cat) {
		// NB in QIF files, split category comes first, so create the split here
		// TODO what if the split is uncategorized?
		addSplit();

		lastSplit().setValue(TransactionInfo.CATEGORY_IDX, cat);
	}

	/** Set the amount in the last split line */
	public void addSplitAmount(String amount) {
		lastSplit().setValue(TransactionInfo.AMOUNT_IDX, amount);
	}

	/** Set the memo in the last split line */
	public void addSplitMemo(String memo) {
		lastSplit().setValue(TransactionInfo.MEMO_IDX, memo);
	}

	/** Get the transaction date */
	private QDate getDate() {
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
	}
}