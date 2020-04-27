package moneymgr.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import moneymgr.util.Common;
import moneymgr.util.QDate;

/**
 * Track a block of shares bought or sold as a unit<br>
 * Particular bits of relevant information:<br>
 * 1. Acquisition date (for basis/gain, see getAcquisitionDate())<br>
 * 2. Cost (what is actually paid for the shares, see getCostBasis())<br>
 * 3. Value at acquisition (determines basis + discount for options/ESPP)<br>
 * 4. Disposal date (determines short/long term gains, from disposing txn)
 */
public class Lot {
	public final int lotid;
	public final int acctid;
	public final int secid;
	public final BigDecimal shares;
	public final BigDecimal basisPrice;

	/** When created (via purchase, grant, transfer, leftover from sale, etc) */
	public final QDate createDate;

	/** Lot this is derived from if not original purchase/grant */
	private final Lot sourceLot;
	private final List<Lot> childLots;

	/** The transaction that created this lot */
	public final InvestmentTxn createTransaction;

	/** The transaction that invalidated this lot */
	private InvestmentTxn disposingTransaction;

	/**
	 * TODO unused addshares is always true<br>
	 * True if this represents adding shares to the associated account
	 */
	public boolean addshares = true;

	/**
	 * Common constructor for a lot that is not yet disposed of or transferred.<br>
	 * From purchase, transfer, split, etc.
	 *
	 * @param lotid      The lot id
	 * @param acctid     Account where the lot's shares are kept
	 * @param date       Date the lot object was created (may not be date of
	 *                   acquisition - see source lot)
	 * @param secid      The security involved
	 * @param shares     The number of shares in the lot
	 * @param basisPrice The cost basis per share of the lot
	 * @param createTxn  The transaction that created this lot
	 * @param srcLot     The lot this is derived from if not a brand new lot
	 */
	private Lot(int lotid, int acctid, QDate date, int secid, //
			BigDecimal shares, BigDecimal basisPrice, //
			InvestmentTxn createTxn, //
			Lot srcLot) {
		this.lotid = lotid;
		this.acctid = acctid;
		this.createDate = date;
		this.secid = secid;
		this.shares = shares.abs();
		this.basisPrice = basisPrice;
		this.createTransaction = createTxn;
		this.disposingTransaction = null;
		this.sourceLot = srcLot;
		this.childLots = new ArrayList<>();

		MoneyMgrModel.currModel.addLot(this);
	}

	/**
	 * Constructor for a lot that is not yet disposed of or transferred.<br>
	 * From purchase, transfer, split, etc.
	 *
	 * @param lotid      The lot id
	 * @param acctid     Account where shares are deposited
	 * @param date       Date the lot object was created (i.e. date of acquisition)
	 * @param secid      The security involved
	 * @param shares     The number of shares in the lot
	 * @param basisPrice The cost basis per share of the lot
	 * @param createTxn  The transaction that created this lot
	 */
	public Lot(int lotid, int acctid, QDate date, int secid, //
			BigDecimal shares, BigDecimal basisPrice, //
			InvestmentTxn createTxn) {
		this(lotid, acctid, date, secid, shares, basisPrice, createTxn, null);

		createTxn.lotsCreated.add(this);
	}

	/**
	 * Constructor for a lot that is not yet disposed of or transferred.<br>
	 * From purchase, transfer, split, etc.
	 *
	 * @param acctid     Account where shares are kept
	 * @param date       Date the lot object was created (i.e. date of acquisition)
	 * @param secid      The security involved
	 * @param shares     The number of shares in the lot
	 * @param basisPrice The cost basis per share of the lot
	 * @param createTxn  The transaction that created this lot
	 */
	public Lot(int acctid, QDate date, int secid, //
			BigDecimal shares, BigDecimal basisPrice, //
			InvestmentTxn createTxn) {
		this(MoneyMgrModel.currModel.nextLotId(), acctid, date, secid, shares, basisPrice, createTxn);
	}

	/**
	 * Constructor for dividing a lot for partial sale/transfer.<br>
	 * Result lot is in the same account, with size <= the source lot.
	 *
	 * @param lotid     The lot id
	 * @param srcLot    The original lot
	 * @param acctid    Account where shares are kept
	 * @param shares    The number of shares in the partial lot
	 * @param createTxn The transaction that created the partial lot
	 */
	public Lot(int lotid, Lot srcLot, int acctid, BigDecimal shares, InvestmentTxn createTxn) {
		this(lotid, acctid, srcLot.createDate, srcLot.secid, shares, //
				srcLot.getPriceBasis(), createTxn, srcLot);

		checkSufficientSrcLotShares(srcLot, shares);

		this.sourceLot.childLots.add(this);
		createTxn.lotsCreated.add(this);

		if (!createTxn.lotsDisposed.contains(srcLot)) {
			createTxn.lotsDisposed.add(srcLot);
		}
	}

	/**
	 * Constructor for dividing a lot for partial sale/transfer.<br>
	 * Result lot is in the same account, with size <= the source lot.
	 *
	 * @param srcLot The original lot
	 * @param acctid Account where shares are kept
	 * @param shares The number of shares in the partial lot
	 * @param txn    The transaction that created the partial lot
	 */
	public Lot(Lot srcLot, int acctid, BigDecimal shares, InvestmentTxn txn) {
		this(MoneyMgrModel.currModel.nextLotId(), srcLot, acctid, shares, txn);
	}

	/**
	 * Constructor for a lot that has been disposed of.
	 * 
	 * @param lotid      The lot id
	 * @param srcLot     The lot in the source account
	 * @param acctid     The account containing the source lot
	 * @param createTxn  The transaction creating the lot
	 * @param disposeTxn The transaction disposing of the lot
	 */
	public Lot(int lotid, Lot srcLot, int acctid, InvestmentTxn createTxn, InvestmentTxn disposeTxn) {
		this(lotid, createTxn.getAccountID(), //
				((srcLot != null) ? srcLot.createDate : createTxn.getDate()), //
				srcLot.secid, //
				srcLot.shares.multiply(createTxn.getSplitRatio()), //
				srcLot.basisPrice, createTxn, srcLot);

		// The new lot in the destination is derived from the source lot
		this.sourceLot.childLots.add(this);

		createTxn.lotsCreated.add(this);

		if (disposeTxn != null) {
			disposeTxn.lotsDisposed.add(srcLot);

			// Dispose of the original lot if not already done
			if ((srcLot.disposingTransaction == null) //
					// TODO check createTxn.xferTxn?
					&& (srcLot.acctid == disposeTxn.getAccountID())) {
				srcLot.disposingTransaction = disposeTxn;
			}
		}
	}

	/**
	 * Constructor for a lot that has been disposed of.
	 * 
	 * @param srcLot     The lot in the source account
	 * @param acctid     The account containing the source lot
	 * @param createTxn  The transaction creating the lot
	 * @param disposeTxn The transaction disposing of the lot
	 */
	public Lot(Lot srcLot, int acctid, InvestmentTxn createTxn, InvestmentTxn disposeTxn) {
		this(MoneyMgrModel.currModel.nextLotId(), srcLot, acctid, createTxn, disposeTxn);
	}

	public Lot getSourceLot() {
		return this.sourceLot;
	}

	public List<Lot> getChildLots() {
		return Collections.unmodifiableList(this.childLots);
	}

	public InvestmentTxn getDisposingTransaction() {
		return this.disposingTransaction;
	}

	public void setDisposingTransaction(InvestmentTxn txn) {
		if (txn != null && txn.getAccountID() != this.acctid) {
			System.out.println();
		}

		this.disposingTransaction = txn;
	}

	public boolean isDerivedFrom(Lot other) {
		for (Lot lot = this; lot != null; lot = lot.sourceLot) {
			if (lot == other) {
				return true;
			}
		}

		return false;
	}

	/** Return whether this lot still exists */
	public boolean isOpen() {
		return this.disposingTransaction == null;
	}

	/** Return when this lot's shares were originally acquired */
	public QDate getAcquisitionDate() {
		Lot l = this;

		while (l.sourceLot != null) {
			l = l.sourceLot;
		}

		return l.createDate;
	}

	/** Get the original price per share of this lot */
	public BigDecimal getPriceBasis() {
		return this.basisPrice;
	}

	/** Get the original cost of the shares in this lot */
	public BigDecimal getCostBasis() {
		return this.basisPrice.multiply(this.shares);
	}

	/**
	 * Split this lot to create one lot with a desired number of shares, and another
	 * lot with the remainder. Generally for the purposes of transferring or selling
	 * part of a lot.
	 *
	 * @param txn       The transaction requiring the new lot
	 * @param reqShares The number of shares required
	 * @return A new lot of the desired size
	 */
	public Lot[] split(InvestmentTxn txn, BigDecimal reqShares) {
		BigDecimal remShares = this.shares.subtract(reqShares);

		Lot remLot = new Lot(this, this.acctid, remShares, txn);
		Lot retLot = new Lot(this, this.acctid, reqShares, txn);

		setDisposingTransaction(txn);

		return new Lot[] { retLot, remLot };
	}

	/** Verify that child lots' shares do not exceed the total amount */
	private void checkSufficientSrcLotShares(Lot lot, BigDecimal additionalShares) {
		BigDecimal sum = BigDecimal.ZERO;

		for (Lot child : lot.childLots) {
			sum = sum.add(child.shares);
		}

		if (sum.add(additionalShares).compareTo(lot.shares) > 0) {
			Common.reportError("Oops, too much child lots");
		}
	}

	public String toString() {
		String ret = "[" + this.lotid + //
				((this.sourceLot != null) ? (":" + this.sourceLot.lotid) : "") + //
				"] " + //
				this.acctid + " " + //
				getAcquisitionDate().toString() + " " + //
				MoneyMgrModel.currModel.getSecurity(this.secid).getSymbol() + " " + //
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

	public boolean matches(Lot other) {
		if (this.childLots.size() != other.childLots.size()) {
			return false;
		}

		for (int idx = 0; idx < this.childLots.size(); ++idx) {
			Lot child = this.childLots.get(idx);
			Lot ochild = other.childLots.get(idx);

			if (child.lotid != ochild.lotid) {
				return false;
			}
		}

		if ((this.disposingTransaction != null) != (other.disposingTransaction != null)) {
			return false;
		}

		if ((this.disposingTransaction != null) //
				&& (this.disposingTransaction.txid != other.disposingTransaction.txid)) {
			return false;
		}

		return this.acctid == other.acctid //
				&& this.addshares == other.addshares //
				&& this.secid == other.secid //
				&& Common.isEffectivelyEqual(this.shares, other.shares) //
				&& Common.isEffectivelyEqual(this.basisPrice, other.basisPrice) //
				&& this.createTransaction.txid == other.createTransaction.txid;
	}
}
