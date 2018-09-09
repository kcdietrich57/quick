package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class GenericTxn //
		extends SimpleTxn //
		implements Comparable<GenericTxn> {

	private static List<GenericTxn> allTransactionsByID = new ArrayList<GenericTxn>();
	private static List<GenericTxn> allTransactionsByDate = new ArrayList<GenericTxn>();
	private static List<GenericTxn> allTransactionsByID_readonly = null;

	public static List<GenericTxn> getAllTransactions() {
		if (allTransactionsByID_readonly == null) {
			allTransactionsByID_readonly = Collections.unmodifiableList(allTransactionsByID);
		}

		return allTransactionsByID_readonly;
	}

	public static List<GenericTxn> getTransactionsByDate() {
		return allTransactionsByDate;
	}

	public static List<GenericTxn> getInvestmentTransactions(QDate start, QDate end) {
		List<GenericTxn> txns = new ArrayList<GenericTxn>();

		for (GenericTxn txn : getTransactions(start, end)) {
			if (txn instanceof InvestmentTxn) {
				txns.add(txn);
			}
		}

		return txns;
	}

	public static List<GenericTxn> getTransactions(QDate start, QDate end) {
		int idx1 = getTransactionIndexByDate(start, true);
		int idx2 = getTransactionIndexByDate(end, false);

		return allTransactionsByDate.subList(idx1, idx2);
	}

	public static void addTransaction(GenericTxn txn) {
		while (allTransactionsByID.size() < txn.txid + 1) {
			allTransactionsByID.add(null);
		}

		allTransactionsByID.set(txn.txid, txn);
		// TODO not necessary allTransactionsByID_readonly = null;

		if (txn.getDate() != null) {
			addTransactionDate(txn);
		}
	}

	public static void addTransactionDate(GenericTxn txn) {
		int idx = getTransactionIndexByDate(txn, false);

		allTransactionsByDate.add(idx, txn);
	}

	private static final Comparator<GenericTxn> c = new Comparator<GenericTxn>() {
		public int compare(GenericTxn o1, GenericTxn o2) {
			return o1.getDate().subtract(o2.getDate());
		}
	};

	private static final GenericTxn SEARCH = new NonInvestmentTxn(0);

	private static int getTransactionIndexByDate(QDate date, boolean before) {
		SEARCH.setDate(date);

		return getTransactionIndexByDate(SEARCH, before);
	}

	/**
	 * Return the index for inserting a transaction by date
	 * 
	 * @param txn
	 * @param before If true, the insertion point before other txns on the date
	 *               otherwise, the point after all txns on the date.
	 * @return Index for insert
	 */
	private static int getTransactionIndexByDate(GenericTxn txn, boolean before) {
		int idx = Collections.binarySearch(allTransactionsByDate, txn, c);

		if (idx < 0) {
			idx = (-idx) - 1;
		}

		while (before //
				&& (idx > 0) //
				&& allTransactionsByDate.get(idx - 1).getDate().equals(txn.getDate())) {
			--idx;
		}

		while (!before //
				&& (idx + 1 < allTransactionsByDate.size()) //
				&& allTransactionsByDate.get(idx + 1).getDate().equals(txn.getDate())) {
			++idx;
		}

		return idx;
	}

	public static QDate getFirstTransactionDate() {
		return (allTransactionsByDate.isEmpty()) //
				? null //
				: allTransactionsByDate.get(0).getDate();
	}

	public static QDate getLastTransactionDate() {
		return (allTransactionsByDate.isEmpty()) //
				? null //
				: allTransactionsByDate.get(allTransactionsByDate.size() - 1).getDate();
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
		if ((this.acctid != 0) && (this.date != null)) {
			allTransactionsByDate.remove(this);
		}

		this.date = date;

		if ((this.acctid != 0) && (this.date != null)) {
			addTransactionDate(this);
		}
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

	public int compareTo(GenericTxn other) {
		int diff = this.date.compareTo(other.date);
		if (diff != 0) {
			return diff;
		}

		diff = this.getCheckNumber() - other.getCheckNumber();
		if (diff != 0) {
			return diff;
		}

		return 0;
	}
}
