package qif.ui;

import qif.data.GenericTxn;

/** Supports responding to transaction selection events */
public interface TransactionSelectionListener {

	void transactionSelected(GenericTxn transaction);

}