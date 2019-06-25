package moneymgr.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import app.QifDom;
import moneymgr.io.cvs.CSVImport.TupleInfo;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/** Transaction for investment account (may involve security) */
public class InvestmentTxn extends GenericTxn {
	private static final List<InvestmentTxn> NO_XFER_TXNS = //
			Collections.unmodifiableList(new ArrayList<InvestmentTxn>());

	public enum ShareAction {
		NO_ACTION, NEW_SHARES, DISPOSE_SHARES, TRANSFER_OUT, TRANSFER_IN, SPLIT
	}

	/** TODO TxAction not for non-investment txns? Action taken by transaction */
	private TxAction action;

	public Security security;
	public StockOption option;
	public BigDecimal price;
	private BigDecimal quantity;
	public BigDecimal commission;

	public String accountForTransfer;
	public BigDecimal amountTransferred;
	private List<InvestmentTxn> xferTxns;

	public final List<Lot> lots;
	public final List<Lot> lotsCreated;
	public final List<Lot> lotsDisposed;

	// public String textFirstLine;

	public InvestmentTxn(int acctid) {
		super(acctid);

		this.action = TxAction.OTHER;
		this.security = null;
		this.option = null;
		this.price = BigDecimal.ZERO;
		this.quantity = BigDecimal.ZERO;
		// this.textFirstLine = "";
		this.commission = null;
		this.accountForTransfer = "";
		this.amountTransferred = null;
		this.xferTxns = NO_XFER_TXNS;

		this.lots = new ArrayList<>();
		this.lotsCreated = new ArrayList<>();
		this.lotsDisposed = new ArrayList<>();
	}

	/** Construct a dummy transaction for a split (see LotProcessor) */
	public InvestmentTxn(int acctid, InvestmentTxn txn) {
		super(acctid);

		setDate(txn.getDate());
		setAmount(txn.getAmount());

		this.runningTotal = BigDecimal.ZERO;
		this.stmtdate = null;

		this.action = txn.action;
		this.security = txn.security;
		this.option = txn.option;
		this.price = txn.price;
		this.quantity = txn.quantity;
		this.commission = txn.commission;
		this.accountForTransfer = txn.accountForTransfer;
		this.amountTransferred = txn.amountTransferred;
		this.xferTxns = NO_XFER_TXNS;

		this.lots = new ArrayList<>();
		this.lotsCreated = new ArrayList<>();
		this.lotsDisposed = new ArrayList<>();
	}

	public int compareToXX(TupleInfo tuple, SimpleTxn othersimp) {
		int diff;

		diff = super.compareToXX(tuple, othersimp);
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

	public String getSecurityName() {
		if (this.security != null) {
			return this.security.getName();
		}

		return "";
	}

	public List<InvestmentTxn> getXferTxns() {
		return (this.xferTxns == NO_XFER_TXNS) //
				? this.xferTxns //
				: Collections.unmodifiableList(this.xferTxns);
	}

	public void setXferTxns(List<InvestmentTxn> txns) {
		if (this.xferTxns == NO_XFER_TXNS) {
			this.xferTxns = new ArrayList<InvestmentTxn>(txns);
		} else {
			this.xferTxns.clear();
			this.xferTxns.addAll(txns);
		}
	}

	public void setQuantity(BigDecimal qty) {
		this.quantity = qty;
	}

	public boolean isStockOptionTransaction() {
		if (this.option != null) {
			if ((getAction() == TxAction.STOCKSPLIT) //
					|| this.option.name.equals("espp")) {
				return false;
			}

			if (this.quantity.signum() != 0) {
				// System.out.println("xyzzy");
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
				|| isStockOptionTransaction() //
				|| (getAction() == TxAction.STOCKSPLIT)) {
			return BigDecimal.ZERO;
		}

		return this.quantity;
	}

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
		case WITHDRAWX:
		case XOUT:
			return true;

		default:
			return false;
		}
	}

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

	/** For a stock split transaction, calculate the split share multiplier */
	public BigDecimal getSplitRatio() {
		if ((this.quantity == null) || (getAction() != TxAction.STOCKSPLIT)) {
			return BigDecimal.ONE;
		}

		return this.quantity.divide(BigDecimal.TEN);
	}

	/** Get lots for shares affected by this transaction */
	public List<Lot> getLots() {
		return this.lotsDisposed;
	}

	/**
	 * Get the total amount of a buy/sell transaction.<br>
	 * This returns the absolute value.
	 */
	public BigDecimal getBuySellAmount() {
		BigDecimal tot = super.getCashAmount();

		if (tot == null) {
			tot = getXferAmount();
		}

		return (tot != null) ? tot.abs() : BigDecimal.ZERO;
	}

	public BigDecimal getCashAmount() {
		BigDecimal tot = super.getCashAmount();

		if (tot == null) {
			tot = getXferAmount();
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
			tot = BigDecimal.ZERO;
			break;

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

	public BigDecimal getXferAmount() {
		if (this.amountTransferred == null) {
			return super.getXferAmount();
		}

		switch (getAction()) {
		case SELLX:
			return this.amountTransferred.negate();

		default:
			return this.amountTransferred;
		}
	}

	/** Correct issues with this loaded transaction */
	public void repair() {
		TxAction action = getAction();

		if (action == TxAction.OTHER) {
			Common.reportError("Transaction has unknown type: " + //
					Account.getAccountByID(getAccountID()).name);
			return;
		}

		if ((action == TxAction.CASH) //
				&& (getAmount() == null) && (this.amountTransferred == null)) {
			setAmount(BigDecimal.ZERO);
		}

		if (action == TxAction.XOUT) {
			this.amountTransferred = this.amountTransferred.negate();
			setAmount(this.amountTransferred);
		}

		switch (action) {
		case SHRS_OUT:
		case SELL:
		case SELLX:
		case EXERCISE:
		case EXERCISEX:
			this.quantity = this.quantity.negate();
			break;

		case XIN: // amt/xamt
		case INT_INC: // amt
		case MISC_INCX: // amt
		case CONTRIBX: // amt/xamt
		case WITHDRAWX: // + amt/xamt
		case DIV: // amt
			// This, apparently, is to treat MM balances as cash
			this.security = null;
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
			repairBuySell();
			break;

		default:
			break;
		}

		// We lack lots of information needed to properly track options
		switch (getAction()) {
		case GRANT:
			// Strike price, open/close price, vest/expire date, qty
			// TODO Add missing option info to memo
			break;
		case VEST:
			// Connect to Grant
			break;
		case EXERCISE:
		case EXERCISEX:
			// Connect to Grant, qty/price
			break;
		case EXPIRE:
			// Connect to Grant, qty
			break;

		default:
			break;
		}

		if ((getAmount() == null) && (getXferAmount() != null)) {
			setAmount(getXferAmount());
		}

		super.repair();
	}

	/** Fix issues with loaded stock buy/sell transaction */
	private void repairBuySell() {
		final BigDecimal amt = getBuySellAmount();

		BigDecimal tot = this.quantity.multiply(this.price);
		if (this.commission == null) {
			this.commission = BigDecimal.ZERO;
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

		if (diff.compareTo(new BigDecimal("0.005")) > 0) {
			final BigDecimal newprice = tot.divide(this.quantity).abs();

			String s = "Inconsistent " + this.action + " transaction:" + //
					" acct=" + Account.getAccountByID(getAccountID()).name + //
					" " + getDate().toString() + "\n" + //
					"  sec=" + this.security.getName() + //
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
		}
	}

	public String toStringShort(boolean veryshort) {
		String s = String.format("%s %s %s:%s", //
				((this.stmtdate != null) ? "*" : " "), //
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

		s += " " + ((this.security != null) ? this.security.getSymbol() : getPayee());

		if (isStockOptionTransaction() && (this.option != null)) {
			s += "  Option info: " + this.option.toString();
		}

		return s;
	}

	public String toStringLong() {
		String s = ((this.stmtdate != null) ? "*" : " ");
		QDate d = getDate();
		s += ((d != null) ? d.toString() : "null");
		s += " Tx" + this.txid + ": I ";
		Account a = Account.getAccountByID(getAccountID());
		s += ((a != null) ? a.name : "null");
		s += " " + Common.formatAmount(getAmount()).trim();
		s += " " + this.action;
		if (this.security != null) {
			s += " " + this.security.getName();
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
		s += " xactid=" + getXferAcctid();
		s += " xamt=" + getXferAmount();
		if (isStockOptionTransaction() && (this.option != null)) {
			s += "\n  Option info: " + this.option.toString();
		}

		return s;
	}

	public static int calcWeight(Lot lot, List<Lot> lots, List<Lot> disposedLots) {
		boolean existing = (lot.sourceLot == null) || !lots.contains(lot.sourceLot);
		boolean disposed = disposedLots.contains(lot);

		// 0 preexisting not disposed (should be none)
		// 1 Preexisting disposed
		// 2 new disposed
		// 3 new not disposed

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
					this.security.getSymbol(), //
					Common.formatAmount3(this.quantity.abs()).trim(), //
					Common.formatAmount(this.price).trim());
		}

		if (this.amountTransferred != null) {
			Account xacct = Account.getAccountByID(getXferAcctid());
			String xacctname = (xacct != null) ? xacct.name : null;

			ret += "\n";
			ret += String.format("  XFER: [%s]   %s", //
					Common.formatString(xacctname, 20).trim(), //
					Common.formatAmount(this.amountTransferred).trim());
		}

		if (isStockOptionTransaction() && (this.option != null)) {
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
			BasisInfo info = Lot.getBasisInfo(this.lots);

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

				if (lot.sourceLot == null) {
					status = "+";
				} else {
					srcidx = lots.indexOf(lot.sourceLot);

					if (srcidx < 0) {
						status = " ";
					} else if (srcidx < idx) {
						status = "+"; // SRC_LOT_" + lot.sourceLot.lotid;
					} else {
						status = String.format( //
								"LOTS_SORTED_IMPROPERLY([%d]%d -> [%d]%d)", //
								srcidx, lot.sourceLot.lotid, idx, lot.lotid);
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
}