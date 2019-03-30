package moneymgr.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import moneymgr.util.Common;

// TODO Lot should have more comprehensive info (e.g. option grant basis vs time)
/** This class represents tax basis info for a lot or lots. */
public class BasisInfo {
	public List<Lot> lots = new ArrayList<Lot>();
	public BigDecimal totalShares = BigDecimal.ZERO;
	public BigDecimal totalCost = BigDecimal.ZERO;
	public BigDecimal averagePrice = BigDecimal.ZERO;

	/** Add info for an additional lot */
	public void addLot(Lot lot) {
		if (this.lots.contains(lot)) {
			return;
		}

		this.lots.add(lot);
		this.totalShares = this.totalShares.add(lot.shares);
		this.totalCost = this.totalCost.add(lot.getCostBasis());

		if (this.totalShares.signum() > 0) {
			this.averagePrice = this.totalCost.divide(this.totalShares, RoundingMode.HALF_UP);
		}
	}

	public String toString() {
		return String.format( //
				"\nTotal shares: %s\nAvg price: %s\nCostBasis: %s\n", //
				Common.formatAmount3(this.totalShares), //
				Common.formatAmount(this.averagePrice), //
				Common.formatAmount(this.totalCost));
	}
}
