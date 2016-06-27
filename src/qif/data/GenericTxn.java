
package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class SimpleTxn {
	private static final List<SimpleTxn> NOSPLITS = new ArrayList<SimpleTxn>();
	static int cashok = 0;
	static int cashbad = 0;

	protected static int nextid = 1;

	public enum Action {
		OTHER, CASH, XIN, XOUT, WITHDRAWX, //
		CONTRIBX, INT_INC, MISC_INCX, //
		BUY, BUYX, SELL, SELLX, //
		GRANT, VEST, EXERCISEX, EXPIRE, //
		SHRS_IN, SHRS_OUT, //
		DIV, REINV_DIV, REINV_LG, REINV_SH, //
		REINV_INT, //
		STOCKSPLIT, REMINDER
	};

	public final int txid;

	public int domid;
	public int acctid;

	private BigDecimal amount;
	public String memo;

	public int xacctid;
	public int catid; // >0: CategoryID; <0 AccountID
	public SimpleTxn xtxn;

	public SimpleTxn(int domid, int acctid) {
		this.txid = nextid++;

		this.domid = domid;
		this.acctid = acctid;
		this.amount = null;
		this.memo = null;

		this.catid = 0;
		this.xacctid = 0;
		this.xtxn = null;
	}

	public SimpleTxn(int domid, SimpleTxn other) {
		this.txid = nextid++;

		this.domid = domid;
		this.acctid = other.acctid;
		this.amount = other.amount;
		this.memo = other.memo;
		this.catid = other.catid;

		this.xacctid = 0;
		this.xtxn = null;
	}

	public Account getAccount() {
		final QifDom dom = QifDom.getDomById(this.domid);

		return dom.getAccount(this.acctid);
	}

	public Action getAction() {
		return Action.OTHER;
	}

	public boolean hasSplits() {
		return false;
	}

	public List<SimpleTxn> getSplits() {
		return NOSPLITS;
	}

	public short getXferAcctid() {
		return (short) ((this.catid < 0) ? -this.catid : 0);
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public BigDecimal getAmount() {
		return this.amount;
	}

	public BigDecimal getXferAmount() {
		return this.amount;
	}

	// Return the impact of this transaction on the account cash position
	public BigDecimal getCashAmount() {
		return this.amount;
	}

	public String toString() {
		return toStringShort();
	}

	// This is a compact, single-line version of the transaction, such as would
	// appear in a list for displaying and/or selecting transactions.
	public String toStringShort() {
		// TODO implement SimpleTxn::toStringShort()
		return "Tx" + this.txid;
	}

	// This is a more complete summary of the transaction, possibly split into
	// multiple lines.
	public String toStringLong() {
		final QifDom dom = QifDom.getDomById(this.domid);

		String s = "Tx" + this.txid + ":";
		s += dom.getAccount(this.acctid).name;
		s += " amt=" + this.amount;
		s += " memo=" + this.memo;

		// TODO why have both negative cat and xacct to represent the same
		// thing?
		if (this.xacctid < (short) 0) {
			s += " xacct=" + dom.getAccount(-this.xacctid).name;
		} else if (this.catid < (short) 0) {
			s += " xcat=" + dom.getAccount(-this.catid).name;
		} else if (this.catid > (short) 0) {
			s += " cat=" + dom.getCategory(this.catid).name;
		}

		return s;
	}

	public boolean amountIsEqual(SimpleTxn other, boolean strict) {
		final BigDecimal amt1 = getXferAmount();
		final BigDecimal amt2 = other.getXferAmount();

		if (amt1.abs().compareTo(amt2.abs()) != 0) {
			return false;
		}

		if (BigDecimal.ZERO.compareTo(amt1) == 0) {
			return true;
		}

		// We know the magnitude is the same and non-zero
		// Check whether they are equal or negative of each other
		final boolean eq = amt1.equals(amt2);

		final boolean ret = !eq || !strict;

		if ((getAction() == Action.CASH) //
				|| (other.getAction() == Action.CASH)) {
			if (eq) {
				++cashbad;
			} else {
				++cashok;
			}

			System.out.println(toString());
			System.out.println(other.toString());
			System.out.println("Cash ok=" + cashok + " bad=" + cashbad);

			return ret;
		}

		return ret;
	}
};

// This handles the case where we have multiple splits that involve
// transferring from another account. The other account may have a single
// entry that corresponds to more than one split in the other account.
// N.B. Alternatively, we could merge the splits into one.
class MultiSplitTxn extends SimpleTxn {
	public List<SimpleTxn> subsplits = new ArrayList<SimpleTxn>();

	public MultiSplitTxn(int domid, int acctid) {
		super(domid, acctid);
	}

	public MultiSplitTxn(int domid, MultiSplitTxn other) {
		super(domid, other);

		for (final SimpleTxn st : other.subsplits) {
			this.subsplits.add(new SimpleTxn(domid, st));
		}
	}

	public BigDecimal getCashAmount() {
		BigDecimal total = BigDecimal.ZERO;

		for (final SimpleTxn t : this.subsplits) {
			total = total.add(t.getAmount());
		}

		return total;
	}
};

public abstract class GenericTxn extends SimpleTxn {
	private Date date;
	public String clearedStatus;
	public Date stmtdate;
	private String payee;
	public BigDecimal runningTotal;

	public static GenericTxn clone(int domid, GenericTxn txn) {
		if (txn instanceof NonInvestmentTxn) {
			return new NonInvestmentTxn(domid, (NonInvestmentTxn) txn);
		}
		if (txn instanceof InvestmentTxn) {
			return new InvestmentTxn(domid, (InvestmentTxn) txn);
		}

		return null;
	}

	public GenericTxn(int domid, int acctid) {
		super(domid, acctid);

		this.date = null;
		this.payee = "";
		this.clearedStatus = null;
		this.stmtdate = null;
		this.runningTotal = null;
	}

	public GenericTxn(int domid, GenericTxn other) {
		super(domid, other);

		this.date = other.date;
		this.payee = other.payee;
		this.clearedStatus = other.clearedStatus;
		this.stmtdate = other.stmtdate;
		this.runningTotal = null;
	}

	public int getCheckNumber() {
		return 0;
	}

	public void repair() {
		if (getAmount() == null) {
			setAmount(BigDecimal.ZERO);
		}
	}

	public String getPayee() {
		return this.payee;
	}

	public void setPayee(String payee) {
		this.payee = payee;
	}

	public boolean isCleared() {
		return this.stmtdate != null;
	}

	public void clear(Statement s) {
		this.stmtdate = s.date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Date getDate() {
		return this.date;
	}

	public String formatForSave() {
		final String s = String.format("T;%s;%d;%5.2f", //
				Common.getDateString(getDate()), //
				getCheckNumber(), //
				getCashAmount());
		return s;
	}
};

class NonInvestmentTxn extends GenericTxn {
	public enum TransactionType {
		Check, Deposit, Payment, Investment, ElectronicPayee
	};

	public String chkNumber;

	public List<String> address;
	public List<SimpleTxn> split;

	public NonInvestmentTxn(int domid, int acctid) {
		super(domid, acctid);

		this.chkNumber = "";

		this.address = new ArrayList<String>();
		this.split = new ArrayList<SimpleTxn>();
	}

	public NonInvestmentTxn(int domid, NonInvestmentTxn other) {
		super(domid, other);

		this.chkNumber = other.chkNumber;

		this.address = new ArrayList<String>();
		this.split = new ArrayList<SimpleTxn>();

		for (final String a : other.address) {
			this.address.add(new String(a));
		}
		for (final SimpleTxn st : other.split) {
			this.split.add(st);
		}
	}

	public int getCheckNumber() {
		if ((this.chkNumber == null) || (this.chkNumber.length() == 0) //
				|| !Character.isDigit(this.chkNumber.charAt(0))) {
			return 0;
		}

		return Integer.parseInt(this.chkNumber);
	}

	public boolean hasSplits() {
		return !this.split.isEmpty();
	}

	public List<SimpleTxn> getSplits() {
		return this.split;
	}

	public void verifySplit() {
		if (this.split.isEmpty()) {
			return;
		}

		BigDecimal dec = BigDecimal.ZERO;

		for (final SimpleTxn txn : this.split) {
			dec = dec.add(txn.getAmount());
		}

		if (!dec.equals(getAmount())) {
			Common.reportError("Total(" + getAmount() + ") does not match split total (" + dec + ")");
		}
	}

	public String toStringShort() {
		return String.format("%s %s %5s  %10.2f  %s", //
				((this.stmtdate != null) ? "*" : " "), //
				Common.getDateString(getDate()), //
				this.chkNumber, //
				getAmount(), //
				getPayee());
	}

	public String toStringLong() {
		final QifDom dom = QifDom.getDomById(this.domid);

		String s = ((this.stmtdate != null) ? "*" : " ") + "Tx" + this.txid + ":";
		s += " date=" + Common.getDateString(getDate());
		s += " acct=" + dom.getAccount(this.acctid).name;
		s += " clr:" + this.clearedStatus;
		s += " num=" + this.chkNumber;
		s += " payee=" + getPayee();
		s += " amt=" + getAmount();
		s += " memo=" + this.memo;

		if (this.catid < (short) 0) {
			s += " xacct=[" + dom.getAccount(-this.catid).name + "]";
		} else if (this.catid > (short) 0) {
			s += " cat=" + dom.getCategory(this.catid).name;
		}

		s += " bal=" + this.runningTotal;

		if (!this.address.isEmpty()) {
			s += "\n  addr= ";
			for (final String a : this.address) {
				s += "\n  " + a;
			}
		}

		if (!this.split.isEmpty()) {
			s += "\n  splits \n";

			for (final SimpleTxn txn : this.split) {
				if (txn.catid < (short) 0) {
					s += " [" + dom.getAccount(-txn.catid).name + "]";
				} else if (txn.catid > (short) 0) {
					s += " " + dom.getCategory(txn.catid).name;
				}

				s += " " + txn.getAmount();

				if (txn.memo != null) {
					s += " " + txn.memo;
				}

				s += "\n";
			}
		}

		return s;
	}
};

class InvestmentTxn extends GenericTxn {
	public Action action;
	public Security security;
	public BigDecimal price;
	public BigDecimal quantity;
	public String textFirstLine;
	public BigDecimal commission;
	public String accountForTransfer;
	public BigDecimal amountTransferred;
	public List<InvestmentTxn> xferInv;

	public InvestmentTxn(int domid, int acctid) {
		super(domid, acctid);

		this.action = Action.OTHER;
		this.security = null;
		this.price = null;
		this.quantity = null;
		this.textFirstLine = "";
		this.commission = null;
		this.accountForTransfer = "";
		this.amountTransferred = null;
		this.xferInv = null;
	}

	public InvestmentTxn(int domid, InvestmentTxn other) {
		super(domid, other);

		final QifDom dom = QifDom.getDomById(domid);

		this.action = other.action;
		this.security = dom.findSecurityByName(other.security.getName());
		this.price = other.price;
		this.quantity = other.quantity;
		this.textFirstLine = other.textFirstLine;
		this.commission = other.commission;
		this.accountForTransfer = other.accountForTransfer;
		this.amountTransferred = other.amountTransferred;

		this.xferInv = null;
	}

	public void repair() {
		switch (getAction()) {
		case CASH: {
			if ((getAmount() == null) && (this.amountTransferred == null)) {
				setAmount(BigDecimal.ZERO);
			}
		}
			break;

		case BUY:
		case SHRS_IN:
		case REINV_DIV:
		case REINV_LG:
		case REINV_SH:
		case GRANT:
		case EXPIRE:
			if (this.quantity == null) {
				// System.out.println("NULL quantities: " + ++nullQuantities);
				this.quantity = BigDecimal.ZERO;
			}

			// fall through

		case BUYX:
		case REINV_INT:
		case VEST:
			if (this.price == null) {
				this.price = BigDecimal.ZERO;
			}
			break;

		case SHRS_OUT:
		case SELL:
		case SELLX:
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
			break;

		case XOUT: { // + amt/xamt
			final BigDecimal amt = this.amountTransferred.negate();
			this.amountTransferred = amt;
			setAmount(amt);
			break;
		}

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

		case DIV:
			assert (this.price == null) && (this.quantity == null);
			break;

		case GRANT:
			// Strike price, open/close price, vest/expire date, qty
			// System.out.println(this);
			break;

		case VEST:
			// Connect to Grant
			// System.out.println(this);
			break;

		case EXERCISEX:
			// Connect to Grant, qty/price
			// System.out.println(this);
			break;

		case EXPIRE:
			// Connect to Grant, qty
			// System.out.println(this);
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

		case OTHER:
			Common.reportError("Transaction has unknown type: " + //
					QifDom.getDomById(1).getAccount(this.acctid).name);
			break;
		}

		super.repair();
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
					" acct=" + QifDom.getDomById(this.domid).getAccount(this.acctid).name + //
					" " + Common.getDateString(getDate()) + "\n" + //
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

			Common.reportWarning(s);

			this.price = newprice;
		}
	}

	public Action getAction() {
		return this.action;
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
				Common.getDateString(getDate()), //
				getAction().toString(), //
				secString, //
				getCashAmount());
		return s;
	}

	public String toStringShort() {
		return String.format("%s %s %10s  %10.2f  %s", //
				((this.stmtdate != null) ? "*" : " "), //
				Common.getDateString(getDate()), //
				this.action.toString(), //
				getAmount(), //
				((this.security != null) ? this.security.getName() : getPayee()));
	}

	public String toStringLong() {
		final QifDom dom = QifDom.getDomById(this.domid);

		String s = ((this.stmtdate != null) ? "*" : " ") + "InvTx" + this.txid + ":";
		s += " dt=" + Common.getDateString(getDate());
		s += " acct=" + dom.getAccount(this.acctid).name;
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
};
