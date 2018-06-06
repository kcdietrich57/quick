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

	public SecurityPortfolio(SecurityPortfolio other) {
		this();

		for (final SecurityPosition p : other.positions) {
			this.positions.add(new SecurityPosition(p));
		}
	}

	public void addTransaction(InvestmentTxn itx) {
		if (itx.security == null) {
			return;
		}

		final SecurityPosition p = getPosition(itx.security);
		if (itx.getAction() == TxAction.STOCKSPLIT) {
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
