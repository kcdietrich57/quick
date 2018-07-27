package qif.ui.model;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;

import qif.data.Account;
import qif.data.Category;
import qif.data.Common;
import qif.data.GenericTxn;
import qif.data.InvestmentTxn;
import qif.data.QDate;
import qif.data.QifDom;
import qif.data.Statement;
import qif.ui.AccountSelectionListener;
import qif.ui.MainWindow;
import qif.ui.model.TableProperties.ColumnProperties;

/**
 * Model for TransactionTable - supply transactions in an account or statement
 */
@SuppressWarnings("serial")
public class TransactionTableModel //
		extends AbstractTableModel //
		implements AccountSelectionListener {

	private static TableProperties properties = null;

	public TransactionTableModel() {
		if (properties == null) {
			properties = new TableProperties();

			properties.addColumn("Date", 100);
			properties.addColumn("Type", 100);
			properties.addColumn("Payee", 100);
			properties.addColumn("Amount", 100);
			properties.addColumn("Category", 100);
			properties.addColumn("Memo", 100);
			properties.addColumn("Shares", 100);
			properties.addColumn("Cash Balance", 100);
		}
	}

	public void setColumnWidth(int idx, int value) {
		TableProperties.ColumnProperties cprop = properties.getColumnProperties(idx);

		if (cprop != null) {
			cprop.width = value;
		}
	}

	public void updateQifColumnProperties() {
		for (int idx = 0; idx < properties.getNumColumns(); ++idx) {
			ColumnProperties cprop = properties.getColumnProperties(idx);
			String key = "transactionTable." + cprop.name;

			String propstr = String.format("%d,%d,%d,%s", //
					cprop.id, cprop.position, cprop.width, //
					((cprop.visible) ? "y" : "n"));

			QifDom.qifProperties.setProperty(key, propstr);
		}
	}

	public void loadColumnProperties() {
		for (int idx = 0; idx < properties.getNumColumns(); ++idx) {
			ColumnProperties cprop = properties.getColumnProperties(idx);
			String key = "transactionTable." + cprop.name;

			String propstr = QifDom.qifProperties.getProperty(key);

			if (propstr != null) {
				String[] props = propstr.split(",");

				cprop.position = Integer.parseInt(props[1]);
				cprop.width = Integer.parseInt(props[2]);
				cprop.visible = props[3].charAt(0) == 'y';
			}
		}
	}

	private Object curObject = null;

	private Account curAccount = null;
	private Statement curStatement = null;

	private final List<GenericTxn> transactions = new ArrayList<GenericTxn>();

	public void setColumnWidths(TableColumnModel tranColumnModel) {
		for (int idx = 0; idx < properties.getNumVisibleColumns(); ++idx) {
			ColumnProperties cprop = properties.getVisibleColumnProperties(idx);
			tranColumnModel.getColumn(cprop.position).setPreferredWidth(cprop.width);
		}
	}

	public void accountSelected(Account account, boolean update) {
		setObject(account, update);
	}

	public void statementSelected(Statement stmt) {
		setObject(stmt, false);
	}

	private void setTransactions(List<GenericTxn> txns) {
		this.transactions.clear();

		if ((txns != null) && !txns.isEmpty()) {
			if (MainWindow.instance.asOfDate.compareTo(QDate.today()) < 0) {
				for (GenericTxn txn : txns) {
					if (MainWindow.instance.asOfDate.compareTo(txn.getDate()) >= 0) {
						this.transactions.add(txn);
					}
				}
			} else {
				this.transactions.addAll(txns);
			}
		}

		fireTableDataChanged();
	}

	private void setObject(Object obj, boolean update) {
		if (update || (obj != this.curObject)) {
			if (obj == null) {
				curStatement = null;
				curAccount = null;

				setTransactions(null);
			} else if (obj instanceof Account) {
				curAccount = (Account) obj;
				curStatement = null;

				setTransactions(this.curAccount.transactions);
			} else if (obj instanceof Statement) {
				curStatement = (Statement) obj;
				curAccount = Account.getAccountByID(curStatement.acctid);

				setTransactions(curStatement.transactions);
			} else {
				return;
			}

			curObject = obj;
		}
	}

	public void setStatementDate(QDate date) {
		if ((date == null) || (curAccount == null)) {
			return;
		}

		Statement stmt = curAccount.getStatement(date, null);
		if (stmt != null) {
			this.curStatement = stmt;
			setTransactions(stmt.transactions);
		}
	}

	public int getColumnCount() {
		return properties.getNumVisibleColumns();
	}

	public int getRowCount() {
		return transactions.size();
	}

	public String getColumnName(int col) {
		ColumnProperties cprop = properties.getVisibleColumnProperties(col);

		return (cprop != null) ? cprop.name : "???";
	}

	public GenericTxn getTransactionAt(int row) {
		if (row < 0 || row >= transactions.size()) {
			return null;
		}

		return this.transactions.get(row);
	}

	public Object getValueAt(int row, int col) {
		GenericTxn tx = getTransactionAt(row);

		if (tx == null || col < 0 || col >= properties.getNumVisibleColumns()) {
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
}