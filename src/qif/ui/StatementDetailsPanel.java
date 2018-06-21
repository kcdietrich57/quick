package qif.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import qif.data.Statement;

/** This panel displays details about the selected statement */
@SuppressWarnings("serial")
public class StatementDetailsPanel //
		extends JPanel //
		implements StatementSelectionListener {

	private JLabel dateValue;
	private JLabel numTransactionsValue;

	private TransactionPanel transactionPanel;

	public StatementDetailsPanel() {
		setLayout(new BorderLayout());

		JPanel infoPanel = new JPanel(new BorderLayout());
		JPanel infoPanel2 = new JPanel(new GridBagLayout());

		infoPanel2.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
		infoPanel.setBorder(BorderFactory.createLineBorder(new Color(16, 16, 16)));

		// TODO use GridBagUtility
		JLabel dateLabel = new JLabel("Date:");
		JLabel ntxLabel = new JLabel("Number of transactions:");

		dateValue = new JLabel("<date>");
		numTransactionsValue = new JLabel("<ntx>");

		Font bfont = new Font("Helvetica", Font.BOLD, 16);
		Font font = new Font("Helvetica", Font.PLAIN, 16);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(3, 5, 3, 5);
		dateLabel.setFont(bfont);
		infoPanel2.add(dateLabel, gbc);

		gbc.gridx = 1;
		dateValue.setFont(font);
		infoPanel2.add(dateValue, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		ntxLabel.setFont(bfont);
		infoPanel2.add(ntxLabel, gbc);

		gbc.gridx = 1;
		numTransactionsValue.setFont(font);
		infoPanel2.add(numTransactionsValue, gbc);

		infoPanel.add(infoPanel2, BorderLayout.WEST);
		add(infoPanel, BorderLayout.NORTH);

		transactionPanel = new TransactionPanel(false);

		add(transactionPanel, BorderLayout.CENTER);
	}

	public void statementSelected(Statement statement) {
		setStatement(statement);
	}

	private void setStatement(Statement stmt) {
		if (stmt == null) {
			dateValue.setText("");
			numTransactionsValue.setText("");

			transactionPanel.transactionTableModel.statementSelected(null);
		} else {
			dateValue.setText(stmt.date.toString());
			numTransactionsValue.setText(Integer.toString(stmt.transactions.size()));

			transactionPanel.transactionTableModel.statementSelected(stmt);
		}
	}
}