package qif.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
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
import qif.data.SecurityPortfolio;
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

	private JLabel dateLabel;
	private JTextField closingCashField;
	private JLabel openBalanceLabel;
	private JLabel lastStmtDateLabel;
	private JLabel creditsLabel;
	private JLabel debitsLabel;
	private JLabel clearedCashBalanceLabel;
	private JLabel cashDiffLabel;
	private JLabel portfolioOKLabel;

	private BigDecimal clearedCashBalance;
	private BigDecimal cashDiffValue;

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

		this.clearedCashBalanceLabel = null;

		this.accountPanel = accountPanel;
		this.statementPanel = statementPanel;
		this.reconcileTransactionsPanel = reconcileTransactionsPanel;

		JPanel infoPanel = new JPanel(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;

		gbc.insets = new Insets(5, 2, 5, 2);
		gbc.gridwidth = 1;

		this.dateLabel = GridBagUtility.addValue( //
				infoPanel, gbc, 0, 0, GridBagUtility.bold16);
		this.closingCashField = GridBagUtility.addTextField( //
				infoPanel, gbc, 0, 1, GridBagUtility.bold12);
		this.lastStmtDateLabel = GridBagUtility.addLabeledValue( //
				infoPanel, gbc, 1, 1, "Last Stmt", 12);

		gbc.insets = new Insets(0, 5, 0, 0);
		gbc.gridwidth = 1;

		this.openBalanceLabel = GridBagUtility.addLabeledValue( //
				infoPanel, gbc, 1, 0, "Open Cash Balance", 13);
		this.creditsLabel = GridBagUtility.addLabeledValue( //
				infoPanel, gbc, 2, 0, "Credits", 13);
		this.debitsLabel = GridBagUtility.addLabeledValue( //
				infoPanel, gbc, 3, 0, "Debits", 13);
		this.clearedCashBalanceLabel = GridBagUtility.addLabeledValue( //
				infoPanel, gbc, 4, 0, "Cleared Cash Balance", 13);

		this.portfolioOKLabel = GridBagUtility.addLabeledValue( //
				infoPanel, gbc, 3, 1, "Portfolio Ok", 14);
		this.cashDiffLabel = GridBagUtility.addLabeledValue( //
				infoPanel, gbc, 4, 1, "Cash Difference", 14);

		JPanel buttonPanel = new JPanel(new GridLayout(0, 1));
		
		this.selectAllButton = new JButton("Select All");
		this.deselectAllButton = new JButton("Deselect All");
		this.finishButton = new JButton("Finish");

		buttonPanel.add(selectAllButton, gbc);
		buttonPanel.add(deselectAllButton, gbc);
		buttonPanel.add(finishButton, gbc);

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

		add(infoPanel, BorderLayout.WEST);
		add(buttonPanel, BorderLayout.EAST);
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
		this.dateLabel.setText(datestr);
		this.closingCashField.setText(((this.stmt != null) && (this.stmt.cashBalance != null))//
				? Common.formatAmount(this.stmt.cashBalance) //
				: "Enter closing balance");
		Statement laststmt = (stmt != null) ? stmt.prevStatement : null;
		this.lastStmtDateLabel.setText((laststmt != null) //
				? laststmt.date.longString //
				: "---");

		ReconcileTransactionTableModel model = reconcileTransactionsPanel.reconcileTransactionTableModel;

		this.openBalanceLabel.setText((this.stmt != null) //
				? Common.formatAmount(this.stmt.getOpeningCashBalance()) //
				: "---");
		this.creditsLabel.setText((this.stmt != null) //
				? Common.formatAmount(model.getCredits()) //
				: "---");
		this.debitsLabel.setText((this.stmt != null) //
				? Common.formatAmount(model.getDebits()) //
				: "---");

		this.clearedCashBalance = model.getClearedCashBalance();
		this.clearedCashBalanceLabel.setText((this.stmt != null) //
				? Common.formatAmount(this.clearedCashBalance) //
				: "---");

		this.cashDiffValue = ((this.stmt != null) && (this.stmt.cashBalance != null)) //
				? this.stmt.cashBalance.subtract(this.clearedCashBalance) //
				: null;
		this.cashDiffLabel.setText((this.cashDiffValue != null) //
				? Common.formatAmount(cashDiffValue) //
				: "---");
		
		SecurityPortfolio delta = model.getPortfolioDelta();
		boolean holdingsMatch = delta.equals(this.stmt.holdings);
		this.portfolioOKLabel.setText((holdingsMatch) ? "Yes" : "No");

		boolean isBalanced = (this.cashDiffValue != null) && (this.cashDiffValue.signum() == 0);
		this.finishButton.setEnabled(isBalanced && holdingsMatch);
		
		this.cashDiffLabel.setForeground((isBalanced) ? Color.BLACK : Color.RED);
		this.portfolioOKLabel.setForeground((holdingsMatch) ? Color.BLACK : Color.RED);
	}

	public void transactionSelected(GenericTxn transaction) {
		updateValues();
	}
}