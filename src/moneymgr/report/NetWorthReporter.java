package moneymgr.report;

import java.math.BigDecimal;

import app.QifDom;
import moneymgr.model.Account;
import moneymgr.model.MoneyMgrModel;
import moneymgr.report.CashFlowModel.AcctInfo;
import moneymgr.report.StatusForDateModel.AccountSummary;
import moneymgr.report.StatusForDateModel.Section;
import moneymgr.report.StatusForDateModel.SecuritySummary;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/**
 * Create text based net worth summary report.<br>
 * Groups/sums assets by category and account.
 */
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

	public final MoneyMgrModel model;

	public NetWorthReporter(MoneyMgrModel model) {
		this.model = model;
	}

	/** Current net worth - obsolete QifLoader only */
	public void reportCurrentNetWorth() {
		reportNetWorthForDate(QDate.today());
	}

	/** Net worth for date - obsolete QifLoader only */
	public void reportNetWorthForDate(QDate d) {
		StatusForDateModel sfdModel = new StatusForDateModel(this.model, d);

		String s = generateReportStatusForDate(sfdModel);

		System.out.println(s);
	}

	// TODO the following methods/functions could be relocated into a better spot

	/** Generate itemized list of account information and net worth summary */
	public String generateReportStatusForDate(CashFlowModel cashFlowModel) {
		String sb1 = "";
		String sb2 = "";

		for (AcctInfo ainfo : cashFlowModel.acctinfoMonth) {
			if (ainfo.balanceMatches()) {
				sb1 += ainfo.toString();
				sb1 += "\n";
			} else {
				sb2 += ainfo.toString();
				sb2 += "\n";
			}
		}

		String ret = cashFlowModel.toString();
		if (QifDom.verbose) {
			// model.getSummary()
			ret += "\n--------\n" + sb2 + sb1;
		}

		return ret;
	}

	/** Generate itemized list of account information and net worth summary */
	public String generateReportStatusForDate(StatusForDateModel statusForDateModel) {
		StringBuilder sb = new StringBuilder();

		sb.append("\n");
		sb.append(String.format("Global status for date: %s\n", statusForDateModel.date.toString()));
		sb.append("--------------------------------------------------------\n");
		sb.append(String.format("Assets:      %15.2f\n", statusForDateModel.assets));
		sb.append(String.format("Liabilities: %15.2f\n", statusForDateModel.liabilities));
		sb.append(String.format("Balance:     %15.2f\n", statusForDateModel.netWorth));
		sb.append("--------------------------------------------------------\n");
		sb.append("\n");

		sb.append(String.format("  %-36s : %10s\n", "Account", "Balance\n"));

		for (Section sect : statusForDateModel.sections) {
			String label = sect.acctCategory.label + " Accounts ";
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

		return sb.toString();
	}

	/** Calculate/return summary balance information for a date */
	public Balances getBalancesForDate(QDate d) {
		final Balances b = new Balances();

		if (d == null) {
			d = QDate.today();
		}

		b.date = d;

		for (Account a : this.model.getAccounts()) {
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
