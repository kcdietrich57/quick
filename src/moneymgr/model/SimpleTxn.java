package moneymgr.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import app.QifDom;
import moneymgr.io.TransactionInfo;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/**
 * Minimal transaction interface. Not sure this is necessary, isn't really used
 */
interface Txn {
	QDate getDate();

	void setDate(QDate date);

	int getAccountID();

	int getTxid();
}

/**
 * Minimal transaction info (acct, amt, category, memo). This can be<br>
 * specialized for various transaction types, or<br>
 * used to specify part of a split transaction that inherit other info like date
 * from the containing transaction.
 */
public abstract class SimpleTxn implements Txn {
	private static final List<SplitTxn> NOSPLITS = //
			Collections.unmodifiableList(new ArrayList<SplitTxn>());

	/** Keep track of any cash transfers that don't match */
	private static int cashok = 0;
	private static int cashbad = 0;

	public static enum ShareAction {
		NO_ACTION("None"), NEW_SHARES("New"), DISPOSE_SHARES("Dispose"), //
		TRANSFER_OUT("Xout"), TRANSFER_IN("Xin"), SPLIT("Split");

		private String name;

		private ShareAction(String name) {
			this.name = name;
		}

		public String toString() {
			return this.name;
		}
	}

	public final MoneyMgrModel model;
	private final long xid;
	private final int acctid;
	private final int txid;

	/** Dollar (cash) amount of the transaction */
	private BigDecimal amount;
	private String memo;

	/** Category id or transfer Account id (>0: CategoryID; <0 -AccountID) */
	private int catid;

	/** In the case of a cash transfer, the other transaction involved */
	private SimpleTxn xtxn_cash;
	
	public TransactionInfo info = null;

	public SimpleTxn(int txid, int acctid) {
		this.model = MoneyMgrModel.currModel;
		
		this.acctid = acctid;
		this.txid = (txid > 0) ? txid : this.model.createTxid();
		this.xid = (((long) acctid) << 32) + this.txid;

		this.amount = null;
		this.memo = null;

		this.catid = 0;
		this.xtxn_cash = null;
	}

	public SimpleTxn(int acctid) {
		this(0, acctid);
	}

	/** Do sanity check of splits belonging to this txn */
	public void verifySplit() {
		if (!hasSplits()) {
			return;
		}

		BigDecimal dec = BigDecimal.ZERO;

		for (SimpleTxn txn : getSplits()) {
			if (txn.hasSplits()) {
				txn.verifySplit();
			}

			dec = dec.add(txn.getAmount());
		}

		if (!dec.equals(getAmount())) {
			Common.reportError("Total(" + getAmount() + ") does not match split total (" + dec + ")");
		}
	}

	/** Do sanity check of splits for the root transaction containing this one */
	public void verifyAllSplit() {
		SimpleTxn root = this;
		while (root instanceof SplitTxn) {
			SplitTxn sroot = (SplitTxn) root;
			if (sroot.getParent() == null) {
				break;
			}

			root = sroot.getParent();
		}

		root.verifySplit();
	}

	public int getTxid() {
		return (int) this.xid;
	}

	public int getAccountID() {
		return (int) (this.xid >> 32);
	}

	/**
	 * TODO UNUSED<br>
	 * Check whether this transaction matches up with a split line in another to
	 * determine if this could be a transfer
	 */
	private boolean matchesSplit(SimpleTxn other) {
		if (other.hasSplits()) {
			for (SplitTxn stx : other.getSplits()) {
				int diff = (this.acctid == other.acctid) //
						? getAmount().compareTo(stx.getAmount()) //
						: getAmount().compareTo(stx.getAmount().negate());
				if (diff == 0) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Compares two transactions for testing CSV import matching up with QIF<br>
	 * (date/action/type/amount/.......)
	 */
	public int compareWith(TransactionInfo tuple, SimpleTxn other) {
		int diff;

		diff = getAccountID() - other.getAccountID();
		if (diff != 0) {
			// Sometimes we go to transfers
			// return diff;
		}

		diff = getDate().subtract(other.getDate());
		if (diff != 0) {
			if (Math.abs(diff) > 5) {
				return diff;
			}

			tuple.datemismatch = true;
//			tuple.addMessage("Date mismatch ignored: " //
//					+ getDate().toString() + " vs " + other.getDate().toString());
		}

		diff = (getAction().isEquivalentTo(other.getAction())) ? 0 : 1;
		if (diff != 0) {
			if (!Common.isEffectivelyZero(getAmount()) && (getAction() != TxAction.OTHER)) {
				tuple.addActionMessage("Can't replace " + getAction().toString() //
						+ " action in transaction with " + other.getAction().toString());
				return diff;
			}
//			if (getAction() == TxAction.CASH) {
//				if ((other.getAction() != TxAction.XIN) && (other.getAction() != TxAction.XOUT)) {
//					return diff;
//				}
//			} else if (other.getAction() == TxAction.CASH) {
//				if ((getAction() != TxAction.XIN) && (getAction() != TxAction.XOUT)) {
//					return diff;
//				}
//			}

			tuple.fixaction = true;
//			tuple.addMessage("Replacing OTHER action in transaction with " //
//					+ other.getAction().toString());
			this.setAction(other.getAction());
		}

//		diff = (this.acctid == other.acctid) //
//				? getAmount().compareTo(other.getAmount()) //
//				: getAmount().compareTo(other.getAmount().negate());
//		if (diff != 0) {
//			SimpleTxn xfer = getXtxn();
//			SimpleTxn oxfer = other.getXtxn();
//
//			if (other.getXferAcctid() == this.acctid) {
//				diff = 0;
//			} else if ((xfer != null) && (xfer.matchesSplit(other) || other.matchesSplit(xfer))) {
//				diff = 0;
//			} else if ((oxfer != null) && (this.matchesSplit(oxfer) || oxfer.matchesSplit(this))) {
//				diff = 0;
//			}
//
//			if (diff != 0) {
//				return diff;
//			}
//		}

		diff = getGain().compareTo(other.getGain());
		if (diff != 0) {
			return diff;
		}

		diff = (isCredit() == other.isCredit()) ? 0 : -1;
		if (diff != 0) {
			return diff;
		}

		// TODO mac InvTxn transferring to subsplit fails this test
		diff = getCashAmount().compareTo(other.getCashAmount());
		if (diff != 0) {
			// return diff;
		}

		// TODO THIS HAPPENS FREQUENTLY
		diff = getCatid() - other.getCatid();
		if (diff != 0) {
			// return diff;
		}

		diff = getCashTransferAcctid() - other.getCashTransferAcctid();
		if (diff != 0) {
			return diff;
		}

		// TODO mac InvTxn transferring to subsplit fails this test
		diff = getCashTransferAmount().compareTo(other.getCashTransferAmount());
		if (diff != 0) {
			// return diff;
		}

		// TODO this happens e.g. mac txn matched with win multipsplit
		diff = hasSplits() == other.hasSplits() ? 0 : -1;
		if (diff != 0) {
			// return diff;
		}

		// TODO compare splits

		// TODO THIS HAPPENS FREQUENTLY (e.g. Win Payee is Mac Description)
		diff = getPayee().compareTo(other.getPayee());
		if (diff != 0) {
			// return diff;
		}

		// TODO THIS HAPPENS FREQUENTLY
		diff = getMemo().compareTo(other.getMemo());
		if (diff != 0) {
			// return diff;
		}

		diff = this.getCheckNumber() - other.getCheckNumber();
		if (diff != 0) {
			// TODO cknum return diff;
		}

		return 0;
	}

	public Account getAccount() {
		return this.model.getAccountByID(getAccountID());
	}

	public final boolean isCredit() {
		// TODO is increasing security holdings a credit w/o cash impact?
		return (getCashAmount().signum() > 0) //
				? true //
				: addsShares();
	}

	/** Answer if this increases a security position in the account */
	public boolean addsShares() {
		return false;
	}

	/** Answer if this reduces a security position in the account */
	public boolean removesShares() {
		return false;
	}

	public TxAction getAction() {
		return TxAction.OTHER;
	}

	public void setAction(TxAction action) {
		// not implemented
	}

	public QDate getStatementDate() {
		return null;
	}

	/** Whether this transaction belongs to a statement */
	public final boolean isCleared() {
		return getStatementDate() != null;
	}

	public void setStatementDate(QDate date) {
		// not applicable
	}

	public boolean hasSplits() {
		return false;
	}

	public List<SplitTxn> getSplits() {
		return NOSPLITS;
	}

	public void addSplit(SplitTxn txn) {
		// not implemented
	}

	public int getCashTransferAcctid() {
		// TODO should not have splits and catid both
		if (hasSplits() && this.catid != 0) {
			//Common.reportWarning("Split tx has category value!");
		}
		return (!hasSplits() && this.catid < 0) ? -this.catid : 0;
	}

	public void setCashTransferAcctid(int acctid) {
		this.catid = -acctid;
	}

	private static final List<InvestmentTxn> NO_TXN = //
			Collections.unmodifiableList(new ArrayList<InvestmentTxn>());

	public List<InvestmentTxn> getSecurityTransferTxns() {
		return NO_TXN;
	}

	public SimpleTxn getCashTransferTxn() {
		return this.xtxn_cash;
	}

	public void setCashTransferTxn(SimpleTxn txn) {
		this.xtxn_cash = txn;
	}

	private static int intSign(int i) {
		return (i == 0) ? 0 : ((i < 0) ? -1 : 1);
	}

	public String getPayee() {
		return "";
	}

	public void setPayee(String payee) {
		// Not applicable
	}

	public String getCategory() {
		if (hasSplits()) {
			return "[Split]";
		}

		switch (intSign(this.catid)) {
		case 1:
			return this.model.getCategory(this.catid).name;
		case -1:
			return "[" + this.model.getAccountByID(-this.catid).name + "]";
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
		if (amount != null && //
				(this.amount == null || amount.compareTo(this.amount) != 0)) {
			this.amount = amount;
		}
	}

	public final BigDecimal getAmount() {
		if (this.amount == null) {
			// return BigDecimal.ZERO;
		}

		return this.amount;
	}

	public int getCheckNumber() {
		return 0;
	}

	/** Return the net amount of cash transferred in/out by this transaction */
	public BigDecimal getCashTransferAmount() {
		BigDecimal xfer = BigDecimal.ZERO;

		if (this.catid < 0) {
			xfer = this.amount;
		} else if (this.hasSplits()) {
			for (SimpleTxn split : this.getSplits()) {
				xfer = xfer.add(split.getCashTransferAmount());
			}
		}

		return xfer;
	}

	/** Return the impact of this transaction on the account's cash position */
	public BigDecimal getCashAmount() {
		BigDecimal cash = BigDecimal.ZERO;

		if (this.hasSplits()) {
			for (SimpleTxn split : this.getSplits()) {
				cash = cash.add(split.getCashAmount());
			}
		} else if (this.amount != null) {
			cash = this.amount;
		}

		return cash;
	}

	public void setCheckNumber(String cknum) {
		// not implemented
	}

	public BigDecimal getGain() {
		return BigDecimal.ZERO;
	}

	public String getMemo() {
		return (this.memo != null) ? this.memo : "";
	}

	public void setMemo(String memo) {
		this.memo = memo;
	}

	public Security getSecurity() {
		return null;
	}

	public final int getSecurityId() {
		Security sec = getSecurity();

		return (sec != null) ? sec.secid : 0;
	}

	public final String getSecuritySymbol() {
		Security sec = getSecurity();

		return (sec != null) ? sec.getSymbol() : "";
	}

	public final String getSecurityName() {
		Security sec = getSecurity();

		return (sec != null) ? sec.getName() : "";
	}

	/**
	 * Compare two transactions' values to see if they can represent a transfer.<br>
	 * If strict is false, we compare absolute values rather than the exact values.
	 */
	public boolean amountIsEqual(SimpleTxn other, boolean strict) {
		BigDecimal amt1 = getCashTransferAmount();
		BigDecimal amt2 = other.getCashTransferAmount();

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

		// If strict, the amounts should be negative of each other
		boolean ret = !eq || !strict;

		// TODO why is it 'bad' for the transactions to both be CASH?
		// NB action is only meaningful for investment transactions?
		if ((getAction() == TxAction.CASH) //
				|| (other.getAction() == TxAction.CASH)) {
			if (eq) {
				++cashbad;

				if (QifDom.verbose) {
					System.out.println(toString());
					System.out.println(other.toString());
					System.out.println("Cash ok=" + cashok + " bad=" + cashbad);
				}
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
		String s = " ";
		QDate d = getDate();
		s += ((d != null) ? d.toString() : "null");
		s += " Tx" + this.txid + ": ";
		s += (this instanceof GenericTxn) ? "  " : "S ";
		Account a = getAccount();
		s += ((a != null) ? a.name : "null");
		s += " " + Common.formatAmount(this.amount).trim();
		s += " " + getCategory();
		if (getCashTransferAcctid() > 0) {
			s += "(";
			s += "" + ((this.xtxn_cash != null) ? this.xtxn_cash.txid : "-");
			s += ")";
		}
		s += " memo=" + getMemo();

		return s;
	}

	public String matches(SimpleTxn other) {
		if (this.txid != other.txid //
				|| this.acctid != other.acctid //
				|| getAction() != other.getAction() //
				|| !getDate().equals(other.getDate()) //
				|| this.catid != other.catid //
				|| !Common.isEffectivelyEqual(getAmount(), other.getAmount()) //
				|| !Common.isEffectivelyEqual(getCashAmount(), other.getCashAmount()) //
				|| !Common.isEffectivelyEqual(getGain(), other.getGain()) //
				|| hasSplits() != other.hasSplits() //
				|| getCashTransferAcctid() != other.getCashTransferAcctid() //
				|| getSecurityId() != other.getSecurityId() //
				|| isCredit() != other.isCredit() //
				|| addsShares() != other.addsShares() //
				|| !getPayee().equals(other.getPayee()) //
				|| !Common.safeEquals(this.memo, other.memo) //
				|| getCheckNumber() != other.getCheckNumber() //
		) {
			return "genInfo";
		}

		if (this.getSecurityTransferTxns().size() != other.getSecurityTransferTxns().size()) {
			return "numSecXfer";
		}

		for (int idx = 0; idx < getSecurityTransferTxns().size(); ++idx) {
			InvestmentTxn t1 = getSecurityTransferTxns().get(idx);
			InvestmentTxn t2 = other.getSecurityTransferTxns().get(idx);

			if (t1.getTxid() != t2.getTxid()) {
				return "xtxn:mismatch";
			}
		}

		if (getSplits().size() != other.getSplits().size()) {
			return "numsplits";
		}

		for (int idx = 0; idx < getSplits().size(); ++idx) {
			SplitTxn t1 = getSplits().get(idx);
			SplitTxn t2 = other.getSplits().get(idx);

			String res = t1.matches(t2);
			if (res != null) {
				return "splittxn:" + res;
			}
		}

		return null;
	}
}