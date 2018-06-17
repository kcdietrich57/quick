package qif.report;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import qif.data.Account;
import qif.data.Common;
import qif.data.QDate;
import qif.data.Security;
import qif.data.SecurityPosition;
import qif.data.StockOption;
import qif.report.StatusForDateModel.AccountSummary;
import qif.report.StatusForDateModel.Section;
import qif.report.StatusForDateModel.SecuritySummary;

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
		StatusForDateModel model = buildReportStatusForDate(d);

		String s = generateReportStatusForDate(model);

		System.out.println(s);
	}

	public static QDate getFirstTransactionDate() {
		QDate retdate = null;

		for (final Account a : Account.getAccounts()) {
			final QDate d = a.getFirstTransactionDate();

			if ((d != null) && ((retdate == null) || d.compareTo(retdate) < 0)) {
				retdate = d;
			}
		}

		return retdate;
	}

	public static QDate getLastTransactionDate() {
		QDate retdate = null;

		for (final Account a : Account.getAccounts()) {
			QDate d = a.getLastTransactionDate();

			if ((d != null) && ((retdate == null) || d.compareTo(retdate) > 0)) {
				retdate = d;
			}
		}

		return retdate;
	}

	public static List<StatusForDateModel> getMonthlyNetWorth() {
		List<StatusForDateModel> balances = new ArrayList<StatusForDateModel>();

		QDate d = getFirstTransactionDate();
		QDate lastTxDate = QDate.today(); // getLastTransactionDate();

		int year = d.getYear();
		int month = d.getMonth();

		do {
			d = QDate.getDateForEndOfMonth(year, month);
			StatusForDateModel b = buildReportStatusForDate(d);

			balances.add(b);

			if (month == 12) {
				++year;
				month = 1;
			} else {
				++month;
			}
		} while (d.compareTo(lastTxDate) <= 0);

		return balances;
	}

	public static void reportMonthlyNetWorth() {
		System.out.println();

		QDate d = getFirstTransactionDate();
		QDate lastTxDate = getLastTransactionDate();

		int year = d.getYear();
		int month = d.getMonth();

		System.out.println(String.format("  %-10s %-15s %-15s %-15s", //
				"Date", "NetWorth", "Assets", "Liabilities"));

		do {
			d = QDate.getDateForEndOfMonth(year, month);
			final Balances b = getBalancesForDate(d);

			System.out.println(String.format("%s,%15.2f,%15.2f,%15.2f", //
					d.longString, b.netWorth, b.assets, b.liabilities));

			if (month == 12) {
				++year;
				month = 1;
			} else {
				++month;
			}
		} while (d.compareTo(lastTxDate) <= 0);
	}

	public static void reportYearlyNetWorth() {
		System.out.println();

		QDate firstTxDate = getFirstTransactionDate();
		QDate lastTxDate = getLastTransactionDate();

		for (int year = firstTxDate.getYear(); year <= lastTxDate.getYear(); ++year) {
			NetWorthReporter.reportNetWorthForDate(QDate.getDateForEndOfMonth(year, 12));
		}
	}

	// ===============================================================

	public static StatusForDateModel buildReportStatusForDate(QDate d) {
		StatusForDateModel model = new StatusForDateModel();
		model.d = d;

		for (Account a : Account.getAccounts()) {
			BigDecimal amt = a.getValueForDate(d);

			if (!a.isOpenOn(d) //
					&& Common.isEffectivelyZero(amt) //
					&& (a.getFirstUnclearedTransaction() == null) //
					&& a.securities.isEmptyForDate(d)) {
				continue;
			}

			StatusForDateModel.Section modelsect = model.getSectionForAccount(a);

			StatusForDateModel.AccountSummary asummary = new StatusForDateModel.AccountSummary();
			modelsect.accounts.add(asummary);
			modelsect.subtotal = modelsect.subtotal.add(amt);

			asummary.name = a.getDisplayName(36);
			asummary.balance = asummary.cashBalance = amt;

			List<StockOption> opts = StockOption.getOpenOptions(a, d);
			if (!opts.isEmpty()) {
				StatusForDateModel.SecuritySummary ssummary = new StatusForDateModel.SecuritySummary();

				StockOption opt = opts.get(0);
				Security sec = Security.getSecurity(opt.secid);

				ssummary.name = "Options:" + sec.getName();
				ssummary.shares = opt.getAvailableShares(true);
				ssummary.price = sec.getPriceForDate(d).getPrice();
				ssummary.value = opt.getValueForDate(d);

				asummary.securities.add(ssummary);
			}

			if (!a.securities.isEmptyForDate(d)) {
				BigDecimal portValue = a.getSecuritiesValueForDate(d);

				if (!Common.isEffectivelyZero(portValue)) {
					asummary.cashBalance = amt.subtract(portValue);

					for (SecurityPosition pos : a.securities.positions) {
						BigDecimal posval = pos.getSecurityPositionValueForDate(d);

						if (!Common.isEffectivelyZero(posval)) {
							StatusForDateModel.SecuritySummary ssummary = new StatusForDateModel.SecuritySummary();
							asummary.securities.add(ssummary);

							String nn = pos.security.getName();
							if (nn.length() > 34) {
								nn = nn.substring(0, 31) + "...";
							}

							ssummary.name = nn;
							ssummary.value = posval;
							ssummary.price = pos.security.getPriceForDate(d).getPrice();

							int idx = pos.getTransactionIndexForDate(d);
							if (idx >= 0) {
								ssummary.shares = pos.shrBalance.get(idx);
							}
						}
					}
				}
			}

			modelsect.subtotal.add(amt);

			if (modelsect.info.isAsset) {
				model.assets = model.assets.add(amt);
			} else {
				model.liabilities = model.liabilities.add(amt);
			}

			model.netWorth = model.netWorth.add(amt);
		}

		return model;
	}

	public static String generateReportStatusForDate(StatusForDateModel model) {
		StringBuilder sb = new StringBuilder();

		sb.append("\n");
		sb.append(String.format("Global status for date: %s\n", model.d.toString()));
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