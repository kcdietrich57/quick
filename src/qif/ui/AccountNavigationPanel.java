package qif.ui;

import java.awt.BorderLayout;

import javax.swing.JPanel;

/** This panel displays accounts and selection drives AccountPanel */
@SuppressWarnings("serial")
public class AccountNavigationPanel extends JPanel {
	private AccountControlPanel controlsPanel;
	private AccountListPanel accountListPanel;
	private SummaryPanel summaryPanel;

	public AccountNavigationPanel() {
		setLayout(new BorderLayout());

		controlsPanel = new AccountControlPanel(this);

		accountListPanel = new AccountListPanel(true);
		MainWindow.instance.accountListPanel = this.accountListPanel;
		summaryPanel = new SummaryPanel();
		MainWindow.instance.summaryPanel = this.summaryPanel;

		add(controlsPanel, BorderLayout.NORTH);
		add(accountListPanel, BorderLayout.CENTER);
		add(summaryPanel, BorderLayout.SOUTH);
	}

	public void refreshAccountList() {
		this.accountListPanel.refreshAccountList();
	}

	public void showOpenAccounts(boolean yesno) {
		accountListPanel.showOpenAccounts(yesno);
	}

	public void addAccountSelectionListener(AccountSelectionListener listener) {
		this.accountListPanel.addAccountSelectionListener(listener);
	}
}