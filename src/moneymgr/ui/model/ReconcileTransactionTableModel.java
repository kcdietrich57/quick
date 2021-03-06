package moneymgr.ui.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import moneymgr.io.Reconciler;
import moneymgr.model.Account;
import moneymgr.model.GenericTxn;
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.SecurityPortfolio;
import moneymgr.model.Statement;
import moneymgr.ui.AccountSelectionListener;
import moneymgr.ui.MainFrame;
import moneymgr.ui.StatementSelectionListener;
import moneymgr.util.Common;

/** Manage txns in a statement being reconciled */
@SuppressWarnings("serial")
public class ReconcileTransactionTableModel //
		extends GenericTransactionTableModel //
		implements AccountSelectionListener, StatementSelectionListener {

	private final MoneyMgrModel model;
	private final List<GenericTxn> clearedTransactions;
	private final Reconciler reconciler;

	public ReconcileTransactionTableModel() {
		super("reconcileTransactionTable");

		this.model = MainFrame.appFrame.model;
		this.clearedTransactions = new ArrayList<GenericTxn>();
		this.reconciler = new Reconciler(this.model);
	}

	protected void setObject(Object obj, boolean update) {
		if (!update && (obj == this.curObject)) {
			return;
		}

		if (obj == null) {
			this.curStatement = null;
			this.curAccount = null;
		} else if (obj instanceof Account) {
			this.curStatement = null;
			this.curAccount = (Account) obj;
		} else if (obj instanceof Statement) {
			this.curStatement = (Statement) obj;
			this.curAccount = this.model.getAccountByID(curStatement.acctid);
		} else {
			return;
		}

		setTransactions();

		this.curObject = obj;
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

	/** Get portfolio changes for the current statement */
	public SecurityPortfolio getPortfolioDelta() {
		return (this.curStatement != null) //
				? this.curStatement.getPortfolioDelta(this.clearedTransactions) //
				: null;
	}

	/** Manufacture a dummy statement from uncleared transactions for display */
	public Statement createNextStatementToReconcile() {
		return (this.curAccount != null) //
				? this.curAccount.getNextStatementToReconcile() //
				: null;
	}

	/** Is a transaction marked as cleared */
	public boolean isCleared(GenericTxn txn) {
		return this.clearedTransactions.contains(txn);
	}

	/** Accept our cleared transactions for the current statement */
	public void finishStatement() {
		if (this.curStatement != null) {
			this.curStatement.unclearAllTransactions();
			this.curStatement.clearTransactions(this.clearedTransactions);

			BigDecimal clearedbal = this.curStatement.getClearedCashBalance();

			if ((clearedbal != null) //
					&& Common.isEffectivelyEqual(clearedbal, this.curStatement.getCashBalance())) {
				this.curStatement.setIsBalanced(true);

				this.curAccount.addStatement(this.curStatement);

				this.curStatement.dirty = true;
				this.reconciler.saveReconciledStatement(this.curStatement);
			} else {
				System.out.println("Can't finish statement");
			}
		}
	}

	/** Sort statement transactions - if cleared, then basic compare */
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

	/** Sum the credit transactions in this statement */
	public BigDecimal getCredits() {
		BigDecimal tot = BigDecimal.ZERO;

		for (GenericTxn txn : this.clearedTransactions) {
			if (txn.getCashAmount().signum() > 0) {
				tot = tot.add(txn.getCashAmount());
			}
		}

		return tot;
	}

	/** Sum the debit transactions in this statement */
	public BigDecimal getDebits() {
		BigDecimal tot = BigDecimal.ZERO;

		for (GenericTxn txn : this.clearedTransactions) {
			if (txn.getCashAmount().signum() < 0) {
				tot = tot.add(txn.getCashAmount());
			}
		}

		return tot;
	}

	/** Calculate the current cleared cash balance */
	public BigDecimal getClearedCashBalance() {
		if (this.curStatement == null) {
			return BigDecimal.ZERO;
		}

		BigDecimal tot = curStatement.getOpeningCashBalance();

		for (GenericTxn txn : this.clearedTransactions) {
			tot = tot.add(txn.getCashAmount());
		}

		return tot;
	}

	/** Mark all txns as cleared */
	public void clearAll() {
		if (this.clearedTransactions.size() < this.allTransactions.size()) {
			this.clearedTransactions.clear();
			this.clearedTransactions.addAll(this.allTransactions);

			sortTransactionsForDisplay();
			fireTableDataChanged();
		}
	}

	/** Mark all txns as uncleared */
	public void unclearAll() {
		if (!this.clearedTransactions.isEmpty()) {
			this.clearedTransactions.clear();

			sortTransactionsForDisplay();

			fireTableDataChanged();
		}
	}

	/** Toggle the cleared state of a transaction */
	public void toggleTransactionCleared(GenericTxn txn) {
		this.curStatement.toggleCleared(txn);

		if (clearedTransactions.contains(txn)) {
			clearedTransactions.remove(txn);
		} else {
			clearedTransactions.add(txn);
		}

		sortTransactionsForDisplay();

		fireTableDataChanged();
	}
}
