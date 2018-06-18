package qif.ui.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import qif.data.Account;
import qif.data.Category;
import qif.data.Common;
import qif.data.GenericTxn;
import qif.data.InvestmentTxn;
import qif.data.QDate;
import qif.data.Statement;
import qif.ui.AccountSelectionListener;

public class ReconcileTransactionTableModel //
		extends AbstractTableModel //
		implements AccountSelectionListener {
	private static final long serialVersionUID = 1L;

	private static final String[] columnNames = { //
			"Date", //
			"Type", //
			"Payee", //
			"Amount", //
			"Category", //
			"Memo", //
			"Balance"//
	};

	Object curObject;
	Account curAccount;
	Statement curStatement;

	public final List<GenericTxn> allTransactions;
	public final List<GenericTxn> clearedTransactions;

	public ReconcileTransactionTableModel() {
		this.curObject = null;
		this.curAccount = null;
		this.curStatement = null;
		this.allTransactions = new ArrayList<GenericTxn>();
		this.clearedTransactions = new ArrayList<GenericTxn>();
	}

	public Statement getStatement() {
		return this.curStatement;
	}

	public void accountSelected(Account account) {
		setAccount(account);
	}

	public void setAccount(Account acct) {
		setObject(acct);
	}

	public void setStatement(Statement stmt) {
		setObject(stmt);
	}

	private void setTransactions() {
		this.allTransactions.clear();
		this.clearedTransactions.clear();

		if (this.curStatement != null) {
			this.clearedTransactions.addAll(this.curStatement.transactions);

			this.allTransactions.addAll(this.clearedTransactions);
			this.allTransactions.addAll(this.curStatement.unclearedTransactions);

			sortTransactionsForDisplay();
		}

		fireTableDataChanged();
	}

	private void sortTransactionsForDisplay() {
		if (this.allTransactions != null) {
			Collections.sort(this.allTransactions, (t1, t2) -> {
				boolean cleared1 = this.clearedTransactions.contains(t1);
				boolean cleared2 = this.clearedTransactions.contains(t2);
				if (cleared1 != cleared2) {
					return (cleared1) ? -1 : 1;
				}

				return t1.compareTo(t2);
			});
		}
	}

	public BigDecimal getCredits() {
		BigDecimal tot = BigDecimal.ZERO;

		for (GenericTxn txn : this.clearedTransactions) {
			if (txn.getCashAmount().signum() > 0) {
				tot = tot.add(txn.getCashAmount());
			}
		}

		return tot;
	}

	public BigDecimal getDebits() {
		BigDecimal tot = BigDecimal.ZERO;

		for (GenericTxn txn : this.clearedTransactions) {
			if (txn.getCashAmount().signum() < 0) {
				tot = tot.add(txn.getCashAmount());
			}
		}

		return tot;
	}

	public BigDecimal getClearedCashBalance() {
		if (this.curStatement == null) {
			return BigDecimal.ZERO;
		}

		BigDecimal tot = curStatement.getOpeningBalance();

		for (GenericTxn txn : this.clearedTransactions) {
			tot = tot.add(txn.getCashAmount());
		}

		return tot;
	}

	private void setObject(Object obj) {
		if (obj == this.curObject) {
			return;
		}

		if (obj == null) {
			this.curStatement = null;
			this.curAccount = null;

			setTransactions();
		} else if (obj instanceof Statement) {
			this.curStatement = (Statement) obj;
			this.curAccount = Account.getAccountByID(curStatement.acctid);

			setTransactions();
		} else {
			return;
		}

		this.curObject = obj;
	}

	public void setStatementDate(QDate date) {
		if ((date == null) || (curAccount == null)) {
			return;
		}

		Statement s = curAccount.getStatement(date, null);
		if (s != null) {
			this.curStatement = s;
			setTransactions();
		}
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return allTransactions.size();
	}

	public String getColumnName(int col) {
		return columnNames[col];
	}

	public GenericTxn getTransactionAt(int row) {
		if (row < 0 || row >= this.allTransactions.size()) {
			return null;
		}

		return this.allTransactions.get(row);
	}

	public void clearAll() {
		if (this.clearedTransactions.size() < this.allTransactions.size()) {
			this.clearedTransactions.clear();
			this.clearedTransactions.addAll(this.allTransactions);

			sortTransactionsForDisplay();
			fireTableDataChanged();
		}
	}

	public void unclearAll() {
		if (!this.clearedTransactions.isEmpty()) {
			this.clearedTransactions.clear();

			sortTransactionsForDisplay();
			fireTableDataChanged();
		}
	}

	public void toggleTransactionCleared(GenericTxn txn) {
		if (clearedTransactions.contains(txn)) {
			clearedTransactions.remove(txn);
		} else {
			clearedTransactions.add(txn);
		}

		sortTransactionsForDisplay();
	}

	public Object getValueAt(int row, int col) {
		GenericTxn tx = getTransactionAt(row);

		if (tx == null || col < 0 || col >= columnNames.length) {
			return null;
		}

		switch (col) {
		case 0:
			return tx.getDate().toString();

		case 1:
			return tx.getAction().toString();

		case 2:
			if (tx instanceof InvestmentTxn) {
				InvestmentTxn itx = (InvestmentTxn) tx;

				if (itx.security != null) {
					return itx.security.getName();
				}
			}

			return Common.stringValue(tx.getPayee());

		case 3:
			return Common.stringValue(tx.getAmount());

		case 4: {
			if (tx.catid > 0) {
				return Category.getCategory(tx.catid).name;
			}

			int acctid = 0;

			if (tx.catid < 0) {
				acctid = -tx.catid;
			} else if (tx instanceof InvestmentTxn) {
				acctid = ((InvestmentTxn) tx).getXferAcctid();
			}

			if (acctid > 0) {
				return "[" + Account.getAccountByID(acctid).getName() + "]";
			}

			return "";
		}

		case 5:
			return Common.stringValue(tx.memo);

		case 6:
			return Common.stringValue(tx.runningTotal);
		}

		return null;
	}

	// public Class getColumnClass(int c) {
	// return getValueAt(0, c).getClass();
	// }

	// Don't need to implement this method unless your table's editable.
	// public boolean isCellEditable(int row, int col) {
	// // Note that the data/cell address is constant,
	// // no matter where the cell appears onscreen.
	// if (col < 2) {
	// return false;
	// } else {
	// return true;
	// }
	// }

	// Don't need to implement this method unless your table's editable.
	// public void setValueAt(Object value, int row, int col) {
	// while (values.size() <= row) {
	// values.add(null);
	// }
	//
	// //values.get(row)[col] = value;
	// fireTableCellUpdated(row, col);
	// }
}
