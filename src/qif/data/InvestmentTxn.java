package qif.data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class InvestmentTxn extends GenericTxn {
	private static final List<InvestmentTxn> NO_XFER_TXNS = //
			Collections.unmodifiableList(new ArrayList<InvestmentTxn>());

	private TxAction action;
	public Security security;
	public BigDecimal price;
	private BigDecimal quantity;
	// public String textFirstLine;
	public BigDecimal commission;
	public String accountForTransfer;

	public BigDecimal amountTransferred;
	public List<InvestmentTxn> xferTxns;

	public List<Lot> lotsCreated;
	public List<Lot> lotsDisposed;

	public InvestmentTxn(int acctid) {
		super(acctid);

		this.action = TxAction.OTHER;
		this.security = null;
		this.price = BigDecimal.ZERO;
		this.quantity = BigDecimal.ZERO;
		// this.textFirstLine = "";
		this.commission = null;
		this.accountForTransfer = "";
		this.amountTransferred = null;
		this.xferTxns = NO_XFER_TXNS;

		this.lotsCreated = new ArrayList<Lot>();
		this.lotsDisposed = new ArrayList<Lot>();
	}

	public InvestmentTxn(int acctid, InvestmentTxn txn) {
		super(acctid);

		setDate(txn.getDate());
		setAmount(txn.getAmount());

		this.clearedStatus = txn.clearedStatus;
		this.runningTotal = BigDecimal.ZERO;
		this.stmtdate = null;

		this.action = txn.action;
		this.security = txn.security;
		this.price = txn.price;
		this.quantity = txn.quantity;
		// this.textFirstLine = "";
		this.commission = txn.commission;
		this.accountForTransfer = txn.accountForTransfer;
		this.amountTransferred = txn.amountTransferred;
		this.xferTxns = NO_XFER_TXNS;

		this.lotsCreated = new ArrayList<Lot>();
		this.lotsDisposed = new ArrayList<Lot>();
	}

	public void setAction(TxAction action) {
		this.action = action;
	}

	public TxAction getAction() {
		return this.action;
	}

	public void setQuantity(BigDecimal qty) {
		this.quantity = qty;
	}

	private boolean isStockOption() {
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
				|| isStockOption() //
				|| (getAction() == TxAction.STOCKSPLIT)) {
			return BigDecimal.ZERO;
		}

		return this.quantity;
	}

	public static enum ShareAction {
		NO_ACTION, NEW_SHARES, DISPOSE_SHARES, TRANSFER_OUT, TRANSFER_IN, SPLIT
	};

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
		//case EXERCISE:
		//case EXERCISEX:
		//case EXPIRE:
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

	public BigDecimal getSplitRatio() {
		if ((this.quantity == null) || (getAction() != TxAction.STOCKSPLIT)) {
			return BigDecimal.ONE;
		}

		return this.quantity.divide(BigDecimal.TEN);
	}

	public void repair() {
		TxAction action = getAction();

		if (action == TxAction.OTHER) {
			Common.reportError("Transaction has unknown type: " + //
					Account.getAccountByID(this.acctid).getName());
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

	public List<Lot> getLots() {
		return this.lotsDisposed;
	}

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
					" acct=" + Account.getAccountByID(this.acctid).getName() + //
					" " + getDate().toString() + "\n" + //
					"  sec=" + this.security.getName() + //
					" qty=" + this.quantity + //
					" price=" + this.price;

			if (this.commission != null && //
					this.commission.compareTo(BigDecimal.ZERO) != 0) {
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

	// Get the total amount of a buy/sell transaction.
	// This returns the absolute value.
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

	public String formatForSave() {
		String secString = ";";
		if (this.security != null) {
			secString = this.security.getSymbol() + ";";
			if (this.quantity != null) {
				secString += String.format("%5.2f", this.quantity);
			}
		}

		final String s = String.format("I;%s;%s;%s;%5.2f", //
				getDate().toString(), //
				getAction().toString(), //
				secString, //
				getCashAmount());
		return s;
	}

	public String getSecurityName() {
		if (this.security != null) {
			return this.security.getName();
		}

		return "N/A";
	}

	class AccountLotsInfo {
		Account acct;
		List<Lot> lots;
		BigDecimal bal;

		public AccountLotsInfo(Account acct) {
			this.acct = acct;
			this.lots = new ArrayList<Lot>();
			this.bal = BigDecimal.ZERO;
		}

		public boolean processLotForTransaction(InvestmentTxn tx, Lot lot) {
			if (lot.createTransaction == tx) {
				tx.lotsCreated.add(lot);

				this.lots.add(lot);
				this.bal = this.bal.add(lot.shares);

				return true;
			}

			if (lot.expireTransaction == tx) {
				tx.lotsDisposed.add(lot);

				this.lots.add(lot);
				this.bal = this.bal.subtract(lot.shares);

				return true;
			}

			if (tx.action == TxAction.SHRS_IN) {
				System.out.println("INSHRS=" + Common.formatAmount3(tx.quantity) //
						+ " LOTSHRS=" + Common.formatAmount3(lot.shares));

				if (tx.quantity.equals(lot.shares)) {
					System.out.println("match");
				}
			}

			return false;
		}

		public String toString() {
			return "acct=" + acct.getName() + " " + //
					Common.formatAmount3(this.bal) + " shares in " + //
					this.lots.size() + " lots";
		}
	}

	public void organizeLots() {
		Security sec = this.security;
		if (sec == null) {
			return;
		}

		List<InvestmentTxn> txns = new ArrayList<InvestmentTxn>(sec.transactions);
		Collections.sort(txns, new Comparator<InvestmentTxn>() {
			public int compare(InvestmentTxn t1, InvestmentTxn t2) {
				return t1.getDate().compareTo(t2.getDate());
			}
		});

		List<Lot> lots = new ArrayList<Lot>(sec.getLots());
		Collections.sort(lots, new Comparator<Lot>() {
			public int compare(Lot l1, Lot l2) {
				return l1.createDate.compareTo(l2.createDate);
			}
		});

		for (Lot lot : lots) {
			System.out.println(lot.toString());
		}

		Map<Account, AccountLotsInfo> lotinfo = new HashMap<Account, AccountLotsInfo>();

		for (InvestmentTxn tx : txns) {
			Account acct = tx.getAccount();

			AccountLotsInfo ali = lotinfo.get(acct);
			if (ali == null) {
				ali = new AccountLotsInfo(acct);
				lotinfo.put(acct, ali);
			}

			tx.lotsCreated.clear();
			tx.lotsDisposed.clear();

			for (Iterator<Lot> iter = lots.iterator(); iter.hasNext();) {
				Lot lot = iter.next();
				if (lot.acctid == acct.acctid) {
					if (ali.processLotForTransaction(tx, lot)) {
						iter.remove();
					}
				}
			}

			if (tx.lotsCreated.isEmpty() && tx.lotsDisposed.isEmpty()) {
				System.out.println("No lots for tx?");
			}
		}

		return;
	}

	public String formatValue() {
		// organizeLots();

		String ret = String.format("[%d]%10s %d %5s",
				// "%10s %-30s %s %13s %-15s %-10s", //
				this.txid, //
				Common.formatDate(getDate().toDate()), //
				// this.getPayee(), //
				// ((isCleared()) ? "C" : " "), //
				// Common.formatAmount(getAmount()), //
				// getCategory(), //
				// getMemo(),
				this.acctid, //
				getAction().name());

		ret += "\n    ";
		if (this.amountTransferred != null) {
			ret += String.format(" XFER(%d %s)", //
					getXferAcctid(), //
					Common.formatAmount(this.amountTransferred).trim());
		}

		if (this.security != null) {
			List<Lot> lots = new ArrayList<Lot>(this.lotsCreated);
			lots.addAll(this.lotsDisposed);
			// List<Lot> lots = this.security.getLotsForTransaction(this);

			ret += String.format(" SEC(%s %s, %d lots)", //
					this.security.getSymbol(), //
//					Common.formatAmount(this.price), //
					Common.formatAmount3(quantity).trim(), //
					lots.size());

//			switch (this.action) {
//			case BUY:
//			case BUYX:
//			case EXERCISE:
//			case EXERCISEX:
//			case SHRS_IN:
//				this.dstLots = lots;
//				break;
//
//			case EXPIRE:
//			case SELL:
//			case SELLX:
//				this.srcLots = lots;
//				break;
//			}
		}

		ret += "\n";

		// public String textFirstLine;
		// public BigDecimal commission;

		if (hasSplits()) {
			for (Iterator<SimpleTxn> iter = getSplits().iterator(); iter.hasNext();) {
				SimpleTxn split = iter.next();
				ret += "\n";
				ret += split.formatValue();
			}
		}

//		public List<InvestmentTxn> xferTxns;

		if (this.lotsCreated != null && !this.lotsCreated.isEmpty()) {
			for (Lot lot : this.lotsCreated) {
				ret += "\n";
				ret += "   create " + lot.toString();
			}
		}
		if (this.lotsDisposed != null && !this.lotsDisposed.isEmpty()) {
			for (Lot lot : this.lotsDisposed) {
				ret += "\n";
				ret += "  dispose " + lot.toString();
			}
		}

		return ret;
	}

	public String toStringShort(boolean veryshort) {
		String s = String.format("%s %s %s:%s", //
				((this.stmtdate != null) ? "*" : " "), //
				getDate().toString(), //
				getAccount().getName(), //
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

		return s;
	}

	public String toStringLong() {
		String s = ((this.stmtdate != null) ? "*" : " ") + "InvTx" + this.txid + ":";
		s += " dt=" + getDate().toString();
		s += " acct=" + Account.getAccountByID(this.acctid).getName();
		s += " act=" + this.action;
		if (this.security != null) {
			s += " sec=" + this.security.getName();
		} else {
			s += " payee=" + getPayee();
		}
		s += " price=" + this.price;
		s += " qty=" + this.quantity;
		s += " amt=" + getAmount();
		s += " clr=" + this.clearedStatus;
		// s += " txt=" + this.textFirstLine;
		s += " memo=" + getMemo();
		s += " comm=" + this.commission;
		s += " xact=" + this.accountForTransfer;
		s += " xamt=" + this.amountTransferred;
		s += "\n";

		return s;
	}
}