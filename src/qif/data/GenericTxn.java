package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class GenericTxn extends SimpleTxn {

	private static List<GenericTxn> allTransactionsByID = new ArrayList<GenericTxn>();
	private static List<GenericTxn> allTransactionsByID_readonly = null;

	public static List<GenericTxn> getAllTransactions() {
		if (allTransactionsByID_readonly == null) {
			allTransactionsByID_readonly = Collections.unmodifiableList(allTransactionsByID);
		}

		return allTransactionsByID_readonly;
	}

	public static void addTransaction(GenericTxn txn) {
		while (allTransactionsByID.size() < txn.txid + 1) {
			allTransactionsByID.add(null);
		}

		allTransactionsByID.set(txn.txid, txn);
		allTransactionsByID_readonly = null;
	}

	private QDate date;
	public String clearedStatus;
	public QDate stmtdate;
	private String payee;
	public BigDecimal runningTotal;

	public GenericTxn(int acctid) {
		super(acctid);

		this.date = null;
		this.payee = "";
		this.clearedStatus = null;
		this.stmtdate = null;
		this.runningTotal = null;

		GenericTxn.addTransaction(this);
	}

	public GenericTxn(GenericTxn other) {
		super(other);

		this.date = other.date;
		this.payee = other.payee;
		this.clearedStatus = other.clearedStatus;
		this.stmtdate = other.stmtdate;
		this.runningTotal = null;

		GenericTxn.addTransaction(this);
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

	public void setDate(QDate date) {
		this.date = date;
	}

	public QDate getDate() {
		return this.date;
	}

	public String formatForSave() {
		final String s = String.format("T;%s;%d;%5.2f", //
				getDate().toString(), //
				getCheckNumber(), //
				getCashAmount());
		return s;
	}
};
