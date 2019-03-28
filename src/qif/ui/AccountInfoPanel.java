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
 * statements, reconcile)<br>
 * AccountInfo<br>
 * AccountTabs<br>
 * Register | StatementView | Reconcile<br>
 * Statements | StatementDetails
 */
@SuppressWarnings("serial")
public class AccountInfoPanel extends JPanel //
		implements AccountSelectionListener {
	private AccountInfoHeaderPanel acctInfoPanel;
	private JSplitPane statementViewSplit;
	private AccountInfoStatementPanel statementPanel;
	private JTabbedPane acctTabbedPane;
	private AccountInfoStatementDetailsPanel statementDetailsPanel;
	private AccountInfoReconcilePanel reconcilePanel;
	private TransactionPanel registerTransactionPanel;

	public AccountInfoPanel() {
		setLayout(new BorderLayout());

		this.acctInfoPanel = new AccountInfoHeaderPanel();
		this.registerTransactionPanel = new TransactionPanel(true);
		this.statementPanel = new AccountInfoStatementPanel();
		this.statementDetailsPanel = new AccountInfoStatementDetailsPanel();
		this.reconcilePanel = new AccountInfoReconcilePanel();

		MainWindow.instance.acctInfoPanel = this.acctInfoPanel;
		MainWindow.instance.registerTransactionPanel = this.registerTransactionPanel;
		MainWindow.instance.statementPanel = this.statementPanel;
		MainWindow.instance.statementDetailsPanel = this.statementDetailsPanel;
		MainWindow.instance.reconcilePanel = this.reconcilePanel;

		this.statementViewSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, //
				this.statementPanel, this.statementDetailsPanel);

		this.acctTabbedPane = new JTabbedPane();

		this.acctTabbedPane.addTab("Register", this.registerTransactionPanel);
		this.acctTabbedPane.add("Statements", this.statementViewSplit);
		this.acctTabbedPane.add("Reconcile", this.reconcilePanel);

		add(this.acctInfoPanel, BorderLayout.NORTH);
		add(this.acctTabbedPane, BorderLayout.CENTER);

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