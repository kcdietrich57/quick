package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
					this.pos.transactions, start);
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
			return pos.security.getPriceForDate(this.start).getPrice();
		}

		public BigDecimal getStartValue() {
			return this.startShares.multiply(getStartPrice());
		}

		public BigDecimal getEndPrice() {
			return pos.security.getPriceForDate(this.end).getPrice();
		}

		public BigDecimal getEndValue() {
			return this.endShares.multiply(getEndPrice());
		}
	}

	public final Security security;
	private BigDecimal startShares;
	public BigDecimal endingShares;

	/** Transactions for this security */
	public final List<InvestmentTxn> transactions;

	/** Running share balance per transaction */
	public List<BigDecimal> shrBalance;

	/** Set if this represents a PIT (i.e. statement). Null otherwise. */
	public BigDecimal value;

	public SecurityPosition(Security sec, BigDecimal shares, BigDecimal value) {
		this.security = sec;
		this.endingShares = shares;
		this.transactions = new ArrayList<InvestmentTxn>();
		this.shrBalance = new ArrayList<BigDecimal>();
		this.value = value;
	}

	public SecurityPosition(Security sec, BigDecimal shares) {
		this(sec, shares, null);
	}

	public SecurityPosition(Security sec) {
		this(sec, BigDecimal.ZERO);
	}

	/** Build a copy of a position (minus transactions) */
	public SecurityPosition(SecurityPosition other) {
		this(other.security, other.endingShares);
	}

	public BigDecimal getStartingShares() {
		return this.startShares;
	}

	public BigDecimal getEndingShares() {
		return this.endingShares;
	}

	/** Reset history with starting share balance and transactions */
	public void setTransactions(List<InvestmentTxn> txns, BigDecimal startBal) {
		Collections.sort(txns, (t1, t2) -> t1.getDate().compareTo(t2.getDate()));

		this.transactions.clear();
		this.transactions.addAll(txns);
		this.shrBalance.clear();

		this.startShares = startBal;

		for (InvestmentTxn t : this.transactions) {
			startBal = startBal.add(t.getShares());
			this.shrBalance.add(startBal);
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
		return getValueForIndex( //
				d, //
				GenericTxn.getLastTransactionIndexOnOrBeforeDate(this.transactions, d));
	}

	/** Get shares held on a given date */
	public BigDecimal getSharesForDate(QDate date) {
		int idx = GenericTxn.getLastTransactionIndexOnOrBeforeDate(this.transactions, date);
		return (idx >= 0) ? shrBalance.get(idx) : BigDecimal.ZERO;
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

	/** Encode position transactions for persistence: name;numtx[;txid;shrbal] */
	public String formatForSave(Statement stat) {
		int numtx = this.transactions.size();
		String s = this.security.getName() + ";" + numtx;

		for (int ii = 0; ii < numtx; ++ii) {
			InvestmentTxn t = this.transactions.get(ii);
			int txidx = stat.transactions.indexOf(t);
			BigDecimal bal = this.shrBalance.get(ii);

			assert txidx >= 0;

			s += String.format(";%d;%f", txidx, bal);
		}

		return s;
	}

	public String toString() {
		String s = String.format( //
				"%-20s   %s shrs  %d txns", //
				this.security.getName(), //
				Common.formatAmount3(this.endingShares), //
				this.transactions.size());

		if (this.value != null) {
			s += "  " + Common.formatAmount3(this.value);
		}

		return s;
	}
}