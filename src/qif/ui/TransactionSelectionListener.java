package qif.ui;

import qif.data.GenericTxn;

public interface TransactionSelectionListener {
	public void transactionSelected(GenericTxn transaction);
}