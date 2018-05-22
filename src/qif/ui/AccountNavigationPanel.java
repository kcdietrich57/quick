package qif.ui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import qif.data.Common;
import qif.data.QifDom;
import qif.data.QifDom.Balances;
import qif.data.Statement;

public class AccountNavigationPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	Statement s;

	JButton openButton = new JButton("Open Accounts");
	JButton closedButton = new JButton("Closed Accounts");

	AccountPanel accountPanel;

	public AccountNavigationPanel() {
		setLayout(new BorderLayout());

		JPanel controlsPanel = new JPanel(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		controlsPanel.add(openButton, gbc);

		gbc.gridx = 1;
		controlsPanel.add(closedButton, gbc);

		add(controlsPanel, BorderLayout.NORTH);

		accountPanel = new AccountPanel(true);

		add(accountPanel, BorderLayout.CENTER);

		JPanel summaryPanel = new JPanel(new GridBagLayout());
		JLabel summaryValue;

		QifDom dom = QifDom.getDomById(1);
		Balances bals = dom.getNetWorthForDate(null);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		summaryPanel.add(new JLabel("Assets"), gbc);
		gbc.gridx = 1;
		summaryValue = new JLabel(Common.formatAmount(bals.assets));
		summaryValue.setBorder(BorderFactory.createLoweredBevelBorder());
		summaryPanel.add(summaryValue, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		summaryPanel.add(new JLabel("Liabilities"), gbc);
		gbc.gridx = 1;
		summaryValue = new JLabel(Common.formatAmount(bals.liabilities));
		summaryValue.setBorder(BorderFactory.createLoweredBevelBorder());
		summaryPanel.add(summaryValue, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		summaryPanel.add(new JLabel("Net Worth"), gbc);
		gbc.gridx = 1;
		summaryValue = new JLabel(Common.formatAmount(bals.netWorth));
		summaryValue.setBorder(BorderFactory.createLoweredBevelBorder());
		summaryPanel.add(summaryValue, gbc);

		add(summaryPanel, BorderLayout.SOUTH);

		addListeners();
	}

	public void addListeners() {
		openButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				accountPanel.showOpenAccounts();
			}
		});

		closedButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				accountPanel.showClosedAccounts();
			}
		});
	}
}