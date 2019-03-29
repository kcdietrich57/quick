package qif.ui;

import qif.data.Statement;

/** Support responding to statement selections */
public interface StatementSelectionListener {

	void statementSelected(Statement statement);

}