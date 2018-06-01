package qif.data;

import java.math.BigDecimal;
import java.util.Date;

public class Lot {
	public Lot sourceLot;
	public Date purchaseDate;
	public int secid;
	public BigDecimal shares;
	public BigDecimal costBasis;
	public InvestmentTxn transaction = null;

	public Lot(Date date, int secid, BigDecimal shares, BigDecimal cost, InvestmentTxn txn) {
		this.purchaseDate = date;
		this.secid = secid;
		this.shares = shares;
		this.costBasis = cost;
		this.transaction = txn;
	}

	public Lot(Lot src, BigDecimal shares, BigDecimal cost, InvestmentTxn txn) {
		this.purchaseDate = src.purchaseDate;
		this.secid = src.secid;
		this.shares = shares;
		this.costBasis = cost;
		this.transaction = txn;
	}

	public Lot[] divide(BigDecimal dstShares, InvestmentTxn dstTxn, InvestmentTxn srcTxn) {
		Lot[] ret = new Lot[2];

		BigDecimal srcShares = this.shares.subtract(dstShares);
		BigDecimal price = this.costBasis.divide(this.shares);

		BigDecimal dstCost = dstShares.multiply(price);
		BigDecimal srcCost = this.costBasis.subtract(dstCost);

		ret[0] = new Lot(this, dstShares, dstCost, dstTxn);
		ret[1] = new Lot(this, srcShares, srcCost, dstTxn);

		return ret;
	}

	public String toString() {
		String s = "Lot(";

		s += Common.formatDate(this.purchaseDate).trim();
		s += ", '" + Security.getSecurity(this.secid).getName() + "'";
		s += ", " + Common.formatAmount3(this.shares).trim();
		s += ", cost=" + Common.formatAmount3(costBasis).trim();
		s += ",\n    tx=" + ((this.transaction != null) ? this.transaction.toString() : "NA");

		s += ")";

		if (this.sourceLot != null) {
			s += "\n  src: " + this.sourceLot.toString();
		}

		return s;
	}
}
