package qif.report;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import qif.data.Account;
import qif.data.Common;
import qif.data.QDate;
import qif.report.StatusForDateModel.AccountSummary;
import qif.report.StatusForDateModel.Section;
import qif.report.StatusForDateModel.SecuritySummary;
import qif.ui.MainWindow;

public class NetWorthReporter {

	public static class Balances {
		public QDate date;
		public BigDecimal netWorth = BigDecimal.ZERO;
		public BigDecimal assets = BigDecimal.ZERO;
		public BigDecimal cashAssets = BigDecimal.ZERO;
		public BigDecimal investmentAssets = BigDecimal.ZERO;
		public BigDecimal retirementAssets = BigDecimal.ZERO;
		public BigDecimal liabilities = BigDecimal.ZERO;
	}

	public static void reportCurrentNetWorth() {
		reportNetWorthForDate(QDate.today());
	}

	public static void reportNetWorthForDate(QDate d) {
		StatusForDateModel model = new StatusForDateModel(d);

		String s = generateReportStatusForDate(model);

		System.out.println(s);
	}

	private static QDate getFirstTransactionDate() {
		QDate retdate = null;

		for (final Account a : Account.getAccounts()) {
			final QDate d = a.getFirstTransactionDate();

			if ((d != null) && ((retdate == null) || d.compareTo(retdate) < 0)) {
				retdate = d;
			}
		}

		return retdate;
	}

	public static List<StatusForDateModel> getNetWorthData() {
		return getNetWorthData(null, MainWindow.instance.asOfDate, //
				MainWindow.instance.reportUnit);
	}

	public static List<StatusForDateModel> getNetWorthData( //
			QDate start, QDate end, MainWindow.IntervalUnit unit) {
		List<StatusForDateModel> balances = new ArrayList<StatusForDateModel>();

		QDate d = (start != null) ? start : getFirstTransactionDate();
		QDate lastTxDate = (end != null) ? end : QDate.today(); // getLastTransactionDate();

		int year = d.getYear();
		int month = d.getMonth();
			d = QDate.getDateForEndOfMonth(year, month);

		do {
			StatusForDateModel b = new StatusForDateModel(d);

			balances.add(b);

			d = unit.nextDate(d);
		} while (d.compareTo(lastTxDate) <= 0);

		return balances;
	}

	// ===============================================================

	public static String generateReportStatusForDate(StatusForDateModel model) {
		StringBuilder sb = new StringBuilder();

		sb.append("\n");
		sb.append(String.format("Global status for date: %s\n", model.date.toString()));
		sb.append("--------------------------------------------------------\n");
		sb.append(String.format("  %-36s : %10s\n", "Account", "Balance\n"));

		for (Section sect : model.sections) {
			String label = sect.info.label + " Accounts ";
			while (label.length() < 30) {
				label += "=";
			}

			sb.append(String.format( //
					"======== %s===========================\n", //
					label));

			if (!sect.accounts.isEmpty()) {
				for (AccountSummary asum : sect.accounts) {
					sb.append(String.format("  %-36s: %s\n", //
							asum.name, Common.formatAmount(asum.balance)));

					if (!Common.isEffectivelyZero(asum.cashBalance) //
							&& !asum.securities.isEmpty()) {
						sb.append(String.format("    %-34s: ..%s\n", //
								"Cash", Common.formatAmount(asum.cashBalance)));
					}

					for (SecuritySummary ssum : asum.securities) {
						if (!Common.isEffectivelyZero(ssum.shares)) {
							sb.append(String.format("    %-34s: ..%s %s %s\n", //
									ssum.name, Common.formatAmount(ssum.value), //
									Common.formatAmount3(ssum.shares), //
									Common.formatAmount3(ssum.price)));
						}
					}
				}

				sb.append("\n");
				sb.append(String.format("Section Total: - - - - - - - - - - - %15.2f\n", sect.subtotal));
			}

			sb.append("\n");
		}

		sb.append(String.format("Assets:      %15.2f\n", model.assets));
		sb.append(String.format("Liabilities: %15.2f\n", model.liabilities));
		sb.append(String.format("Balance:     %15.2f\n", model.netWorth));
		sb.append("\n");

		return sb.toString();
	}

	public static Balances getBalancesForDate(QDate d) {
		final Balances b = new Balances();

		if (d == null) {
			d = QDate.today();
		}

		b.date = d;

		for (Account a : Account.accounts) {
			final BigDecimal amt = a.getValueForDate(d);

			b.netWorth = b.netWorth.add(amt);

			if (a.isAsset()) {
				b.assets = b.assets.add(amt);
				if (a.isCashAccount()) {
					b.cashAssets = b.cashAssets.add(amt);
				} else if (a.isInvestmentAccount()) {
					b.investmentAssets = b.investmentAssets.add(amt);
				} else {
					b.retirementAssets = b.retirementAssets.add(amt);
				}
			} else if (a.isLiability()) {
				b.liabilities = b.liabilities.add(amt);
			}
		}

		return b;
	}
}
