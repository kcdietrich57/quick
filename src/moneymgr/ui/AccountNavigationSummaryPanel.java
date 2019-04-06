package moneymgr.ui;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import moneymgr.report.NetWorthReporter;
import moneymgr.report.NetWorthReporter.Balances;
import moneymgr.util.Common;

/**
 * This panel displays a short summary of the overall status<br>
 * Assets<br>
 * Liabilities<br>
 * NetWorth
 */
@SuppressWarnings("serial")
public class AccountNavigationSummaryPanel extends JPanel {
	private JLabel assLabel;
	private JLabel liabLabel;
	private JLabel netLabel;

	public AccountNavigationSummaryPanel() {
		super(new GridBagLayout());

		setBorder(new EmptyBorder(10, 0, 10, 0));

		Font bfont = new Font("Helvetica", Font.BOLD, 16);
		Font font = new Font("Helvetica", Font.PLAIN, 16);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;

		this.assLabel = GridBagUtility.addLabeledValue(this, gbc, 0, 0, "Assets", bfont, font);
		this.liabLabel = GridBagUtility.addLabeledValue(this, gbc, 1, 0, "Liabilities", bfont, font);
		this.netLabel = GridBagUtility.addLabeledValue(this, gbc, 2, 0, "Net Worth", bfont, font);

		updateValues();
	}

	public void updateValues() {
		Balances bals = NetWorthReporter.getBalancesForDate( //
				MainWindow.instance.asOfDate());

		this.assLabel.setText(Common.formatAmount(bals.assets));
		this.liabLabel.setText(Common.formatAmount(bals.liabilities));
		this.netLabel.setText(Common.formatAmount(bals.netWorth));
	}
}