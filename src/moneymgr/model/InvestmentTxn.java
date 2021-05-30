package moneymgr.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import app.QifDom;
import moneymgr.io.TransactionInfo;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/** Transaction for investment account (may involve a security) */
public class InvestmentTxn extends GenericTxn {

	/** TODO TxAction not for non-investment txns? Action taken by transaction */
	private TxAction action;

	private Security security;
	private StockOption option;
	private BigDecimal price;
	private BigDecimal quantity;
	private BigDecimal commission;

	private String accountForTransfer;
	/** amountTransferred is cash only */
	private BigDecimal cashTransferred;
	private List<InvestmentTxn> xferTxns;

	/** These break down security activity by lots */
	private final List<Lot> lots;
	private final List<Lot> lotsCreated;
	private final List<Lot> lotsDisposed;

	// public String textFirstLine;

	public InvestmentTxn(int txid, int acctid) {
		super(txid, acctid);

		this.action = TxAction.OTHER;
		this.security = null;
		this.option = null;
		this.price = BigDecimal.ZERO;
		this.quantity = BigDecimal.ZERO;
		// this.textFirstLine = "";
		this.commission = null;
		this.accountForTransfer = "";
		this.cashTransferred = null;
		this.xferTxns = new ArrayList<InvestmentTxn>();

		this.lots = new ArrayList<>();
		this.lotsCreated = new ArrayList<>();
		this.lotsDisposed = new ArrayList<>();
	}

	public InvestmentTxn(int acctid) {
		this(MoneyMgrModel.currModel.createTxid(), acctid);
	}

	/** Construct a dummy transaction for a split (see LotProcessor) */
	public InvestmentTxn(int acctid, InvestmentTxn txn) {
		super(acctid);

		setDate(txn.getDate());
		setAmount(txn.getAmount());

		setRunningTotal(BigDecimal.ZERO);

		this.action = txn.action;
		this.security = txn.security;
		this.option = txn.option;
		this.price = txn.price;
		this.quantity = txn.quantity;
		this.commission = txn.commission;
		this.accountForTransfer = txn.accountForTransfer;
		this.cashTransferred = txn.cashTransferred;
		this.xferTxns = new ArrayList<InvestmentTxn>();

		this.lots = new ArrayList<>();
		this.lotsCreated = new ArrayList<>();
		this.lotsDisposed = new ArrayList<>();
	}

	public Security getSecurity() {
		return this.security;
	}

	public void setSecurity(Security sec) {
		this.security = sec;
	}

	public StockOption getOption() {
		return this.option;
	}
	
	public void setOption(StockOption opt) {
		this.option = opt;
	}

	public BigDecimal getQuantity() {
		return this.quantity;
	}

	public void setQuantity(BigDecimal qty) {
		this.quantity = qty;
	}

	public BigDecimal getPrice() {
		return this.price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}
	
	public BigDecimal getCommission() {
		return this.commission;
	}
	
	public void setCommission(BigDecimal commission) {
		this.commission = commission;
	}
	
	public BigDecimal getCashTransferred() {
		return this.cashTransferred;
	}
	
	public void setCashTransferred(BigDecimal cash) {
		this.cashTransferred = cash;
	}
	
	public String getAccountForTransfer() {
		return this.accountForTransfer;
	}
	
	public void setAccountForTransfer(String acctName) {
		this.accountForTransfer = acctName;
	}

	public int compareWith(TransactionInfo tuple, SimpleTxn othersimp) {
		int diff;

		diff = super.compareWith(tuple, othersimp);
		if (diff != 0) {
			return diff;
		}

		if (!(othersimp instanceof InvestmentTxn)) {
			return -1;
		}

		InvestmentTxn other = (InvestmentTxn) othersimp;

		String xact1 = Common.formatString(this.accountForTransfer, 0);
		if (xact1.startsWith("[")) {
			xact1 = xact1.substring(1, xact1.length() - 1);
		}
		String xact2 = Common.formatString(other.accountForTransfer, 0);
		if (xact2.startsWith("[")) {
			xact2 = xact2.substring(1, xact2.length() - 1);
		}

		diff = xact1.compareTo(xact2);

		if (diff != 0) {
			return diff;
		}

		return 0;
	}

	public void setAction(TxAction action) {
		this.action = action;
	}

	public TxAction getAction() {
		return this.action;
	}

	public List<InvestmentTxn> getSecurityTransferTxns() {
		return (this.xferTxns.isEmpty()) //
				? this.xferTxns //
				: Collections.unmodifiableList(this.xferTxns);
	}

	public void setSecurityTransferTxns(List<InvestmentTxn> txns) {
		if (this.xferTxns.isEmpty()) {
			this.xferTxns = new ArrayList<InvestmentTxn>();
		}

		this.xferTxns.clear();
		this.xferTxns.addAll(txns);
	}

	public void addSecurityTransferTxn(InvestmentTxn txn) {
		if (!this.xferTxns.contains(txn)) {
			this.xferTxns.add(txn);
			txn.addSecurityTransferTxn(this);
		}
	}

	public boolean isStockOptionTxn() {
		if (this.option != null) {
			if ((getAction() == TxAction.STOCKSPLIT) //
					// TODO this espp isn't mysterious at all
					|| this.option.name.equals("espp")) {
				return false;
			}

			// TODO distinguish ESPP vs OPTION
			// return true;
		}

		switch (getAction()) {
		case GRANT:
		case VEST:
		case EXPIRE:
		case EXERCISE:
		case EXERCISEX:
			return true;

		default:
			return false;
		}
	}

	public BigDecimal getShares() {
		if ((this.quantity == null) //
				|| isStockOptionTxn() //
				|| (getAction() == TxAction.STOCKSPLIT)) {
			return BigDecimal.ZERO;
		}

		return this.quantity;
	}

	/** Determine what impact, if any, this transaction has on a security */
	public ShareAction getShareAction() {
		switch (getAction()) {
		case BUY:
		case BUYX:
		case REINV_INT:
		case REINV_DIV:
		case REINV_SH:
		case REINV_LG:
			return ShareAction.NEW_SHARES;

		case SHRS_IN:
			return (this.xferTxns.isEmpty()) //
					? ShareAction.NEW_SHARES //
					: ShareAction.TRANSFER_IN;

		case SHRS_OUT:
			return (this.xferTxns.isEmpty()) //
					? ShareAction.DISPOSE_SHARES //
					: ShareAction.TRANSFER_OUT;

		case SELL:
		case SELLX:
			return ShareAction.DISPOSE_SHARES;

		case STOCKSPLIT:
			return ShareAction.SPLIT;

		case EXERCISE:
		case EXERCISEX:
			return ShareAction.NO_ACTION;

		case CASH:
		case GRANT:
		case VEST:
		case EXPIRE:
		case CONTRIBX:
		case DIV:
		case INT_INC:
		case MISC_INCX:
		case OTHER:
		case REMINDER:
		case WITHDRAWX:
		case XIN:
		case XOUT:
			break;
		}

		return ShareAction.NO_ACTION;
	}

	public boolean removesShares() {
		switch (getAction()) {
		// case EXERCISE:
		// case EXERCISEX:
		// case EXPIRE:
		case SELL:
		case SELLX:
		case SHRS_OUT:
			return true;

		// TOOD Do withdrawx and xout really remove shares?
		case WITHDRAWX:
		case XOUT:
			return true;

		default:
			return false;
		}
	}

	/** Calculate the per-share price for this transaction */
	public BigDecimal getShareCost() {
		BigDecimal shares = getShares();

		try {
			return (shares == BigDecimal.ZERO) //
					? shares //
					: this.getAmount().divide(shares, 3, RoundingMode.HALF_UP);
		} catch (Exception e) {
			return BigDecimal.ZERO;
		}
	}

	/**
	 * For a stock split transaction, calculate the split share multiplier.<br>
	 * Quicken represents splits in units of 10% (e.g. 2 for 1 is 20)
	 */
	public BigDecimal getSplitRatio() {
		if ((this.quantity == null) || (getAction() != TxAction.STOCKSPLIT)) {
			return BigDecimal.ONE;
		}

		return this.quantity.divide(BigDecimal.TEN);
	}

	/** Get lots that contain shares affected by this transaction */
	public List<Lot> getLots() {
		return Collections.unmodifiableList(this.lots);
	}

	/** Get lots that contain shares affected by this transaction */
	public List<Lot> getCreatedLots() {
		return Collections.unmodifiableList(this.lotsCreated);
	}

	/** Get lots that contain shares affected by this transaction */
	public List<Lot> getDisposedLots() {
		return Collections.unmodifiableList(this.lotsDisposed);
	}

	public void addLot(Lot lot) {
		if ((lot != null) && !this.lots.contains(lot)) {
			for (int idx = 0; idx < this.lots.size(); ++idx) {
				if (lot.lotid < this.lots.get(idx).lotid) {
					this.lots.add(idx, lot);
					return;
				}
			}

			this.lots.add(lot);
		}
	}

	public void addCreatedLot(Lot lot) {
		if ((lot != null) && !this.lotsCreated.contains(lot)) {
			for (int idx = 0; idx < this.lotsCreated.size(); ++idx) {
				if (lot.lotid < this.lotsCreated.get(idx).lotid) {
					this.lotsCreated.add(idx, lot);
					return;
				}
			}

			this.lotsCreated.add(lot);
		}
	}

	public void addDisposedLot(Lot lot) {
		if ((lot != null) && (lot.acctid != getAccountID())) {
			return;
		}

		if ((lot != null) && !this.lotsDisposed.contains(lot)) {
			for (int idx = 0; idx < this.lotsDisposed.size(); ++idx) {
				if (lot.lotid < this.lotsDisposed.get(idx).lotid) {
					this.lotsDisposed.add(idx, lot);
					return;
				}
			}

			this.lotsDisposed.add(lot);
		}
	}

	/**
	 * Get the total cash amount of a buy/sell transaction.<br>
	 * This returns the absolute value.
	 */
	public BigDecimal getBuySellAmount() {
		BigDecimal tot = super.getCashAmount();

		if (tot == null) {
			tot = getCashTransferAmount();
		}

		return (tot != null) ? tot.abs() : BigDecimal.ZERO;
	}

	public BigDecimal getCashAmount() {
		BigDecimal tot = super.getCashAmount();

		if (tot == null) {
			tot = getCashTransferAmount();
		}

		switch (getAction()) {
		case BUY:
		case WITHDRAWX:
			tot = tot.negate();
			break;

		case CASH:
			return tot;

		case SHRS_IN:
		case SHRS_OUT: // no xfer info?
		case BUYX:
		case SELLX:
		case REINV_DIV:
		case REINV_INT:
		case REINV_LG:
		case REINV_SH:
		case GRANT:
		case VEST:
		case EXERCISEX:
		case EXPIRE:
		case STOCKSPLIT:
			// No net cash change
			return BigDecimal.ZERO;

		case EXERCISE:
		case SELL:
		case CONTRIBX:
		case DIV:
		case INT_INC:
		case MISC_INCX:
		case OTHER:
		case REMINDER:
		case XIN:
		case XOUT:
			break;
		}

		return tot;
	}

	public BigDecimal getCashTransferAmount() {
		return (this.cashTransferred != null) //
				? this.cashTransferred //
				: super.getCashTransferAmount();

	}

	/** Detect/correct any quicken issues with this loaded transaction */
	public void repair(TransactionInfo tinfo) {
		TxAction action = getAction();

		if (action == TxAction.OTHER) {
			if (this.security != null) {
				Common.reportError("Transaction has unknown type: " + //
						MoneyMgrModel.currModel.getAccountByID(getAccountID()).name);
				return;
			}

			if (getCatid() < 0) {
				this.action = (isCredit()) ? TxAction.XIN : TxAction.XOUT;
				tinfo.setValue(TransactionInfo.ACTION_IDX, (isCredit()) //
						? TxAction.XIN.toString() //
						: TxAction.XOUT.toString());
			} else {
				this.action = TxAction.CASH;
				tinfo.setValue(TransactionInfo.ACTION_IDX, TxAction.CASH.toString());
			}
		}

		if ((action == TxAction.CASH) //
				&& (getAmount() == null) && (this.cashTransferred == null)) {
			setAmount(BigDecimal.ZERO);
			tinfo.setValue(TransactionInfo.AMOUNT_IDX, "0.00");
		}

		if (this.cashTransferred != null) {
			// Some xfers store amount with the opposite sign than what we expect
			if (action == TxAction.XOUT) {
				this.cashTransferred = this.cashTransferred.negate();
				tinfo.setValue(TransactionInfo.XAMOUNT_IDX, this.cashTransferred.toString());

				setAmount(this.cashTransferred);
			} else if (action == TxAction.SELLX) {
				this.cashTransferred = this.cashTransferred.negate();
				tinfo.setValue(TransactionInfo.XAMOUNT_IDX, this.cashTransferred.toString());
			}
		}

		switch (action) {
		case SHRS_OUT:
		case SELL:
		case SELLX:
		case EXERCISE:
		case EXERCISEX:
			// Sign of quantity is opposite of what we want
			this.quantity = this.quantity.negate();
			tinfo.setValue(TransactionInfo.SHARES_IDX, this.quantity.toString());
			break;

		case XIN: // amt/xamt
		case INT_INC: // amt
		case MISC_INCX: // amt
		case CONTRIBX: // amt/xamt
		case WITHDRAWX: // + amt/xamt
		case DIV: // amt
			// TODO This, apparently, is to treat MM balances as cash
			this.security = null;
			tinfo.setValue(TransactionInfo.SECURITY_IDX, "");
			break;

		default:
			break;
		}

		switch (getAction()) {
		case BUY:
		case BUYX:
		case REINV_DIV:
		case REINV_INT:
		case REINV_LG:
		case REINV_SH:
		case SELL:
		case SELLX:
			repairBuySell(tinfo);
			break;

		default:
			break;
		}

		// Some cash transfers aren't reflected in the tx amount
		if ((getAmount() == null) && (getCashTransferAmount() != null)) {
			setAmount(getCashTransferAmount());
			tinfo.setValue(TransactionInfo.AMOUNT_IDX, getCashTransferAmount().toString());
		}

		super.repair(tinfo);
	}

	/** Fix quicken issues with buy/sell stock */
	private void repairBuySell(TransactionInfo tinfo) {
		BigDecimal amt = getBuySellAmount();

		// Calculate total cost including commission
		BigDecimal tot = this.quantity.multiply(this.price);

		// Make sure we have a valid commission value, even if zero
		if (this.commission == null) {
			this.commission = BigDecimal.ZERO;
			tinfo.setValue(TransactionInfo.COMMISSION_IDX, "0.00");
		}

		tot = tot.add(this.commission);

		BigDecimal diff;

		switch (getAction()) {
		case SELL:
		case SELLX:
			diff = tot.add(amt).abs();
			break;

		default:
			diff = tot.subtract(amt).abs();
			break;
		}

		// TODO Adjust price if there is a discrepancy (does this ever happen?)
		if (!Common.isEffectivelyZero(diff)) {
			BigDecimal newprice = tot.divide(this.quantity).abs();

			int acctid = getAccountID();
			String s = "Inconsistent " + this.action + " transaction:" + //
					" acct=" + MoneyMgrModel.currModel.getAccountByID(acctid).name + //
					" " + getDate().toString() + "\n" + //
					"  sec=" + this.getSecurityName() + //
					" qty=" + this.quantity + //
					" price=" + this.price;

			if ((this.commission != null) && //
					(this.commission.compareTo(BigDecimal.ZERO) != 0)) {
				s += " comm=" + this.commission;
			}

			s += " tot=" + tot + //
					" txamt=" + amt + //
					" diff=" + diff + "\n";
			s += "  Corrected price: " + newprice;

			if (QifDom.verbose) {
				Common.reportWarning(s);
			}

			this.price = newprice;
			tinfo.setValue(TransactionInfo.PRICE_IDX, newprice.toString());
		}
	}

	public String toStringShort(boolean veryshort) {
		String s = String.format("%s %s %s:%s", //
				(isCleared() ? "*" : " "), //
				getDate().toString(), //
				getAccount().name, //
				this.action.toString());

		if (this.action == TxAction.STOCKSPLIT) {
			s += String.format(" %5.2f", //
					getSplitRatio());
		} else {
			s += String.format(" %8.2f %8.2f %8.2f", //
					getShares(), //
					getAmount(), //
					getCashAmount());
		}

		s += " " + ((this.security != null) ? this.getSecuritySymbol() : getPayee());

		if (isStockOptionTxn() && (this.option != null)) {
			s += "  Option info: " + this.option.toString();
		}

		return s;
	}

	public String toStringLong() {
		String s = (isCleared() ? "*" : " ");
		QDate d = getDate();
		s += ((d != null) ? d.toString() : "null");
		s += " Tx" + getTxid() + ": I ";
		Account a = MoneyMgrModel.currModel.getAccountByID(getAccountID());
		s += ((a != null) ? a.name : "null");
		s += " " + Common.formatAmount(getAmount()).trim();
		s += " " + this.action;
		if (this.security != null) {
			s += " " + this.getSecurityName();
			s += " price=" + this.price;
			if (getAction() == TxAction.STOCKSPLIT) {
				s += " spratio=" + getSplitRatio();

			} else {
				s += " qty=" + this.quantity;
			}
		} else {
			s += " " + getPayee();
		}

		s += " memo=" + getMemo();
		s += " comm=" + this.commission;
		s += " xactid=" + getCashTransferAcctid();
		s += " xamt=" + getCashTransferAmount();
		if (isStockOptionTxn() && (this.option != null)) {
			s += "\n  Option info: " + this.option.toString();
		}

		return s;
	}

	/**
	 * Return a weight for grouping lots in the display by type.<br>
	 * 
	 * 0 preexisting not disposed (should be none)<br>
	 * 1 Preexisting disposed<br>
	 * 2 new disposed<br>
	 * 3 new not disposed
	 */
	private static int calcWeight(Lot lot, List<Lot> lots, List<Lot> disposedLots) {
		boolean existing = (lot.getSourceLot() == null) || !lots.contains(lot.getSourceLot());
		boolean disposed = disposedLots.contains(lot);

		if (existing) {
			return (disposed) ? 1 : 0;
		} else {
			return (disposed) ? 2 : 3;
		}
	}

	/** TODO formatValue - put this information into the UI appropriately */
	public String formatValue() {
		String ret = "";

		String datestr = getDate().toString().trim();

		ret += String.format("%-8s %5s %s", //
				datestr, //
				getAction().name(), //
				// Common.formatAmount(getAmount()).trim());
				getSecurityName());

		if (this.security != null) {
			ret += "\n";
			ret += String.format("  %s   %s @ %s", //
					getSecuritySymbol(), //
					Common.formatAmount3(this.quantity.abs()).trim(), //
					Common.formatAmount(this.price).trim());
		}

		if (this.cashTransferred != null) {
			Account xacct = MoneyMgrModel.currModel.getAccountByID(getCashTransferAcctid());
			String xacctname = (xacct != null) ? xacct.name : null;

			ret += "\n";
			ret += String.format("  XFER: [%s]   %s", //
					Common.formatString(xacctname, 20).trim(), //
					Common.formatAmount(this.cashTransferred).trim());
		}

		if (isStockOptionTxn() && (this.option != null)) {
			ret += "\n";
			ret += "  Option info : " + this.option.toString();
			ret += "\n";
			ret += "  Option value: " + this.option.formatInfo(getDate());
		}

		ret += "\n";

		if (hasSplits()) {
			for (Iterator<SplitTxn> iter = getSplits().iterator(); iter.hasNext();) {
				SimpleTxn split = iter.next();
				ret += "\n";
				ret += split.formatValue();
			}
		}

		if ((getAction() == TxAction.SELL) || (getAction() == TxAction.SELLX)) {
			BasisInfo info = new BasisInfo(this.lots);

			ret += String.format( //
					"\n%-10s @ %-8s %12s | %12s : %12s\n", //
					"Shares", "AvgPrice", "Basis", "Proceeds", "Gain/loss");
			ret += String.format( //
					"%10s @ %8s %12s | %12s : %12s\n", //
					Common.formatAmount3(info.totalShares).trim(), //
					Common.formatAmount(info.averagePrice).trim(), //
					Common.formatAmount(info.totalCost), //
					Common.formatAmount(getAmount()), //
					Common.formatAmount(getAmount().subtract(info.totalCost)));
		}

		List<Lot> lots = new ArrayList<>(this.lotsCreated);
		for (Lot lot : this.lotsDisposed) {
			if (!lots.contains(lot)) {
				lots.add(lot);
			}
		}

		if (!lots.isEmpty()) {
			Comparator<Lot> compr = new Comparator<Lot>() {
				public int compare(Lot l1, Lot l2) {
					if (l1 == l2) {
						return 0;
					}

					int l1n = calcWeight(l1, lots, lotsDisposed);
					int l2n = calcWeight(l2, lots, lotsDisposed);

					if (l1.isDerivedFrom(l2)) {
						return 1;
					} else if (l2.isDerivedFrom(l1)) {
						return -1;
					}

					if (l1n != l2n) {
						return l1n - l2n;
					}

					return l1.getAcquisitionDate().compareTo(l2.getAcquisitionDate());
				}
			};

			Collections.sort(lots, compr);

			ret += String.format("\n%s %-8s: %-5s   %-8s   %-9s   %-8s", //
					"  ", "idx/src", " ID", " Date", " Shares", " Basis");
			ret += String.format("\n%s %-8s: %-5s   %-8s   %-9s   %-8s", //
					"==", "========", "=====", "========", "=========", "========");

			for (Lot lot : lots) {
				String status;
				int idx = lots.indexOf(lot);
				int srcidx = -1;

				if ((lot.getSourceLot() == null) && (lot.createTransaction == this)) {
					status = "+";
				} else {
					srcidx = lots.indexOf(lot.getSourceLot());

					if (srcidx < 0) {
						status = " ";
					} else if (srcidx < idx) {
						status = "+"; // SRC_LOT_" + lot.sourceLot.lotid;
					} else {
						status = String.format( //
								"LOTS_SORTED_IMPROPERLY([%d]%d -> [%d]%d)", //
								srcidx, lot.getSourceLot().lotid, idx, lot.lotid);
					}

					status += (this.lotsDisposed.contains(lot)) ? "x" : " ";
				}

				ret += "\n";
				ret += String.format("%s %-8s: %5d   %8s   %9s   %8s", //
						status, //
						("[" + ((srcidx >= 0) ? (srcidx + "->") : "") + idx + "]"), //
						// lot.toString(), //
						lot.lotid, //
						lot.getAcquisitionDate().toString(), //
						Common.formatAmount3(lot.shares).trim(), //
						Common.formatAmount(lot.getCostBasis()).trim());
			}
		}

		System.out.println("\n=============================\n" + ret + "\n");

		return ret;
	}

	public String lotListMatches(List<Lot> list1, List<Lot> list2) {
		if (list1.size() != list2.size()) {
			return "size";
		}

		for (int idx = 0; idx < list1.size(); ++idx) {
			Lot l1 = list1.get(idx);
			Lot l2 = list2.get(idx);

			String res = l1.matches(l2);
			if (res != null) {
				return res;
			}
		}

		return null;
	}

	public String matches(InvestmentTxn other) {
		String res = super.matches(other);
		if (res != null) {
			return res;
		}

		if (!Common.isEffectivelyEqual(getRunningTotal(), other.getRunningTotal()) //
				|| (getShareAction() != other.getShareAction()) //
				|| !Common.safeEquals(this.accountForTransfer, other.accountForTransfer) //
				|| !Common.isEffectivelyEqual(this.cashTransferred, other.cashTransferred) //
				|| !Common.isEffectivelyEqual(this.price, other.price) //
				|| !Common.isEffectivelyEqual(this.commission, other.commission) //
				|| (isStockOptionTxn() != other.isStockOptionTxn()) //
		) {
			return "genInfo";
		}

		if (getAction() != TxAction.VEST) {
			if (!Common.isEffectivelyEqual(this.quantity, other.quantity)) {
				return "nonVestQuantity";
			}

			// TODO System.out.println("xyzzy - ignoring vest quantity");
		}

		res = lotListMatches(this.lots, other.lots);
		if (res != null) {
			return "lots:" + res;
		}
		res = lotListMatches(this.lotsCreated, other.lotsCreated);
		if (res != null) {
			return "lotsCreated:" + res;
		}
		res = lotListMatches(this.lotsDisposed, other.lotsDisposed);
		if (res != null) {
			return "lotsDisposed:" + res;
		}

		if (isStockOptionTxn()) {
			if ((other.option == null) || (this.option.optid != other.option.optid)) {
				return "TxOption";
			}
		}

		return null;
	}
}