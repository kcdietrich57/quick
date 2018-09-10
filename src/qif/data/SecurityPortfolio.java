package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
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

		SecurityPosition pos = getPosition(itx.security);

		pos.endingShares = (itx.getAction() == TxAction.STOCKSPLIT) //
				? pos.endingShares.multiply(itx.getSplitRatio()) //
				: pos.endingShares.add(itx.getShares());

		pos.transactions.add(itx);
		pos.shrBalance.add(pos.endingShares);
	}

	/**
	 * Build state from transactions
	 *
	 * @param stat Statement containing my transactions
	 */
	public void captureTransactions(Statement stat) {
		SecurityPortfolio dport = stat.getPortfolioDelta();
		SecurityPortfolio prevPort = (stat.prevStatement != null) //
				? stat.prevStatement.holdings //
				: new SecurityPortfolio();

		for (SecurityPosition pos : this.positions) {
			SecurityPosition dpos = dport.findPosition(pos.security);

			BigDecimal prevbal = prevPort.getPosition(pos.security).endingShares;
			pos.setTransactions(dpos.transactions, prevbal);
		}
	}

	/**
	 * Find a position for a security, if it exists
	 *
	 * @param sec The security
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
	 * @param sec The security
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

			if (Common.isEffectivelyZero(p.endingShares)) {
				iter.remove();
			}
		}
	}

	public void getPositionsForDate(QDate d) {
		for (SecurityPosition pos : this.positions) {
			pos.getPositionForDate(d);
		}
	}

	public BigDecimal getPortfolioValueForDate(QDate d) {
		BigDecimal portValue = BigDecimal.ZERO;

		for (final SecurityPosition pos : this.positions) {
			portValue = portValue.add(pos.getValueForDate(d));
		}

		return portValue;
	}

	public static class HoldingsComparison {
		public List<SecurityPosition> desiredPositions = new ArrayList<SecurityPosition>();
		public List<SecurityPosition> actualPositions = new ArrayList<SecurityPosition>();

		public void addPosition(SecurityPosition desired, SecurityPosition actual) {
			this.desiredPositions.add(desired);
			this.actualPositions.add(actual);
		}

		public String getSecurityName(int idx) {
			if ((idx < 0) || (idx >= this.desiredPositions.size())) {
				return "";
			}

			SecurityPosition pos = getPosition(idx);

			return pos.security.getName();
		}

		public BigDecimal getDesiredShares(int idx) {
			if ((idx < 0) || (idx >= this.desiredPositions.size())) {
				return BigDecimal.ZERO;
			}

			SecurityPosition pos = this.desiredPositions.get(idx);

			return (pos != null) ? pos.endingShares : BigDecimal.ZERO;
		}

		public BigDecimal getActualShares(int idx) {
			if ((idx < 0) || (idx >= this.actualPositions.size())) {
				return BigDecimal.ZERO;
			}

			SecurityPosition pos = this.actualPositions.get(idx);

			return (pos != null) ? pos.endingShares : BigDecimal.ZERO;
		}

		private SecurityPosition getPosition(int idx) {
			assert (idx >= 0) && (idx < this.desiredPositions.size());

			SecurityPosition pos = this.desiredPositions.get(idx);
			return (pos != null) ? pos : this.actualPositions.get(idx);
		}

		public boolean holdingsMatch() {
			for (int ii = 0; ii < this.desiredPositions.size(); ++ii) {
				SecurityPosition pos1 = this.desiredPositions.get(ii);
				SecurityPosition pos2 = this.actualPositions.get(ii);

				BigDecimal val1 = (pos1 != null) ? pos1.endingShares : BigDecimal.ZERO;
				BigDecimal val2 = (pos2 != null) ? pos2.endingShares : BigDecimal.ZERO;

				if (!Common.isEffectivelyEqual(val1, val2)) {
					return false;
				}
			}

			return true;
		}
	}

	/** Compare this (desired) holdings to actual holdings (other) */
	public HoldingsComparison comparisonTo(SecurityPortfolio other) {
		HoldingsComparison comp = new HoldingsComparison();

		for (SecurityPosition pos : this.positions) {
			comp.addPosition(pos, other.findPosition(pos.security));
		}

		for (SecurityPosition opos : other.positions) {
			SecurityPosition pos = findPosition(opos.security);

			if (pos == null) {
				comp.addPosition(pos, opos);
			}
		}

		return comp;
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
			int ii = GenericTxn.getTransactionIndexByDate(p.transactions, d, true);

			if ((ii >= 0) && (ii < p.transactions.size()) //
					&& !Common.isEffectivelyZero(p.shrBalance.get(ii))) {
				return false;
			}
		}

		return true;
	}
}
