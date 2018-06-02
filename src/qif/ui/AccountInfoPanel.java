package qif.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;

import qif.data.Account;
import qif.data.Common;

public class AccountInfoPanel //
		extends JPanel //
		implements AccountSelectionListener {
	private static final long serialVersionUID = 1L;

	JPanel innerPanel;

	Account account = null;

	JLabel accountName;
	JLabel accountType;
	JLabel accountDescription;
	JLabel accountOpen;
	JLabel accountClose;
	JLabel accountBalance;
	JLabel accountLimit;

	public AccountInfoPanel() {
		super();

		setLayout(new BorderLayout());

		innerPanel = new JPanel(new GridBagLayout());
		add(innerPanel, BorderLayout.WEST);

		GridBagConstraints gbc = new GridBagConstraints();

		gbc.insets = new Insets(10, 5, 10, 5);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 2;
		this.accountName = addValue(gbc, 0, 0);
		this.accountName.setFont(new Font("Helvetica", Font.BOLD, 20));

		gbc.insets = new Insets(0, 15, 0, 0);
		gbc.gridwidth = 1;

		this.accountType = addLabeledValue(gbc, 1, 0, "Type");
		this.accountDescription = addLabeledValue(gbc, 1, 1, "Description");
		this.accountOpen = addLabeledValue(gbc, 2, 0, "Open Date");
		this.accountClose = addLabeledValue(gbc, 2, 1, "Closed");
		this.accountBalance = addLabeledValue(gbc, 2, 2, "Balance");
		// this.accountLimit = addLabeledValue(gbc, 2, 3, "Limit");
		// public BigDecimal clearedBalance;
	}

	private JLabel addLabeledValue(GridBagConstraints gbc, int row, int col, String text) {
		JLabel label = new JLabel(text);
		label.setFont(new Font("Helvetica", Font.BOLD, 12));

		gbc.gridy = row;
		gbc.gridx = col * 2;
		innerPanel.add(label, gbc);

		label = addValue(gbc, row, col * 2 + 1);
		label.setFont(new Font("Helvetica", Font.PLAIN, 12));

		return label;
	}

	private JLabel addValue(GridBagConstraints gbc, int row, int col) {
		JLabel value = new JLabel("---");

		gbc.gridy = row;
		gbc.gridx = col;
		innerPanel.add(value, gbc);

		return value;
	}

	public void accountSelected(Account account) {
		this.account = account;

		this.accountName.setText((account != null) ? account.getName() : "---");

		this.accountType.setText((account != null) ? account.type.toString() : "---");
		this.accountDescription.setText((account != null) ? account.description : "---");
		this.accountOpen.setText((account != null) ? account.getOpenDate().toString() : "---");
		String close = (account != null) && account.isOpenOn(null) ? "No" : "Yes";
		this.accountClose.setText((account != null) ? close : "---");
		this.accountBalance.setText((account != null) ? Common.formatAmount(account.balance) : "---");
		// this.accountLimit.setText((account != null) ?
		// Common.formatAmount(account.creditLimit) : "---");
	}
}