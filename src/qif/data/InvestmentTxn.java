package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class InvestmentTxn extends GenericTxn {
	private TxAction action;
	public Security security;
	public BigDecimal price;
	private BigDecimal quantity;
	public String textFirstLine;
	public BigDecimal commission;
	public String accountForTransfer;
	public BigDecimal amountTransferred;
	public List<InvestmentTxn> xferInv;
	// Buys/grants will create one lot; others may affect multiple lots (or none)
	public List<Lot> lots = new ArrayList<Lot>();

	public InvestmentTxn(int acctid) {
		super(acctid);

		this.action = TxAction.OTHER;
		this.security = null;
		this.price = null;
		this.quantity = null;
		this.textFirstLine = "";
		this.commission = null;
		this.accountForTransfer = "";
		this.amountTransferred = null;
		this.xferInv = null;
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

	public BigDecimal getShares() {
		if ((this.quantity == null) || (getAction() == TxAction.STOCKSPLIT)) {
			return BigDecimal.ZERO;
		}

		return this.quantity;
	}

	public BigDecimal getSplitRatio() {
		if ((this.quantity == null) || (getAction() != TxAction.STOCKSPLIT)) {
			return BigDecimal.ONE;
		}

		return this.quantity.divide(BigDecimal.TEN);
	}

	public void repair() {
		switch (getAction()) {
		case CASH:
			if ((getAmount() == null) && (this.amountTransferred == null)) {
				setAmount(BigDecimal.ZERO);
			}
			break;

		case BUY:
		case SHRS_IN:
		case REINV_DIV:
		case REINV_LG:
		case REINV_SH:
			if (this.quantity == null) {
				// System.out.println("NULL quantities: " + ++nullQuantities);
				this.quantity = BigDecimal.ZERO;
			}
			if (this.price == null) {
				this.price = BigDecimal.ZERO;
			}
			break;

		case BUYX:
		case REINV_INT:
			if (this.price == null) {
				this.price = BigDecimal.ZERO;
			}
			break;

		case SHRS_OUT:
		case SELL:
		case SELLX:
		case EXERCISE:
		case EXERCISEX:
			if (this.price == null) {
				this.price = BigDecimal.ZERO;
			}

			this.quantity = this.quantity.negate();
			break;

		case STOCKSPLIT:
			break;

		case XIN: // amt/xamt
		case INT_INC: // amt
		case MISC_INCX: // amt
		case CONTRIBX: // amt/xamt
		case WITHDRAWX: // + amt/xamt
		case DIV: // amt
			if (this.security != null) {
				this.security = null;
			}
			break;

		case XOUT: { // + amt/xamt
			final BigDecimal amt = this.amountTransferred.negate();
			this.amountTransferred = amt;
			setAmount(amt);
			break;
		}

		// TODO We lack lots of information needed to properly track options
		case GRANT:
		case EXPIRE:
			if (this.quantity == null) {
				this.quantity = BigDecimal.ZERO;
			}
		case VEST:
			if (this.price == null) {
				this.price = BigDecimal.ZERO;
			}
			break;

		case REMINDER:
			break;

		case OTHER:
			Common.reportError("Transaction has unknown type: " + //
					QifDom.dom.getAccountByID(this.acctid).getName());
			return;
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

		case DIV:
			assert (this.price == null) && (this.quantity == null);
			break;

		case SHRS_IN:
		case SHRS_OUT:
			break;

		case STOCKSPLIT:
			break;

		case CASH:
		case CONTRIBX:
		case INT_INC:
		case MISC_INCX:
		case REMINDER:
		case WITHDRAWX:
		case XIN:
		case XOUT:
			break;

		// TODO We lack lots of information needed to properly track options
		case GRANT:
			// Strike price, open/close price, vest/expire date, qty
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

		case OTHER:
			assert (false); // can't happen
			return;
		}

		// TODO what if amount is null and xferamt is not? We don't want to set amount
		// then, right?
		super.repair();

		setupLots();
	}

	private void createLot() {
		Lot lot = new Lot();

		lot.purchaseDate = getDate();
		lot.secid = this.security.secid;
		lot.originalShares = this.quantity;

		addLot(lot);
	}

	private void addLot(Lot lot) {
		if (this.lots == null) {
			this.lots = new ArrayList<Lot>();
		}

		// TODO ultimately, we need to track lots in the security itself
		// GlobalPortfolio keeps track of all lot activity for each security
		// AccountPortfolio keeps track of lot activity in the account context
		// StatementHoldings keeps track of lot activity in the account/statement
		this.lots.add(lot);
	}

	private void setupLots() {
		switch (getAction()) {
		case BUY:
		case BUYX:
		case REINV_DIV:
		case REINV_INT:
		case REINV_LG:
		case REINV_SH:
			createLot();
			break;

		// TODO this is a bit different - create lot(s) for this account?
		case SHRS_IN:
			break;

		case GRANT:
			// TODO new lot - Strike price, open/close price, vest/expire date, qty
			break;

		case VEST:
			// TODO Adjust lot info
			break;

		case SELL:
		case SELLX:
			// TODO pick shares from available lots
			break;

		case EXERCISE:
		case EXERCISEX:
			// TODO This may convert the option to shares, or cash out
			break;

		// TODO this is the same as selling, or different?
		case SHRS_OUT:
			break;

		case EXPIRE:
			// TODO adjust lot info
			break;

		// TODO perhaps we need to adjust lot shares here?
		case STOCKSPLIT:
			break;

		// Cash-only transactions (no lots affected)
		case DIV:
		case CASH:
		case CONTRIBX:
		case INT_INC:
		case MISC_INCX:
		case WITHDRAWX:
		case XIN:
		case XOUT:
		case REMINDER:
			break;

		case OTHER:
			assert (false); // can't happen
			return;
		}
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
					" acct=" + QifDom.dom.getAccountByID(this.acctid).getName() + //
					" " + Common.formatDate(getDate()) + "\n" + //
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

			// Common.reportWarning(s);

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

	public short getXferAcctid() {
		return (short) -this.xacctid;
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
				Common.formatDate(getDate()), //
				getAction().toString(), //
				secString, //
				getCashAmount());
		return s;
	}

	public String toStringShort(boolean veryshort) {
		String s = String.format("%s %s %s", //
				((this.stmtdate != null) ? "*" : " "), //
				Common.formatDate(getDate()), //
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
		s += " dt=" + Common.formatDate(getDate());
		s += " acct=" + QifDom.dom.getAccountByID(this.acctid).getName();
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
		s += " txt=" + this.textFirstLine;
		s += " memo=" + this.memo;
		s += " comm=" + this.commission;
		s += " xact=" + this.accountForTransfer;
		s += " xamt=" + this.amountTransferred;
		s += "\n";

		return s;
	}
}