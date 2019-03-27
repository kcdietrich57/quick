package qif.ui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;

import qif.data.Account;

/** This panel displays summary information about the selected account */
@SuppressWarnings("serial")
public class AccountInfoHeaderPanel
		extends JPanel //
		implements AccountSelectionListener {
	Account account = null;

	JLabel accountName;
	JLabel accountType;
	JLabel accountDescription;
	JLabel accountOpen;
	JLabel accountClose;
	JLabel accountBalance;

	public AccountInfoHeaderPanel() {
		super(new BorderLayout());

		JPanel innerPanel = new JPanel(new GridBagLayout());
		add(innerPanel, BorderLayout.WEST);

		GridBagConstraints gbc = new GridBagConstraints();

		gbc.insets = new Insets(10, 5, 10, 5);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 2;
		this.accountName = GridBagUtility.addValue( //
				innerPanel, gbc, 0, 0, GridBagUtility.bold16);
		this.accountDescription = GridBagUtility.addValue( //
				innerPanel, gbc, 0, 2, 14);
	}

	public void accountSelected(Account acct, boolean update) {
		if (update || (acct != this.account)) {
			this.account = acct;

			this.accountName.setText((acct != null) ? acct.name : "---");
			this.accountDescription.setText((acct != null) ? acct.description : "");
		}
	}
}