package moneymgr.ui.model;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import moneymgr.model.Account;
import moneymgr.ui.MainWindow;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/** Model for account list - categorized, open vs closed by date */
@SuppressWarnings("serial")
public class AccountTableModel //
		extends AbstractTableModel {

	private static final String[] columnNames = { "Name", "Type", "Balance" };

	private final List<Account> accounts = new ArrayList<Account>();

	private boolean showOpenAccounts = true;

	/** Reload data and set to show open vs closed accounts */
	public void reload(boolean showOpenAccounts) {
		this.showOpenAccounts = showOpenAccounts;

		reload();
	}

	/** Refresh model data */
	public void reload() {
		this.accounts.clear();

		List<Account> accts = Account.getSortedAccounts();
		for (Account acct : accts) {
			if (this.showOpenAccounts == accountIsOpenInPeriod(acct)) {
				this.accounts.add(acct);
			}
		}

		fireTableDataChanged();
	}

	/** Return whether an account is open in the date period being shown */
	private boolean accountIsOpenInPeriod(Account acct) {
		QDate start = MainWindow.instance.startAsOfDate;
		QDate end = MainWindow.instance.asOfDate;

		return acct.isOpenDuring(start, end);
//		return (acct.getOpenDate().compareTo(end) <= 0) //
//				&& ((acct.closeDate == null) || (acct.closeDate.compareTo(start) >= 0));
	}

	/** Get the position of the account in the displayed list */
	public int getAccountIndex(Account acct) {
		return this.accounts.indexOf(acct);
	}

	/** Get the account at a given position in the displayed list */
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
			return a.name;
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