package qif.ui;

import qif.data.GenericTxn;

public interface TransactionSelectionListener {
	void transactionSelected(GenericTxn transaction);
}