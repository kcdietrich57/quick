package qif.data;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BasisInfo {
	public BigDecimal totalShares = BigDecimal.ZERO;
	public BigDecimal totalCost = BigDecimal.ZERO;
	public BigDecimal averagePrice = BigDecimal.ZERO;

	public void addLot(Lot lot) {
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
