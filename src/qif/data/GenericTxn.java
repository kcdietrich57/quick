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
		int idx1 = getTransactionIndexByDate(start);
		int idx2 = getTransactionIndexByDate(end);

		if (idx1 < 0) {
			idx1 = -idx1 - 1;
		}

		if (idx2 < 0) {
			idx2 = -idx2 - 1;
		}

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
		int idx = getTransactionInsertIndexByDate(txn);

		allTransactionsByDate.add(idx, txn);
	}

	private static final Comparator<GenericTxn> c = new Comparator<GenericTxn>() {
		public int compare(GenericTxn o1, GenericTxn o2) {
			return o1.getDate().subtract(o2.getDate());
		}
	};

	private static final GenericTxn SEARCH = new NonInvestmentTxn(0);

	/**
	 * Indicate which transaction index to return from get by date.
	 * 
	 * @value INSERT The index at which to insert a new transaction
	 * @value FIRST The first transaction on the date
	 * @value LAST The last transaction on the date
	 */
	public static enum TxIndexType {
		INSERT, FIRST, LAST
	}

	public static int getLastTransactionIndexOnOrBeforeDate( //
			List<? extends GenericTxn> txns, QDate d) {
		if (txns.isEmpty()) {
			return -1;
		}

		int idx = getTransactionInsertIndexByDate(txns, d);

		int n = (idx < 0) ? -idx - 1 : idx;
		if (n >= txns.size()) {
			n = txns.size() - 1;
		}

		GenericTxn tx = txns.get(n);
		int diff = tx.getDate().compareTo(d);

		if (diff <= 0) {
			return n;
		}

		return (n > 0) ? n - 1 : -1;
	}

	/**
	 * Return the first transaction on a date.
	 * 
	 * @return -1 if no such transaction exists
	 */
	private static int getTransactionIndexByDate(QDate date) {
		SEARCH.setDate(date);

		return getTransactionIndexByDate(allTransactionsByDate, SEARCH, TxIndexType.FIRST);
	}

	/**
	 * Return the index for inserting a transaction into a list sorted by date
	 */
	private static int getTransactionInsertIndexByDate(GenericTxn txn) {
		return getTransactionIndexByDate(allTransactionsByDate, txn, TxIndexType.INSERT);
	}

	/**
	 * Return the index for inserting a transaction into a sorted list by date
	 * 
	 * @param txn
	 * @param before If true, the insertion point before the first txn on the date
	 *               otherwise, the point after all txns on the date.
	 * @return Index for insert
	 */
	private static int getTransactionInsertIndexByDate(List<? extends GenericTxn> txns, QDate date) {
		SEARCH.setDate(date);

		return getTransactionIndexByDate(txns, SEARCH, TxIndexType.INSERT);
	}

	/**
	 * Return the index of a transaction in a list sorted by date
	 * 
	 * @param txn
	 * @param TxIndexType If FIRST/LAST, the index of the first/last txn on that
	 *                    date; If INSERT the index after all txns on or before the
	 *                    date.
	 * @return Index; -(insert index + 1) if FIRST/LAST and no match exists
	 */
	private static int getTransactionIndexByDate(List<? extends GenericTxn> txns, GenericTxn txn, TxIndexType which) {
		int idx = Collections.binarySearch(txns, txn, c);

		if (idx < 0) {
			int n = -idx - 1;
			if (which == TxIndexType.INSERT) {
				return n;
			}

			if (n >= txns.size()) {
				n = txns.size() - 1;
			}

			if (!txns.get(n).getDate().equals(txn.getDate())) {
				return idx;
			}

			idx = n;
		}

		int sz = txns.size();

		switch (which) {
		case FIRST:
			while ((idx > 0) //
					&& txns.get(idx - 1).getDate().equals(txn.getDate())) {
				--idx;
			}
			break;

		case LAST:
			while (((idx + 1) < sz) //
					&& txns.get(idx + 1).getDate().equals(txn.getDate())) {
				++idx;
			}
			break;

		case INSERT:
			while ((idx < sz) //
					&& txns.get(idx).getDate().equals(txn.getDate())) {
				++idx;
			}
			break;
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

	public String formatValue() {
		return super.formatValue();
	}
}
