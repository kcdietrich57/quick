package moneymgr.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import app.QifDom;
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
					Common.formatAmount3(this.shares).trim(), //
					Common.formatAmount3(this.price).trim(), //
					Common.formatAmount(this.value).trim());
		}
	}

	/**
	 * Helper to analyze performance of a security over a period of time.<br>
	 * This accounts for<br>
	 * Contributions(purchases)<br>
	 * Matching contributions from employer<br>
	 * Dividends<br>
	 * Price changes
	 */
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

		/**
		 * TODO SecurityPerformance (incomplete) Gather performance data from security
		 * transactions
		 */
		private void analyzeTransactions() {
			int idx = MoneyMgrModel.currModel.getLastTransactionIndexOnOrBeforeDate( //
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
					// TODO SecurityPerformance should the performance info follow the shares?
					break;

				case BUY:
				case SELL:
				case GRANT:
				case VEST:
				case EXERCISE:
				case STOCKSPLIT:
					// These don't fit into any performance category (neutral)
					break;

				// TODO SecurityPerformance consider these action types
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

	/** Return if this position has no data */
	public boolean isEmpty() {
		return this.transactions.isEmpty() //
				&& Common.isEffectivelyZeroOrNull(getStartingShares()) //
				&& Common.isEffectivelyZeroOrNull(this.expectedEndingShares);
	}

	/** Check whether this position has any holdings on a given date */
	public boolean isEmptyForDate(QDate d) {
		int ii = MoneyMgrModel.currModel.getLastTransactionIndexOnOrBeforeDate(getTransactions(), d);

		return (ii >= 0) && Common.isEffectivelyZero(this.shrBalance.get(ii));
	}

	/** Reset transactions and sharecount */
	public void initializeTransactions() {
		this.transactions.clear();
		SecurityPosition ppos = getPreviousPosition();
		this.actualEndingShares = (ppos != null) //
				? ppos.getExpectedEndingShares() //
				: BigDecimal.ZERO;
	}

	/** Get the starting position for this security (in stmts context) */
	private SecurityPosition getPreviousPosition() {
		return (this.portfolio.prevPortfolio != null) //
				? this.portfolio.prevPortfolio.findPosition(this.security)//
				: null;
	}

	public QDate getFirstTransactionDate() {
		return (this.transactions.isEmpty()) ? null //
				: this.transactions.get(0).getDate();
	}

	public List<InvestmentTxn> getTransactions() {
		return Collections.unmodifiableList(this.transactions);
	}

	public BigDecimal getStartingShares() {
		SecurityPosition prevpos = getPreviousPosition();

		return (prevpos != null) //
				? prevpos.getExpectedEndingShares() //
				: BigDecimal.ZERO;
	}

	public BigDecimal getEndingShares() {
		return this.actualEndingShares;
	}

	public BigDecimal getExpectedEndingShares() {
		if (this.expectedEndingShares == null) {
			if (QifDom.verbose) {
				Common.reportWarning("Position expected ending shares not set");
			}

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

	/** Add a transaction, adjust share count and adjust for split */
	public void addTransaction(InvestmentTxn txn) {
		if (this.transactions.size() != this.shrBalance.size()) {
			Common.reportError("SecurityPosition tx/bal sizes don't match");
		}
		if (this.transactions.contains(txn)) {
			Common.reportError("Transaction added to portfolio twice");
		}

		switch (txn.getAction()) {
		case BUY:
		case SHRS_IN:
		case REINV_DIV:
		case REINV_LG:
		case REINV_SH:
		case BUYX:
		case REINV_INT:
		case SHRS_OUT:
		case SELL:
		case SELLX:
		case STOCKSPLIT:
			break;

		case GRANT:
		case VEST:
		case EXERCISE:
		case EXERCISEX:
		case EXPIRE:
		case CASH:
		case DIV:
		case INT_INC:
		case MISC_INCX:
		default:
			return;
		}

		boolean hassplit = false;
		BigDecimal splitratio = null;

		int idx = 0;
		while (idx < this.transactions.size() //
				&& (compareByDate.compare(txn, this.transactions.get(idx)) > 0)) {
			++idx;
		}

		while (idx < this.transactions.size() //
				&& (compareByDate.compare(txn, this.transactions.get(idx)) > 0)) {
			InvestmentTxn tx = this.transactions.get(idx);
			if (tx.getAction() == TxAction.STOCKSPLIT) {
				if (hassplit) {
					if (tx.getSplitRatio().compareTo(splitratio) != 0) {
						Common.reportError("Mismatched splits");
					}
				} else {
					hassplit = true;
					splitratio = tx.getSplitRatio();
				}
			}

			++idx;
		}

		if ((txn.getAction() == TxAction.STOCKSPLIT) && hassplit) {
			return;
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

	/** Remove a transaction, reset running totals, etc */
	public void removeTransaction(InvestmentTxn itx) {
		int idx = this.transactions.indexOf(itx);

		if (idx >= 0) {
			this.transactions.remove(idx);
			this.shrBalance.remove(idx);

			setTransactions(this.transactions);
		}
	}

	/** Reset history with starting share balance and transactions */
	public void setTransactions(List<InvestmentTxn> txns) {
		if (txns == this.transactions) {
			txns = new ArrayList<InvestmentTxn>(txns);
		}
		Collections.sort(txns, compareByDate);

		this.transactions.clear();
		this.shrBalance.clear();

		for (InvestmentTxn txn : txns) {
			addTransaction(txn);
		}
	}

	/** Update running share totals in this position */
	public void updateShareBalances() {
		this.shrBalance.clear();

		BigDecimal shrbal = (getPreviousPosition() != null) //
				? getPreviousPosition().getEndingShares() //
				: BigDecimal.ZERO;

		for (InvestmentTxn t : this.transactions) {
			if (t.getAction() == TxAction.STOCKSPLIT) {
				shrbal = shrbal.multiply(t.getSplitRatio());
			} else if (t.getShares() != null) {
				shrbal = shrbal.add(t.getShares());
			}

			this.shrBalance.add(shrbal);
		}
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
		try {
			int txidx = MoneyMgrModel.currModel.getLastTransactionIndexOnOrBeforeDate( //
					this.transactions, d);
			return getValueForIndex(d, txidx);
		} catch (Exception e) {
			e.printStackTrace();
			return BigDecimal.ZERO;
		}
	}

	/** Get shares held on a given date */
	public BigDecimal getSharesForDate(QDate date) {
		int idx = MoneyMgrModel.currModel.getLastTransactionIndexOnOrBeforeDate(this.transactions, date);
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