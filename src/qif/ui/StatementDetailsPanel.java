package qif.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

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
		infoPanel.setBorder(BorderFactory.createLineBorder(UICommon.DARK_GRAY));

		Font bfont = new Font("Helvetica", Font.BOLD, 16);
		Font font = new Font("Helvetica", Font.PLAIN, 16);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;

		this.dateValue = GridBagUtility.addLabeledValue( //
				infoPanel2, gbc, 0, 0, "Date:", bfont, font);
		this.numTransactionsValue = GridBagUtility.addLabeledValue( //
				infoPanel2, gbc, 1, 0, "Number of transactions:", bfont, font);

		infoPanel.add(infoPanel2, BorderLayout.WEST);
		add(infoPanel, BorderLayout.NORTH);

		this.transactionPanel = new TransactionPanel(false);
		MainWindow.instance.statementTransactionPanel = this.transactionPanel;

		add(this.transactionPanel, BorderLayout.CENTER);
	}

	public void statementSelected(Statement statement) {
		setStatement(statement);
	}

	private void setStatement(Statement stmt) {
		if (stmt == null) {
			dateValue.setText("");
			numTransactionsValue.setText("");

			transactionPanel.statementSelected(null);
		} else {
			dateValue.setText(stmt.date.toString());
			numTransactionsValue.setText(Integer.toString(stmt.transactions.size()));

			transactionPanel.statementSelected(stmt);
		}
	}
}