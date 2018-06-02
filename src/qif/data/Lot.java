package qif.data;

import java.math.BigDecimal;
import java.util.Date;

public class Lot {
	/** When created (via purchase, grant, transfer, leftover from sale, etc) */
	public final Date createDate;
	/** Lot this is derived from if not original purchase/grant */
	public final Lot sourceLot;
	/** Date this lot ceases to exist via sale, expiration, transfer, etc */
	public Date expireDate;
	
	public final int secid;
	public final BigDecimal shares;
	public final Date purchaseDate;
	public final BigDecimal costBasis;

	public final InvestmentTxn transaction;

	public Lot(Date date, int secid, BigDecimal shares, BigDecimal cost, InvestmentTxn txn) {
		this.createDate = date;
		this.sourceLot = null;
		this.expireDate = null;
		this.secid = secid;
		this.shares = shares;
		this.purchaseDate = date;
		this.costBasis = cost;
		this.transaction = txn;
	}

	public Lot(Lot src, BigDecimal shares, BigDecimal cost, InvestmentTxn txn) {
		this.createDate = null;
		this.sourceLot = src;
		this.expireDate = null;
		this.secid = src.secid;
		this.shares = shares;
		this.purchaseDate = src.purchaseDate;
		this.costBasis = cost;
		this.transaction = txn;
	}

	public BigDecimal getPrice() {
		return this.costBasis.divide(this.shares);
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
