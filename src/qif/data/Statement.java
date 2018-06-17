package qif.data;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Statement {
	// Info about reconciled statements goes in this file
	public static File stmtLogFile = new File(QifDom.qifDir, "statementLog.dat");

	public int acctid;
	public QDate date;

	public Statement prevStatement;

	public boolean isBalanced;

	// open/close balance are total balance, including cash and securities
	public BigDecimal closingBalance;

	// Separate closing balance for cash and securities
	public BigDecimal cashBalance;
	public SecurityPortfolio holdings;

	// Transactions included in this statement
	public final List<GenericTxn> transactions;

	// Transactions that as of the closing date are not cleared
	public final List<GenericTxn> unclearedTransactions;

	/** Whether this statement has been saved to the reconcile log file */
	public boolean dirty = false;

	public Statement(int acctid) {
		this.isBalanced = false;
		this.acctid = acctid;
		this.date = null;
		this.prevStatement = null;
		this.closingBalance = null;
		this.cashBalance = null;
		this.holdings = new SecurityPortfolio();

		this.transactions = new ArrayList<GenericTxn>();
		this.unclearedTransactions = new ArrayList<GenericTxn>();
	}

	public BigDecimal getOpeningBalance() {
		final BigDecimal openingBalance = (this.prevStatement != null) //
				? this.prevStatement.closingBalance //
				: BigDecimal.ZERO;
		return openingBalance;
	}

	public BigDecimal getOpeningCashBalance() {
		final BigDecimal openingBalance = (this.prevStatement != null) //
				? this.prevStatement.cashBalance //
				: BigDecimal.ZERO;
		return openingBalance;
	}

	public BigDecimal getCredits() {
		BigDecimal cr = new BigDecimal(0);

		for (GenericTxn s : this.transactions) {
			BigDecimal amt = s.getCashAmount();

			if (amt.compareTo(new BigDecimal(0)) >= 0) {
				cr = cr.add(amt);
			}
		}

		return cr;
	}

	public BigDecimal getDebits() {
		BigDecimal db = new BigDecimal(0);

		for (GenericTxn s : this.transactions) {
			BigDecimal amt = s.getCashAmount();

			if (amt.compareTo(new BigDecimal(0)) < 0) {
				db = db.add(amt.negate());
			}
		}

		return db;
	}

	public void addTransactions(Collection<GenericTxn> txns) {
		addTransactions(txns, false);
	}

	public void addTransactions(Collection<GenericTxn> txns, boolean checkDate) {
		if (!checkDate) {
			this.transactions.addAll(txns);
		} else {
			for (GenericTxn t : txns) {
				if (t.getDate().compareTo(this.date) <= 0) {
					this.transactions.add(t);
				}
			}
		}
	}

	public void addTransaction(GenericTxn txn) {
		this.transactions.add(txn);
	}

	public void clearTransactions( //
			List<GenericTxn> txns, List<GenericTxn> unclearedTxns) {
		for (final GenericTxn t : txns) {
			t.stmtdate = this.date;
		}
		for (final GenericTxn t : unclearedTxns) {
			t.stmtdate = null;
		}

		this.transactions.clear();
		this.unclearedTransactions.clear();

		this.transactions.addAll(txns);
		this.unclearedTransactions.addAll(unclearedTxns);
	}

	public void clearAllTransactions() {
		clearTransactions(this.unclearedTransactions);
	}

	public void unclearAllTransactions() {
		unclearTransactions(this.transactions);
	}

	public void clearTransactions(List<GenericTxn> txns) {
		for (final GenericTxn t : txns) {
			t.stmtdate = this.date;
		}

		this.transactions.addAll(txns);
		this.unclearedTransactions.removeAll(txns);
	}

	public void unclearTransactions(List<GenericTxn> txns) {
		for (final GenericTxn t : txns) {
			t.stmtdate = null;
		}

		this.unclearedTransactions.addAll(txns);
		this.transactions.removeAll(txns);
	}

	/**
	 * Check cleared transactions against expected cash balance
	 *
	 * @return True if balance matches
	 */
	public boolean cashMatches() {
		return cashMatches(this.transactions);
	}

	/**
	 * Check cleared transactions against expected cash balance
	 *
	 * @param txns
	 *            The transactions to check
	 * @return True if balance matches
	 */
	public boolean cashMatches(List<GenericTxn> txns) {
		return getCashDifference(txns).signum() == 0;
	}

	/**
	 * Check list of transactions against expected cash balance
	 *
	 * @return Difference from expected value
	 */
	public BigDecimal getCashDifference() {
		return getCashDifference(this.transactions);
	}

	/**
	 * Check list of transactions against expected cash balance
	 *
	 * @param txns
	 *            The transactions to check
	 * @return Difference from expected balance
	 */
	private BigDecimal getCashDifference(List<GenericTxn> txns) {
		final BigDecimal cashTotal = Common.sumCashAmounts(txns);
		final BigDecimal cashExpected = this.cashBalance.subtract(getOpeningCashBalance());
		BigDecimal cashDiff = cashTotal.subtract(cashExpected);

		final BigDecimal newbal = getCashDelta(txns);
		cashDiff = this.cashBalance.subtract(newbal);

		return cashDiff;
	}

	/**
	 * Calculate the resulting cash position from the previous balance and a list of
	 * transactions
	 *
	 * @param txns
	 *            The transactions
	 * @return The new cash balance
	 */
	public BigDecimal getCashDelta(List<GenericTxn> txns) {
		return getOpeningCashBalance().add(Common.sumCashAmounts(txns));
	}

	/**
	 * Calculate the resulting cash position from the previous balance and cleared
	 * transactions
	 *
	 * @return The new cash balance
	 */
	public BigDecimal getCashDelta() {
		return getCashDelta(this.transactions);
	}

	/**
	 * Check cleared transactions against expected security holdings
	 *
	 * @return True if they match
	 */
	public boolean holdingsMatch() {
		if (this.holdings.positions.isEmpty()) {
			return true;
		}

		final SecurityPortfolio delta = getPortfolioDelta();

		for (final SecurityPosition p : this.holdings.positions) {
			final SecurityPosition op = delta.getPosition(p.security);

			return Common.isEffectivelyEqual(p.shares, //
					(op != null) ? op.shares : BigDecimal.ZERO);
		}

		for (final SecurityPosition p : delta.positions) {
			final SecurityPosition op = this.holdings.getPosition(p.security);

			return (op != null) //
					? Common.isEffectivelyEqual(p.shares, op.shares) //
					: Common.isEffectivelyZero(p.shares);
		}

		return true;
	}

	/**
	 * Build a Portfolio position from the previous holdings and a list of
	 * transactions
	 *
	 * @param txns
	 *            The transactions
	 * @return new portfolio holdings
	 */
	private SecurityPortfolio getPortfolioDelta(List<GenericTxn> txns) {
		final SecurityPortfolio clearedPositions = (this.prevStatement != null) //
				? new SecurityPortfolio(this.prevStatement.holdings) //
				: new SecurityPortfolio();

		for (final GenericTxn t : txns) {
			if (!(t instanceof InvestmentTxn)) {
				continue;
			}

			final InvestmentTxn itx = (InvestmentTxn) t;

			clearedPositions.addTransaction(itx);
		}

		return clearedPositions;
	}

	/**
	 * Build a Portfolio position from the previous holdings and the current
	 * transactions
	 *
	 * @return Portfolio with holdings
	 */
	public SecurityPortfolio getPortfolioDelta() {
		return getPortfolioDelta(this.transactions);
	}

	public String toString() {
		final String s = this.date.toString() //
				+ "  " + this.closingBalance //
				+ " tran=" + ((this.transactions != null) ? this.transactions.size() : null);

		return s;
	}
};
