package qif.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
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
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

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
	// private JTextField closingCashField;
	private JLabel openBalanceLabel;
	private JLabel lastStmtDateLabel;
	private JLabel creditsLabel;
	private JLabel debitsLabel;
	private JLabel clearedCashBalanceLabel;
	// private JLabel cashDiffLabel;
	// private JLabel portfolioOKLabel;

	private BigDecimal clearedCashBalance;
	private BigDecimal cashDiffValue;

	private JButton selectAllButton;
	private JButton deselectAllButton;
	private JButton finishButton;

	private JTable holdingsTable;
	private HoldingsTableModel holdingsTableModel;

	public ReconcileStatusPanel() {
		super(new BorderLayout());

		JPanel infoPanel = new JPanel(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;

		// ===================================================
		// Cash information
		// ===================================================
		gbc.insets = new Insets(5, 2, 5, 2);
		gbc.gridwidth = 1;

		this.dateLabel = GridBagUtility.addValue( //
				infoPanel, gbc, 0, 0, GridBagUtility.bold16);

		gbc.insets = new Insets(0, 5, 0, 0);
		gbc.gridwidth = 1;

		this.lastStmtDateLabel = GridBagUtility.addLabeledValue( //
				infoPanel, gbc, 1, 0, "Last Stmt", 12);
		this.openBalanceLabel = GridBagUtility.addLabeledValue( //
				infoPanel, gbc, 2, 0, "Open Cash Balance", 13);
		this.creditsLabel = GridBagUtility.addLabeledValue( //
				infoPanel, gbc, 3, 0, "Credits", 13);
		this.debitsLabel = GridBagUtility.addLabeledValue( //
				infoPanel, gbc, 4, 0, "Debits", 13);

		gbc.insets = new Insets(5, 5, 0, 0);
		this.clearedCashBalanceLabel = GridBagUtility.addLabeledValue( //
				infoPanel, gbc, 5, 0, "Cleared Balance", 13);
		// this.cashDiffLabel = GridBagUtility.addLabeledValue( //
		// infoPanel, gbc, 5, 1, "Difference", 14);

		gbc.insets = new Insets(0, 5, 0, 0);

		// ===================================================
		// Portfolio
		// ===================================================

		this.holdingsTableModel = new HoldingsTableModel();
		this.holdingsTable = new JTable(this.holdingsTableModel);
		this.holdingsTable.setDefaultRenderer(Object.class, new HoldingsTableCellRenderer());

		JScrollPane holdingsTableScroller = new JScrollPane(this.holdingsTable);
		this.holdingsTable.setMinimumSize(new Dimension(100, 50));
		this.holdingsTable.setMaximumSize(new Dimension(500, 75));
		this.holdingsTable.setPreferredScrollableViewportSize(new Dimension(200, 100));

		// ===================================================
		// Buttons
		// ===================================================

		JPanel buttonPanel = new JPanel(new GridLayout(0, 1));

		this.selectAllButton = new JButton("Select All");
		this.deselectAllButton = new JButton("Deselect All");
		this.finishButton = new JButton("Finish");

		buttonPanel.add(selectAllButton, gbc);
		buttonPanel.add(deselectAllButton, gbc);
		buttonPanel.add(finishButton, gbc);

		updateValues();

		selectAllButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				MainWindow.instance.reconcileTransactionsPanel.reconcileTransactionTableModel.clearAll();
				updateValues();
			}
		});

		deselectAllButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				MainWindow.instance.reconcileTransactionsPanel.reconcileTransactionTableModel.unclearAll();
				updateValues();
			}
		});

		finishButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				finishStatement();
			}
		});

		// ===================================================

		add(infoPanel, BorderLayout.WEST);
		add(holdingsTableScroller, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.EAST);
	}

	private void finishStatement() {
		ReconcileTransactionTableModel model = //
				MainWindow.instance.reconcileTransactionsPanel.reconcileTransactionTableModel;

		model.finishStatement();

		Statement stmt = model.createNextStatementToReconcile();
		Account acct = Account.getAccountByID(stmt.acctid);

		// Update list of statements to include the new statement
		MainWindow.instance.statementPanel.accountSelected(acct, true);
		MainWindow.instance.accountPanel.accountSelected(acct, true);

		MainWindow.instance.accountListPanel.refreshAccountList();
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
		// this.closingCashField.setText(((this.stmt != null) && (this.stmt.cashBalance
		// != null))//
		// ? Common.formatAmount(this.stmt.cashBalance) //
		// : "Enter closing balance");
		Statement laststmt = (stmt != null) ? stmt.prevStatement : null;
		this.lastStmtDateLabel.setText((laststmt != null) //
				? laststmt.date.longString //
				: "---");

		ReconcileTransactionTableModel model = (this.stmt != null) //
				? MainWindow.instance.reconcileTransactionsPanel.reconcileTransactionTableModel //
				: null;

		this.openBalanceLabel.setText((this.stmt != null) //
				? Common.formatAmount(this.stmt.getOpeningCashBalance()) //
				: "---");
		this.creditsLabel.setText((model != null) //
				? Common.formatAmount(model.getCredits()) //
				: "---");
		this.debitsLabel.setText((model != null) //
				? Common.formatAmount(model.getDebits()) //
				: "---");

		this.clearedCashBalance = (model != null) //
				? model.getClearedCashBalance() //
				: BigDecimal.ZERO;
		this.cashDiffValue = ((this.stmt != null) && (this.stmt.cashBalance != null)) //
				? this.stmt.cashBalance.subtract(this.clearedCashBalance) //
				: null;

		boolean isBalanced = (this.cashDiffValue != null) //
				&& (this.cashDiffValue.signum() == 0);

		String str = "---";

		if (this.stmt != null) {
			str = Common.formatAmount(this.clearedCashBalance);

			if (!isBalanced) {
				String dval = (cashDiffValue != null) //
						? Common.formatAmount(cashDiffValue).trim() //
						: "N/A";
				str += "(" + dval + ")";
			}
		}

		this.clearedCashBalanceLabel.setText(str);
		this.clearedCashBalanceLabel.setForeground((isBalanced) ? Color.BLACK : Color.RED);

		boolean holdingsMatch = true;

		if ((this.stmt != null) && (this.stmt.holdings != null)) {
			SecurityPortfolio.HoldingsComparison comparison = //
					this.stmt.holdings.comparisonTo(model.getPortfolioDelta());
			this.holdingsTableModel.holdingsComparision = comparison;
			holdingsMatch = comparison.holdingsMatch();
		}

		if ((this.stmt != null) && isBalanced && holdingsMatch) {
			this.finishButton.setText("Finish");
			this.finishButton.setEnabled(true);
			this.finishButton.setForeground(Color.BLACK);
		} else {
			if (this.stmt == null) {
				this.finishButton.setText("No Statement");
			} else if (!isBalanced && !holdingsMatch) {
				this.finishButton.setText("Cash/Holdings not balanced");
			} else if (!isBalanced) {
				this.finishButton.setText("Cash not balanced");
			} else if (!holdingsMatch) {
				this.finishButton.setText("Holdings not balanced");
			}

			this.finishButton.setEnabled(false);
			this.finishButton.setForeground(Color.RED);
		}

		this.holdingsTableModel.fireTableDataChanged();
	}

	public void transactionSelected(GenericTxn transaction) {
		updateValues();
	}
}

@SuppressWarnings("serial")
class HoldingsTableModel extends AbstractTableModel {
	private static final String headers[] = { "Security", "Desired", "Actual" };

	public SecurityPortfolio.HoldingsComparison holdingsComparision;

	public int getRowCount() {
		return (this.holdingsComparision != null) //
				? this.holdingsComparision.desiredPositions.size() //
				: 0;
	}

	public int getColumnCount() {
		return 3;
	}

	public String getColumnName(int col) {
		return headers[col];
	}

	public Object getValueAt(int row, int col) {
		switch (col) {
		case 0:
			return this.holdingsComparision.getSecurityName(row);
		case 1:
			return Common.formatAmount3(this.holdingsComparision.getDesiredShares(row));
		case 2:
			return Common.formatAmount3(this.holdingsComparision.getActualShares(row));
		}

		return "N/A";
	}
}

@SuppressWarnings("serial")
class HoldingsTableCellRenderer extends DefaultTableCellRenderer {
	private static final Font BALANCED_FONT = new Font("Helvetica", Font.PLAIN, 12);
	private static final Color BALANCED_COLOR = Color.BLACK;
	private static final Font UNBALANCED_FONT = new Font("Helvetica", Font.BOLD, 14);
	private static final Color UNBALANCED_COLOR = Color.RED;

	public Component getTableCellRendererComponent( //
			JTable table, Object value, boolean isSelected, boolean hasFocus, //
			int row, int column) {
		Component c = super.getTableCellRendererComponent( //
				table, value, isSelected, hasFocus, row, column);

		HoldingsTableModel model = (HoldingsTableModel) table.getModel();

		if (model.getValueAt(row, 1).equals(model.getValueAt(row, 2))) {
			c.setFont(BALANCED_FONT);
			c.setForeground(BALANCED_COLOR);
		} else {
			c.setFont(UNBALANCED_FONT);
			c.setForeground(UNBALANCED_COLOR);
		}

		return c;
	}
}
