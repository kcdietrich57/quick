package qif.data;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

	/** The transaction that created this lot */
	public final InvestmentTxn createTransaction;

	/** The transaction that invalidated this lot */
	public InvestmentTxn expireTransaction;

	/**
	 * TODO unused<br>
	 * True if this represents adding shares to the associated account
	 */
	public boolean addshares = true;

	/** Constructor for new shares added via BUY, etc */
	public Lot(int acctid, QDate date, int secid, BigDecimal shares, BigDecimal cost, InvestmentTxn txn) {
		this.createDate = date;
		this.sourceLot = null;
		this.acctid = acctid;
		this.secid = secid;
		this.shares = shares;
		this.costBasis = cost;

		this.createTransaction = txn;
		this.expireTransaction = null;

		txn.lotsCreated.add(this);
	}

	/** Constructor for the remainder from the disposal of part of a lot */
	public Lot(Lot srcLot, int acctid, BigDecimal shares, InvestmentTxn txn) {
		checkChildLots(srcLot, shares);

		this.createDate = srcLot.createDate;
		this.sourceLot = srcLot;
		this.sourceLot.childLots.add(this);
		this.acctid = acctid;
		this.secid = srcLot.secid;
		this.shares = shares.abs();
		this.costBasis = shares.multiply(srcLot.getPrice());

		this.createTransaction = txn;
		this.expireTransaction = null;

		txn.lotsCreated.add(this);
		txn.lotsDisposed.add(srcLot);
	}

	/** Constructor for the destination lot for a transfer/split transaction */
	public Lot(Lot srcLot, int acctid, InvestmentTxn srcTxn, InvestmentTxn dstTxn) {
		this.createDate = srcLot.createDate;
		this.sourceLot = srcLot;
		this.sourceLot.childLots.add(this);
		this.acctid = acctid;
		this.secid = srcLot.secid;
		this.shares = srcLot.shares.multiply(dstTxn.getSplitRatio());
		this.costBasis = srcLot.costBasis;

		// Dispose of the original lot if not already done
		if ((srcTxn != null) && (srcLot.expireTransaction == null)) {
			srcLot.expireTransaction = srcTxn;
		}

		this.createTransaction = dstTxn;
		this.expireTransaction = null;

		dstTxn.lotsCreated.add(this);
		dstTxn.lotsDisposed.add(srcLot);
	}

	public QDate getAcquisitionDate() {
		Lot l = this;

		while (l.sourceLot != null) {
			l = l.sourceLot;
		}

		return l.createDate;
	}

	/**
	 * Split this lot to create one lot with a desired number of shares, and another
	 * lot with the remainder.
	 * 
	 * @param txn    The transaction requiring the new lot
	 * @param shares The number of shares required
	 * @return A new lot of the desired size
	 */
	public Lot[] split(InvestmentTxn txn, BigDecimal shares) {
		Lot remainderLot = new Lot(this, this.acctid, this.shares.subtract(shares), txn);
		Lot returnLot = new Lot(this, this.acctid, shares, txn);

		this.expireTransaction = txn;
		txn.lotsDisposed.add(this);

		return new Lot[] { returnLot, remainderLot };
	}

	public BigDecimal getPrice() {
		return this.costBasis.divide(this.shares, 3, RoundingMode.HALF_UP);
	}

	void checkChildLots(Lot lot, BigDecimal additionalShares) {
		BigDecimal sum = BigDecimal.ZERO;

		for (Lot child : lot.childLots) {
			sum = sum.add(child.shares);
		}

		if (sum.add(additionalShares).compareTo(lot.shares) > 0) {
			Common.reportError("Oops, too much child lots");
		}
	}

	public String toString() {
		String ret = "*** " + this.acctid + " " + //
		// Common.formatDate(this.createDate.toDate()) + expireDate + " " + //
		// Security.getSecurity(this.secid).getSymbol() + " " + //
				Common.formatAmount3(this.shares) + " " //
				+ ((this.createTransaction != null) ? "C" : "-") //
				+ ((this.expireTransaction != null) ? "X" : "-");
		if (!this.childLots.isEmpty()) {
			ret += "\n  -> " + this.childLots.toString();
		}

		return ret;

//		String expireDate = ((this.expireTransaction != null) //
//				? ("-" + Common.formatDate(this.expireTransaction.getDate().toDate())) //
//				: "");
//
//		String s = (this.expireTransaction != null) ? "Lot-exp(" : "Lot(";
//
//		s += this.createDate.longString;
//		if (this.sourceLot == null) {
//			s += "(" + this.createTransaction.getAction() + ")";
//		}
//
//		if ((this.expireTransaction != null)) {
//			s += "-" + this.expireTransaction.getDate().longString;
//			s += "(" + this.expireTransaction.getAction() //
//					+ " " + Common.formatAmount3(this.expireTransaction.getShares()).trim() //
//					+ ")";
//		}
//
//		s += ", " + Security.getSecurity(this.secid).getSymbol();
//		s += ", " + Common.formatAmount3(this.shares).trim();
//
//		s += ") [";
//
//		for (Lot child : this.childLots) {
//			s += " " + Common.formatAmount3(child.shares).trim();
//		}
//
//		s += " ]";
	}
}
