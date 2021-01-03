package moneymgr.ui.model;

import java.util.List;

import moneymgr.model.Account;
import moneymgr.model.GenericTxn;
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.Statement;
import moneymgr.ui.AccountSelectionListener;
import moneymgr.ui.MainWindow;
import moneymgr.ui.StatementSelectionListener;
import moneymgr.util.QDate;

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
			txns = this.curAccount.getTransactions();
		} else if (obj instanceof Statement) {
			curStatement = (Statement) obj;
			curAccount = MoneyMgrModel.currModel.getAccountByID(curStatement.acctid);
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
			QDate aod = MainWindow.instance.getAsOfDate();

			if (aod.compareTo(QDate.today()) < 0) {
				for (GenericTxn txn : txns) {
					if (aod.compareTo(txn.getDate()) >= 0) {
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