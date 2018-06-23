package qif.ui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import qif.data.Account;
import qif.data.Common;
import qif.data.GenericTxn;
import qif.data.Statement;
import qif.ui.model.ReconcileTransactionTableModel;

/**
 * This panel shows info about the reconcile process, and has controls to
 * perform actions
 */
@SuppressWarnings("serial")
class ReconcileStatusPanel //
		extends JPanel //
		implements StatementSelectionListener, TransactionSelectionListener {
	private Statement stmt;

	private JLabel lastStmt;
	private JLabel date;
	private JLabel open;
	private JLabel credits;
	private JLabel debits;
	private JLabel reconciledBalance;
	private JLabel difference;

	private JTextField closingCashField;
	private BigDecimal clearedCashBalance;
	private BigDecimal diffValue;

	private JButton selectAllButton;
	private JButton deselectAllButton;
	private JButton finishButton;

	private AccountPanel accountPanel;
	private ReconcileTransactionsPanel reconcileTransactionsPanel;
	private StatementPanel statementPanel;

	public ReconcileStatusPanel( //
			AccountPanel accountPanel, //
			StatementPanel statementPanel, //
			ReconcileTransactionsPanel reconcileTransactionsPanel) {
		super(new BorderLayout());

		this.reconciledBalance = null;

		this.accountPanel = accountPanel;
		this.statementPanel = statementPanel;
		this.reconcileTransactionsPanel = reconcileTransactionsPanel;

		JPanel innerPanel = new JPanel(new GridBagLayout());
		add(innerPanel, BorderLayout.WEST);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;

		gbc.insets = new Insets(5, 2, 5, 2);
		gbc.gridwidth = 2;
		this.date = GridBagUtility.addValue(innerPanel, gbc, 0, 0, //
				GridBagUtility.bold16);
		this.closingCashField = GridBagUtility.addTextField(innerPanel, gbc, 0, 1, //
				GridBagUtility.bold12);
		this.lastStmt = GridBagUtility.addLabeledValue(innerPanel, gbc, 0, 1, //
				"Last Stmt", 12);
		this.open = GridBagUtility.addValue(innerPanel, gbc, 0, 4, //
				GridBagUtility.bold16);

		gbc.insets = new Insets(0, 5, 0, 0);
		gbc.gridwidth = 1;

		this.credits = GridBagUtility.addLabeledValue(innerPanel, gbc, 1, 0, //
				"Credits", 13);
		this.debits = GridBagUtility.addLabeledValue(innerPanel, gbc, 1, 1, //
				"Debits", 13);

		gbc.insets = new Insets(10, 15, 0, 0);
		this.reconciledBalance = GridBagUtility.addLabeledValue(innerPanel, gbc, 2, 0, //
				"Cleared Balance", 14);

		gbc.insets = new Insets(10, 15, 0, 0);
		this.difference = GridBagUtility.addLabeledValue(innerPanel, gbc, 2, 1, //
				"Difference", 14);

		this.selectAllButton = new JButton("Select All");
		this.deselectAllButton = new JButton("Deselect All");
		this.finishButton = new JButton("Finish");

		gbc.insets = new Insets(0, 0, 0, 0);
		gbc.gridy = 0;
		gbc.gridx = 6;
		innerPanel.add(selectAllButton, gbc);
		gbc.gridy = 1;
		innerPanel.add(deselectAllButton, gbc);
		gbc.gridy = 2;
		innerPanel.add(finishButton, gbc);

		selectAllButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reconcileTransactionsPanel.reconcileTransactionTableModel.clearAll();
				updateValues();
			}
		});

		deselectAllButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reconcileTransactionsPanel.reconcileTransactionTableModel.unclearAll();
				updateValues();
			}
		});

		finishButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				finishStatement();
			}
		});

		this.closingCashField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setClosingValue();
			}
		});
	}

	private void finishStatement() {
		ReconcileTransactionTableModel model = this.reconcileTransactionsPanel.reconcileTransactionTableModel;

		model.finishStatement();

		Statement stmt = model.createNextStatementToReconcile();
		Account acct = Account.getAccountByID(stmt.acctid);

		// Update list of statements to include the new statement
		this.statementPanel.accountSelected(acct, true);

		this.accountPanel.accountSelected(acct, true);
	}

	private void setClosingValue() {
		try {
			BigDecimal val = new BigDecimal(closingCashField.getText());
			this.stmt.closingBalance = val;
			this.stmt.cashBalance = val;
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		updateValues();
	}

	public void statementSelected(Statement stmt) {
		if (stmt != this.stmt) {
			this.stmt = stmt;

			updateValues();
		}
	}

	private void updateValues() {
		String datestr = (this.stmt != null) ? this.stmt.date.longString : "---";
		this.date.setText(datestr);
		this.closingCashField.setText(((this.stmt != null) && (this.stmt.cashBalance != null))//
				? Common.formatAmount(this.stmt.cashBalance) //
				: "Enter closing balance");
		Statement laststmt = (stmt != null) ? stmt.prevStatement : null;
		this.lastStmt.setText((laststmt != null) //
				? laststmt.date.longString //
				: "---");
		this.open.setText((this.stmt != null) //
				? Common.formatAmount(this.stmt.getOpeningCashBalance()) //
				: "---");

		ReconcileTransactionTableModel model = reconcileTransactionsPanel.reconcileTransactionTableModel;

		this.credits.setText((this.stmt != null) //
				? Common.formatAmount(model.getCredits()) //
				: "---");
		this.debits.setText((this.stmt != null) //
				? Common.formatAmount(model.getDebits()) //
				: "---");

		this.clearedCashBalance = model.getClearedCashBalance();
		this.reconciledBalance.setText((this.stmt != null) //
				? Common.formatAmount(this.clearedCashBalance) //
				: "---");

		this.diffValue = ((this.stmt != null) && (this.stmt.cashBalance != null)) //
				? this.stmt.cashBalance.subtract(this.clearedCashBalance) //
				: null;
		this.difference.setText((this.diffValue != null) //
				? Common.formatAmount(diffValue) //
				: "---");

		this.finishButton.setEnabled( //
				((this.diffValue != null) && (this.diffValue.signum() == 0)));
	}

	public void transactionSelected(GenericTxn transaction) {
		updateValues();
	}
}