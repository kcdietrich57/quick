package moneymgr.ui;

import moneymgr.model.Statement;

/** Support responding to statement selections */
public interface StatementSelectionListener {

	void statementSelected(Statement statement);

}