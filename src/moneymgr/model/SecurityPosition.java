package moneymgr.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import moneymgr.util.Common;
import moneymgr.util.QDate;

/** Holdings/price history for a single security */
public class SecurityPosition {
	/** Holdings/value for a security on a specified date */
	public static class PositionInfo {
		public final QDate date;
		public final Security security;
		public final BigDecimal shares;
		public final BigDecimal price;
		public final BigDecimal value;

		public PositionInfo(Security security, QDate date) {
			this.security = security;
			this.date = date;

			this.shares = BigDecimal.ZERO;
			this.price = security.getPriceForDate(date).getPrice();
			this.value = BigDecimal.ZERO;
		}

		public PositionInfo( //
				Security security, QDate date, //
				BigDecimal shares, BigDecimal price, BigDecimal value) {
			this.security = security;
			this.date = date;
			this.shares = shares;
			this.price = price;
			this.value = value;
		}

		public String toString() {
			return String.format("%8s: %12s %8s %12s", //
					this.security.getSymbol(), //
					Common.formatAmount3(this.shares), //
					Common.formatAmount3(this.price), //
					Common.formatAmount(this.value));
		}
	}

	/** Helper to analyze performance of a security over a period of time */
	public static class SecurityPerformance {
		private final SecurityPosition pos;

		public final QDate start;
		public final QDate end;
		public final BigDecimal startShares;
		public final BigDecimal endShares;

		public BigDecimal contribution = BigDecimal.ZERO;
		public BigDecimal match = BigDecimal.ZERO;
		public BigDecimal dividend = BigDecimal.ZERO;
		// public BigDecimal dividendShares = BigDecimal.ZERO;

		public SecurityPerformance(SecurityPosition pos, QDate start, QDate end) {
			this.pos = pos;
			this.start = start;
			this.end = end;

			this.startShares = this.pos.getSharesForDate(this.start);
			this.endShares = this.pos.getSharesForDate(this.end);

			analyzeTransactions();
		}

		/** TODO (incomplete) Gather performance data from security transactions */
		private void analyzeTransactions() {
			int idx = GenericTxn.getLastTransactionIndexOnOrBeforeDate( //
					this.pos.transactions, this.start);
			if (idx < 0) {
				return;
			}

			while (idx < this.pos.transactions.size()) {
				InvestmentTxn txn = this.pos.transactions.get(idx++);

				switch (txn.getAction()) {
				case XIN:
					this.contribution = this.contribution.add(txn.getAmount());
					break;

				case REINV_DIV:
				case REINV_INT:
				case REINV_LG:
				case REINV_SH:

				case DIV:
				case INT_INC:
				case MISC_INCX:
					this.dividend = this.dividend.add(txn.getAmount());
					break;

				case SHRS_IN:
				case SHRS_OUT:
					// TODO should the performance info follow the shares?
					break;

				case BUY:
				case SELL:
				case GRANT:
				case VEST:
				case EXERCISE:
				case STOCKSPLIT:
					// These don't fit into any performance category (neutral)
					break;

				// TODO consider these action types
				case BUYX:
				case SELLX:
				case CASH:
				case CONTRIBX:
				case WITHDRAWX:
				case XOUT:
				case EXERCISEX:
				case EXPIRE:
				case REMINDER:
				case OTHER:
					break;

				default:
					Common.reportWarning("Unknown action " + txn.getAction().toString());
					break;
				}
			}
		}

		public BigDecimal getStartPrice() {
			return this.pos.security.getPriceForDate(this.start).getPrice();
		}

		public BigDecimal getStartValue() {
			return this.startShares.multiply(getStartPrice());
		}

		public BigDecimal getEndPrice() {
			return this.pos.security.getPriceForDate(this.end).getPrice();
		}

		public BigDecimal getEndValue() {
			return this.endShares.multiply(getEndPrice());
		}
	}

	public final SecurityPortfolio portfolio;
	public final Security security;
	private BigDecimal actualEndingShares;
	private BigDecimal expectedEndingShares;

	/** Transactions for this security */
	private final List<InvestmentTxn> transactions;

	/** Running share balance per transaction */
	public final List<BigDecimal> shrBalance;

	/** Set if this represents a PIT (i.e. statement). Null otherwise. */
	public BigDecimal endingValue;

	/**
	 * Create a position
	 * 
	 * @param portfolio The portfolio containing the position
	 * @param sec       The security
	 * @param endShares For a statement - the ending share count
	 * @param value     For a statement - the ending value
	 */
	private SecurityPosition(SecurityPortfolio portfolio, Security sec, //
			BigDecimal endShares, BigDecimal value) {
		this.portfolio = portfolio;
		this.security = sec;

		this.transactions = new ArrayList<>();
		this.shrBalance = new ArrayList<>();

		this.expectedEndingShares = endShares;
		this.endingValue = value;

		// If this is statement holdings, start with the last stmt's closing
		BigDecimal lastStmtEndshares = BigDecimal.ZERO;
		if (portfolio.prevPortfolio != null) {
			SecurityPosition ppos = portfolio.prevPortfolio.findPosition(sec);
			if (ppos != null) {
				lastStmtEndshares = ppos.getExpectedEndingShares();
			}
		}

		// Prior to adding any txns, the ending shares are equal to last stmt
		this.actualEndingShares = lastStmtEndshares;
	}

	/**
	 * Create a security position
	 * 
	 * @param portfolio The portfolio containing the position
	 * @param sec       The security
	 * @param endShares For a statement - the ending share count
	 */
	private SecurityPosition(SecurityPortfolio portfolio, Security sec, //
			BigDecimal endShares) {
		this(portfolio, sec, endShares, null);
	}

	/**
	 * Create a security position
	 * 
	 * @param portfolio The portfolio containing the position
	 * @param sec       The security
	 */
	public SecurityPosition(SecurityPortfolio portfolio, Security sec) {
		this(portfolio, sec, null);
	}

	/** Build a copy of a position (minus transactions) */
	public SecurityPosition(SecurityPortfolio portfolio, SecurityPosition other) {
		this(portfolio, other.security, other.actualEndingShares);
	}

	public boolean isEmpty() {
		return this.transactions.isEmpty() //
				&& Common.isEffectivelyZeroOrNull(getStartingShares()) //
				&& Common.isEffectivelyZeroOrNull(this.expectedEndingShares);
	}

	/** Check whether this position has any holdings on a given date */
	public boolean isEmptyForDate(QDate d) {
		int ii = GenericTxn.getLastTransactionIndexOnOrBeforeDate(getTransactions(), d);

		return (ii >= 0) && Common.isEffectivelyZero(this.shrBalance.get(ii));
	}

	public void initializeTransactions() {
		this.transactions.clear();
		SecurityPosition ppos = getPreviousPosition();
		this.actualEndingShares = (ppos != null) //
				? ppos.getExpectedEndingShares() //
				: BigDecimal.ZERO;
	}

	private SecurityPosition getPreviousPosition() {
		return (this.portfolio.prevPortfolio != null) //
				? this.portfolio.prevPortfolio.findPosition(this.security)//
				: null;
	}

	public List<InvestmentTxn> getTransactions() {
		return Collections.unmodifiableList(this.transactions);
	}

	public BigDecimal getEndingShares() {
		return this.actualEndingShares;
	}

	public BigDecimal getExpectedEndingShares() {
		if (this.expectedEndingShares == null) {
			Common.reportWarning("Position expected ending shares not set");
			this.expectedEndingShares = BigDecimal.ZERO;
		}

		return this.expectedEndingShares;
	}

	public void setExpectedEndingShares(BigDecimal shares) {
		if (this.expectedEndingShares != null) {
			Common.reportError("Position expected ending shares is immutable");
		}

		this.expectedEndingShares = shares;
	}

	public void addTransaction(InvestmentTxn txn) {
		if (this.transactions.size() != this.shrBalance.size()) {
			Common.reportError("SecurityPosition tx/bal sizes don't match");
		}
		if (this.transactions.contains(txn)) {
			Common.reportError("Transaction added to portfolio twice");
		}

		int idx = 0;
		while (idx < this.transactions.size() //
				&& (compareByDate.compare(txn, this.transactions.get(idx)) > 0)) {
			++idx;
		}

		this.transactions.add(idx, txn);
		this.shrBalance.add(idx, null);

		for (; idx < this.transactions.size(); ++idx) {
			BigDecimal prevbal = (idx > 0) //
					? this.shrBalance.get(idx - 1) //
					: getStartingShares();

			txn = this.transactions.get(idx);

			BigDecimal shrbal = prevbal;

			if (txn.getAction() == TxAction.STOCKSPLIT) {
				shrbal = shrbal.multiply(txn.getSplitRatio());
			} else {
				BigDecimal shrs = txn.getShares();
				if (shrs != null) {
					shrbal = shrbal.add(shrs);
				}
			}

			this.shrBalance.set(idx, shrbal);
			this.actualEndingShares = shrbal;
		}
	}

	private static Comparator<InvestmentTxn> compareByDate = //
			(t1, t2) -> t1.getDate().compareTo(t2.getDate());

	public void removeTransaction(InvestmentTxn itx) {
		int idx = this.transactions.indexOf(itx);

		if (idx >= 0) {
			this.transactions.remove(idx);
			this.shrBalance.remove(idx);

			setTransactions(this.transactions); // , getStartingShares());
		}
	}

	/** Reset history with starting share balance and transactions */
	public void setTransactions(List<InvestmentTxn> txns) {// , BigDecimal startBal) {
		Collections.sort(txns, compareByDate);

		this.transactions.clear();
		this.shrBalance.clear();

		for (InvestmentTxn txn : this.transactions) {
			addTransaction(txn);
		}
	}

	/** Update running share totals in a position */
	public void updateShareBalances() {
		BigDecimal shrbal = BigDecimal.ZERO;
		this.shrBalance.clear();

		for (InvestmentTxn t : this.transactions) {
			if (t.getAction() == TxAction.STOCKSPLIT) {
				shrbal = shrbal.multiply(t.getSplitRatio());
			} else if (t.getShares() != null) {
				shrbal = shrbal.add(t.getShares());
			}

			this.shrBalance.add(shrbal);
		}
	}

	public BigDecimal getStartingShares() {
		SecurityPosition prevpos = getPreviousPosition();

		return (prevpos != null) //
				? prevpos.getExpectedEndingShares() //
				: BigDecimal.ZERO;
	}

	/** Get the value for the Nth transaction */
	private BigDecimal getValueForIndex(QDate date, int idx) {
		if (idx < 0) {
			return BigDecimal.ZERO;
		}

		BigDecimal shares = this.shrBalance.get(idx);
		BigDecimal price = this.security.getPriceForDate(date).getPrice();

		BigDecimal value = shares.multiply(price);

		return value;
	}

	/** Get value as of a given date */
	public BigDecimal getValueForDate(QDate d) {
		return getValueForIndex( //
				d, //
				GenericTxn.getLastTransactionIndexOnOrBeforeDate(this.transactions, d));
	}

	/** Get shares held on a given date */
	public BigDecimal getSharesForDate(QDate date) {
		int idx = GenericTxn.getLastTransactionIndexOnOrBeforeDate(this.transactions, date);
		return (idx >= 0) ? this.shrBalance.get(idx) : getStartingShares();
	}

	/** Create a PositionInfo summarizing the position/value on a given date */
	public PositionInfo getPositionForDate(QDate date) {
		BigDecimal shares = getSharesForDate(date);
		return (shares == null) //
				? null //
				: new PositionInfo( //
						this.security, //
						date, //
						shares, //
						this.security.getPriceForDate(date).getPrice(), //
						getValueForDate(date));
	}

// TODO defunct
//	/** Encode position transactions for persistence: name;numtx[;txid;shrbal] */
//	public String formatForSave(Statement stat) {
//		int numtx = this.transactions.size();
//		String s = this.security.getName() + ";" + numtx;
//
//		for (int ii = 0; ii < numtx; ++ii) {
//			InvestmentTxn t = this.transactions.get(ii);
//			int txidx = stat.transactions.indexOf(t);
//			BigDecimal bal = this.shrBalance.get(ii);
//
//			assert txidx >= 0;
//
//			s += String.format(";%d;%f", txidx, bal);
//		}
//
//		return s;
//	}

	public String toString() {
		String s = String.format( //
				"%-20s   %s shrs (expect %s, start %s)  %d tx ", //
				this.security.getName(), //
				Common.formatAmount3(this.actualEndingShares).trim(), //
				Common.formatAmount3(this.expectedEndingShares).trim(), //
				Common.formatAmount3(getStartingShares()).trim(), //
				this.transactions.size());

		if (this.endingValue != null) {
			s += "  " + Common.formatAmount3(this.endingValue);
		}

		return s;
	}
}