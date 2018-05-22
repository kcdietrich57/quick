package qif.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import qif.data.Common;
import qif.data.QifDom;
import qif.data.Statement;

public class StatementDetailsPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	Statement s;

	JLabel dateLabel = new JLabel("Date:");
	JLabel date = new JLabel("<date>");
	JLabel acctLabel = new JLabel("Account:");
	JLabel acct = new JLabel("<acct>");
	JLabel ntxLabel = new JLabel("Number of transactions:");
	JLabel ntx = new JLabel("<ntx>");

	TransactionPanel transactionPanel;

	public StatementDetailsPanel() {
		setLayout(new BorderLayout());

		JPanel infoPanel = new JPanel(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		dateLabel.setBorder(BorderFactory.createRaisedBevelBorder());
		infoPanel.add(dateLabel, gbc);

		gbc.gridx = 1;
		date.setBorder(BorderFactory.createRaisedBevelBorder());
		infoPanel.add(date, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		infoPanel.add(acctLabel, gbc);

		gbc.gridx = 1;
		infoPanel.add(acct, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		infoPanel.add(ntxLabel, gbc);

		gbc.gridx = 1;
		infoPanel.add(ntx, gbc);

		add(infoPanel, BorderLayout.NORTH);

		transactionPanel = new TransactionPanel(false);

		add(transactionPanel, BorderLayout.CENTER);
	}

	public void setStatement(Statement stmt) {
		if (stmt == null) {
			date.setText("");
			acct.setText("");
			ntx.setText("");

			transactionPanel.transactionTableModel.setStatement(null);
		} else {
			date.setText(Common.formatDate(stmt.date));
			acct.setText(QifDom.getDomById(1).getAccount(stmt.acctid).getName());
			ntx.setText(Integer.toString(stmt.transactions.size()));

			transactionPanel.transactionTableModel.setStatement(stmt);
		}
	}
}