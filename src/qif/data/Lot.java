package qif.data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class Lot {
	private static int nextlotid = 1;

	public final int lotid;
	public final int acctid;
	public final int secid;
	public final BigDecimal shares;
	public final BigDecimal costBasis;

	/** When created (via purchase, grant, transfer, leftover from sale, etc) */
	public final QDate createDate;

	/** Lot this is derived from if not original purchase/grant */
	public final Lot sourceLot;
	public final List<Lot> childLots;

	/** The transaction that created this lot */
	public final InvestmentTxn createTransaction;

	/** The transaction that invalidated this lot */
	public InvestmentTxn expireTransaction;

	/**
	 * TODO unused<br>
	 * True if this represents adding shares to the associated account
	 */
	public boolean addshares = true;

	private Lot(int acctid, QDate date, int secid, //
			BigDecimal shares, BigDecimal cost, //
			InvestmentTxn createTxn, // InvestmentTxn expireTxn, //
			Lot srcLot) {
		this.lotid = nextlotid++;

		this.acctid = acctid;
		this.createDate = date;
		this.secid = secid;
		this.shares = shares.abs();
		this.costBasis = cost;
		this.createTransaction = createTxn;
		this.expireTransaction = null;
		this.sourceLot = srcLot;
		this.childLots = new ArrayList<Lot>();
	}

	/** Constructor for new shares added via BUY, etc */
	public Lot(int acctid, QDate date, int secid, //
			BigDecimal shares, BigDecimal cost, //
			InvestmentTxn txn) {
		this(acctid, date, secid, shares, cost, txn, null);

		txn.lotsCreated.add(this);
	}

	/** Constructor for the dividing a lot for partial sale */
	public Lot(Lot srcLot, int acctid, BigDecimal shares, InvestmentTxn txn) {
		this(acctid, srcLot.createDate, srcLot.secid, shares, //
				shares.multiply(srcLot.getPrice()), txn, srcLot);

		checkChildLots(srcLot, shares);

		this.sourceLot.childLots.add(this);
		txn.lotsCreated.add(this);

		if (!txn.lotsDisposed.contains(srcLot)) {
			txn.lotsDisposed.add(srcLot);
		}
	}

	/** Constructor for the destination lot for a transfer/split transaction */
	public Lot(Lot srcLot, int acctid, InvestmentTxn srcTxn, InvestmentTxn dstTxn) {
		this(dstTxn.acctid, srcLot.createDate, srcLot.secid, //
				srcLot.shares.multiply(dstTxn.getSplitRatio()), //
				srcLot.costBasis, dstTxn, srcLot);

		this.sourceLot.childLots.add(this);
		dstTxn.lotsCreated.add(this);
		srcTxn.lotsDisposed.add(srcLot);

		// Dispose of the original lot if not already done
		if (srcLot.expireTransaction == null) {
			srcLot.expireTransaction = srcTxn;
		}
	}

	public boolean isOpen() {
		return this.expireTransaction == null;
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
		String ret = "[" + this.lotid + "] " + this.acctid + " " + //
				Common.formatDate(getAcquisitionDate()) + " " + //
				// Common.formatDate(this.createDate.toDate()) + expireDate + " " + //
				// Security.getSecurity(this.secid).getSymbol() + " " + //
				Common.formatAmount3(this.shares) + " " //
				// TODO will this ever be null?
				+ ((this.createTransaction != null) //
						? this.createTransaction.getAction().toString() //
						: "") //
				+ "-" //
				+ ((this.expireTransaction != null) //
						? this.expireTransaction.getAction().toString() //
						: "");
		if (!this.childLots.isEmpty()) {
//			ret += "\n  -> " + this.childLots.toString();
			ret += " (" + this.childLots.size() + " children)";
		}

		if (this.sourceLot != null) {
			ret += "\n  <- " + this.sourceLot.toString();
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
