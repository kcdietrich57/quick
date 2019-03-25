package qif.ui.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import qif.data.Account;
import qif.data.Common;
import qif.data.GenericTxn;
import qif.data.SecurityPortfolio;
import qif.data.Statement;
import qif.persistence.Reconciler;
import qif.ui.AccountSelectionListener;
import qif.ui.StatementSelectionListener;

@SuppressWarnings("serial")
public class ReconcileTransactionTableModel //
		extends GenericTableModel //
		implements AccountSelectionListener, StatementSelectionListener {

	private final List<GenericTxn> clearedTransactions;

	public ReconcileTransactionTableModel() {
		super("reconcileTransactionTable");

		this.clearedTransactions = new ArrayList<GenericTxn>();
	}

	protected void setObject(Object obj, boolean update) {
		if (!update && (obj == this.curObject)) {
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

	public SecurityPortfolio getPortfolioDelta() {
		return (this.curStatement != null) //
				? this.curStatement.getPortfolioDelta(this.clearedTransactions) //
				: null;
	}

	public Statement createNextStatementToReconcile() {
		return (this.curAccount != null) //
				? this.curAccount.getNextStatementToReconcile() //
				: null;
	}

	public boolean isCleared(GenericTxn txn) {
		return this.clearedTransactions.contains(txn);
	}

	public void finishStatement() {
		if (this.curStatement != null) {
			this.curStatement.unclearAllTransactions();
			this.curStatement.clearTransactions(this.clearedTransactions);

			BigDecimal clearedbal = this.curStatement.getClearedCashBalance();

			if ((clearedbal != null) //
					&& Common.isEffectivelyEqual(clearedbal, this.curStatement.cashBalance)) {
				this.curStatement.isBalanced = true;

				if (!this.curAccount.statements.contains(this.curStatement)) {
					this.curAccount.statements.add(this.curStatement);
				}

				this.curStatement.dirty = true;
				Reconciler.saveReconciledStatement(this.curStatement);
			} else {
				System.out.println("Can't finish statement");
			}
		}
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

		BigDecimal tot = curStatement.getOpeningCashBalance();

		for (GenericTxn txn : this.clearedTransactions) {
			tot = tot.add(txn.getCashAmount());
		}

		return tot;
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
}
