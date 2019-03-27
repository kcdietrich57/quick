package qif.ui;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import qif.data.Account;
import qif.data.Statement;

/**
 * This panel contains the UI for reconciling a statement (status/control +
 * transactions). Contents are driven by account selection.
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

		this.reconcileTransactionsPanel = new AccountInfoReconcileTransactionsPanel();
		this.reconcileStatusPanel = new AccountInfoReconcileStatusPanel();

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