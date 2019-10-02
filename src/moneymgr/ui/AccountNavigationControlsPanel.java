package moneymgr.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * This panel contains controls affecting the account list<br>
 * OpenBut CloseBut
 */
@SuppressWarnings("serial")
public class AccountNavigationControlsPanel //
		extends JPanel {

	private static final String[] SHOW_OPEN_LABELS = { //
			"ShowClosed", "HideClosed" //
	};

	private static final String[] INCLUDE_ZERO_LABELS = { //
			"Show0", "Hide0" //
	};

	private static final String[] SHOW_TODAY_LABELS = { //
			"Today", "AsOf" //
	};

	private AccountNavigationPanel acctNavigationPanel;
	private JButton includeClosedButton;
	private JButton includeZeroBalanceButton;
	private JButton todayButton;
	private int closedLabelIdx;
	private int zeroLabelIdx;
	private int todayLabelIdx;

	public AccountNavigationControlsPanel(AccountNavigationPanel anp) {
		super(new GridBagLayout());

		this.acctNavigationPanel = anp;
		this.closedLabelIdx = this.zeroLabelIdx = 0;

		this.includeClosedButton = new JButton(SHOW_OPEN_LABELS[this.closedLabelIdx]);
		this.includeZeroBalanceButton = new JButton(INCLUDE_ZERO_LABELS[this.zeroLabelIdx]);
		this.todayButton = new JButton(SHOW_TODAY_LABELS[this.zeroLabelIdx]);

		setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, UIConstants.DARK_GRAY));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		add(this.includeClosedButton, gbc);

		gbc.gridx = 1;
		add(this.includeZeroBalanceButton, gbc);
		gbc.gridx = 2;
		add(this.todayButton, gbc);

		addListeners(anp);
	}

	private void toggleIncludeClosedAccounts() {
		this.closedLabelIdx = 1 - this.closedLabelIdx;
		this.includeClosedButton.setText(SHOW_OPEN_LABELS[this.closedLabelIdx]);
		this.acctNavigationPanel.setIncludeClosedAccounts(this.closedLabelIdx > 0);
	}

	private void toggleIncludeZeroBalanceAccounts() {
		this.zeroLabelIdx = 1 - this.zeroLabelIdx;
		this.includeZeroBalanceButton.setText(INCLUDE_ZERO_LABELS[this.zeroLabelIdx]);
		this.acctNavigationPanel.setIncludeZeroBalanceAccounts(this.zeroLabelIdx > 0);
	}

	private void toggleShowTodayBalance() {
		this.todayLabelIdx = 1 - this.todayLabelIdx;
		this.todayButton.setText(SHOW_TODAY_LABELS[this.todayLabelIdx]);
		this.acctNavigationPanel.setShowTodayBalance(this.todayLabelIdx > 0);
	}

	public void addListeners(AccountNavigationPanel anp) {
		this.includeClosedButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				toggleIncludeClosedAccounts();
			}
		});

		this.includeZeroBalanceButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				toggleIncludeZeroBalanceAccounts();
			}
		});

		this.todayButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				toggleShowTodayBalance();
			}
		});
	}
}