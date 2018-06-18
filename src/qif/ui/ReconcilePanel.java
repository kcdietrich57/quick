package qif.ui;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import qif.data.Account;
import qif.data.Statement;

public class ReconcilePanel //
		extends JPanel //
		implements AccountSelectionListener {
	private static final long serialVersionUID = 1L;

	Account account;
	Statement stmt;

	ReconcileStatusPanel reconcileStatus;
	ReconcileTransactionsPanel reconcileTransactions;

	public ReconcilePanel() {
		super(new BorderLayout());

		this.reconcileTransactions = new ReconcileTransactionsPanel();
		this.reconcileStatus = new ReconcileStatusPanel(this.reconcileTransactions);

		add(reconcileStatus, BorderLayout.NORTH);
		add(reconcileTransactions, BorderLayout.CENTER);

		this.reconcileTransactions.addTransactionSelectionListener(this.reconcileStatus);
	}

	public void accountSelected(Account account) {
		if (this.account != account) {
			this.account = account;

			Statement stmt = (account != null) //
					? account.createNextStatementToReconcile() //
					: null;

			//this.reconcileTransactions.accountSelected(account);
			this.reconcileTransactions.statementSelected(stmt);
			this.reconcileStatus.setStatement(stmt);
		}
	}
}