package qif.ui;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import qif.data.Common;
import qif.report.NetWorthReporter;
import qif.report.NetWorthReporter.Balances;

/** This panel displays a short summary of the overall status */
@SuppressWarnings("serial")
public class SummaryPanel extends JPanel {
	public SummaryPanel() {
		super(new GridBagLayout());

		setBorder(new EmptyBorder(10, 0, 10, 0));

		Balances bals = NetWorthReporter.getBalancesForDate(null);

		Font bfont = new Font("Helvetica", Font.BOLD, 16);
		Font font = new Font("Helvetica", Font.PLAIN, 16);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;

		JLabel assLabel = GridBagUtility.addLabeledValue(this, gbc, 0, 0, "Assets", bfont, font);
		JLabel liabLabel = GridBagUtility.addLabeledValue(this, gbc, 1, 0, "Liabilities", bfont, font);
		JLabel netLabel = GridBagUtility.addLabeledValue(this, gbc, 2, 0, "Net Worth", bfont, font);

		assLabel.setText(Common.formatAmount(bals.assets));
		liabLabel.setText(Common.formatAmount(bals.liabilities));
		netLabel.setText(Common.formatAmount(bals.netWorth));
	}
}