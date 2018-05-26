package qif.data;

import java.math.BigDecimal;
import java.util.Date;

public abstract class GenericTxn extends SimpleTxn {
	private Date date;
	public String clearedStatus;
	public Date stmtdate;
	private String payee;
	public BigDecimal runningTotal;

	private void addToDom() {
		QifDom.dom.addTransaction(this);
	}

	public GenericTxn(int acctid) {
		super(acctid);

		this.date = null;
		this.payee = "";
		this.clearedStatus = null;
		this.stmtdate = null;
		this.runningTotal = null;

		addToDom();
	}

	public GenericTxn(GenericTxn other) {
		super(other);

		this.date = other.date;
		this.payee = other.payee;
		this.clearedStatus = other.clearedStatus;
		this.stmtdate = other.stmtdate;
		this.runningTotal = null;

		addToDom();
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
