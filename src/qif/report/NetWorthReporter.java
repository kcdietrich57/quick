package qif.report;

import java.math.BigDecimal;

import qif.data.Account;
import qif.data.Common;
import qif.data.QDate;
import qif.data.QifDom;
import qif.data.QifDom.Balances;
import qif.data.SecurityPosition;
import qif.report.StatusForDateModel.AccountSummary;
import qif.report.StatusForDateModel.Section;
import qif.report.StatusForDateModel.SecuritySummary;

public class NetWorthReporter {

	public static void reportCurrentNetWorth() {
		reportNetWorthForDate(QDate.today());
	}

	public static void reportNetWorthForDate(QDate d) {
		StatusForDateModel model = buildReportStatusForDate(d);

		String s = generateReportStatusForDate(model);

		System.out.println(s);
	}

	public static QDate getFirstTransactionDate(QifDom dom) {
		QDate retdate = null;

		for (final Account a : Account.getAccounts()) {
			final QDate d = a.getFirstTransactionDate();

			if ((d != null) && ((retdate == null) || d.compareTo(retdate) < 0)) {
				retdate = d;
			}
		}

		return retdate;
	}

	public static QDate getLastTransactionDate(QifDom dom) {
		QDate retdate = null;

		for (final Account a : Account.getAccounts()) {
			QDate d = a.getLastTransactionDate();

			if ((d != null) && ((retdate == null) || d.compareTo(retdate) > 0)) {
				retdate = d;
			}
		}

		return retdate;
	}

	public static void reportMonthlyNetWorth() {
		QifDom dom = QifDom.dom;

		System.out.println();

		QDate d = getFirstTransactionDate(dom);
		QDate lastTxDate = getLastTransactionDate(dom);

		int year = d.getYear();
		int month = d.getMonth();

		System.out.println(String.format("  %-10s %-15s %-15s %-15s", //
				"Date", "NetWorth", "Assets", "Liabilities"));

		do {
			d = Common.getDateForEndOfMonth(year, month);
			final Balances b = dom.getNetWorthForDate(d);

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
		QifDom dom = QifDom.dom;

		System.out.println();

		QDate firstTxDate = getFirstTransactionDate(dom);
		QDate lastTxDate = getLastTransactionDate(dom);

		for (int year = firstTxDate.getYear(); year <= lastTxDate.getYear(); ++year) {
			NetWorthReporter.reportNetWorthForDate(Common.getDateForEndOfMonth(year, 12));
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
							ssummary.price = pos.security.getPriceForDate(d).price;

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
}