package qif.ui.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import qif.data.Account;
import qif.data.Category;
import qif.data.Common;
import qif.data.GenericTxn;
import qif.data.InvestmentTxn;
import qif.data.QifDom;
import qif.data.Statement;
import qif.ui.AccountSelectionListener;

public class TransactionTableModel //
		extends AbstractTableModel //
		implements AccountSelectionListener {
	private static final long serialVersionUID = 1L;

	Object curObject = null;

	Account curAccount = null;
	Statement curStatement = null;

	String[] columnNames = { //
			"Date", //
			"Type", //
			"Payee", //
			"Amount", //
			"Category", //
			"Memo", //
			"Balance"//
	};

	List<GenericTxn> transations = new ArrayList<GenericTxn>();

	public void accountSelected(Account account) {
		setAccount(account);
	}

	public void setAccount(Account acct) {
		setObject(acct);
	}

	public void setStatement(Statement stmt) {
		setObject(stmt);
	}

	private void setTransactions(List<GenericTxn> txns) {
		transations.clear();

		if (txns != null) {
			this.transations = new ArrayList<GenericTxn>(txns);
		}

		fireTableDataChanged();
	}

	private void setObject(Object obj) {
		if (obj == curObject) {
			return;
		}

		if (obj == null) {
			curStatement = null;
			curAccount = null;

			setTransactions(null);
		} else if (obj instanceof Account) {
			curAccount = (Account) obj;
			curStatement = null;

			setTransactions(curAccount.transactions);
		} else if (obj instanceof Statement) {
			curStatement = (Statement) obj;
			curAccount = QifDom.dom.getAccountByID(curStatement.acctid);

			setTransactions(curStatement.transactions);
		} else {
			return;
		}

		curObject = obj;
	}

	public void setStatementDate(Date date) {
		if ((date == null) || (curAccount == null)) {
			return;
		}

		Statement s = curAccount.getStatement(date, null);
		if (s != null) {
			this.curStatement = s;
			setTransactions(s.transactions);
		}
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return transations.size();
	}

	public String getColumnName(int col) {
		return columnNames[col];
	}

	public GenericTxn getTransactionAt(int row) {
		if (row < 0 || row >= transations.size()) {
			return null;
		}

		return this.transations.get(row);
	}

	public Object getValueAt(int row, int col) {
		GenericTxn tx = getTransactionAt(row);

		if (tx == null || col < 0 || col >= columnNames.length) {
			return null;
		}

		switch (col) {
		case 0:
			return Common.formatDate(tx.getDate());

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
				return "[" + QifDom.dom.getAccountByID(acctid).getName() + "]";
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