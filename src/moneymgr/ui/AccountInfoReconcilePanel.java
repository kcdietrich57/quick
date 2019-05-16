package moneymgr.ui;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import moneymgr.model.Account;
import moneymgr.model.SecurityPortfolio;
import moneymgr.model.Statement;

/**
 * This panel contains the UI for reconciling a statement (status/control +
 * transactions). Contents are driven by account selection.<br>
 * Status<br>
 * Transactions
 */
@SuppressWarnings("serial")
public class AccountInfoReconcilePanel extends JPanel //
		implements AccountSelectionListener {
	private Account account;

	private AccountInfoReconcileStatusPanel reconcileStatusPanel;
	private AccountInfoReconcileTransactionsPanel reconcileTransactionsPanel;
	private AccountInfoReconcileSecurityPanel reconcileTransactionsSecurityPanel;
	private JSplitPane securitySplit;

	public AccountInfoReconcilePanel() {
		super(new BorderLayout());

		this.reconcileStatusPanel = new AccountInfoReconcileStatusPanel(this);
		this.reconcileTransactionsPanel = new AccountInfoReconcileTransactionsPanel();
		this.reconcileTransactionsSecurityPanel = new AccountInfoReconcileSecurityPanel();

		securitySplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, //
				this.reconcileTransactionsPanel, //
				this.reconcileTransactionsSecurityPanel);

		add(this.reconcileStatusPanel, BorderLayout.NORTH);
		add(this.securitySplit, BorderLayout.CENTER);

		this.reconcileTransactionsPanel.addTransactionSelectionListener(this.reconcileStatusPanel);

		MainWindow.instance.reconcileStatusPanel = this.reconcileStatusPanel;
		MainWindow.instance.reconcileTransactionsPanel = this.reconcileTransactionsPanel;
	}

	public void setSplitPosition() {
		this.securitySplit.setDividerLocation(.75);
	}

	/** Get portfolio changes for the current statement */
	public SecurityPortfolio getPortfolioDelta() {
		return this.reconcileTransactionsPanel.getPortfolioDelta();
	}

	public void accountSelected(Account acct, boolean update) {
		if (update || (this.account != acct)) {
			this.account = acct;

			Statement stmt = (acct != null) //
					? acct.getNextStatementToReconcile() //
					: null;

			this.reconcileTransactionsPanel.statementSelected(stmt);
			this.reconcileStatusPanel.statementSelected(stmt);
			this.reconcileTransactionsSecurityPanel.statementSelected(stmt);
		}
	}
}