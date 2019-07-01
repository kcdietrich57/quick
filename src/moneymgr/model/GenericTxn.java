package moneymgr.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import moneymgr.io.TransactionInfo;
import moneymgr.util.QDate;

/** Common transaction info - subclassed by Investment vs NonInvestment txn */
public abstract class GenericTxn //
		extends SimpleTxn //
		implements Comparable<GenericTxn> {

	/**
	 * Indicator for which transaction index to return from get by date.
	 *
	 * @value INSERT The index at which to insert a new transaction
	 * @value FIRST The first transaction on the date (-1 if not found)
	 * @value LAST The last transaction on the date (-1 if not found)
	 */
	private enum TxIndexType {
		INSERT, FIRST, LAST
	}

	public static boolean rememberTransactions = true;

	private static final List<GenericTxn> allTransactionsByID = new ArrayList<>();
	private static final List<GenericTxn> allTransactionsByDate = new ArrayList<>();
	public static final List<GenericTxn> alternateTransactions = new ArrayList<>();

	/** Dummy transaction for binary search */
	public static final GenericTxn SEARCH;
	static {
		SEARCH = new NonInvestmentTxn(0);
		allTransactionsByID.remove(SEARCH.txid);
		allTransactionsByDate.remove(SEARCH);
	}

	private static final Comparator<GenericTxn> compareByDate = new Comparator<GenericTxn>() {
		public int compare(GenericTxn o1, GenericTxn o2) {
			return o1.getDate().subtract(o2.getDate());
		}
	};

	public static List<GenericTxn> getAllTransactions() {
		return Collections.unmodifiableList(allTransactionsByID);
	}

	public static List<GenericTxn> getTransactionsByDate() {
		return allTransactionsByDate;
	}

	public static void addTransaction(GenericTxn txn) {
		if (rememberTransactions) {
			while (allTransactionsByID.size() < (txn.txid + 1)) {
				allTransactionsByID.add(null);
			}

			allTransactionsByID.set(txn.txid, txn);

			if (txn.getDate() != null) {
				addTransactionDate(txn);
			}
		} else {
			alternateTransactions.add(txn);
		}
	}

	private static void addTransactionDate(GenericTxn txn) {
		int idx = getTransactionInsertIndexByDate(allTransactionsByDate, txn);

		allTransactionsByDate.add(idx, txn);
	}

	public static QDate getFirstTransactionDate() {
		return (allTransactionsByDate.isEmpty()) //
				? null //
				: allTransactionsByDate.get(0).getDate();
	}

	public static QDate getLastTransactionDate() {
		return (allTransactionsByDate == null || allTransactionsByDate.isEmpty()) //
				? null //
				: allTransactionsByDate.get(allTransactionsByDate.size() - 1).getDate();
	}

	/**
	 * Return investment transactions for a date range
	 *
	 * @param start Index of earliest matching transaction
	 * @param end   Index of latest matching transaction
	 */
	public static List<GenericTxn> getInvestmentTransactions(QDate start, QDate end) {
		List<GenericTxn> txns = new ArrayList<>();

		for (GenericTxn txn : getTransactions(start, end)) {
			if (txn instanceof InvestmentTxn) {
				txns.add(txn);
			}
		}

		return txns;
	}

	/**
	 * Return all transactions for a date range
	 *
	 * @param start Index of earliest matching transaction
	 * @param end   Index of latest matching transaction
	 */
	private static List<GenericTxn> getTransactions(QDate start, QDate end) {
		int idx1 = getFirstTransactionIndexByDate(allTransactionsByDate, start);
		int idx2 = getLastTransactionIndexByDate(allTransactionsByDate, end);

		if (idx1 < 0) {
			idx1 = -idx1 - 1;
		}

		// Advance index to the last matching transaction
		while ((idx2 >= 0) //
				&& ((idx2 + 1) < allTransactionsByDate.size()) //
				&& allTransactionsByDate.get(idx2 + 1).getDate().equals(end)) {
			++idx2;
		}

		if (idx2 < 0) {
			idx2 = -idx2 - 1;

			// Move idx back to previous matching transaction
			if (idx2 > 0) {
				--idx2;
			}
		}

		return allTransactionsByDate.subList(idx1, idx2);
	}

	/** Return the index of the last txn on or prior to a given date */
	public static int getLastTransactionIndexOnOrBeforeDate( //
			List<? extends GenericTxn> txns, QDate d) {
		if (txns.isEmpty()) {
			return -1;
		}

		int idx = getLastTransactionIndexByDate(txns, d);
		if (idx >= 0) {
			return idx;
		}

		idx = getTransactionInsertIndexByDate(txns, SEARCH);

		return (idx > 0) ? idx - 1 : -1;
	}

	/** Return the index of the first transaction on a date. (-1 if none) */
	private static int getFirstTransactionIndexByDate( //
			List<? extends GenericTxn> txns, QDate date) {
		SEARCH.setDate(date);

		return getTransactionIndexByDate(txns, SEARCH, TxIndexType.FIRST);
	}

	/** Return the index of the last transaction on a date. (-1 if none) */
	private static int getLastTransactionIndexByDate( //
			List<? extends GenericTxn> txns, QDate date) {
		SEARCH.setDate(date);

		return getTransactionIndexByDate(txns, SEARCH, TxIndexType.LAST);
	}

	/** Return the index for inserting a transaction into a list sorted by date */
	public static int getTransactionInsertIndexByDate( //
			List<? extends GenericTxn> txns, GenericTxn txn) {
		return getTransactionIndexByDate(txns, txn, TxIndexType.INSERT);
	}

	/**
	 * Return the index for inserting a transaction into a sorted list by date
	 *
	 * @param txn
	 * @param before If true, the insertion point before the first txn on the date
	 *               otherwise, the point after all txns on the date.
	 * @return Index for insert (cannot be negative)
	 */
	private static int getTransactionInsertIndexByDate( //
			List<? extends GenericTxn> txns, QDate date) {
		SEARCH.setDate(date);

		return getTransactionIndexByDate(txns, SEARCH, TxIndexType.INSERT);
	}

	/**
	 * Return the index of a transaction in a list sorted by date
	 *
	 * @param txn
	 * @param TxIndexType If FIRST/LAST, the index of the first/last txn on that
	 *                    date (-1 if no such txn exists);<br>
	 *                    If INSERT the index after all txns on or before the date.
	 * @return Index; -(insert index + 1) if FIRST/LAST and no match exists
	 */
	private static int getTransactionIndexByDate(List<? extends GenericTxn> txns, GenericTxn txn, TxIndexType which) {
		if (txns.isEmpty()) {
			return (which == TxIndexType.INSERT) ? 0 : -1;
		}

		int sz = txns.size();

		int idx = Collections.binarySearch(txns, txn, compareByDate);

		while ((idx >= 0) && (idx < sz)) {
			QDate dt = txns.get(idx).getDate();
			int diff = dt.subtract(txn.getDate());
			if (diff > 0) {
				--idx;
			} else if (diff > 0) {
				++idx;
			} else {
				break;
			}
		}

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

	// TODO make txn properties immutable
	private QDate date;
	private String payee;
	public String chkNumber;

	public QDate stmtdate;

	/** Keeps track of account balance - depends on order of transactions */
	public BigDecimal runningTotal;

	public GenericTxn(int acctid) {
		super(acctid);

		this.date = null;
		this.payee = "";
		this.stmtdate = null;
		this.runningTotal = null;

		GenericTxn.addTransaction(this);
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

	/** Correct missing/bad information from input data */
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

	/** Update the date of this transaction */
	public void setDate(QDate date) {
		if ((getAccountID() != 0) && (getDate() != null)) {
			allTransactionsByDate.remove(this);
		}

		this.date = date;

		if ((getAccountID() != 0) && (getDate() != null)) {
			addTransactionDate(this);
		}
	}

	/** Comparison by date and check number */
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
}
