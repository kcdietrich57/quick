
package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class SimpleTxn {
	private static final List<SimpleTxn> NOSPLITS = new ArrayList<SimpleTxn>();

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

	public final int id;

	public short domid;
	public short acctid;

	private BigDecimal amount;
	public String memo;

	public short xacctid;
	public short catid; // >0: CategoryID; <0 AccountID
	public SimpleTxn xtxn;

	public SimpleTxn(short domid, short acctid) {
		this.id = nextid++;

		this.domid = domid;
		this.acctid = acctid;
		this.amount = null;
		this.memo = null;

		this.catid = 0;
		this.xacctid = 0;
		this.xtxn = null;
	}

	public SimpleTxn(short domid, SimpleTxn other) {
		this.id = nextid++;

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

	public BigDecimal getTotalAmount() {
		return this.amount;
	}

	public String toString() {
		return toStringLong();
	}

	public String toStringShort() {
		// TODO implement
		return "";
	}

	public String toStringLong() {
		final QifDom dom = QifDom.getDomById(this.domid);

		String s = "Tx" + this.id + ":";
		s += " acct=" + dom.getAccount(this.acctid).name;
		s += " amt=" + this.amount;
		s += " memo=" + this.memo;

		if (this.xacctid < (short) 0) {
			s += " xacct=" + dom.getAccount(-this.xacctid).name;
		} else if (this.catid < (short) 0) {
			s += " xcat=" + dom.getAccount(-this.catid).name;
		} else if (this.catid > (short) 0) {
			s += " cat=" + dom.getCategory(this.catid).name;
		}

		return s;
	}
};

class MultiSplitTxn extends SimpleTxn {
	public List<SimpleTxn> subsplits = new ArrayList<SimpleTxn>();

	public MultiSplitTxn(short domid, short acctid) {
		super(domid, acctid);
	}

	public MultiSplitTxn(short domid, MultiSplitTxn other) {
		super(domid, other);

		for (final SimpleTxn st : other.subsplits) {
			this.subsplits.add(new SimpleTxn(domid, st));
		}
	}

	public BigDecimal getTotalAmount() {
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
	public BigDecimal runningTotal;

	public static GenericTxn clone(short domid, GenericTxn txn) {
		if (txn instanceof NonInvestmentTxn) {
			return new NonInvestmentTxn(domid, (NonInvestmentTxn) txn);
		}
		if (txn instanceof InvestmentTxn) {
			return new InvestmentTxn(domid, (InvestmentTxn) txn);
		}

		return null;
	}

	public GenericTxn(short domid, short acctid) {
		super(domid, acctid);

		this.date = null;
		this.clearedStatus = null;
		this.stmtdate = null;
		this.runningTotal = null;
	}

	public GenericTxn(short domid, GenericTxn other) {
		super(domid, other);

		this.date = other.date;
		this.clearedStatus = other.clearedStatus;
		this.stmtdate = other.stmtdate;
		this.runningTotal = null;
	}

	public void repair() {
		if (getAmount() == null) {
			setAmount(BigDecimal.ZERO);
		}
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
};

class NonInvestmentTxn extends GenericTxn {
	public enum TransactionType {
		Check, Deposit, Payment, Investment, ElectronicPayee
	};

	public String chkNumber;
	public String payee;

	public List<String> address;
	public List<SimpleTxn> split;

	public NonInvestmentTxn(short domid, short acctid) {
		super(domid, acctid);

		this.chkNumber = "";
		this.payee = "";

		this.address = new ArrayList<String>();
		this.split = new ArrayList<SimpleTxn>();
	}

	public NonInvestmentTxn(short domid, NonInvestmentTxn other) {
		super(domid, other);

		this.chkNumber = other.chkNumber;
		this.payee = other.payee;

		this.address = new ArrayList<String>();
		this.split = new ArrayList<SimpleTxn>();

		for (final String a : other.address) {
			this.address.add(new String(a));
		}
		for (final SimpleTxn st : other.split) {
			this.split.add(st);
		}
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
		String s = Common.getDateString(getDate());
		s += " " + this.chkNumber;
		s += " " + getAmount();
		s += " " + this.payee;

		return s;
	}

	public String toStringLong() {
		final QifDom dom = QifDom.getDomById(this.domid);

		String s = "Tx" + this.id + ":";
		s += " acct=" + dom.getAccount(this.acctid).name;
		s += " date=" + Common.getDateString(getDate());
		s += " clr:" + this.clearedStatus;
		s += " num=" + this.chkNumber;
		s += " payee=" + this.payee;
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

	public InvestmentTxn(short domid, short acctid) {
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

	public InvestmentTxn(short domid, InvestmentTxn other) {
		super(domid, other);

		final QifDom dom = QifDom.getDomById(domid);

		this.action = other.action;
		this.security = dom.findSecurityByName(other.security.name);
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
				// TODO what to do about this?
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
		BigDecimal amt = getTotalAmount();
		if (amt == null) {
			amt = BigDecimal.ZERO;
		}

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
					" acct=" + QifDom.getDomById(1).getAccount(this.acctid).name + //
					" " + Common.getDateString(getDate()) + "\n" + //
					"  sec=" + this.security.name + //
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

	public BigDecimal getTotalAmount() {
		BigDecimal tot = super.getTotalAmount();

		if (tot == null) {
			tot = getXferAmount();
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

	public String toStringLong() {
		final QifDom dom = QifDom.getDomById(this.domid);

		String s = "InvTx:";
		s += " acct=" + dom.getAccount(this.acctid).name;
		s += " dt=" + Common.getDateString(getDate());
		s += " act=" + this.action;
		if (this.security != null) {
			s += " sec=" + this.security.name;
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
