package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Lot {
	private static int nextlotid = 1;

	public final int lotid;
	public final int acctid;
	public final int secid;
	public final BigDecimal shares;
	public final BigDecimal basisPrice;

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

	/**
	 * Construct a lot to track share history and tax basis.<br>
	 * This may be from a sale/exercise, stock split, remainder from a partial sale,
	 * or simple transfer between accounts.
	 * 
	 * @param acctid     Account where shares are deposited
	 * @param date       Date the lot object was created (may not be date of
	 *                   acquisition)
	 * @param secid      The security
	 * @param shares     The number of shares in the lot
	 * @param basisPrice The cost basis of the lot
	 * @param createTxn  The transaction that created this lot
	 * @param srcLot     The lot this is derived from if not a brand new lot
	 */
	private Lot(int acctid, QDate date, int secid, //
			BigDecimal shares, BigDecimal basisPrice, //
			InvestmentTxn createTxn, //
			Lot srcLot) {
		this.lotid = nextlotid++;

		this.acctid = acctid;
		this.createDate = date;
		this.secid = secid;
		this.shares = shares.abs();
		this.basisPrice = basisPrice;
		this.createTransaction = createTxn;
		this.expireTransaction = null;
		this.sourceLot = srcLot;
		this.childLots = new ArrayList<Lot>();
	}

	/**
	 * Constructor for new shares added via BUY, etc
	 * 
	 * @param acctid     Account where shares are deposited
	 * @param date       Date the lot object was created (i.e. date of acquisition)
	 * @param secid      The security
	 * @param shares     The number of shares in the lot
	 * @param basisPrice The cost basis of the lot
	 * @param txn        The transaction that created this lot
	 */
	public Lot(int acctid, QDate date, int secid, //
			BigDecimal shares, BigDecimal basisPrice, //
			InvestmentTxn txn) {
		this(acctid, date, secid, shares, basisPrice, txn, null);

		txn.lotsCreated.add(this);
	}

	/**
	 * Constructor for the dividing a lot for partial sale
	 * 
	 * @param srcLot The lot this is derived from if not a brand new lot
	 * @param acctid Account where shares are deposited
	 * @param shares The number of shares in the lot
	 * @param txn    The transaction that created this lot
	 */
	public Lot(Lot srcLot, int acctid, BigDecimal shares, InvestmentTxn txn) {
		this(acctid, srcLot.createDate, srcLot.secid, shares, //
				srcLot.getPriceBasis(), txn, srcLot);

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
				srcLot.basisPrice, dstTxn, srcLot);

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

	public BigDecimal getPriceBasis() {
		return this.basisPrice;
	}

	public BigDecimal getCostBasis() {
		return this.basisPrice.multiply(this.shares);
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
				Security.getSecurity(this.secid).getSymbol() + " " + //
				Common.formatAmount3(this.shares) + " " + //
				Common.formatAmount(getCostBasis());

		// TODO will this ever be null?
//		ret += ((this.createTransaction != null) //
//				? this.createTransaction.getAction().toString() //
//				: "") //
//				+ "-" //
//				+ ((this.expireTransaction != null) //
//						? this.expireTransaction.getAction().toString() //
//						: "");
//		if (!this.childLots.isEmpty()) {
//			ret += " (" + this.childLots.size() + " children)";
//		}
//
//		if (this.sourceLot != null) {
//			ret += "\n  <- " + this.sourceLot.toString();
//		}

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

	public static BasisInfo getBasisInfo(List<Lot> lots) {
		BasisInfo info = new BasisInfo();

		for (Lot lot : lots) {
			info.addLot(lot);
		}

		return info;
	}
}
