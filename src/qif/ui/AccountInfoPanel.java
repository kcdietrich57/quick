package qif.ui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;

import qif.data.Account;
import qif.data.Common;

/** This panel displays summary information about the selected account */
@SuppressWarnings("serial")
public class AccountInfoPanel //
		extends JPanel //
		implements AccountSelectionListener {
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

	public void accountSelected(Account acct, boolean update) {
		if (update || (acct != this.account)) {
			this.account = acct;

			this.accountName.setText((acct != null) ? acct.getName() : "---");

			this.accountType.setText((acct != null) ? acct.type.toString() : "---");
			this.accountDescription.setText((acct != null) ? acct.description : "---");
			this.accountOpen.setText((acct != null) ? acct.getOpenDate().toString() : "---");
			String close = (acct != null) && acct.isOpenOn(null) ? "No" : "Yes";
			this.accountClose.setText((acct != null) ? close : "---");
			this.accountBalance.setText((acct != null) ? Common.formatAmount(acct.balance) : "---");
		}
	}
}