package moneymgr.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import moneymgr.io.TransactionInfo;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/** Common transaction info - subclassed by Investment vs NonInvestment txn */
public abstract class GenericTxn //
		extends SimpleTxn //
		implements Comparable<GenericTxn> {

	// TODO make txn properties immutable
	private QDate date;
	private String payee;
	private String chkNumber;

	// TODO move splits to SimpleTxn/GenericTxn?//
	// Why shouldn't an investment txn have splits?
	private final List<SplitTxn> splits;

	private QDate stmtdate;

	/** Keeps track of account balance - depends on order of transactions */
	private BigDecimal runningTotal;

	public BigDecimal getRunningTotal() {
		return this.runningTotal;
	}

	public void setRunningTotal(BigDecimal tot) {
		this.runningTotal = tot;
	}

	public GenericTxn(int txid, int acctid) {
		super(txid, acctid);

		this.date = null;
		this.payee = "";
		this.stmtdate = null;
		this.runningTotal = null;
		this.chkNumber = "";

		this.splits = new ArrayList<>();

		if (this.txid > 0 && getAccountID() > 0) {
			MoneyMgrModel.currModel.addTransaction(this);
		}
	}

	public GenericTxn(int acctid) {
		this(MoneyMgrModel.currModel.createTxid(), acctid);
	}

	public QDate getStatementDate() {
		return this.stmtdate;
	}

	public void setCheckNumber(String cknum) {
		this.chkNumber = cknum;
	}

	public String getCheckNumberString() {
		return this.chkNumber;
	}

	public int getCheckNumber() {
		if ((this.chkNumber == null) || (this.chkNumber.length() == 0)) {
			return 0;
		}

		// Quicken puts other info (e.g. 'DEP') in this field as well
		if (!Character.isDigit(this.chkNumber.charAt(0))) {
			return 0;
		}

		try {
			return Integer.parseInt(this.chkNumber);
		} catch (Exception e) {
			return 0;
		}
	}

	public boolean hasSplits() {
		return (this.splits != null) && !this.splits.isEmpty();
	}

	public List<SplitTxn> getSplits() {
		return this.splits;
	}

	public void addSplit(SplitTxn txn) {
		this.splits.add(txn);
	}

	/** Do sanity check of splits */
	public void verifySplit() {
		if (this.splits.isEmpty()) {
			return;
		}

		BigDecimal dec = BigDecimal.ZERO;

		for (SimpleTxn txn : this.splits) {
			dec = dec.add(txn.getAmount());
		}

		if (!dec.equals(getAmount())) {
			Common.reportError("Total(" + getAmount() + ") does not match split total (" + dec + ")");
		}
	}

	/** TODO relocate? Correct missing/bad information from input data */
	public void repair(TransactionInfo tinfo) {
		if (getAmount() == null) {
			setAmount(BigDecimal.ZERO);
			tinfo.setValue(TransactionInfo.AMOUNT_IDX, "0.00");
		}
	}

	public String getPayee() {
		return this.payee;
	}

	public void setPayee(String payee) {
		this.payee = payee;
	}

	/** Disassociate this transaction from its statement */
	public void clear(Statement s) {
		this.stmtdate = s.date;
	}

	public QDate getDate() {
		return this.date;
	}

	/** Update the date of this transaction - update date-sorted list */
	public void setDate(QDate date) {
		QDate olddate = this.date;
		this.date = date;

		MoneyMgrModel.currModel.changeTransactionDate(this, olddate);
	}

	public final void setStatementDate(QDate date) {
		this.stmtdate = date;
	}

	/** TODO poorly named - Comparison by date and check number */
	public int compareTo(GenericTxn other) {
		int diff = getDate().compareTo(other.getDate());
		if (diff != 0) {
			return diff;
		}

		diff = this.getCheckNumber() - other.getCheckNumber();
		if (diff != 0) {
			return diff;
		}

		return 0;
	}

	public String formatValue() {
		return super.formatValue();
	}

	public String matches(GenericTxn other) {
		String res = super.matches(other);
		if (res != null) {
			return res;
		}

		if (!Common.isEffectivelyEqual(getRunningTotal(), other.getRunningTotal())) {
			return "runningTotal";
		}

		return null;
	}
}
