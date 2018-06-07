package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Lot {
	public final int acctid;
	public final int secid;
	public final BigDecimal shares;
	public final BigDecimal costBasis;

	/** When created (via purchase, grant, transfer, leftover from sale, etc) */
	public final QDate createDate;

	/** Lot this is derived from if not original purchase/grant */
	public final Lot sourceLot;
	public final List<Lot> childLots = new ArrayList<Lot>();

	public final InvestmentTxn createTransaction;
	public InvestmentTxn expireTransaction;

	public boolean addshares = true;

	public Lot(int acctid, QDate date, int secid, BigDecimal shares, BigDecimal cost, InvestmentTxn txn) {
		this.createDate = date;
		this.sourceLot = null;
		this.acctid = acctid;
		this.secid = secid;
		this.shares = shares;
		this.costBasis = cost;
		this.createTransaction = txn;
		this.expireTransaction = null;
	}

	public Lot(Lot srcLot, BigDecimal shares, InvestmentTxn txn) {
		this.createDate = srcLot.createDate;
		this.sourceLot = srcLot;
		this.sourceLot.childLots.add(this);
		this.acctid = srcLot.acctid;
		this.secid = srcLot.secid;
		this.shares = shares;
		this.costBasis = shares.multiply(srcLot.getPrice());
		this.createTransaction = txn;
		this.expireTransaction = null;

		srcLot.expireTransaction = txn;
	}

	public Lot(Lot srcLot, InvestmentTxn txn) {
		this.createDate = srcLot.createDate;
		this.sourceLot = srcLot;
		this.sourceLot.childLots.add(this);
		this.acctid = srcLot.acctid;
		this.secid = srcLot.secid;
		this.shares = srcLot.shares.multiply(txn.getSplitRatio());
		this.costBasis = srcLot.costBasis;
		this.createTransaction = txn;
		this.expireTransaction = null;

		srcLot.expireTransaction = txn;
	}

	public BigDecimal getPrice() {
		return this.costBasis.divide(this.shares);
	}

	public String toString() {
		String s = (this.expireTransaction != null) ? "Lot-exp(" : "Lot(";

		s += this.createDate.longString;
		if (this.sourceLot == null) {
			s += "(" + this.createTransaction.getAction() + ")";
		}

		if ((this.expireTransaction != null)) {
			s += "-" + this.expireTransaction.getDate().longString;
			s += "(" + this.expireTransaction.getAction() //
					+ " " + Common.formatAmount3(this.expireTransaction.getShares()) //
					+ ")";
		}
		s += ", " + Security.getSecurity(this.secid).getSymbol();
		s += ", " + Common.formatAmount3(this.shares).trim();

		s += ")";

		return s;
	}
}
