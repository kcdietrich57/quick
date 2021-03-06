package moneymgr.ui.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import moneymgr.model.Account;
import moneymgr.model.Statement;
import moneymgr.ui.AccountSelectionListener;
import moneymgr.ui.MainWindow;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/** Model for statement table displaying the statements for an account */
@SuppressWarnings("serial")
public class StatementTableModel //
		extends AbstractTableModel //
		implements AccountSelectionListener {

	private static final String[] columnNames = { //
			"Clr", //
			"Date", //
			"Balance", //
			"Cash", //
			"Securities", //
			"Credits", //
			"Debits", //
			"NumTx" };

	private Account curAccount = null;

	private final List<Statement> statements = new ArrayList<Statement>();

	public void accountSelected(Account acct, boolean update) {
		if (!update && (acct == this.curAccount)) {
			return;
		}

		this.curAccount = acct;
		this.statements.clear();

		Statement unclearedStmt = null;

		if (acct != null) {
			QDate aod = MainWindow.instance.getAsOfDate();

			if (aod.compareTo(QDate.today()) < 0) {
				Statement laststmt = null;

				for (Statement stmt : acct.getStatements()) {
					if (aod.compareTo(stmt.date) >= 0) {
						this.statements.add(stmt);
						laststmt = stmt;
					}
				}

				// TODO do we keep creating statements each time the acct is selected?
				unclearedStmt = acct.createUnclearedStatement(laststmt);
			} else {
				this.statements.addAll(acct.getStatements());

				unclearedStmt = acct.getUnclearedStatement();
			}

			if (unclearedStmt != null) {
				this.statements.add(unclearedStmt);
			}
		}

		fireTableDataChanged();
	}

	/** Return the statement at a given position in the list */
	public Statement getStatementAt(int row) {
		if (row < 0 || row >= this.statements.size()) {
			return null;
		}

		return this.statements.get(row);
	}

	/** Return the statement date at a given position in the list */
	public Date getDate(int rownum) {
		String datestr = Common.stringValue(getValueAt(rownum, 0));

		return (!datestr.isEmpty()) ? Common.parseDate(datestr) : null;
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return this.statements.size();
	}

	public String getColumnName(int col) {
		return columnNames[col];
	}

	public Object getValueAt(int row, int col) {
		if (row < 0 || col < 0 || col >= columnNames.length || row >= statements.size()) {
			return null;
		}

		Statement s = this.statements.get(row);

		switch (col) {
		case 0:
			return (s.isBalanced()) ? "yes" : "no";
		case 1:
			return s.date.toString();
		case 2:
			return Common.stringValue(s.closingBalance);
		case 3:
			return Common.stringValue(s.getCashBalance());
		case 4:
			return (s.holdings != null && !s.holdings.isEmpty()) //
					? Common.formatAmount(s.holdings.getPortfolioValueForDate(s.date)) //
					: "";
		case 5:
			return Common.stringValue(s.getCredits());
		case 6:
			return Common.stringValue(s.getDebits());
		case 7:
			return Integer.toString(s.transactions.size());
		}

		return null;
	}

	// public Class getColumnClass(int c) {
	// return getValueAt(0, c).getClass();
	// }
}