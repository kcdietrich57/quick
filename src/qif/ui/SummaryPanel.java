package qif.ui;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import qif.data.Common;
import qif.data.QifDom;
import qif.data.QifDom.Balances;

public class SummaryPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	public SummaryPanel() {
		super(new GridBagLayout());

		setBorder(new EmptyBorder(10, 0, 10, 0));

		Balances bals = QifDom.getNetWorthForDate(null);

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

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(3, 5, 3, 5);
		add(assLabel, gbc);

		gbc.gridx = 1;
		add(assValue, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		add(liabLabel, gbc);

		gbc.gridx = 1;
		add(liabValue, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		add(netLabel, gbc);

		gbc.gridx = 1;
		add(netValue, gbc);
	}
}