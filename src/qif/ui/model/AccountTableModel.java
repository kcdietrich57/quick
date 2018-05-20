package qif.ui.model;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import qif.data.Account;
import qif.data.QifDom;

public class AccountTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;

	private String[] columnNames = { "Name", "Type", "Balance" };
	private List<Object[]> values = new ArrayList<Object[]>();

	public AccountTableModel() {
	}

	public Object getValue(int row, int col) {
		if (row < 0 || col < 0 || col >= columnNames.length || row >= values.size()) {
			return null;
		}

		return values.get(row)[col];
	}

	// public Account getSelectedAccount() {
	// int idx =
	// }

	public void load(QifDom dom) {
		List<Account> accts = dom.getSortedAccounts();

		for (Account a : accts) {
			if (a != null && !a.isClosedAsOf(null)) {
				Object[] row = new Object[] { a.getName(), a.type.toString(), a.balance.toString() };
				values.add(row);
			}
		}

		fireTableDataChanged();
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
		return values.get(row)[col];
	}

	// public Class getColumnClass(int c) {
	// return getValueAt(0, c).getClass();
	// }
}