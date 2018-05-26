package qif.ui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

public class AccountControlPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	public JButton openButton;
	public JButton closedButton;

	public AccountControlPanel(AccountNavigationPanel anp) {
		super(new GridBagLayout());

		openButton = new JButton("Open Accounts");
		closedButton = new JButton("Closed Accounts");

		setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(16, 16, 16)));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		add(openButton, gbc);

		gbc.gridx = 1;
		add(closedButton, gbc);

		addListeners(anp);
	}

	public void addListeners(AccountNavigationPanel anp) {
		openButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				anp.showOpenAccounts(true);
			}
		});

		closedButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				anp.showOpenAccounts(false);
			}
		});
	}
}