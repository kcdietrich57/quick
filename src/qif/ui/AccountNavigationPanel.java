package qif.ui;

import java.awt.BorderLayout;

import javax.swing.JPanel;

public class AccountNavigationPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	AccountControlPanel controlsPanel;
	AccountListPanel accountListPanel;
	SummaryPanel summaryPanel;

	public AccountNavigationPanel() {
		setLayout(new BorderLayout());

		controlsPanel = new AccountControlPanel(this);
		accountListPanel = new AccountListPanel(true);
		summaryPanel = new SummaryPanel();

		add(controlsPanel, BorderLayout.NORTH);
		add(accountListPanel, BorderLayout.CENTER);
		add(summaryPanel, BorderLayout.SOUTH);
	}

	public void showOpenAccounts(boolean yesno) {
		accountListPanel.showOpenAccounts(yesno);
	}
}