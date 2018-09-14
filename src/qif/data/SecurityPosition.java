package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SecurityPosition {
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

			build();
		}

		private void build() {
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

				case BUY:
				case SELL:
				case GRANT:
				case VEST:
				case EXERCISE:
				case STOCKSPLIT:
				case SHRS_IN:
				case SHRS_OUT:
					// These don't fit into any performance category (neutral)
					break;

				case REINV_DIV:
				case REINV_INT:
				case REINV_LG:
				case REINV_SH:
					this.dividend = this.dividend.add(txn.getAmount());
					break;

				default:
					System.out.println();
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

	public Security security;
	private BigDecimal startShares;
	public BigDecimal endingShares;

	public List<InvestmentTxn> transactions;

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

	public BigDecimal getValueForTransaction(InvestmentTxn tx) {
		int idx = this.transactions.indexOf(tx);

		if (idx >= 0) {
			return this.shrBalance.get(idx).multiply( //
					this.security.getPriceForDate(tx.getDate()).getPrice());
		}

		return BigDecimal.ZERO;
	}

	public BigDecimal getValueForDate(QDate d) {
		if (this.transactions.isEmpty()) {
			return this.endingShares.multiply(this.security.getPriceForDate(d).getPrice());
		}

		int idx = GenericTxn.getLastTransactionIndexOnOrBeforeDate(this.transactions, d);

		if (idx < 0) {
			return BigDecimal.ZERO;
		}

		final InvestmentTxn txn = this.transactions.get(idx);
		final BigDecimal tshrbal = this.shrBalance.get(idx);
		final BigDecimal price = txn.security.getPriceForDate(d).getPrice();

		return price.multiply(tshrbal);
	}

	public BigDecimal getSharesForDate(QDate date) {
		int idx = GenericTxn.getLastTransactionIndexOnOrBeforeDate(this.transactions, date);

		return (idx >= 0) ? shrBalance.get(idx) : BigDecimal.ZERO;
	}

	public void getPositionForDate(QDate d) {
		// FIXME implement getPositionForDate
		// throw new Exception("not implemented");
	}

	// name;numtx[;txid;shrbal]
	public String formatForSave(Statement stat) {
		final int numtx = this.transactions.size();
		String s = this.security.getName() + ";" + numtx;

		for (int ii = 0; ii < numtx; ++ii) {
			final InvestmentTxn t = this.transactions.get(ii);
			final int txidx = stat.transactions.indexOf(t);
			assert txidx >= 0;
			final BigDecimal bal = this.shrBalance.get(ii);

			s += String.format(";%d;%f", txidx, bal);
		}

		return s;
	}
}