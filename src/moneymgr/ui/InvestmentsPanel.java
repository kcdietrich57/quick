package moneymgr.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.math.BigDecimal;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import moneymgr.model.Account;
import moneymgr.model.SecurityPortfolio;
import moneymgr.model.SecurityPosition;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/**
 * This panel displays overall information<br>
 * Balances | ReconcileStatus
 */
@SuppressWarnings("serial")
public class InvestmentsPanel extends JPanel {
	private JTextArea accountsText;
	private JTextArea securitiesText;

	public InvestmentsPanel() {
		super(new BorderLayout());

		this.accountsText = new JTextArea();
		this.accountsText.setFont(new Font("Courier", Font.PLAIN, 12));

		this.securitiesText = new JTextArea();
		this.securitiesText.setFont(new Font("Courier", Font.PLAIN, 12));

		JScrollPane accountsScroller = new JScrollPane(this.accountsText);
		JScrollPane securitiesScroller = new JScrollPane(this.securitiesText);

		JPanel accountsPanel = new JPanel(new BorderLayout());
		accountsPanel.add(accountsScroller, BorderLayout.CENTER);

		JPanel securitiesPanel = new JPanel(new BorderLayout());
		securitiesPanel.add(securitiesScroller, BorderLayout.CENTER);

		JTabbedPane tabs = new JTabbedPane();

		tabs.add("Securities", securitiesPanel);
		tabs.add("Accounts", accountsPanel);

		add(tabs, BorderLayout.CENTER);

		String accountsModel = buildAccountsModel();
		this.accountsText.setText(accountsModel);

		String securitiesModel = buildSecuritiesModel();
		this.securitiesText.setText(securitiesModel);
	}

	public void changeDate() {
		String accountsModel = buildAccountsModel();
		this.accountsText.setText(accountsModel);
		this.accountsText.setCaretPosition(0);

		String securitiesModel = buildSecuritiesModel();
		this.securitiesText.setText(securitiesModel);
		this.securitiesText.setCaretPosition(0);
	}

	private String buildSecuritiesModel() {
		StringBuffer ret = new StringBuffer();

		QDate curdate = MainWindow.instance.asOfDate();

		BigDecimal totalValue = BigDecimal.ZERO;

		ret.append("=====================================\n");
		ret.append("Securities Holdings/Value for ");
		ret.append(Common.formatDate(MainWindow.instance.asOfDate()));
		ret.append("\n");
		ret.append("=====================================\n");
		ret.append("\n");
		ret.append(String.format("[NN]: %-40s %10s %10s %13s\n", //
				"Name", "Shares", "Price", "Value"));

		int num = 1;
		for (SecurityPosition pos : SecurityPortfolio.portfolio.positions) {
			BigDecimal shr = pos.getSharesForDate(curdate);
			BigDecimal value = pos.getValueForDate(curdate);

			if ((shr.signum() == 0) || Common.isEffectivelyZero(value)) {
				continue;
			}

			totalValue = totalValue.add(value);

			ret.append(String.format("[%02d]: %-40s %10s %10s %13s\n", //
					num, //
					Common.formatString(pos.security.getName(), -40), //
					Common.formatAmount3(shr).trim(), //
					Common.formatAmount3(pos.security.getPriceForDate(curdate)).trim(), //
					Common.formatAmount(value).trim()));
			++num;
		}

		if (num > 1) {
			ret.append("\n");
			ret.append(String.format("%60s  ====================\n", ""));
			ret.append(String.format("%60s  TOTAL: %13s\n", //
					"", Common.formatAmount(totalValue).trim()));

		}

		return ret.toString();
	}

	private String buildAccountsModel() {
		StringBuffer ret = new StringBuffer();

		QDate curdate = MainWindow.instance.asOfDate();

		ret.append("=====================================\n");
		ret.append("Securities Holdings/Value by Account for ");
		ret.append(Common.formatDate(curdate));
		ret.append("\n");
		ret.append("=====================================\n");
		ret.append("\n");

		int num = 1;
		BigDecimal totalValue = BigDecimal.ZERO;

		for (Account acct : Account.getAccounts()) {
			BigDecimal value = acct.getValueForDate(curdate);
			// acct.securities.getPortfolioValueForDate(MainWindow.instance.asOfDate);

			if (!acct.isInvestmentAccount() || Common.isEffectivelyZero(value)) {
				continue;
			}

			ret.append(String.format("%-40s %15s %13s\n", //
					Common.formatString(acct.name, -30), //
					"", //
					""));

			for (SecurityPosition pos : acct.securities.positions) {
				BigDecimal shr = pos.getSharesForDate(curdate);
				BigDecimal svalue = pos.getValueForDate(curdate);

				if ((shr.signum() == 0) || Common.isEffectivelyZero(svalue)) {
					continue;
				}

				ret.append(String.format("    %-30s %10s %10s %13s\n", //
						Common.formatString(pos.security.getName(), -30), //
						Common.formatAmount3(shr).trim(), //
						Common.formatAmount3(pos.security.getPriceForDate(curdate)).trim(), //
						Common.formatAmount(svalue).trim()));
			}

			BigDecimal cash = value.subtract(acct.securities.getPortfolioValueForDate(curdate));
			if (!Common.isEffectivelyZero(cash)) {
				ret.append(String.format("    %-30s %10s %10s %13s\n", //
						Common.formatString("Cash", -30), //
						"", //
						"", //
						Common.formatAmount(cash).trim()));
			}

			ret.append(String.format("%-40s %15s %13s\n", //
					"", //
					"Total", //
					Common.formatAmount(value).trim()));

			++num;

			ret.append("\n");

			totalValue = totalValue.add(value);
		}

		if (num > 1) {
			ret.append("\n");
			ret.append(String.format("%32s  ====================\n", ""));
			ret.append(String.format("%32s  TOTAL: %13s\n", //
					"", Common.formatAmount(totalValue).trim()));

		}

		return ret.toString();
	}
}