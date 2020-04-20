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
	public static SecurityPortfolio portfolio = new SecurityPortfolio(null);

	/** For statements, this references the prev stmt's ending holdings */
	public final SecurityPortfolio prevPortfolio;

	/** Position history for each security tracked in this portfolio */
	public List<SecurityPosition> positions;

	/** Create a statement holdings object connected to previous statement */
	public SecurityPortfolio(SecurityPortfolio prev) {
		this.positions = new ArrayList<>();

		this.prevPortfolio = prev;

		if (prev != null) {
			for (SecurityPosition ppos : prev.positions) {
				if (!ppos.isEmpty() //
						|| !Common.isEffectivelyZeroOrNull(ppos.getExpectedEndingShares())) {
					SecurityPosition pos = new SecurityPosition(this, ppos.security);
					this.positions.add(pos);
				}
			}
		}
	}

	/** Reset transaction information for positions in this portfolio */
	public void initializeTransactions() {
		for (SecurityPosition pos : this.positions) {
			pos.initializeTransactions();
		}
	}

	/** Add a transaction, update the security position it affects, if any */
	public void addTransaction(GenericTxn txn) {
		if (!(txn instanceof InvestmentTxn)) {
			return;
		}

		InvestmentTxn itx = (InvestmentTxn) txn;
		if (itx.security == null) {
			return;
		}

		SecurityPosition pos = getPosition(itx.security);

		pos.addTransaction(itx);
	}

	/** Remove a transaction, update the security position it affects, if any */
	public void removeTransaction(GenericTxn txn) {
		if (!(txn instanceof InvestmentTxn)) {
			return;
		}

		InvestmentTxn itx = (InvestmentTxn) txn;
		if (itx.security == null) {
			return;
		}

		SecurityPosition pos = getPosition(itx.security);
		pos.removeTransaction(itx);
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

	/** Create a position for a statement, starting with the prev stmt pos */
	public void createPosition(int secid) {
		getPosition(secid);
	}

	/** Find a position for a security id. Create it if it does not exist. */
	public SecurityPosition getPosition(int secid) {
		return getPosition(MoneyMgrModel.currModel.getSecurity(secid));
	}

	/** Find a position for a security. Create it if it does not exist. */
	public SecurityPosition getPosition(Security sec) {
		SecurityPosition pos = findPosition(sec);

		if (pos == null) {
			pos = new SecurityPosition(this, sec);
			this.positions.add(pos);
		}

		return pos;
	}

	/** Remove empty positions from this portfolio */
	public void purgeEmptyPositions() {
		Iterator<SecurityPosition> iter = this.positions.iterator();

		while (iter.hasNext()) {
			SecurityPosition p = iter.next();

			if (Common.isEffectivelyZero(p.getEndingShares())) {
				iter.remove();
			}
		}
	}

	/**
	 * Get all non-empty positions for a given date.
	 * 
	 * @return Map Security to position
	 */
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

	/**
	 * Get non-empty positions for a given date/security.
	 * 
	 * @return Map acct to position
	 */
	public Map<Account, PositionInfo> getOpenPositionsForDateByAccount(Security sec, QDate d) {
		Map<Account, PositionInfo> ret = new HashMap<>();

		for (Account acct : MoneyMgrModel.currModel.getAccounts()) {
			PositionInfo values = acct.getSecurityValueForDate(sec, d);

			if (values != null) {
				ret.put(acct, values);
			}
		}

		return ret;
	}

	/** Calculate total value for a given date */
	public BigDecimal getPortfolioValueForDate(QDate d) {
		BigDecimal portValue = BigDecimal.ZERO;

		for (SecurityPosition pos : this.positions) {
			portValue = portValue.add(pos.getValueForDate(d));
		}

		return portValue;
	}

	/** TODO defunct HoldingsComparison Compares holdings to a target */
	public static class HoldingsComparison {
		private List<SecurityPosition> actualPositions = new ArrayList<>();

		/** Set the positions to compare */
		private void addPosition(SecurityPosition actual) {
			this.actualPositions.add(actual);
		}

		/** Compare desired/actual holdings for each security position */
		public boolean holdingsMatch() {
			for (SecurityPosition pos : this.actualPositions) {
				if (!Common.isEffectivelyEqual(pos.getEndingShares(), pos.getExpectedEndingShares())) {
					return false;
				}
			}

			return true;
		}

		public String toString() {
			return String.format("Match %s, %d positions", //
					Boolean.toString(holdingsMatch()), this.actualPositions.size());
		}
	}

	/** Compare this (desired) holdings to actual holdings (other) */
	public HoldingsComparison comparisonTo(SecurityPortfolio other) {
		HoldingsComparison comp = new HoldingsComparison();

		for (SecurityPosition pos : this.positions) {
			comp.addPosition(pos);
		}

//		for (SecurityPosition opos : other.positions) {
//			SecurityPosition pos = findPosition(opos.security);
//
//			if (pos == null) {
//				comp.addPosition(pos);
//			}
//		}

		return comp;
	}

	/** Check if this portfolio has no data at all */
	public boolean isEmpty() {
		for (SecurityPosition p : this.positions) {
			if (!p.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	/** Check whether this portfolio has any holdings on a given date */
	public boolean isEmptyForDate(QDate d) {
		for (SecurityPosition p : this.positions) {
			if (!p.isEmptyForDate(d)) {
				return false;
			}
		}

		return true;
	}

	public String toString() {
		String s = "Securities Held:\n";

		int nn = 0;
		for (final SecurityPosition p : this.positions) {
			if (!p.isEmpty()) {
				s += "  " + ++nn + ": " + p.toString() + "\n";
			}
		}

		return s;
	}
}
