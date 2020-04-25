package moneymgr.model;

import java.math.BigDecimal;

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
	public String chkNumber;

	public QDate stmtdate;

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

	public int compareWith(TransactionInfo tuple, SimpleTxn othersimp) {
		int diff;

		diff = super.compareWith(tuple, othersimp);
		if (diff != 0) {
			return diff;
		}

//		if (!(othersimp instanceof GenericTxn)) {
//			return -1;
//		}
//
//		GenericTxn other = (GenericTxn) othersimp;

		return 0;
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

	/** Whether this transaction belongs to a statement */
	public boolean isCleared() {
		return this.stmtdate != null;
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

	public boolean matches(GenericTxn other) {
		BigDecimal v1 = getAmount();
		BigDecimal v2 = other.getAmount();
		boolean vmatches = Common.isEffectivelyEqual(v1, v2);
		return getAccountID() == other.getAccountID() //
				&& getDate().equals(other.getDate()) //
				&& vmatches;
	}
}
