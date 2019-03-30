package moneymgr.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import moneymgr.model.Statement;

/**
 * This panel displays details about the selected statement<br>
 * Info<br>
 * Transactions
 */
@SuppressWarnings("serial")
public class AccountInfoStatementDetailsPanel extends JPanel //
		implements StatementSelectionListener {

	private JPanel statementInfoPanel;
	private JLabel dateValue;
	private JLabel numTransactionsValue;
	private TransactionPanel transactionPanel;

	public AccountInfoStatementDetailsPanel() {
		setLayout(new BorderLayout());

		this.statementInfoPanel = new JPanel(new BorderLayout());
		JPanel statementInfoInnerPanel = new JPanel(new GridBagLayout());

		this.statementInfoPanel.setBorder(BorderFactory.createLineBorder(UIConstants.DARK_GRAY));
		statementInfoInnerPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

		Font plainFont = new Font("Helvetica", Font.PLAIN, 16);
		Font boldFont = new Font("Helvetica", Font.BOLD, 16);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;

		this.dateValue = GridBagUtility.addLabeledValue( //
				statementInfoInnerPanel, gbc, 0, 0, "Date:", boldFont, plainFont);
		this.numTransactionsValue = GridBagUtility.addLabeledValue( //
				statementInfoInnerPanel, gbc, 1, 0, "Number of transactions:", boldFont, plainFont);

		this.statementInfoPanel.add(statementInfoInnerPanel, BorderLayout.WEST);
		add(this.statementInfoPanel, BorderLayout.NORTH);

		this.transactionPanel = new TransactionPanel(false);
		MainWindow.instance.statementTransactionPanel = this.transactionPanel;

		add(this.transactionPanel, BorderLayout.CENTER);
	}

	public void statementSelected(Statement statement) {
		setStatement(statement);
	}

	private void setStatement(Statement stmt) {
		if (stmt == null) {
			this.dateValue.setText("");
			this.numTransactionsValue.setText("");
		} else {
			this.dateValue.setText(stmt.date.toString());
			this.numTransactionsValue.setText(Integer.toString(stmt.transactions.size()));
		}

		this.transactionPanel.statementSelected(stmt);
	}
}