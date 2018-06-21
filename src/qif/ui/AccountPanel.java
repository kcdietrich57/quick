package qif.ui;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import qif.data.Account;

/**
 * This panel contains views/operations based on selected account (register,
 * statements, reconcile)
 */
@SuppressWarnings("serial")
public class AccountPanel //
		extends JPanel //
		implements AccountSelectionListener {
	private AccountInfoPanel acctInfoPanel;

	private JSplitPane statementViewSplit;
	private StatementPanel statementPanel;
	private StatementDetailsPanel statementDetailsPanel;

	private ReconcilePanel reconcilePanel;

	private TransactionPanel registerTransactionPanel;

	public AccountPanel() {
		setLayout(new BorderLayout());

		acctInfoPanel = new AccountInfoPanel();

		registerTransactionPanel = new TransactionPanel(true);

		statementPanel = new StatementPanel();
		statementDetailsPanel = new StatementDetailsPanel();

		statementViewSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, //
				statementPanel, statementDetailsPanel);

		reconcilePanel = new ReconcilePanel(statementPanel);

		JTabbedPane acctTabbedPane = new JTabbedPane();
		acctTabbedPane.addTab("Register", registerTransactionPanel);
		acctTabbedPane.add("Statements", statementViewSplit);
		acctTabbedPane.add("Reconcile", reconcilePanel);

		add(acctInfoPanel, BorderLayout.NORTH);
		add(acctTabbedPane, BorderLayout.CENTER);

		this.statementPanel.addStatementSelectionListener(this.statementDetailsPanel);
	}

	public void accountSelected(Account acct, boolean update) {
		this.acctInfoPanel.accountSelected(acct, update);
		this.statementPanel.accountSelected(acct, update);
		this.registerTransactionPanel.accountSelected(acct, update);
		this.reconcilePanel.accountSelected(acct, update);
	}

	public void setSplitPosition() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				statementViewSplit.setDividerLocation(.25);
			}
		});
	}
}