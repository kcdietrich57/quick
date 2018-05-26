package qif.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import qif.data.Common;
import qif.data.QifDom;
import qif.data.QifDom.Balances;
import qif.data.Statement;

public class AccountNavigationPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	Statement s;

	JButton openButton = new JButton("Open Accounts");
	JButton closedButton = new JButton("Closed Accounts");

	AccountListPanel accountPanel;

	public AccountNavigationPanel() {
		setLayout(new BorderLayout());

		JPanel controlsPanel = new JPanel(new GridBagLayout());
		controlsPanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(16, 16, 16)));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		controlsPanel.add(openButton, gbc);

		gbc.gridx = 1;
		controlsPanel.add(closedButton, gbc);

		add(controlsPanel, BorderLayout.NORTH);

		accountPanel = new AccountListPanel(true);

		add(accountPanel, BorderLayout.CENTER);

		Balances bals = QifDom.dom.getNetWorthForDate(null);

		Font bfont = new Font("Helvetica", Font.BOLD, 16);
		Font font = new Font("Helvetica", Font.PLAIN, 16);

		JLabel assLabel = new JLabel("Assets");
		assLabel.setFont(bfont);
		JLabel assValue = new JLabel(Common.formatAmount(bals.assets));
		assValue.setFont(font);
		// assValue.setBorder(BorderFactory.createLoweredBevelBorder());

		JLabel liabLabel = new JLabel("Liabilities");
		liabLabel.setFont(bfont);
		JLabel liabValue = new JLabel(Common.formatAmount(bals.liabilities));
		// liabValue.setBorder(BorderFactory.createLoweredBevelBorder());
		liabValue.setFont(font);

		JLabel netLabel = new JLabel("Net Worth");
		netLabel.setFont(bfont);
		JLabel netValue = new JLabel(Common.formatAmount(bals.netWorth));
		// netValue.setBorder(BorderFactory.createLoweredBevelBorder());
		netValue.setFont(font);

		JPanel summaryPanel = new JPanel(new GridBagLayout());
		summaryPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(3, 5, 3, 5);
		summaryPanel.add(assLabel, gbc);

		gbc.gridx = 1;
		summaryPanel.add(assValue, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		summaryPanel.add(liabLabel, gbc);

		gbc.gridx = 1;
		summaryPanel.add(liabValue, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		summaryPanel.add(netLabel, gbc);

		gbc.gridx = 1;
		summaryPanel.add(netValue, gbc);

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