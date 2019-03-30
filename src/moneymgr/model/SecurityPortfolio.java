package moneymgr.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import moneymgr.model.SecurityPosition.PositionInfo;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/**
 * History for a group of securities, either global, or for a single account or
 * statement
 */
public class SecurityPortfolio {

	/** The global history of all securities */
	public static SecurityPortfolio portfolio = new SecurityPortfolio();

	/** Position history for each security tracked in this portfolio */
	public List<SecurityPosition> positions;

	public SecurityPortfolio() {
		this.positions = new ArrayList<>();
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

	/** Build state from transactions in a statement */
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

	/** Find a position for a security, if it exists */
	public SecurityPosition findPosition(Security sec) {
		for (SecurityPosition pos : this.positions) {
			if (pos.security == sec) {
				return pos;
			}
		}

		return null;
	}

	/** Find a position for a security. Create it if it does not exist. */
	public SecurityPosition getPosition(int secid) {
		return getPosition(Security.getSecurity(secid));
	}

	/** Find a position for a security. Create it if it does not exist. */
	public SecurityPosition getPosition(Security sec) {
		SecurityPosition pos = findPosition(sec);

		if (pos == null) {
			pos = new SecurityPosition(sec);
			this.positions.add(pos);
		}

		return pos;
	}

	/** Remove empty positions from this portfolio */
	public void purgeEmptyPositions() {
		Iterator<SecurityPosition> iter = this.positions.iterator();

		while (iter.hasNext()) {
			SecurityPosition p = iter.next();

			if (Common.isEffectivelyZero(p.endingShares)) {
				iter.remove();
			}
		}
	}

	/** Get all non-empty positions for a given date (map Security to position) */
	public Map<Security, PositionInfo> getOpenPositionsForDate(QDate d) {
		Map<Security, PositionInfo> ret = new HashMap<>();

		for (SecurityPosition pos : this.positions) {
			PositionInfo values = pos.getPositionForDate(d);

			if (values != null) {
				ret.put(pos.security, values);
			}
		}

		return ret;
	}

	/** Get non-empty positions for a given date/security (map acct to position) */
	public Map<Account, PositionInfo> getOpenPositionsForDateByAccount(Security sec, QDate d) {
		Map<Account, PositionInfo> ret = new HashMap<>();

		for (Account acct : Account.getAccounts()) {
			PositionInfo values = acct.getSecurityValueForDate(sec, d);

			if (values != null) {
				ret.put(acct, values);
			}
		}

		return ret;
	}

	/** Calculate value for a given date */
	public BigDecimal getPortfolioValueForDate(QDate d) {
		BigDecimal portValue = BigDecimal.ZERO;

		for (SecurityPosition pos : this.positions) {
			portValue = portValue.add(pos.getValueForDate(d));
		}

		return portValue;
	}

	/** Compares holdings to a target (e.g. for statement reconciliation) */
	public static class HoldingsComparison {
		public List<SecurityPosition> desiredPositions = new ArrayList<>();
		public List<SecurityPosition> actualPositions = new ArrayList<>();

		/** Set the positions to compare */
		public void addPosition(SecurityPosition desired, SecurityPosition actual) {
			this.desiredPositions.add(desired);
			this.actualPositions.add(actual);
		}

		public String getSecurityName(int idx) {
			SecurityPosition pos = getPosition(idx);
			return (pos != null) ? pos.security.getName() : "";
		}

		/** Compare desired/actual holdings for each security position */
		public boolean holdingsMatch() {
			for (int ii = 0; ii < this.desiredPositions.size(); ++ii) {
				if (!Common.isEffectivelyEqual(getDesiredShares(ii), getActualShares(ii))) {
					return false;
				}
			}

			return true;
		}

		public BigDecimal getDesiredShares(int idx) {
			SecurityPosition pos = this.desiredPositions.get(idx);
			return (pos != null) ? pos.endingShares : BigDecimal.ZERO;
		}

		public BigDecimal getActualShares(int idx) {
			SecurityPosition pos = this.actualPositions.get(idx);
			return (pos != null) ? pos.endingShares : BigDecimal.ZERO;
		}

		/** Return desired or actual position by index */
		private SecurityPosition getPosition(int idx) {
			SecurityPosition pos = this.desiredPositions.get(idx);
			return (pos != null) ? pos : this.actualPositions.get(idx);
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

	/** Check if this portfolio has no data at all */
	public boolean isEmpty() {
		for (SecurityPosition p : this.positions) {
			if (!p.transactions.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	/** Check whether this portfolio has any holdings on a given date */
	public boolean isEmptyForDate(QDate d) {
		for (SecurityPosition p : this.positions) {
			int ii = GenericTxn.getLastTransactionIndexOnOrBeforeDate(p.transactions, d);

			if ((ii < 0) || !Common.isEffectivelyZero(p.shrBalance.get(ii))) {
				return false;
			}
		}

		return true;
	}

	public String toString() {
		String s = "Securities Held:\n";

		int nn = 0;
		for (final SecurityPosition p : this.positions) {
			if (!p.transactions.isEmpty()) {
				s += "  " + ++nn + ": " + p.toString() + "\n";
			}
		}

		return s;
	}
}
