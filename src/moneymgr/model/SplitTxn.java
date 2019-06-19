package moneymgr.model;

import moneymgr.util.Common;
import moneymgr.util.QDate;

public class SplitTxn extends SimpleTxn {
	private SimpleTxn parent;

	public SplitTxn(SimpleTxn parent) {
		super(parent.getAccountID());
		
		this.parent = parent;
	}

	public int getAccountID() {
		GenericTxn parent = getGenericTxn();
		return parent.getAccountID();
	}

	public QDate getDate() {
		GenericTxn parent = getGenericTxn();
		return parent.getDate();
	}
	
	public void setDate(QDate date) {
		Common.reportError("Can't set date on split");
	}

	public GenericTxn getGenericTxn() {
		SimpleTxn parent = this;

		while ((parent instanceof SplitTxn) && (((SplitTxn) parent).parent != null)) {
			parent = ((SplitTxn) parent).parent;
		}

		return (GenericTxn) parent;
	}
}
