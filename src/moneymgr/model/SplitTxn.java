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

	public SplitTxn(SimpleTxn parent) {
		super(parent.getAccountID());

		this.parent = parent;
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
