package qif.ui;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import qif.data.Account;

public class AccountPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	Account account;

	JTabbedPane acctTabbedPane;

	AccountInfoPanel acctInfoPanel;

	public JSplitPane statementViewSplit;
	StatementPanel statementPanel;
	StatementDetailsPanel statementDetails;
	ReconcilePanel reconcilePanel;

	TransactionPanel transactionPanel;

	public AccountPanel() {
		setLayout(new BorderLayout());

		acctInfoPanel = new AccountInfoPanel();

		transactionPanel = new TransactionPanel(true);

		statementPanel = new StatementPanel();
		statementDetails = new StatementDetailsPanel();

		statementViewSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, statementPanel, statementDetails);

		reconcilePanel = new ReconcilePanel(statementPanel);

		acctTabbedPane = new JTabbedPane();
		acctTabbedPane.addTab("Register", transactionPanel);
		acctTabbedPane.add("Statements", statementViewSplit);
		acctTabbedPane.add("Reconcile", reconcilePanel);

		add(acctInfoPanel, BorderLayout.NORTH);
		add(acctTabbedPane, BorderLayout.CENTER);

		this.statementPanel.addStatementSelectionListener(this.statementDetails);
	}

	public void addAccountSelectionListeners(AccountListPanel accountListPanel) {
		accountListPanel.addAccountSelectionListener(this.acctInfoPanel);
		accountListPanel.addAccountSelectionListener(this.statementPanel);
		accountListPanel.addAccountSelectionListener(this.transactionPanel);
		accountListPanel.addAccountSelectionListener(this.reconcilePanel);
	}

	public void setSplitPosition() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				statementViewSplit.setDividerLocation(.25);
			}
		});
	}
}