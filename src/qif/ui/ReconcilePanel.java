package qif.ui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import qif.data.Account;
import qif.data.Common;
import qif.data.Statement;

class ReconcileStatusPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	Statement stmt;

	JLabel lastStmt;
	JLabel date;
	JLabel open;
	JLabel credits;
	JLabel debits;
	JLabel difference;

	public ReconcileStatusPanel() {
		super(new BorderLayout());

		JPanel innerPanel = new JPanel(new GridBagLayout());
		add(innerPanel, BorderLayout.WEST);

		GridBagConstraints gbc = new GridBagConstraints();

		gbc.insets = new Insets(10, 5, 10, 5);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 2;
		this.date = GridBagUtility.addValue(innerPanel, gbc, 0, 0, //
				GridBagUtility.bold20);

		gbc.insets = new Insets(0, 15, 0, 0);
		gbc.gridwidth = 1;

		this.lastStmt = GridBagUtility.addLabeledValue(innerPanel, gbc, 0, 1, //
				"Last Stmt", 16);
		this.open = GridBagUtility.addLabeledValue(innerPanel, gbc, 1, 0, //
				"Opening Balance", 16);
		this.credits = GridBagUtility.addLabeledValue(innerPanel, gbc, 2, 0, //
				"Credits", 16);
		this.debits = GridBagUtility.addLabeledValue(innerPanel, gbc, 4, 0, //
				"Debits", 16);
		this.difference = GridBagUtility.addLabeledValue(innerPanel, gbc, 5, 0, //
				"Difference", 16);
	}

	public void setStatement(Statement stmt) {
		if (stmt != this.stmt) {
			this.stmt = stmt;

			this.date.setText((this.stmt != null) ? this.stmt.date.longString : "---");
			Statement laststmt = (stmt != null) ? stmt.prevStatement : null;
			this.lastStmt.setText((laststmt != null) //
					? laststmt.date.longString //
					: "---");
			this.open.setText((this.stmt != null) //
					? Common.formatAmount(this.stmt.getOpeningCashBalance()) //
					: "---");
			this.credits.setText((this.stmt != null) //
					? Common.formatAmount(this.stmt.getCredits()) //
					: "---");
			this.debits.setText((this.stmt != null) //
					? Common.formatAmount(this.stmt.getDebits()) //
					: "---");
			this.open.setText((this.stmt != null) //
					? Common.formatAmount(this.stmt.getOpeningCashBalance()) //
					: "---");
		}
	}
}

public class ReconcilePanel //
		extends JPanel //
		implements AccountSelectionListener {
	private static final long serialVersionUID = 1L;

	Account account;
	Statement stmt;

	ReconcileStatusPanel reconcileStatus = new ReconcileStatusPanel();

	public ReconcilePanel() {
		super(new BorderLayout());

		add(reconcileStatus, BorderLayout.NORTH);
		add(new JButton("Reconcile panel goes here"), BorderLayout.CENTER);
	}

	public void accountSelected(Account account) {
		if (this.account != account) {
			this.account = account;

			this.reconcileStatus.setStatement(//
					(account != null) ? account.createNextStatementToReconcile() : null);
		}
	}
}