package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// TODO could be abstract with a SplitTransation subclass
/**
 * Minimal transaction info (acct, amt, category, memo). This can be<br>
 * specialized for various transaction types, or<br>
 * used to specify part of a split transaction that inherit other info like date
 * from the containing transaction.
 */
public class SimpleTxn {
	private static final List<SimpleTxn> NOSPLITS = //
			Collections.unmodifiableList(new ArrayList<SimpleTxn>());

	/** Keep track of any cash transfers that don't match */
	private static int cashok = 0;
	private static int cashbad = 0;

	private static int nextid = 1;

	public final int acctid;
	public final int txid;

	/** Dollar amount of the transaction. For simple transactions, this is cash */
	private BigDecimal amount;
	private String memo;

	/** Category id or transfer Account id (>0: CategoryID; <0 -AccountID) */
	private int catid;

	/** In the case of a transfer, the other transaction involved */
	private SimpleTxn xtxn;

	public SimpleTxn(int acctid) {
		this.txid = nextid++;

		this.acctid = acctid;
		this.amount = null;
		this.memo = null;

		this.catid = 0;
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

	private int intSign(int i) {
		return (i == 0) ? 0 : ((i < 0) ? -1 : 1);
	}

	public String getCategory() {
		if (hasSplits()) {
			return "[Split]";
		}

		switch (intSign(this.catid)) {
		case 1:
			return Category.getCategory(this.catid).name;
		case -1:
			return "[" + Account.getAccountByID(-this.catid).name + "]";
		default:
			return "N/A";
		}
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

	/** Return the impact of this transaction on the account's cash position */
	public BigDecimal getCashAmount() {
		return this.amount;
	}

	public String getMemo() {
		return (this.memo != null) ? this.memo : "";
	}

	public void setMemo(String memo) {
		this.memo = memo;
	}

	/**
	 * Compare two transactions' values. If strict is false, we compare absolute
	 * values rather than the exact values.
	 */
	public boolean amountIsEqual(SimpleTxn other, boolean strict) {
		BigDecimal amt1 = getXferAmount();
		BigDecimal amt2 = other.getXferAmount();

		if (amt1.abs().compareTo(amt2.abs()) != 0) {
			return false;
		}

		if (BigDecimal.ZERO.compareTo(amt1) == 0) {
			// If value is zero, there's nothing more to check
			++cashok;
			return true;
		}

		// We know the magnitude is the same and non-zero
		// Check whether they are equal or negative of each other
		boolean eq = amt1.equals(amt2);

		boolean ret = !eq || !strict;

		// TODO why is it 'bad' for the transactions to both be CASH?
		if ((getAction() == TxAction.CASH) //
				|| (other.getAction() == TxAction.CASH)) {
			if (eq) {
				++cashbad;

				System.out.println(toString());
				System.out.println(other.toString());
				System.out.println("Cash ok=" + cashok + " bad=" + cashbad);
			} else {
				++cashok;
			}

			return ret;
		}

		return ret;
	}

	/** Return a representation of this object for display */
	public String formatValue() {
		return String.format("  %10s  %-25s  %-30s", //
				Common.formatAmount(this.amount), //
				getCategory(), //
				getMemo());
	}

	public String toString() {
		return toStringLong();
	}

	/**
	 * Create a compact, single-line version of the transaction, such as would
	 * appear in a list for displaying and/or selecting transactions.
	 */
	public String toStringShort(boolean veryshort) {
		return "Tx" + this.txid;
	}

	/**
	 * Create a more complete summary of the transaction, possibly split into
	 * multiple lines.
	 */
	public String toStringLong() {
		String s = "Tx" + this.txid + ":";
		s += Account.getAccountByID(this.acctid).name;
		s += " amt=" + this.amount;
		s += " memo=" + getMemo();

		if (this.catid < (short) 0) {
			s += " xacct=" + Account.getAccountByID(-this.catid).name;
		} else if (this.catid > (short) 0) {
			s += " cat=" + Category.getCategory(this.catid).name;
		}

		return s;
	}
}