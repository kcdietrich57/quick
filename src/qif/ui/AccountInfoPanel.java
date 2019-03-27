package qif.ui;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import qif.data.Account;
import qif.data.GenericTxn;

/**
 * This panel contains views/operations based on selected account (register,
 * statements, reconcile)
 */
@SuppressWarnings("serial")
public class AccountInfoPanel
		extends JPanel //
		implements AccountSelectionListener {
	private AccountInfoHeaderPanel acctInfoPanel;

	private JSplitPane statementViewSplit;
	private AccountInfoStatementPanel statementPanel;
	private AccountInfoStatementDetailsPanel statementDetailsPanel;

	private AccountInfoReconcilePanel reconcilePanel;

	private TransactionPanel registerTransactionPanel;

	public AccountInfoPanel() {
		setLayout(new BorderLayout());

		this.acctInfoPanel = new AccountInfoHeaderPanel();
		MainWindow.instance.acctInfoPanel = this.acctInfoPanel;
		this.registerTransactionPanel = new TransactionPanel(true);
		MainWindow.instance.registerTransactionPanel = this.registerTransactionPanel;
		this.statementPanel = new AccountInfoStatementPanel();
		MainWindow.instance.statementPanel = this.statementPanel;
		this.statementDetailsPanel = new AccountInfoStatementDetailsPanel();
		MainWindow.instance.statementDetailsPanel = this.statementDetailsPanel;
		this.reconcilePanel = new AccountInfoReconcilePanel();
		MainWindow.reconcilePanel = this.reconcilePanel;

		this.statementViewSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, //
				this.statementPanel, this.statementDetailsPanel);

		JTabbedPane acctTabbedPane = new JTabbedPane();
		acctTabbedPane.addTab("Register", this.registerTransactionPanel);
		acctTabbedPane.add("Statements", this.statementViewSplit);
		acctTabbedPane.add("Reconcile", this.reconcilePanel);

		add(this.acctInfoPanel, BorderLayout.NORTH);
		add(acctTabbedPane, BorderLayout.CENTER);

		this.registerTransactionPanel.addTransactionSelectionListener(new TransactionSelectionListener() {
			public void transactionSelected(GenericTxn transaction) {
				System.out.println("Selected transaction: " + transaction.toString());
			}
		});
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
				AccountInfoPanel.this.statementViewSplit.setDividerLocation(.25);
			}
		});
	}
}