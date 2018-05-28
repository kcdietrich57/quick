package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SimpleTxn {
	private static final List<SimpleTxn> NOSPLITS = new ArrayList<SimpleTxn>();
	static int cashok = 0;
	static int cashbad = 0;

	protected static int nextid = 1;

	public final int txid;

	public int acctid;

	private BigDecimal amount;
	public String memo;

	public int xacctid;
	public int catid; // >0: CategoryID; <0 AccountID
	public SimpleTxn xtxn;

	public SimpleTxn(int acctid) {
		this.txid = nextid++;

		this.acctid = acctid;
		this.amount = null;
		this.memo = null;

		this.catid = 0;
		this.xacctid = 0;
		this.xtxn = null;
	}

	public SimpleTxn(SimpleTxn other) {
		this.txid = nextid++;

		this.acctid = other.acctid;
		this.amount = other.amount;
		this.memo = other.memo;
		this.catid = other.catid;

		this.xacctid = 0;
		this.xtxn = null;
	}

	public Account getAccount() {
		return QifDom.dom.getAccountByID(this.acctid);
	}

	public TxAction getAction() {
		return TxAction.CASH;
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
		return toStringShort(false);
	}

	// This is a compact, single-line version of the transaction, such as would
	// appear in a list for displaying and/or selecting transactions.
	public String toStringShort(boolean veryshort) {
		// TODO implement SimpleTxn::toStringShort()
		return "Tx" + this.txid;
	}

	// This is a more complete summary of the transaction, possibly split into
	// multiple lines.
	public String toStringLong() {
		final QifDom dom = QifDom.dom;

		String s = "Tx" + this.txid + ":";
		s += dom.getAccountByID(this.acctid).getName();
		s += " amt=" + this.amount;
		s += " memo=" + this.memo;

		// TODO why have both negative cat and xacct to represent the same
		// thing?
		if (this.xacctid < (short) 0) {
			s += " xacct=" + dom.getAccountByID(-this.xacctid).getName();
		} else if (this.catid < (short) 0) {
			s += " xcat=" + dom.getAccountByID(-this.catid).getName();
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

		if ((getAction() == TxAction.CASH) //
				|| (other.getAction() == TxAction.CASH)) {
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
}