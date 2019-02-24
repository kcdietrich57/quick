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

		this.acctInfoPanel = new AccountInfoPanel();
		MainWindow.instance.acctInfoPanel = this.acctInfoPanel;
		this.registerTransactionPanel = new TransactionPanel(true);
		MainWindow.instance.registerTransactionPanel = this.registerTransactionPanel;
		this.statementPanel = new StatementPanel();
		MainWindow.instance.statementPanel = this.statementPanel;
		this.statementDetailsPanel = new StatementDetailsPanel();
		MainWindow.instance.statementDetailsPanel = this.statementDetailsPanel;
		this.reconcilePanel = new ReconcilePanel();
		MainWindow.reconcilePanel = this.reconcilePanel;

		this.statementViewSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, //
				statementPanel, statementDetailsPanel);

		JTabbedPane acctTabbedPane = new JTabbedPane();
		acctTabbedPane.addTab("Register", registerTransactionPanel);
		acctTabbedPane.add("Statements", statementViewSplit);
		acctTabbedPane.add("Reconcile", reconcilePanel);

		add(acctInfoPanel, BorderLayout.NORTH);
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
				statementViewSplit.setDividerLocation(.25);
			}
		});
	}
}