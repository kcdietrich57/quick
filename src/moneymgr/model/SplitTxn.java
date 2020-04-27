package moneymgr.model;

import moneymgr.util.Common;
import moneymgr.util.QDate;

/**
 * A split line in a composite transaction. Supplies a subset of properties
 * (category, memo, amount) while inheriting the rest from the parent
 * transaction.
 */
public class SplitTxn extends SimpleTxn {
	private SimpleTxn parent;

	public SplitTxn(int txid, SimpleTxn parent) {
		super(txid, parent.getAccountID());

		this.parent = parent;
		// TODO why does this break things? setAmount(BigDecimal.ZERO);
	}

	public SplitTxn(SimpleTxn parent) {
		this(MoneyMgrModel.currModel.createTxid(), parent);
	}

	public SimpleTxn getParent() {
		return this.parent;
	}

	public void setParent(SimpleTxn parent) {
		this.parent = parent;
	}

	public SimpleTxn getCashTransferTxn() {
		SimpleTxn xtxn = super.getCashTransferTxn();
		return (xtxn != null) ? xtxn : getParent().getCashTransferTxn();
	}

	public int getAccountID() {
		GenericTxn parent = getContainingTxn();
		return parent.getAccountID();
	}

	public QDate getDate() {
		GenericTxn parent = getContainingTxn();
		return parent.getDate();
	}

	public String getPayee() {
		return this.parent.getPayee();
	}

	public int getCheckNumber() {
		return this.parent.getCheckNumber();
	}

	public void setDate(QDate date) {
		Common.reportError("Can't set date on split");
	}

	public GenericTxn getContainingTxn() {
		SimpleTxn parent = this;

		while ((parent instanceof SplitTxn) && (((SplitTxn) parent).parent != null)) {
			parent = ((SplitTxn) parent).parent;
		}

		return (GenericTxn) parent;
	}
}
