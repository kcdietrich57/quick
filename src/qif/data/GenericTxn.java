
package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class SimpleTxn {
	private static final List<SimpleTxn> NOSPLITS = new ArrayList<SimpleTxn>();

	protected static long nextid = 1;
	public final long id;

	public short acctid;

	protected BigDecimal amount;
	public String memo;

	public short xacctid;
	public short catid; // >0: CategoryID; <0 AccountID
	public SimpleTxn xtxn;

	public SimpleTxn(short acctid) {
		this.id = nextid++;

		this.acctid = acctid;
		this.amount = null;
		this.memo = null;

		this.catid = 0;
		this.xacctid = 0;
		this.xtxn = null;
	}

	public SimpleTxn(SimpleTxn other) {
		this.id = nextid++;

		this.acctid = other.acctid;
		this.amount = other.amount;
		this.memo = other.memo;
		this.catid = other.catid;

		this.xacctid = 0;
		this.xtxn = null;
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

	public BigDecimal getXferAmount() {
		return this.amount;
	}

	public String toString(QifDom dom) {
		String s = "Tx" + this.id + ":";
		s += " acct=" + ((dom != null) ? dom.accounts.get(this.acctid).name : this.acctid);
		s += " amt=" + this.amount;
		s += " memo=" + this.memo;

		if (dom == null) {
			s += " acctid=" + this.xacctid;
		} else {
			if (this.xacctid < (short) 0) {
				s += " xacct=" + dom.accounts.get(-this.xacctid).name;
			} else if (this.catid < (short) 0) {
				s += " xcat=" + dom.accounts.get(-this.catid).name;
			} else if (this.catid > (short) 0) {
				s += " cat=" + dom.categories.get(this.catid).name;
			}
		}

		return s;
	}

	public String toString() {
		return toString(null);
	}
};

class MultiSplitTxn extends SimpleTxn {
	public List<SimpleTxn> subsplits = new ArrayList<SimpleTxn>();

	public MultiSplitTxn(short acctid) {
		super(acctid);
	}

	public MultiSplitTxn(MultiSplitTxn other) {
		super(other);

		for (SimpleTxn st : other.subsplits) {
			this.subsplits.add(new SimpleTxn(st));
		}
	}
};

public abstract class GenericTxn extends SimpleTxn {
	private Date date;
	public String clearedStatus;
	public Date stmtdate;

	public static GenericTxn clone(GenericTxn txn) {
		if (txn instanceof NonInvestmentTxn) {
			return new NonInvestmentTxn((NonInvestmentTxn) txn);
		}
		if (txn instanceof InvestmentTxn) {
			return new InvestmentTxn((InvestmentTxn) txn);
		}

		return null;
	}

	public GenericTxn(short acctid) {
		super(acctid);

		this.date = null;
		this.clearedStatus = null;
		this.stmtdate = null;
	}

	public GenericTxn(GenericTxn other) {
		super(other);

		this.date = other.date;
		this.clearedStatus = other.clearedStatus;
		this.stmtdate = other.stmtdate;
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

	public String toStringShort() {
		// TODO implement
		return "";
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

	public NonInvestmentTxn(short acctid) {
		super(acctid);

		this.chkNumber = "";
		this.payee = "";

		this.address = new ArrayList<String>();
		this.split = new ArrayList<SimpleTxn>();
	}

	public NonInvestmentTxn(NonInvestmentTxn other) {
		super(other);

		this.chkNumber = other.chkNumber;
		this.payee = other.payee;

		this.address = new ArrayList<String>();
		this.split = new ArrayList<SimpleTxn>();

		for (String a : other.address) {
			this.address.add(new String(a));
		}
		for (SimpleTxn st : other.split) {
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

		BigDecimal dec = new BigDecimal(0);

		for (SimpleTxn txn : this.split) {
			dec = dec.add(txn.amount);
		}

		if (!dec.equals(this.amount)) {
			Common.reportError("Total(" + this.amount + ") does not match split total (" + dec + ")");
		}
	}

	public String toString() {
		return toString(null);
	}

	public String toString(QifDom dom) {
		return toStringLong(dom);
	}

	public String toStringShort() {
		String s = Common.getDateString(getDate());
		s += " " + this.chkNumber;
		s += " " + this.amount;
		s += " " + this.payee;

		return s;
	}

	public String toStringLong(QifDom dom) {
		String s = "Tx" + this.id + ":";
		s += " acct=" + ((dom != null) ? dom.accounts.get(this.acctid).name : this.acctid);
		s += " date=" + Common.getDateString(getDate());
		s += " clr:" + this.clearedStatus;
		s += " num=" + this.chkNumber;
		s += " payee=" + this.payee;
		s += " amt=" + this.amount;
		s += " memo=" + this.memo;
		if (dom == null) {
			s += " catid=" + this.catid;
		} else {
			if (this.catid < (short) 0) {
				s += " xacct=[" + dom.accounts.get(-this.catid).name + "]";
			} else if (this.catid > (short) 0) {
				s += " cat=" + dom.categories.get(this.catid).name;
			}
		}

		if (!this.address.isEmpty()) {
			s += "\n  addr= ";
			for (String a : this.address) {
				s += "\n  " + a;
			}
		}

		if (!this.split.isEmpty()) {
			s += "\n  splits \n";

			for (SimpleTxn txn : this.split) {
				if (txn.catid < (short) 0) {
					s += " [" + dom.accounts.get(-txn.catid).name + "]";
				} else if (txn.catid > (short) 0) {
					s += " " + dom.categories.get(txn.catid).name;
				}
				s += " " + txn.amount;
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
	public String action;
	public String security;
	public BigDecimal price;
	public BigDecimal quantity;
	public String textFirstLine;
	public BigDecimal commission;
	public String accountForTransfer;
	public BigDecimal amountTransferred;

	public InvestmentTxn(short acctid) {
		super(acctid);

		this.action = "";
		this.security = "";
		this.price = null;
		this.quantity = null;
		this.textFirstLine = "";
		this.commission = null;
		this.accountForTransfer = "";
		this.amountTransferred = null;
	}

	public InvestmentTxn(InvestmentTxn other) {
		super(other);

		this.action = other.action;
		this.security = other.security;
		this.price = other.price;
		this.quantity = other.quantity;
		this.textFirstLine = other.textFirstLine;
		this.commission = other.commission;
		this.accountForTransfer = other.accountForTransfer;
		this.amountTransferred = other.amountTransferred;
	}

	public BigDecimal getXferAmount() {
		return (this.amountTransferred != null) //
				? this.amountTransferred //
				: super.getXferAmount();
	}

	public short getXferAcctid() {
		return (short) -this.xacctid;
	}

	public String toString() {
		String s = "InvTx:";
		s += " dt=" + Common.getDateString(getDate());
		s += " act=" + this.action;
		s += " sec=" + this.security;
		s += " price=" + this.price;
		s += " qty=" + this.quantity;
		s += " amt=" + this.amount;
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
