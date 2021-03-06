package moneymgr.ui.model;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import moneymgr.model.Account;
import moneymgr.model.MoneyMgrModel;
import moneymgr.ui.MainFrame;
import moneymgr.ui.MainWindow;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/** Model for account list - categorized, open vs closed by date */
@SuppressWarnings("serial")
public class AccountTableModel //
		extends AbstractTableModel {

	private static final String[] columnNames = { "Name", "Type", "Balance" };

	private final MoneyMgrModel model;
	private final List<Account> accounts = new ArrayList<Account>();

	private boolean includeClosedAccounts = false;
	private boolean includeZeroBalanceAccounts = false;
	private boolean showTodayBalance = false;

	public AccountTableModel() {
		this.model = MainFrame.appFrame.model;
	}

	/** Reload data and set to show open vs closed accounts */
	public void reload(boolean includeClosedAccounts, //
			boolean includeZeroBalanceAccounts, //
			boolean showTodayBalance) {
		this.includeClosedAccounts = includeClosedAccounts;
		this.includeZeroBalanceAccounts = includeZeroBalanceAccounts;
		this.showTodayBalance = showTodayBalance;

		reload();
	}

	/** Refresh model data */
	public void reload() {
		this.accounts.clear();

		List<Account> accts = this.model.getSortedAccounts(this.showTodayBalance);
		for (Account acct : accts) {
			if (accountIsVisible(acct)) {
				this.accounts.add(acct);
			}
		}

		fireTableDataChanged();
	}

	/** Return whether an account is open in the date period being shown */
	private boolean accountIsVisible(Account acct) {
		QDate start = MainWindow.instance.startAsOfDate;
		QDate end = MainWindow.instance.getAsOfDate();

		boolean isopen = acct.isOpenDuring(start, end);
		boolean isnonzero = !Common.isEffectivelyZero(acct.getValueForDate(end));

		return (isopen || this.includeClosedAccounts) //
				&& (isnonzero || this.includeZeroBalanceAccounts);
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
		case 2: {
			QDate thedate = (this.showTodayBalance) //
					? QDate.today() //
					: MainWindow.instance.getAsOfDate();
			return Common.formatAmount0(a.getValueForDate(thedate));
		}
		}

		return null;
	}

	// public Class getColumnClass(int c) {
	// return getValueAt(0, c).getClass();
	// }
}