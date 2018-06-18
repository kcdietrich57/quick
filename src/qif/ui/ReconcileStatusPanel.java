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

import qif.data.Common;
import qif.data.GenericTxn;
import qif.data.Statement;
import qif.ui.model.ReconcileTransactionTableModel;

class ReconcileStatusPanel //
		extends JPanel //
		implements TransactionSelectionListener {
	private static final long serialVersionUID = 1L;

	Statement stmt;

	JLabel lastStmt;
	JLabel date;
	JLabel open;
	JLabel credits;
	JLabel debits;
	JLabel reconciledBalance;
	JLabel difference;

	JTextField close;
	BigDecimal clearedBalance;
	BigDecimal closingBalance;

	ReconcileTransactionsPanel reconcileTransactionsPanel;

	public ReconcileStatusPanel(ReconcileTransactionsPanel rtp) {
		super(new BorderLayout());

		this.reconciledBalance = null;
		this.closingBalance = null;

		reconcileTransactionsPanel = rtp;

		JPanel innerPanel = new JPanel(new GridBagLayout());
		add(innerPanel, BorderLayout.WEST);

		GridBagConstraints gbc = new GridBagConstraints();

		gbc.insets = new Insets(5, 2, 5, 2);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 2;
		this.date = GridBagUtility.addValue(innerPanel, gbc, 0, 0, //
				GridBagUtility.bold20);
		this.close = GridBagUtility.addTextField(innerPanel, gbc, 0, 1, //
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

		JButton selectAllButton = new JButton("Select All");
		JButton deselectAllButton = new JButton("Deselect All");
		JButton finishButton = new JButton("Finish");

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
				rtp.transactionTableModel.clearAll();
				updateValues();
			}
		});

		deselectAllButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				rtp.transactionTableModel.unclearAll();
				updateValues();
			}
		});

		this.close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setClosingValue();
			}
		});
	}

	private void setClosingValue() {
		try {
			BigDecimal val = new BigDecimal(close.getText());
			this.closingBalance = val;
		} catch (Exception ex) {
			this.closingBalance = null;
		}
		
		updateValues();
	}

	public void setStatement(Statement stmt) {
		if (stmt != this.stmt) {
			this.stmt = stmt;

			updateValues();
		}
	}

	private void updateValues() {
		this.date.setText((this.stmt != null) ? this.stmt.date.longString : "---");
		this.close.setText((this.closingBalance != null) //
				? Common.formatAmount(this.closingBalance) //
				: "Enter closing balance");
		Statement laststmt = (stmt != null) ? stmt.prevStatement : null;
		this.lastStmt.setText((laststmt != null) //
				? laststmt.date.longString //
				: "---");
		this.open.setText((this.stmt != null) //
				? Common.formatAmount(this.stmt.getOpeningCashBalance()) //
				: "---");

		ReconcileTransactionTableModel model = reconcileTransactionsPanel.transactionTableModel;

		this.credits.setText((this.stmt != null) //
				? Common.formatAmount(model.getCredits()) //
				: "---");
		this.debits.setText((this.stmt != null) //
				? Common.formatAmount(model.getDebits()) //
				: "---");

		this.clearedBalance = model.getClearedCashBalance();
		this.reconciledBalance.setText((this.stmt != null) //
				? Common.formatAmount(this.clearedBalance) //
				: "---");

		BigDecimal diff = (this.closingBalance != null) //
				? this.closingBalance.subtract(this.clearedBalance) //
				: null;
		this.difference.setText((diff != null) //
				? Common.formatAmount(diff) //
				: "---");
	}

	public void transactionSelected(GenericTxn transaction) {
		updateValues();
	}
}