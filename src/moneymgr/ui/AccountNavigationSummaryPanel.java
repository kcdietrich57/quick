package moneymgr.ui;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import moneymgr.model.MoneyMgrModel;
import moneymgr.report.NetWorthReporter;
import moneymgr.report.NetWorthReporter.Balances;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/**
 * This panel displays a short summary of the overall status<br>
 * Assets<br>
 * Liabilities<br>
 * NetWorth
 */
@SuppressWarnings("serial")
public class AccountNavigationSummaryPanel extends JPanel {
	public final MoneyMgrModel model;

	private JLabel nowTitle;
	private JLabel asofTitle;
	private JLabel assLabel;
	private JLabel liabLabel;
	private JLabel netLabel;
	private JLabel assLabel2;
	private JLabel liabLabel2;
	private JLabel netLabel2;

	public AccountNavigationSummaryPanel(MoneyMgrModel model) {
		super(new GridBagLayout());

		this.model = model;

		setBorder(new EmptyBorder(10, 0, 10, 0));

		Font bfont = new Font("Helvetica", Font.BOLD, 14);
		Font font = new Font("Helvetica", Font.PLAIN, 14);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;

		this.nowTitle = GridBagUtility.addLabeledValue(this, gbc, 0, 0, "", font, bfont);
		this.asofTitle = GridBagUtility.addValue(this, gbc, 0, 1, bfont);
		this.assLabel = GridBagUtility.addLabeledValue(this, gbc, 1, 0, "Assets", font, font);
		this.liabLabel = GridBagUtility.addLabeledValue(this, gbc, 2, 0, "Liabilities", font, font);
		this.netLabel = GridBagUtility.addLabeledValue(this, gbc, 3, 0, "Net Worth", font, font);
		this.assLabel2 = GridBagUtility.addValue(this, gbc, 1, 1, bfont);
		this.liabLabel2 = GridBagUtility.addValue(this, gbc, 2, 1, bfont);
		this.netLabel2 = GridBagUtility.addValue(this, gbc, 3, 1, bfont);

		updateValues();
	}

	public void updateValues() {
		NetWorthReporter netWorthReporter = new NetWorthReporter(this.model);
		Balances bals = netWorthReporter.getBalancesForDate(QDate.today());
		Balances bals2 = netWorthReporter.getBalancesForDate( //
				MainWindow.instance.getAsOfDate());

		this.nowTitle.setText("Today");
		this.asofTitle.setText("As Of");
		this.assLabel.setText(Common.formatAmount(bals.assets));
		this.liabLabel.setText(Common.formatAmount(bals.liabilities));
		this.netLabel.setText(Common.formatAmount(bals.netWorth));
		this.assLabel2.setText(Common.formatAmount(bals2.assets));
		this.liabLabel2.setText(Common.formatAmount(bals2.liabilities));
		this.netLabel2.setText(Common.formatAmount(bals2.netWorth));
	}
}