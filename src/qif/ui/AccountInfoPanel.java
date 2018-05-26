package qif.ui;

import javax.swing.JButton;

import qif.data.Account;

public class AccountInfoPanel extends JButton implements AccountSelectionListener {
	private static final long serialVersionUID = 1L;

	public AccountInfoPanel() {
		super("---");
	}

	public void accountSelected(Account account) {
		setText((account != null) ? account.getName() : "---");
	}
}