package qif.data;

import java.math.BigDecimal;

public class Lot {
	/** When created (via purchase, grant, transfer, leftover from sale, etc) */
	public final QDate createDate;
	/** Lot this is derived from if not original purchase/grant */
	public final Lot sourceLot;
	/** Date this lot ceases to exist via sale, expiration, transfer, etc */
	public QDate expireDate;

	public final int acctid;
	public final int secid;
	public final BigDecimal shares;
	public final BigDecimal costBasis;

	public final InvestmentTxn transaction;

	public boolean addshares = true;

	public Lot(int acctid, QDate date, int secid, BigDecimal shares, BigDecimal cost, InvestmentTxn txn) {
		this.createDate = date;
		this.sourceLot = null;
		this.expireDate = null;
		this.acctid = acctid;
		this.secid = secid;
		this.shares = shares;
		this.costBasis = cost;
		this.transaction = txn;
	}

	public Lot(Lot srcLot, BigDecimal shares, InvestmentTxn txn) {
		this.createDate = srcLot.createDate;
		this.sourceLot = srcLot;
		this.expireDate = null;
		this.acctid = srcLot.acctid;
		this.secid = srcLot.secid;
		this.shares = shares;
		this.costBasis = shares.multiply(srcLot.getPrice());
		this.transaction = txn;
	}

	public Lot(Lot srcLot, InvestmentTxn txn) {
		this.createDate = srcLot.createDate;
		this.sourceLot = srcLot;
		this.expireDate = null;
		this.acctid = srcLot.acctid;
		this.secid = srcLot.secid;
		this.shares = srcLot.shares.multiply(txn.getSplitRatio());
		this.costBasis = srcLot.costBasis;
		this.transaction = txn;
	}

	public BigDecimal getPrice() {
		return this.costBasis.divide(this.shares);
	}

	public String toString() {
		String s = "Lot(";

		s += this.createDate.toString().trim();
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
