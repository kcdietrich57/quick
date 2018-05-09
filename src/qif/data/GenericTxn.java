package qif.data;

import java.math.BigDecimal;
import java.util.Date;

public abstract class GenericTxn extends SimpleTxn {
	private Date date;
	public String clearedStatus;
	public Date stmtdate;
	private String payee;
	public BigDecimal runningTotal;

	public static GenericTxn clone(int domid, GenericTxn txn) {
		if (txn instanceof NonInvestmentTxn) {
			return new NonInvestmentTxn(domid, (NonInvestmentTxn) txn);
		}
		if (txn instanceof InvestmentTxn) {
			return new InvestmentTxn(domid, (InvestmentTxn) txn);
		}

		return null;
	}

	private void addToDom(int domid) {
		final QifDom dom = QifDom.getDomById(domid);
		dom.addTransaction(this);
	}

	public GenericTxn(int domid, int acctid) {
		super(domid, acctid);

		this.date = null;
		this.payee = "";
		this.clearedStatus = null;
		this.stmtdate = null;
		this.runningTotal = null;

		addToDom(domid);
	}

	public GenericTxn(int domid, GenericTxn other) {
		super(domid, other);

		this.date = other.date;
		this.payee = other.payee;
		this.clearedStatus = other.clearedStatus;
		this.stmtdate = other.stmtdate;
		this.runningTotal = null;

		addToDom(domid);
	}

	public int getCheckNumber() {
		return 0;
	}

	public void repair() {
		if (getAmount() == null) {
			setAmount(BigDecimal.ZERO);
		}
	}

	public String getPayee() {
		return this.payee;
	}

	public void setPayee(String payee) {
		this.payee = payee;
	}

	public boolean isCleared() {
		return this.stmtdate != null;
	}

	public void clear(Statement s) {
		this.stmtdate = s.date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Date getDate() {
		return this.date;
	}

	public String formatForSave() {
		final String s = String.format("T;%s;%d;%5.2f", //
				Common.formatDate(getDate()), //
				getCheckNumber(), //
				getCashAmount());
		return s;
	}
};
