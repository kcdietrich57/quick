package qif.ui.model;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import qif.data.Account;
import qif.data.Common;

@SuppressWarnings("serial")
public class AccountTableModel extends AbstractTableModel {

	private static final String[] columnNames = { "Name", "Type", "Balance" };

	private final List<Account> accounts = new ArrayList<Account>();

	public void load(boolean showOpenAccounts) {
		List<Account> accts = Account.getSortedAccounts();

		this.accounts.clear();

		for (Account acct : accts) {
			if ((acct != null) && (acct.isOpenOn(null) == showOpenAccounts)) {
				accounts.add(acct);
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
		case 0:{
			// TODO highlight accounts properly
			return a.getName() + ((a.isStatementDue()) ? " *" : "");
		}
		case 1:
			return a.type.toString();
		case 2:
			return Common.formatAmount0(a.balance);
		}

		return null;
	}

	// public Class getColumnClass(int c) {
	// return getValueAt(0, c).getClass();
	// }
}