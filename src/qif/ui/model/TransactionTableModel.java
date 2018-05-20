package qif.ui.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import qif.data.Account;
import qif.data.Common;
import qif.data.GenericTxn;
import qif.data.QifDom;
import qif.data.Statement;

public class TransactionTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;

	Object curObject = null;

	Account curAccount = null;
	Statement curStatement = null;

	String[] columnNames = { //
			"Clr", //
			"Date", //
			"Payee", //
			"Amount", //
			"Category", //
			"Memo", //
			"Balance"//
	};

	List<Object[]> values = new ArrayList<Object[]>();

	public TransactionTableModel() {
	}

	private Object[] newRow() {
		return new Object[] { "", "", "", "", "", "", "" };
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

	private void setTransactions(List<GenericTxn> txns) {
		values.clear();

		if (txns != null) {
			int rownum = 0;

			for (GenericTxn tx : txns) {
				Object[] row = newRow();

				while (values.size() <= rownum) {
					values.add(row);
				}

				row[0] = (tx.isCleared()) ? "x" : " ";
				row[1] = Common.formatDate(tx.getDate());
				row[2] = stringValue(tx.getPayee());
				row[3] = stringValue(tx.getAmount());
				row[4] = Integer.toString(tx.catid);
				row[5] = stringValue(tx.memo);
				row[6] = stringValue(tx.runningTotal);

				for (int ii = 0; ii < row.length; ++ii) {
					setValueAt(row[ii], rownum, ii);
				}

				++rownum;
			}
		}

		fireTableDataChanged();
	}

	public void setAccount(Account acct) {
		setObject(acct);
	}

	public void setStatement(Statement stmt) {
		setObject(stmt);
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
			curAccount = QifDom.getDomById(1).getAccount(curStatement.acctid);

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

	// Don't need to implement this method unless your table's editable.
	public boolean isCellEditable(int row, int col) {
		// Note that the data/cell address is constant,
		// no matter where the cell appears onscreen.
		if (col < 2) {
			return false;
		} else {
			return true;
		}
	}

	// Don't need to implement this method unless your table's editable.
	public void setValueAt(Object value, int row, int col) {
		while (values.size() <= row) {
			values.add(newRow());
		}

		values.get(row)[col] = value;
		fireTableCellUpdated(row, col);
	}
}