package qif.ui.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import qif.data.Account;
import qif.data.Common;
import qif.data.Statement;
import qif.ui.AccountSelectionListener;

/** Model for statement table displaying the statements for an account */
@SuppressWarnings("serial")
public class StatementTableModel //
		extends AbstractTableModel //
		implements AccountSelectionListener {

	private static final String[] columnNames = { //
			"Date", //
			"Balance", //
			"Cash", //
			"Securities", //
			"Credits", //
			"Debits", //
			"NumTx" };

	private Account account = null;

	private final List<Statement> statements = new ArrayList<Statement>();

	public void accountSelected(Account acct, boolean update) {
		if (update || (acct != this.account)) {
			this.account = acct;
			this.statements.clear();

			if (acct != null) {
				this.statements.addAll(acct.statements);

				if (acct.getUnclearedTransactionCount() > 0) {
					this.statements.add(acct.getUnclearedStatement());
				}
			}

			fireTableDataChanged();
		}
	}

	public Statement getStatementAt(int row) {
		if (row < 0 || row >= this.statements.size()) {
			return null;
		}

		return this.statements.get(row);
	}

	public Date getDate(int rownum) {
		String datestr = Common.stringValue(getValueAt(rownum, 0));

		return (!datestr.isEmpty()) ? Common.parseDate(datestr) : null;
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return statements.size();
	}

	public String getColumnName(int col) {
		return columnNames[col];
	}

	public Object getValueAt(int row, int col) {
		if (row < 0 || col < 0 || col >= columnNames.length || row >= statements.size()) {
			return null;
		}

		Statement s = statements.get(row);

		switch (col) {
		case 0:
			return s.date.toString();
		case 1:
			return Common.stringValue(s.closingBalance);
		case 2:
			return Common.stringValue(s.cashBalance);
		case 3:
			if (s.holdings != null && !s.holdings.positions.isEmpty()) {
				return Common.formatAmount(s.holdings.getPortfolioValueForDate(s.date));
			} else {
				return "";
			}
		case 4:
			return Common.stringValue(s.getCredits());
		case 5:
			return Common.stringValue(s.getDebits());
		case 6:
			return Integer.toString(s.transactions.size());
		}

		return null;
	}

	// public Class getColumnClass(int c) {
	// return getValueAt(0, c).getClass();
	// }
}