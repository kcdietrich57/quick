package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import qif.data.SimpleTxn.Action;

// This can be global information, or for a single account or statement
class SecurityPortfolio {
	public List<SecurityPosition> positions;

	/** Set if this represents a PIT (i.e. statement). Null otherwise. */
	public Date date;

	public SecurityPortfolio() {
		this((Date) null);
	}

	public SecurityPortfolio(SecurityPortfolio other) {
		this(other.date);

		for (final SecurityPosition p : other.positions) {
			this.positions.add(new SecurityPosition(p));
		}
	}

	public SecurityPortfolio(Date date) {
		this.positions = new ArrayList<SecurityPosition>();
		this.date = date;
	}

	public void addTransaction(InvestmentTxn itx) {
		if (itx.security == null) {
			return;
		}

		final SecurityPosition p = getPosition(itx.security);
		if (itx.getAction() == Action.STOCKSPLIT) {
			p.shares = p.shares.multiply(itx.getSplitRatio());
		} else {
			p.shares = p.shares.add(itx.getShares());
		}
		p.transactions.add(itx);
		p.shrBalance.add(p.shares);
	}

	/**
	 * Build state from transactions
	 *
	 * @param stat
	 *            Statement containing my transactions
	 */
	public void captureTransactions(Statement stat) {
		final SecurityPortfolio dport = stat.getPortfolioDelta();
		final SecurityPortfolio prevPort = (stat.prevStatement != null) //
				? stat.prevStatement.holdings //
				: new SecurityPortfolio();

		for (final SecurityPosition pos : this.positions) {
			final SecurityPosition dpos = dport.findPosition(pos.security);

			final BigDecimal prevbal = prevPort.getPosition(pos.security).shares;
			pos.setTransactions(dpos.transactions, prevbal);
		}
	}

	/**
	 * Find a position for a security, if it exists
	 *
	 * @param sec
	 *            The security
	 * @return The position, null if nonexistent
	 */
	public SecurityPosition findPosition(Security sec) {
		for (final SecurityPosition pos : this.positions) {
			if (pos.security == sec) {
				return pos;
			}
		}

		return null;
	}

	/**
	 * Find a position for a security. Create it if it does not exist.
	 *
	 * @param sec
	 *            The security
	 * @return The position
	 */
	public SecurityPosition getPosition(Security sec) {
		SecurityPosition pos = findPosition(sec);

		if (pos == null) {
			pos = new SecurityPosition(sec);
			this.positions.add(pos);
		}

		return pos;
	}

	public void purgeEmptyPositions() {
		for (final Iterator<SecurityPosition> iter = this.positions.iterator(); iter.hasNext();) {
			final SecurityPosition p = iter.next();

			if (Common.isEffectivelyEqual(p.shares, BigDecimal.ZERO)) {
				iter.remove();
			}
		}
	}

	public BigDecimal getPortfolioValueForDate(Date d) {
		BigDecimal portValue = BigDecimal.ZERO;

		for (final SecurityPosition pos : this.positions) {
			portValue = portValue.add(pos.getSecurityPositionValueForDate(d));
		}

		return portValue;
	}

	public String toString() {
		String s = "Securities Held:\n";

		int nn = 0;
		for (final SecurityPosition p : this.positions) {
			s += "  " + ++nn + ": " + p.toString() + "\n";
		}

		return s;
	}
}

class SecurityPosition {
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

	/**
	 * Copy constructor - does not copy transaction information
	 *
	 * @param other
	 */
	public SecurityPosition(SecurityPosition other) {
		this(other.security, other.shares);
	}

	public void setTransactions(List<InvestmentTxn> txns, BigDecimal startBal) {
		Collections.sort(txns, (t1, t2) -> t1.getDate().compareTo(t2.getDate()));

		this.transactions.clear();
		this.transactions.addAll(txns);
		this.shrBalance.clear();

		for (final InvestmentTxn t : this.transactions) {
			startBal = startBal.add(t.getShares());
			this.shrBalance.add(startBal);
		}
	}

	public String toString() {
		String s = String.format( //
				"%-20s   %10.3f shrs  %d txns", //
				this.security.getName(), //
				this.shares, //
				this.transactions.size());

		if (this.value != null) {
			s += String.format("  %10.3f", this.value);
		}

		return s;
	}

	public BigDecimal getSecurityPositionValueForDate(Date d) {
		final int idx = getTransactionIndexForDate(d);
		if (idx < 0) {
			return BigDecimal.ZERO;
		}

		final InvestmentTxn txn = this.transactions.get(idx);
		final BigDecimal tshrbal = this.shrBalance.get(idx);
		final BigDecimal price = txn.security.getPriceForDate(d).price;

		return price.multiply(tshrbal);
	}

	public BigDecimal reportSecurityPositionForDate(Date d) {
		final int idx = getTransactionIndexForDate(d);
		if (idx < 0) {
			return BigDecimal.ZERO;
		}

		final InvestmentTxn txn = this.transactions.get(idx);
		final BigDecimal tshrbal = this.shrBalance.get(idx);
		final BigDecimal price = txn.security.getPriceForDate(d).price;
		final BigDecimal value = price.multiply(tshrbal);

		String nn = txn.security.getName();
		if (nn.length() > 36) {
			nn = nn.substring(0, 33) + "...";
		}
		System.out.println(String.format("    %-36s %10.3f %10.3f %10.3f", //
				nn, value, tshrbal, price));

		return value;
	}

	public int getTransactionIndexForDate(Date d) {
		final int idx = Common.findLastTransactionOnOrBeforeDate(this.transactions, d);
		if (idx < 0) {
			return -1;
		}

		final BigDecimal tshrbal = this.shrBalance.get(idx);
		if (tshrbal.compareTo(BigDecimal.ZERO) <= 0) {
			return -1;
		}

		return idx;
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
