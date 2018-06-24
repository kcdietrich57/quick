package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

// This can be global information, or for a single account or statement
public class SecurityPortfolio {

	public static final SecurityPortfolio portfolio = new SecurityPortfolio();

	public List<SecurityPosition> positions;

	public SecurityPortfolio() {
		this.positions = new ArrayList<SecurityPosition>();
	}

	/** Build a clone of another portfolio object, minus transactions */
	public SecurityPortfolio(SecurityPortfolio other) {
		this();

		if (other != null) {
			for (SecurityPosition p : other.positions) {
				this.positions.add(new SecurityPosition(p));
			}
		}
	}

	public void addTransaction(InvestmentTxn itx) {
		if (itx.security == null) {
			return;
		}

		SecurityPosition p = getPosition(itx.security);

		p.shares = (itx.getAction() == TxAction.STOCKSPLIT) //
				? p.shares.multiply(itx.getSplitRatio()) //
				: p.shares.add(itx.getShares());

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
		SecurityPortfolio dport = stat.getPortfolioDelta();
		SecurityPortfolio prevPort = (stat.prevStatement != null) //
				? stat.prevStatement.holdings //
				: new SecurityPortfolio();

		for (SecurityPosition pos : this.positions) {
			SecurityPosition dpos = dport.findPosition(pos.security);

			BigDecimal prevbal = prevPort.getPosition(pos.security).shares;
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
	 * @param secid
	 * @return The position
	 */
	public SecurityPosition getPosition(int secid) {
		Security sec = Security.getSecurity(secid);
		SecurityPosition pos = findPosition(sec);

		if (pos == null) {
			pos = new SecurityPosition(sec);
			this.positions.add(pos);
		}

		return pos;
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

			if (Common.isEffectivelyZero(p.shares)) {
				iter.remove();
			}
		}
	}

	public void getPositionsForDate(Date d) {
		for (final SecurityPosition pos : this.positions) {
			pos.getPositionForDate(d);
		}
	}

	public BigDecimal getPortfolioValueForDate(QDate d) {
		BigDecimal portValue = BigDecimal.ZERO;

		for (final SecurityPosition pos : this.positions) {
			portValue = portValue.add(pos.getSecurityPositionValueForDate(d));
		}

		return portValue;
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof SecurityPortfolio)) {
			return false;
		}

		SecurityPortfolio other = (SecurityPortfolio) obj;
		boolean match = true;

		for (SecurityPosition pos : this.positions) {
			SecurityPosition opos = other.findPosition(pos.security);
			BigDecimal oshares = (opos != null) ? opos.shares : BigDecimal.ZERO;

			if (!Common.isEffectivelyEqual(pos.shares, oshares)) {
				// System.out.println( //
				// "Security shares do not match: " + pos.security.getName()
				// + " " + Common.formatAmount3(pos.shares) //
				// + " vs " + Common.formatAmount3(oshares));
				match = false;
			}
		}

		for (SecurityPosition opos : other.positions) {
			SecurityPosition pos = findPosition(opos.security);
			BigDecimal shares = (pos != null) ? pos.shares : BigDecimal.ZERO;

			if (!Common.isEffectivelyEqual(opos.shares, shares)) {
				// System.out.println( //
				// "Security shares do not match: " + opos.security.getName()
				// + " " + Common.formatAmount3(shares) //
				// + " vs " + Common.formatAmount3(opos.shares));
				match = false;
			}
		}

		return match;
	}

	public String toString() {
		String s = "Securities Held:\n";

		int nn = 0;
		for (final SecurityPosition p : this.positions) {
			s += "  " + ++nn + ": " + p.toString() + "\n";
		}

		return s;
	}

	public boolean isEmptyForDate(QDate d) {
		for (final SecurityPosition p : this.positions) {
			final int ii = p.getTransactionIndexForDate(d);

			if ((ii >= 0) && !Common.isEffectivelyZero(p.shrBalance.get(ii))) {
				return false;
			}
		}

		return true;
	}
}
