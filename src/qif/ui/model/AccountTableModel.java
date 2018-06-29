package qif.ui.model;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import qif.data.Account;
import qif.data.Common;
import qif.ui.MainWindow;

@SuppressWarnings("serial")
public class AccountTableModel extends AbstractTableModel {

	private static final String[] columnNames = { "Name", "Type", "Balance" };

	private final List<Account> accounts = new ArrayList<Account>();

	private boolean showOpenAccounts = true;

	public void reload(boolean showOpenAccounts) {
		this.showOpenAccounts = showOpenAccounts;

		reload();
	}

	public void reload() {
		this.accounts.clear();

		for (Account acct : Account.getSortedAccounts()) {
			if ((this.showOpenAccounts //
					&& acct.isOpenOn(MainWindow.instance.asOfDate)) //
					|| (!this.showOpenAccounts //
							&& !acct.isOpenOn(MainWindow.instance.asOfDate) //
							&& (acct.getOpenDate().compareTo(MainWindow.instance.asOfDate) <= 0))) {
				this.accounts.add(acct);
			}
		}

		fireTableDataChanged();
	}

	public Account getAccountAt(int row) {
		if ((row < 0) || (row >= this.accounts.size())) {
			return null;
		}

		return this.accounts.get(row);
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return accounts.size();
	}

	public String getColumnName(int col) {
		return columnNames[col];
	}

	public Object getValueAt(int row, int col) {
		if ((row < 0) || (col < 0) //
				|| (col >= columnNames.length) || (row >= accounts.size())) {
			return null;
		}

		Account a = this.accounts.get(row);
		if (a == null) {
			return "";
		}

		switch (col) {
		case 0:
			return a.getName();
		case 1:
			return a.type.toString();
		case 2:
			return Common.formatAmount0(a.getValueForDate(MainWindow.instance.asOfDate));
		}

		return null;
	}

	// public Class getColumnClass(int c) {
	// return getValueAt(0, c).getClass();
	// }
}