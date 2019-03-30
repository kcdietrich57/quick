package moneymgr.ui;

import moneymgr.model.GenericTxn;

/** Supports responding to transaction selection events */
public interface TransactionSelectionListener {

	void transactionSelected(GenericTxn transaction);

}