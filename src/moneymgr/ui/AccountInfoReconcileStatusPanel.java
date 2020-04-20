package moneymgr.ui;

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

import moneymgr.model.Account;
import moneymgr.model.GenericTxn;
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.SecurityPortfolio;
import moneymgr.model.Statement;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/**
 * This panel shows info about the reconcile process, and has controls to
 * perform actions<br>
 * Info | Holdings | Buttons
 */
@SuppressWarnings("serial")
public class AccountInfoReconcileStatusPanel extends JPanel //
		implements StatementSelectionListener, TransactionSelectionListener {
	private Statement stmt;

	private JLabel dateLabel;
	private JLabel lastStmtDateLabel;
	private JLabel openBalanceLabel;
	private JLabel creditsLabel;
	private JLabel debitsLabel;
	private JLabel clearedCashBalanceLabel;

	private BigDecimal clearedCashBalance;
	private BigDecimal cashDiffValue;

	private JButton selectAllButton;
	private JButton deselectAllButton;
	private JButton finishButton;

	public AccountInfoReconcileStatusPanel(AccountInfoReconcilePanel reconcilePanel) {
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

		gbc.insets = new Insets(0, 5, 0, 0);

		// ===================================================
		// Buttons
		// ===================================================

		JPanel buttonPanel = new JPanel(new GridLayout(0, 1));

		this.selectAllButton = new JButton("Select All");
		this.deselectAllButton = new JButton("Deselect All");
		this.finishButton = new JButton("Finish");

		buttonPanel.add(this.selectAllButton, gbc);
		buttonPanel.add(this.deselectAllButton, gbc);
		buttonPanel.add(this.finishButton, gbc);

		updateValues();

		this.selectAllButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				MainWindow.instance.reconcileTransactionsPanel.clearAll();
				updateValues();
			}
		});

		this.deselectAllButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				MainWindow.instance.reconcileTransactionsPanel.unclearAll();
				updateValues();
			}
		});

		this.finishButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				finishStatement();
			}
		});

		// ===================================================

		add(infoPanel, BorderLayout.WEST);
		add(buttonPanel, BorderLayout.EAST);
	}

	/** Save the newly reconciled statement to the statement log */
	private void finishStatement() {
		MainWindow.instance.reconcileTransactionsPanel.finishStatement();

		Statement stmt = MainWindow.instance.reconcileTransactionsPanel.createNextStatementToReconcile();
		Account acct = MoneyMgrModel.getAccountByID(stmt.acctid);

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

	/** Refresh the information in this pane */
	private void updateValues() {
		String datestr = (this.stmt != null) ? this.stmt.date.longString : "---";
		this.dateLabel.setText(datestr);

		Statement laststmt = (this.stmt != null) ? this.stmt.prevStatement : null;
		this.lastStmtDateLabel.setText((laststmt != null) //
				? laststmt.date.longString //
				: "---");

		AccountInfoReconcileTransactionsPanel reconcilePanel = (this.stmt != null) //
				? MainWindow.instance.reconcileTransactionsPanel //
				: null;

		this.openBalanceLabel.setText((this.stmt != null) //
				? Common.formatAmount(this.stmt.getOpeningCashBalance()) //
				: "---");
		this.creditsLabel.setText((reconcilePanel != null) //
				? Common.formatAmount(reconcilePanel.getCredits()) //
				: "---");
		this.debitsLabel.setText((reconcilePanel != null) //
				? Common.formatAmount(reconcilePanel.getDebits()) //
				: "---");

		this.clearedCashBalance = (reconcilePanel != null) //
				? reconcilePanel.getClearedCashBalance() //
				: BigDecimal.ZERO;
		this.cashDiffValue = (this.stmt != null) //
				? this.stmt.getCashBalance().subtract(this.clearedCashBalance) //
				: null;

		boolean isBalanced = (this.cashDiffValue != null) //
				&& (this.cashDiffValue.signum() == 0);

		String str = "---";

		if (this.stmt != null) {
			str = Common.formatAmount(this.clearedCashBalance);

			if (!isBalanced) {
				String dval = (this.cashDiffValue != null) //
						? Common.formatAmount(this.cashDiffValue).trim() //
						: "N/A";
				str += "(" + dval + ")";
			}
		}

		this.clearedCashBalanceLabel.setText(str);
		this.clearedCashBalanceLabel.setForeground((isBalanced) ? Color.BLACK : Color.RED);

		boolean holdingsMatch = true;

		if ((this.stmt != null) && (this.stmt.holdings != null)) {
			SecurityPortfolio delta = reconcilePanel.getPortfolioDelta();
			SecurityPortfolio.HoldingsComparison comparison = //
					this.stmt.holdings.comparisonTo(delta);
			holdingsMatch = comparison.holdingsMatch();
		}

		if ((this.stmt != null) //
				&& (this.stmt.date.compareTo(QDate.today()) <= 0) //
				&& isBalanced //
				&& holdingsMatch) {
			this.finishButton.setText("Finish");
			this.finishButton.setEnabled(true);
			this.finishButton.setForeground(Color.BLACK);
		} else {
			if (this.stmt == null) {
				this.finishButton.setText("No Statement");
			} else if (this.stmt.date.compareTo(QDate.today()) > 0) {
				this.finishButton.setText("Future statement");
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
	}

	public void transactionSelected(GenericTxn transaction) {
		updateValues();
	}
}
