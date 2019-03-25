package qif.ui.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import qif.data.Account;
import qif.data.Common;
import qif.data.GenericTxn;
import qif.data.InvestmentTxn;
import qif.data.Statement;
import qif.ui.model.TableProperties.ColumnProperties;

@SuppressWarnings("serial")
abstract class GenericTableModel extends AbstractTableModel {

	public class ColumnWidthListener implements PropertyChangeListener {
		private final GenericTableModel tableModel;
		private final TableColumnModel columnModel;

		public ColumnWidthListener( //
				GenericTableModel tableModel, //
				TableColumnModel columnModel) {
			this.tableModel = tableModel;
			this.columnModel = columnModel;
		}

		public void propertyChange(PropertyChangeEvent e) {
			if (e.getPropertyName().equals("preferredWidth")) {
				TableColumn tableColumn = (TableColumn) e.getSource();
				int index = columnModel.getColumnIndex(tableColumn.getHeaderValue());

				int newval = ((Integer) e.getNewValue()).intValue();
				System.out.println(propertyName + ": setting width " + index + " to " + newval);
				this.tableModel.setColumnWidth(index, newval);
			}
		}
	};

	protected Object curObject;
	protected Account curAccount;
	protected Statement curStatement;

	protected final List<GenericTxn> allTransactions;

	private String propertyName;
	private TableProperties properties = null;

	public GenericTableModel(String propName) {
		this.propertyName = propName;

		this.curObject = null;
		this.curAccount = null;
		this.curStatement = null;
		this.allTransactions = new ArrayList<GenericTxn>();

		if (properties == null) {
			properties = new TableProperties(new String[] { //
					"Date", "Type", "Payee", "Amount", //
					"Category", "Memo", "Shares", "Cash Balance" //
			});

			properties.load(this.propertyName);
		}
	}

	public void accountSelected(Account account, boolean update) {
		if (update || (account != this.curAccount)) {
			setObject(account, update);
		}
	}

	public void statementSelected(Statement stmt) {
		setObject(stmt, false);
	}

	protected abstract void setObject(Object obj, boolean update);

	public int getRowCount() {
		return allTransactions.size();
	}

	public GenericTxn getTransactionAt(int row) {
		if (row < 0 || row >= allTransactions.size()) {
			return null;
		}

		return this.allTransactions.get(row);
	}

	public Object getValueAt(int row, int col) {
		GenericTxn tx = getTransactionAt(row);

		if (tx == null || col < 0 || col >= getColumnCount()) {
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

		case 4:
			return tx.getCategory();

		case 5:
			return tx.getMemo();

		case 6:
			if (tx instanceof InvestmentTxn) {
				InvestmentTxn itx = (InvestmentTxn) tx;

				if (itx.security != null) {
					return Common.formatAmount3(itx.getShares()).trim() + "@" //
							+ Common.formatAmount3(itx.getShareCost()).trim();
				}
			}

			return ""; // Common.stringValue(tx.runningTotal);

		case 7:
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

	public void addColumnWidthListeners(JTable table) {
		PropertyChangeListener colWidthListener = //
				new ColumnWidthListener(this, table.getColumnModel());

		for (int colnum = 0; colnum < table.getColumnCount(); ++colnum) {
			TableColumn col = table.getColumnModel().getColumn(colnum);
			col.addPropertyChangeListener(colWidthListener);
		}
	}

	public void updateQifColumnProperties() {
		properties.save(this.propertyName);
	}

	public void setColumnWidths(TableColumnModel tranColumnModel) {
		for (int idx = 0; idx < properties.getNumVisibleColumns(); ++idx) {
			ColumnProperties cprop = properties.getVisibleColumnProperties(idx);

			tranColumnModel.getColumn(cprop.position).setPreferredWidth(cprop.width);
		}
	}

	public void setColumnWidth(int idx, int value) {
		ColumnProperties cprop = properties.getColumnProperties(idx);

		if (cprop != null) {
			cprop.width = value;
		}
	}

	public int getColumnCount() {
		return properties.getNumVisibleColumns();
	}

	public String getColumnName(int col) {
		ColumnProperties cprop = properties.getVisibleColumnProperties(col);

		return (cprop != null) ? cprop.name : "???";
	}
}