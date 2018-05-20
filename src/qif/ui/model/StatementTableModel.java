package qif.ui.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import qif.data.Account;
import qif.data.Common;
import qif.data.Statement;
import qif.ui.StatementDetailsPanel;

public class StatementTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;

	private Account curAccount = null;
	public StatementDetailsPanel detailsPanel;

	String[] columnNames = { //
			"Date", //
			"Balance", //
			"Cash", //
			"Credits", //
			"Debits", //
			"NumTx" };

	List<Object[]> values = new ArrayList<Object[]>();

	public Account getAccount() {
		return this.curAccount;
	}

	public Object getValue(int row, int col) {
		if (row < 0 || col < 0 || col >= columnNames.length || row >= values.size()) {
			return null;
		}

		return values.get(row)[col];
	}

	private Object[] newRow() {
		return new Object[] { "", "", "", "", "", "" };
	}

	private String stringValue(Object o) {
		if (o == null) {
			return "";
		}

		if (!(o instanceof String)) {
			return o.toString();
		}

		return (String) o;
	}

	public void setAccount(Account acct) {
		if (acct == curAccount) {
			return;
		}

		this.detailsPanel.setStatement(null);

		curAccount = acct;

		Account a = (Account) curAccount;

		values.clear();

		int rownum = 0;

		for (Statement s : a.statements) {
			Object[] row = newRow();

			while (values.size() <= rownum) {
				values.add(row);
			}

			int jj = 0;
			row[jj++] = Common.formatDate(s.date);
			row[jj++] = stringValue(s.closingBalance);
			row[jj++] = stringValue(s.cashBalance);
			row[jj++] = stringValue(s.getCredits());
			row[jj++] = stringValue(s.getDebits());
			row[jj++] = Integer.toString(s.transactions.size());

			for (int ii = 0; ii < row.length; ++ii) {
				setValueAt(row[ii], rownum, ii);
			}

			++rownum;
		}

		fireTableDataChanged();
	}

	public Date getDate(int rownum) {
		String datestr = stringValue(getValueAt(rownum, 0));

		return (!datestr.isEmpty()) ? Common.parseDate(datestr) : null;
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return values.size();
	}

	public String getColumnName(int col) {
		return columnNames[col];
	}

	public Object getValueAt(int row, int col) {
		if ((row < 0) || (col < 0) //
				|| (row >= values.size()) //
				|| (col >= columnNames.length)) {
			return null;
		}

		return values.get(row)[col];
	}

	// public Class getColumnClass(int c) {
	// return getValueAt(0, c).getClass();
	// }
}