package qif.ui;

import qif.data.Account;
import qif.data.GenericTxn;
import qif.data.Statement;

public interface AccountSelectionListener {
	public void accountSelected(Account account);
}

interface StatementSelectionListener {
	public void statementSelected(Statement statement);
}

interface TransactionSelectionListener {
	public void transactionSelected(GenericTxn transaction);
}