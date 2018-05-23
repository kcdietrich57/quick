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

		JPanel infoPanel = new JPanel(new BorderLayout());
		JPanel infoPanel2 = new JPanel(new GridBagLayout());
		infoPanel2.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
		infoPanel.setBorder(BorderFactory.createLineBorder(new Color(16, 16, 16)));

		Font bfont = new Font("Helvetica", Font.BOLD, 16);
		Font font = new Font("Helvetica", Font.PLAIN, 16);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(3, 5, 3, 5);
		acctLabel.setFont(bfont);
		infoPanel2.add(acctLabel, gbc);

		gbc.gridx = 1;
		acct.setFont(font);
		infoPanel2.add(acct, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		dateLabel.setFont(bfont);
		infoPanel2.add(dateLabel, gbc);

		gbc.gridx = 1;
		date.setFont(font);
		infoPanel2.add(date, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		ntxLabel.setFont(bfont);
		infoPanel2.add(ntxLabel, gbc);

		gbc.gridx = 1;
		ntx.setFont(font);
		infoPanel2.add(ntx, gbc);

		infoPanel.add(infoPanel2, BorderLayout.WEST);
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