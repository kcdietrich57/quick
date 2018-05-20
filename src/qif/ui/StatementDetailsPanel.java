package qif.ui;

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
		// setLayout(new GridLayout(0, 2));

		add(dateLabel);
		add(date);
		add(acctLabel);
		add(acct);
		add(ntxLabel);
		add(ntx);

		transactionPanel = new TransactionPanel();
		add(transactionPanel);
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