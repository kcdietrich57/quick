package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal transaction info (acct, amt, category, memo). This can be<br>
 * specialized for various transaction types, or<br>
 * used to specify part of a split transaction that inherit other info like date
 * from the containing transaction.
 */
public class SimpleTxn {
	private static final List<SimpleTxn> NOSPLITS = new ArrayList<SimpleTxn>();

	static int cashok = 0;
	static int cashbad = 0;

	private static int nextid = 1;

	public final int acctid;
	public final int txid;

	private BigDecimal amount;
	private String memo;

	private int catid; // >0: CategoryID; <0 AccountID
	private SimpleTxn xtxn;

	public SimpleTxn(int acctid) {
		this.txid = nextid++;

		this.acctid = acctid;
		this.amount = null;
		this.memo = null;

		this.catid = 0;
		this.xtxn = null;
	}

	public SimpleTxn(SimpleTxn other) {
		this.txid = nextid++;

		this.acctid = other.acctid;
		this.amount = other.amount;
		this.memo = other.memo;
		this.catid = other.catid;

		this.xtxn = null;
	}

	public Account getAccount() {
		return Account.getAccountByID(this.acctid);
	}

	public void setDate(QDate date) {
		// simpletxn inherits its date from containing txn
	}

	public boolean removesShares() {
		return false;
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

	public int getXferAcctid() {
		return (this.catid < 0) ? -this.catid : 0;
	}

	public SimpleTxn getXtxn() {
		return this.xtxn;
	}

	public void setXtxn(SimpleTxn txn) {
		this.xtxn = txn;
	}

	public String getCategory() {
		if (hasSplits()) {
			return "[Split]";
		}

		if (this.catid > 0) {
			return Category.getCategory(this.catid).name;
		}

		int acctid = -this.catid;

		return (acctid > 0) //
				? "[" + Account.getAccountByID(acctid).getName() + "]" //
				: "N/A";
	}

	public int getCatid() {
		return this.catid;
	}

	public void setCatid(int catid) {
		this.catid = catid;
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

	public String getMemo() {
		return (this.memo != null) ? this.memo : "";
	}

	public void setMemo(String memo) {
		this.memo = memo;
	}

	public String toString() {
		return toStringLong();
	}

	// This is a compact, single-line version of the transaction, such as would
	// appear in a list for displaying and/or selecting transactions.
	public String toStringShort(boolean veryshort) {
		return "Tx" + this.txid;
	}

	// This is a more complete summary of the transaction, possibly split into
	// multiple lines.
	public String toStringLong() {
		String s = "Tx" + this.txid + ":";
		s += Account.getAccountByID(this.acctid).getName();
		s += " amt=" + this.amount;
		s += " memo=" + getMemo();

		if (this.catid < (short) 0) {
			s += " xacct=" + Account.getAccountByID(-this.catid).getName();
		} else if (this.catid > (short) 0) {
			s += " cat=" + Category.getCategory(this.catid).name;
		}

		return s;
	}

	public String formatValue() {
		return String.format("  %10s  %-25s  %-30s", //
				Common.formatAmount(this.amount), //
				getCategory(), //
				getMemo());
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