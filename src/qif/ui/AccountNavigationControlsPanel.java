package qif.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

/** This panel contains controls affecting the account list */
@SuppressWarnings("serial")
public class AccountNavigationControlsPanel extends JPanel {
	private JButton openButton;
	private JButton closedButton;

	public AccountNavigationControlsPanel(AccountNavigationPanel anp) {
		super(new GridBagLayout());

		this.openButton = new JButton("Open Accounts");
		this.closedButton = new JButton("Closed Accounts");

		setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, UIConstants.DARK_GRAY));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		add(this.openButton, gbc);

		gbc.gridx = 1;
		add(this.closedButton, gbc);

		addListeners(anp);
	}

	public void addListeners(AccountNavigationPanel anp) {
		this.openButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				anp.showOpenAccounts(true);
			}
		});

		this.closedButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				anp.showOpenAccounts(false);
			}
		});
	}
}