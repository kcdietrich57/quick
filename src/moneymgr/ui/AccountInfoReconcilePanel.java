package moneymgr.ui;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import moneymgr.model.Account;
import moneymgr.model.Statement;

/**
 * This panel contains the UI for reconciling a statement (status/control +
 * transactions). Contents are driven by account selection.<br>
 * Status<br>
 * Transactions
 */
@SuppressWarnings("serial")
public class AccountInfoReconcilePanel
		extends JPanel //
		implements AccountSelectionListener {
	private Account account;

	private AccountInfoReconcileStatusPanel reconcileStatusPanel;
	private AccountInfoReconcileTransactionsPanel reconcileTransactionsPanel;

	public AccountInfoReconcilePanel() {
		super(new BorderLayout());

		this.reconcileStatusPanel = new AccountInfoReconcileStatusPanel();
		this.reconcileTransactionsPanel = new AccountInfoReconcileTransactionsPanel();

		add(this.reconcileStatusPanel, BorderLayout.NORTH);
		add(this.reconcileTransactionsPanel, BorderLayout.CENTER);

		this.reconcileTransactionsPanel.addTransactionSelectionListener(this.reconcileStatusPanel);

		MainWindow.instance.reconcileStatusPanel = this.reconcileStatusPanel;
		MainWindow.instance.reconcileTransactionsPanel = this.reconcileTransactionsPanel;
	}

	public void accountSelected(Account acct, boolean update) {
		if (update || (this.account != acct)) {
			this.account = acct;

			Statement stmt = (acct != null) //
					? acct.getNextStatementToReconcile() //
					: null;

			this.reconcileTransactionsPanel.statementSelected(stmt);
			this.reconcileStatusPanel.statementSelected(stmt);
		}
	}
}