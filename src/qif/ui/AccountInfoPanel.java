package qif.ui;

import java.awt.BorderLayout;
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

	Account account = null;

	JLabel accountName;
	JLabel accountType;
	JLabel accountDescription;
	JLabel accountOpen;
	JLabel accountClose;
	JLabel accountBalance;

	public AccountInfoPanel() {
		super(new BorderLayout());

		JPanel innerPanel = new JPanel(new GridBagLayout());
		add(innerPanel, BorderLayout.WEST);

		GridBagConstraints gbc = new GridBagConstraints();

		gbc.insets = new Insets(10, 5, 10, 5);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 2;
		this.accountName = GridBagUtility.addValue(innerPanel, gbc, 0, 0, //
				GridBagUtility.bold20);

		gbc.insets = new Insets(0, 15, 0, 0);
		gbc.gridwidth = 1;

		this.accountType = GridBagUtility.addLabeledValue(innerPanel, gbc, 1, 0, "Type");
		this.accountDescription = GridBagUtility.addLabeledValue(innerPanel, gbc, 1, 1, "Description");
		this.accountOpen = GridBagUtility.addLabeledValue(innerPanel, gbc, 2, 0, "Open Date");
		this.accountClose = GridBagUtility.addLabeledValue(innerPanel, gbc, 2, 1, "Closed");
		this.accountBalance = GridBagUtility.addLabeledValue(innerPanel, gbc, 2, 2, "Balance");
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
	}
}