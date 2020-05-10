package moneymgr.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import moneymgr.model.SecurityPosition.PositionInfo;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/**
 * History for a group of securities, either global, or for a single account or
 * statement
 */
public class SecurityPortfolio {
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
		Security sec = txn.getSecurity();
		if (sec != null) {
			SecurityPosition pos = getPosition(sec);
			pos.addTransaction((InvestmentTxn) txn);
		}
	}

	/** Remove a transaction, update the security position it affects, if any */
	public void removeTransaction(GenericTxn txn) {
		Security sec = txn.getSecurity();
		if (sec != null) {
			SecurityPosition pos = getPosition(sec);
			pos.removeTransaction((InvestmentTxn) txn);
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
			BigDecimal posval = pos.getValueForDate(d);
			portValue = portValue.add(posval);
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

	public QDate getFirstTransactionDate() {
		QDate ret = null;

		for (SecurityPosition pos : this.positions) {
			QDate date = pos.getFirstTransactionDate();

			if ((date != null) && ((ret == null) || (date.compareTo(ret) < 0))) {
				ret = date;
			}
		}

		return ret;
	}

	private List<QDate> getAllTransactionDates() {
		Set<QDate> dateset = new HashSet<QDate>();
		for (SecurityPosition pos : this.positions) {
			for (InvestmentTxn tx : pos.getTransactions()) {
				dateset.add(tx.getDate());
			}
		}

		List<QDate> dates = new ArrayList<QDate>(dateset);
		Collections.sort(dates);

		return dates;
	}

	public String matches(SecurityPortfolio other) {
		QDate d1 = getFirstTransactionDate();
		QDate d2 = other.getFirstTransactionDate();
		List<QDate> dates1 = getAllTransactionDates();
		List<QDate> dates2 = other.getAllTransactionDates();

		if ((this.positions.size() != other.positions.size()) //
				|| !((d1 == d2) || d1.equals(d2)) //
				|| (dates1.size() != dates2.size()) //
		) {
			return "basicInfo";
		}

		for (QDate date : dates1) {
			Map<Security, PositionInfo> pp1 = getOpenPositionsForDate(date);
			Map<Security, PositionInfo> pp2 = other.getOpenPositionsForDate(date);
			BigDecimal v1 = getPortfolioValueForDate(date);
			BigDecimal v2 = other.getPortfolioValueForDate(date);

			if (pp1.size() != pp2.size()) {
				return String.format("openpositions on %s", date.toString());
			}

			for (Security sec : pp1.keySet()) {
				PositionInfo pi1 = pp1.get(sec);
				PositionInfo pi2 = pp2.get(sec);

				if (!pi1.equals(pi1)) {
					return String.format("position(%s) (%s vs %s)", //
							date.toString(), pi1.toString(), pi2.toString());
				}
			}

			if (!Common.isEffectivelyEqual(v1, v2)) {
				getPortfolioValueForDate(date);
				other.getPortfolioValueForDate(date);

				return String.format("value %d (%d/%s vs %d/%s)", date.toString(), //
						Common.formatAmount(v1).trim(), v1.toString(), //
						Common.formatAmount(v2).trim(), v2.toString());
			}
		}

		return null;
	}
}
