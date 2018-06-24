package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class SecurityPosition {
	public Security security;
	public BigDecimal shares;

	public List<InvestmentTxn> transactions;

	/** Running share balance per transaction */
	public List<BigDecimal> shrBalance;

	/** Set if this represents a PIT (i.e. statement). Null otherwise. */
	public BigDecimal value;

	public SecurityPosition(Security sec, BigDecimal shares, BigDecimal value) {
		this.security = sec;
		this.shares = shares;
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
		this(other.security, other.shares);
	}

	public void setTransactions(List<InvestmentTxn> txns, BigDecimal startBal) {
		Collections.sort(txns, (t1, t2) -> t1.getDate().compareTo(t2.getDate()));

		this.transactions.clear();
		this.transactions.addAll(txns);
		this.shrBalance.clear();

		for (InvestmentTxn t : this.transactions) {
			startBal = startBal.add(t.getShares());
			this.shrBalance.add(startBal);
		}
	}

	public String toString() {
		String s = String.format( //
				"%-20s   %s shrs  %d txns", //
				this.security.getName(), //
				Common.formatAmount3(this.shares), //
				this.transactions.size());

		if (this.value != null) {
			s += "  " + Common.formatAmount3(this.value);
		}

		return s;
	}

	public BigDecimal getSecurityPositionValueForDate(QDate d) {
		if (this.transactions.isEmpty()) {
			return this.shares.multiply(this.security.getPriceForDate(d).getPrice());
		}

		final int idx = getTransactionIndexForDate(d);
		if (idx < 0) {
			return BigDecimal.ZERO;
		}

		final InvestmentTxn txn = this.transactions.get(idx);
		final BigDecimal tshrbal = this.shrBalance.get(idx);
		final BigDecimal price = txn.security.getPriceForDate(d).getPrice();

		return price.multiply(tshrbal);
	}

	public void getPositionForDate(Date d) {

	}

	/**
	 * Return the index of the last transaction within this position on or before a
	 * given date.
	 *
	 * @param d
	 * @return The index; -1 if no such transaction exists
	 */
	public int getTransactionIndexForDate(QDate d) {
		final int idx = Common.findLastTransactionOnOrBeforeDate(this.transactions, d);
		if (idx < 0) {
			return -1;
		}

		final BigDecimal tshrbal = this.shrBalance.get(idx);

		return (Common.isEffectivelyZero(tshrbal)) //
				? -1 //
				: idx;
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