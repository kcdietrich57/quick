package qif.ui.model;

import java.util.List;

import qif.data.Account;
import qif.data.GenericTxn;
import qif.data.QDate;
import qif.data.Statement;
import qif.ui.AccountSelectionListener;
import qif.ui.MainWindow;
import qif.ui.StatementSelectionListener;

/** Model for TransactionTable - txns in an account or statement */
@SuppressWarnings("serial")
public class TransactionTableModel //
		extends GenericTransactionTableModel //
		implements AccountSelectionListener, StatementSelectionListener {

	public TransactionTableModel() {
		super("transactionTable");
	}

	protected void setObject(Object obj, boolean update) {
		if (!update && (obj == this.curObject)) {
			return;
		}

		List<GenericTxn> txns = null;

		if (obj == null) {
			curStatement = null;
			curAccount = null;
		} else if (obj instanceof Account) {
			curAccount = (Account) obj;
			curStatement = null;
			txns = this.curAccount.transactions;
		} else if (obj instanceof Statement) {
			curStatement = (Statement) obj;
			curAccount = Account.getAccountByID(curStatement.acctid);
			txns = curStatement.transactions;
		} else {
			return;
		}

		setTransactions(txns);

		curObject = obj;
	}

	private void setTransactions(List<GenericTxn> txns) {
		this.allTransactions.clear();

		if ((txns != null) && !txns.isEmpty()) {
			if (MainWindow.instance.asOfDate.compareTo(QDate.today()) < 0) {
				for (GenericTxn txn : txns) {
					if (MainWindow.instance.asOfDate.compareTo(txn.getDate()) >= 0) {
						this.allTransactions.add(txn);
					}
				}
			} else {
				this.allTransactions.addAll(txns);
			}
		}

		fireTableDataChanged();
	}
}