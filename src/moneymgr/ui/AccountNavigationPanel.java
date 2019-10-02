package moneymgr.ui;

import java.awt.BorderLayout;

import javax.swing.JPanel;

/**
 * This panel displays accounts and selection drives AccountPanel<br>
 * Controls<br>
 * Accounts<br>
 * Summary
 */
@SuppressWarnings("serial")
public class AccountNavigationPanel extends JPanel {
	private AccountNavigationControlsPanel controlsPanel;
	private AccountNavigationListPanel accountListPanel;
	private AccountNavigationSummaryPanel summaryPanel;

	public AccountNavigationPanel() {
		setLayout(new BorderLayout());

		this.controlsPanel = new AccountNavigationControlsPanel(this);

		this.accountListPanel = new AccountNavigationListPanel(false, false);
		MainWindow.instance.accountListPanel = this.accountListPanel;
		this.summaryPanel = new AccountNavigationSummaryPanel();
		MainWindow.instance.summaryPanel = this.summaryPanel;

		add(this.controlsPanel, BorderLayout.NORTH);
		add(this.accountListPanel, BorderLayout.CENTER);
		add(this.summaryPanel, BorderLayout.SOUTH);
	}

	public void refreshAccountList() {
		this.accountListPanel.refreshAccountList();
	}

	public void setIncludeClosedAccounts(boolean yesno) {
		this.accountListPanel.setIncludeClosedAccounts(yesno);
	}

	public void setIncludeZeroBalanceAccounts(boolean yesno) {
		this.accountListPanel.setIncludeZeroBalanceAccounts(yesno);
	}

	public void setShowTodayBalance(boolean yesno) {
		this.accountListPanel.setShowTodayBalance(yesno);
	}

	public void addAccountSelectionListener(AccountSelectionListener listener) {
		this.accountListPanel.addAccountSelectionListener(listener);
	}
}