package moneymgr.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import app.QifDom;
import moneymgr.util.Common;

/** Transaction for investment account (may involve security) */
public class InvestmentTxn extends GenericTxn {
	private static final List<InvestmentTxn> NO_XFER_TXNS = //
			Collections.unmodifiableList(new ArrayList<InvestmentTxn>());

	public enum ShareAction {
		NO_ACTION, NEW_SHARES, DISPOSE_SHARES, TRANSFER_OUT, TRANSFER_IN, SPLIT
	}

	/** TODO not for non-investment txns? Action taken by transaction */
	private TxAction action;

	public Security security;
	public StockOption option;
	public BigDecimal price;
	private BigDecimal quantity;
	public BigDecimal commission;

	public String accountForTransfer;
	public BigDecimal amountTransferred;
	public List<InvestmentTxn> xferTxns;

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

	/** TODO Construct a dummy transaction for a split (see LotProcessor) */
	public InvestmentTxn(int acctid, InvestmentTxn txn) {
		super(acctid);

		setDate(txn.getDate());
		setAmount(txn.getAmount());

		// this.clearedStatus = txn.clearedStatus;
		this.runningTotal = BigDecimal.ZERO;
		this.stmtdate = null;

		this.action = txn.action;
		this.security = txn.security;
		this.option = txn.option;
		this.price = txn.price;
		this.quantity = txn.quantity;
		// this.textFirstLine = "";
		this.commission = txn.commission;
		this.accountForTransfer = txn.accountForTransfer;
		this.amountTransferred = txn.amountTransferred;
		this.xferTxns = NO_XFER_TXNS;

		this.lots = new ArrayList<>();
		this.lotsCreated = new ArrayList<>();
		this.lotsDisposed = new ArrayList<>();
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

		return "N/A";
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
		case CASH:
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

//	/**
//	 * TODO defunct
//	 * Construct a string for persisting this transaction to a file<br>
//	 * Investment Format: I;DATE;ACTION;[SEC];[QTY];AMT
//	 */
//	public String formatForSave() {
//		String secString = ";";
//		if (this.security != null) {
//			secString = this.security.getSymbol() + ";";
//			if (this.quantity != null) {
//				secString += String.format("%5.2f", this.quantity);
//			}
//		}
//
//		final String s = String.format("I;%s;%s;%s;%5.2f", //
//				getDate().toString(), //
//				getAction().toString(), //
//				secString, //
//				getCashAmount());
//		return s;
//	}

	/** Correct issues with this loaded transaction */
	public void repair() {
		TxAction action = getAction();

		if (action == TxAction.OTHER) {
			Common.reportError("Transaction has unknown type: " + //
					Account.getAccountByID(this.acctid).name);
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
					" acct=" + Account.getAccountByID(this.acctid).name + //
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
		String s = ((this.stmtdate != null) ? "*" : " ") + "InvTx" + this.txid + ":";
		s += " dt=" + getDate().toString();
		s += " acct=" + Account.getAccountByID(this.acctid).name;
		s += " act=" + this.action;
		if (this.security != null) {
			s += " sec=" + this.security.getName();
		} else {
			s += " payee=" + getPayee();
		}
		s += " price=" + this.price;
		if (getAction() == TxAction.STOCKSPLIT) {
			s += " spratio=" + getSplitRatio();

		} else {
			s += " qty=" + this.quantity;
		}

		s += " amt=" + getAmount();
		// s += " clr=" + this.clearedStatus;
		// s += " txt=" + this.textFirstLine;
		s += " memo=" + getMemo();
		s += " comm=" + this.commission;
		s += " xact=" + this.accountForTransfer;
		s += " xamt=" + this.amountTransferred;
		if (isStockOptionTransaction() && (this.option != null)) {
			s += "\n  Option info: " + this.option.toString();
		}
		s += "\n";

		return s;
	}

	// TODO put this information into the UI appropriately
	public String formatValue() {
		String ret = "";

		String datestr = getDate().toString();

		ret += String.format("Transaction[%d] %10s %d %5s %s", this.txid, //
				datestr, //
				this.acctid, //
				getAction().name(), //
				Common.formatAmount(getAmount()));

		if (this.amountTransferred != null) {
			Account xacct = Account.getAccountByID(getXferAcctid());
			String xacctname = (xacct != null) ? xacct.name : null;

			ret += String.format("  XFER(%s, %s)", //
					Common.formatString(xacctname, 20).trim(), //
					Common.formatAmount(this.amountTransferred).trim());
		}

		if (this.security != null) {
			List<Lot> lots = new ArrayList<>(this.lotsCreated);
			lots.addAll(this.lotsDisposed);

			ret += "\n";
			ret += String.format("  SEC(%s %s %s, %d lots)", //
					this.security.getSymbol(), //
					Common.formatAmount3(this.quantity).trim(), //
					Common.formatAmount(this.price).trim(), //
					lots.size());
		}

		if (isStockOptionTransaction() && (this.option != null)) {
			ret += "\n";
			ret += "  Option info : " + this.option.toString();
			ret += "\n";
			ret += "  Option value: " + this.option.formatInfo(getDate());
		}

		ret += "\n";

		if (hasSplits()) {
			for (Iterator<SimpleTxn> iter = getSplits().iterator(); iter.hasNext();) {
				SimpleTxn split = iter.next();
				ret += "\n";
				ret += split.formatValue();
			}
		}

		if ((this.lotsCreated != null) && !this.lotsCreated.isEmpty()) {
			for (Lot lot : this.lotsCreated) {
				ret += "\n";
				ret += "   create " + lot.toString();
			}
		}

		if ((this.lotsDisposed != null) && !this.lotsDisposed.isEmpty()) {
			for (Lot lot : this.lotsDisposed) {
				ret += "\n";
				ret += "  dispose " + lot.toString();
			}
		}

		ret += "\n=========================================";

		if ((getAction() == TxAction.SELL) || (getAction() == TxAction.SELLX)) {
			ret += "\n\nLots sold:\n---------------\n";

			for (Lot lot : this.lots) {
				ret += "\n - " + lot.toString();
			}

			ret += "\n\n";
			ret += "Basis Info";
			BasisInfo info = Lot.getBasisInfo(this.lots);

			ret += info.toString();
			ret += String.format("Proceeds    : %s\nGain/loss   : %s\n", //
					Common.formatAmount(getAmount()), //
					Common.formatAmount(getAmount().subtract(info.totalCost)));
		}

		System.out.println("\n=============================\n" + ret + "\n");

		return ret;
	}
}