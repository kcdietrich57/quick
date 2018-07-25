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
public class ReconcilePanel //
		extends JPanel //
		implements AccountSelectionListener {
	private Account account;

	private ReconcileStatusPanel reconcileStatusPanel;
	private ReconcileTransactionsPanel reconcileTransactionsPanel;

	public ReconcilePanel() {
		super(new BorderLayout());

		this.reconcileTransactionsPanel = new ReconcileTransactionsPanel();
		this.reconcileStatusPanel = new ReconcileStatusPanel();

		add(reconcileStatusPanel, BorderLayout.NORTH);
		add(reconcileTransactionsPanel, BorderLayout.CENTER);

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