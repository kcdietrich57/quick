package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import qif.data.QifDom.Balances;
import qif.data.report.StatusForDateModel;
import qif.data.report.StatusForDateModel.AccountSummary;
import qif.data.report.StatusForDateModel.Section;
import qif.data.report.StatusForDateModel.SecuritySummary;

public class QifReporter {
	public static boolean compact = false;

	// ===============================================================

	public static void reportStatusForDate(Date d) {
		StatusForDateModel model = buildReportStatusForDate(d);

		String s = generateReportStatusForDate(model);
		
		System.out.println(s);
	}

	// ===============================================================

	public static StatusForDateModel buildReportStatusForDate(Date d) {
		StatusForDateModel model = new StatusForDateModel();
		model.d = d;

		QifDom dom = QifDom.dom;

		for (int acctid = 1; acctid <= dom.getNumAccounts(); ++acctid) {
			Account a = dom.getAccount(acctid);
			if (a == null) {
				continue;
			}

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
		sb.append(String.format("Global status for date: %s\n", Common.formatDate(model.d)));
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

					if (!Common.isEffectivelyEqual(asum.balance, asum.cashBalance)) {
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

	// ===============================================================

	public static void reportStatus(Account acct, String interval) {
		if (acct.statements.isEmpty()) {
			System.out.println("No statements for " + acct.getDisplayName(36));
		} else {
			System.out.println();
			System.out.println("-------------------------------------\n" //
					+ acct.getDisplayName(36));
			System.out.println(String.format("%d Statements, %d Transactions", //
					acct.statements.size(), acct.transactions.size()));
			System.out.println("-------------------------------------");

			final int nn = Math.max(0, acct.statements.size() - 12);
			int ct = 0;

			for (int ii = nn; ii < acct.statements.size(); ++ii) {
				final Statement s = acct.statements.get(ii);

				if (ct++ == 3) {
					ct = 1;
					System.out.println();
				}

				System.out.print(String.format("   %s  %3d tx  %s", //
						Common.formatDate(s.date), s.transactions.size(), //
						Common.formatAmount(s.closingBalance)));
			}

			System.out.println();

			System.out.println("Uncleared transactions as of last statement:");

			for (final GenericTxn t : acct.statements.get(acct.statements.size() - 1).unclearedTransactions) {
				System.out.println(String.format("  %s  %s  %s", //
						Common.formatDate(t.getDate()), //
						Common.formatAmount(t.getAmount()), //
						t.getPayee()));
			}

			int unclearedCount = 0;

			for (final GenericTxn t : acct.transactions) {
				if (t.stmtdate == null) {
					++unclearedCount;
				}
			}

			System.out.println("Total uncleared transactions: " + unclearedCount);
		}

		System.out.println(String.format("Current value: %s", //
				Common.formatAmount(acct.getCurrentValue())));

		System.out.println();
	}

	public static void reportMonthlyNetWorth() {
		QifDom dom = QifDom.dom;

		System.out.println();

		Date d = dom.getFirstTransactionDate();
		final Date lastTxDate = dom.getLastTransactionDate();

		final Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH);

		System.out.println(String.format("  %-10s %-15s %-15s %-15s", //
				"Date", "NetWorth", "Assets", "Liabilities"));

		do {
			d = Common.getDateForEndOfMonth(year, month);
			final Balances b = dom.getNetWorthForDate(d);

			System.out.println(String.format("%s,%15.2f,%15.2f,%15.2f", //
					Common.formatDateLong(d), //
					b.netWorth, b.assets, b.liabilities));

			if (month == 12) {
				++year;
				month = 1;
			} else {
				++month;
			}
		} while (d.compareTo(lastTxDate) <= 0);
	}

	public static void reportStatistics() {
		QifDom dom = QifDom.dom;

		final List<Account> ranking = new ArrayList<Account>();

		for (int acctid = 1; acctid <= dom.getNumAccounts(); ++acctid) {
			Account a = dom.getAccount(acctid);

			if (a != null) {
				ranking.add(a);
			}
		}

		final Comparator<Account> cmp = (o1, o2) -> {
			if (o1.statements.isEmpty()) {
				return (o2.statements.isEmpty()) ? 0 : -1;
			}
			return (o2.statements.isEmpty()) //
					? 1 //
					: o1.getLastStatementDate().compareTo(o2.getLastStatementDate());
		};

		Collections.sort(ranking, cmp);

		System.out.println();
		System.out.println("Overall");
		System.out.println();

		int unclracct_count = 0;
		int unclracct_utx_count = 0;
		int unclracct_tx_count = 0;

		int clracct_count = 0;
		int clracct_tx_count = 0;

		final Calendar cal = Calendar.getInstance();
		final Date today = cal.getTime();

		final long dayms = 1000L * 24 * 60 * 60;
		final long msCurrent = today.getTime();
		final Date minus30 = new Date(msCurrent - 30 * dayms);
		final Date minus60 = new Date(msCurrent - 60 * dayms);
		final Date minus90 = new Date(msCurrent - 90 * dayms);

		boolean nostat = false;
		boolean stat90 = false;
		boolean stat60 = false;
		boolean stat30 = false;
		boolean statcurrent = false;

		System.out.println(String.format("%3s   %-35s   %-8s  %-10s   %-5s %-5S      %-8s", //
				"N", "Account", "LastStmt", "Balance", "UncTx", "TotTx", "FirstUnc"));

		final int max = ranking.size();
		for (int ii = 0; ii < max; ++ii) {
			final Account a = ranking.get(ii);
			final Date laststatement = a.getLastStatementDate();

			if (laststatement == null) {
				if (!nostat) {
					System.out.println("### No statements");
					nostat = true;
				}
			} else if (laststatement.compareTo(minus90) < 0) {
				if (!stat90) {
					System.out.println("\n### More than 90 days");
					stat90 = true;
				}
			} else if (laststatement.compareTo(minus60) < 0) {
				if (!stat60) {
					System.out.println("\n### 60-90 days");
					stat60 = true;
				}
			} else if (laststatement.compareTo(minus30) < 0) {
				if (!stat30) {
					System.out.println("\n### 30-60 days");
					stat30 = true;
				}
			} else {
				if (!statcurrent) {
					System.out.println("\n### Less than 30 days");
					statcurrent = true;
				}
			}

			final int ucount = a.getUnclearedTransactionCount();
			final int tcount = a.transactions.size();

			if ((ucount > 0) || !a.isClosedAsOf(null)) {
				if (a.isClosedAsOf(null)) {
					System.out.println("Warning! Account " + a.getName() + " is closed!");
				}

				++unclracct_count;
				unclracct_utx_count += ucount;
				unclracct_tx_count += tcount;

				final String nam = a.getDisplayName(25);
				final Statement lStat = a.getLastStatement();
				final Date lStatDate = (lStat != null) ? lStat.date : null;

				System.out.println(String.format("%3d   %-35s : %8s  %s : %5d/%5d :    %8s", //
						unclracct_count, //
						nam, //
						Common.formatDate(lStatDate), //
						Common.formatAmount(a.balance), //
						ucount, //
						tcount, //
						Common.formatDate(a.getFirstUnclearedTransactionDate())));
			} else {
				++clracct_count;
				clracct_tx_count += tcount;
			}
		}

		System.out.println();
		System.out.println(String.format("   %5d / %5d uncleared tx in %4d open accounts", //
				unclracct_utx_count, unclracct_tx_count, unclracct_count));
		System.out.println(String.format("        %5d      cleared tx in %4d closed accounts", //
				clracct_tx_count, clracct_count));

		System.out.println();
	}

	public static void reportCashFlow(Date d1, Date d2) {
		QifDom dom = QifDom.dom;

		AccountPosition[] info = new AccountPosition[dom.getNumAccounts()];

		for (int id = 1; id <= dom.getNumAccounts(); ++id) {
			Account a = dom.getAccount(id);

			if (a != null) {
				info[id - 1].acct = a;

				BigDecimal v1 = a.getCashValueForDate(d1);
			}
		}
	}

	public static void reportActivity(Date d1, Date d2) {
		// TODO Auto-generated method stub
	}

	public static void reportYearlyStatus() {
		QifDom dom = QifDom.dom;

		System.out.println();

		Date d = dom.getFirstTransactionDate();
		final Date lastTxDate = dom.getLastTransactionDate();

		final Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		int year = cal.get(Calendar.YEAR);

		do {
			d = Common.getDateForEndOfMonth(year, 12);

			QifReporter.reportStatusForDate(d);

			++year;
		} while (d.compareTo(lastTxDate) < 0);
	}

	public static void reportAllAccountStatus() {
		final Calendar cal = Calendar.getInstance();
		reportStatusForDate(cal.getTime());
	}

	public static void reportDom(QifDom dom) {
		Object model = buildReportDomModel();

		outputReportDomModel(model);
	}

	private static Object buildReportDomModel() {
		return null;
	}

	private static void outputReportDomModel(Object model) {
		QifDom dom = QifDom.dom;

		reportGlobalPortfolio(dom.portfolio);

		System.out.println("============================");
		System.out.println("Accounts");
		System.out.println("============================");

		for (int idx = 0; idx < dom.getNumAccounts(); ++idx) {
			final Account a = dom.getAccountByTime(idx);

			if ((a == null) || //
					!a.isInvestmentAccount() || //
					(a.balance.compareTo(BigDecimal.ZERO) == 0)) {
				continue;
			}

			if (compact) {
				System.out.println(a.getName() + " " + a.type + " " + a.balance);
				continue;
			}

			switch (a.type) {
			case Bank:
			case CCard:
			case Cash:
			case Asset:
			case Liability:
				reportNonInvestmentAccount(a, true);
				break;

			case Inv401k:
			case InvMutual:
			case InvPort:
			case Invest:
				reportInvestmentAccount(a);
				break;

			default:
				break;
			}
		}
	}

	private static void reportNonInvestmentAccount(Account a, boolean includePseudoStatements) {
		System.out.println("----------------------------");
		// System.out.println(a.name + " " + a.type + " " + a.description);
		System.out.println(a.toString());
		final int ntran = a.transactions.size();

		// System.out.println(" " + ntran + " transactions");

		if (ntran > 0) {
			final GenericTxn ft = a.transactions.get(0);
			final GenericTxn lt = a.transactions.get(ntran - 1);

			System.out.println("    Date range: " //
					+ Common.formatDate(ft.getDate()) //
					+ " - " + Common.formatDate(lt.getDate()));
			if (includePseudoStatements) {
				System.out.println("----------------------------");
			}

			int curNumTxn = 0;
			int curYear = -1;
			int curMonth = -1;
			BigDecimal bal = BigDecimal.ZERO;
			final Calendar cal = Calendar.getInstance();

			for (final GenericTxn t : a.transactions) {
				final Date d = t.getDate();
				cal.setTime(d);

				if (includePseudoStatements) {
					if ((cal.get(Calendar.YEAR) != curYear) //
							|| (cal.get(Calendar.MONTH) != curMonth)) {
						System.out.println(Common.formatDate(t.getDate()) //
								+ ": " + bal //
								+ " " + curNumTxn + " transactions");

						curNumTxn = 0;
						curYear = cal.get(Calendar.YEAR);
						curMonth = cal.get(Calendar.MONTH);
					}

					++curNumTxn;
				}

				bal = bal.add(t.getAmount());
			}

			System.out.println("    " + ntran + " transactions");
			System.out.println("    Final: " + bal);
			// System.out.println(a.name + " " + a.type + " " + a.description);
		}

		System.out.println("----------------------------");
	}

	public static void reportInvestmentAccount(Account a) {
		System.out.println("----------------------------");
		// System.out.println(a.name + " " + a.type + " " + a.description);
		System.out.println(a.toString());

		final int ntran = a.transactions.size();
		System.out.println("" + ntran + " transactions");

		if (ntran > 0) {
			final GenericTxn ft = a.transactions.get(0);
			final GenericTxn lt = a.transactions.get(ntran - 1);

			System.out.println("Date range: " //
					+ Common.formatDate(ft.getDate()) //
					+ " - " + Common.formatDate(lt.getDate()));
			System.out.println("----------------------------");
		}

		reportPortfolio(a.securities);
	}

	public static void reportPortfolio(SecurityPortfolio port) {
		for (final SecurityPosition p : port.positions) {
			System.out.println("Sec: " + p.security.getName());

			System.out.println(String.format( //
					"  %-12s  %-10s  %10s  %10s", //
					"Date", "Action", "Shares", "Balance"));

			for (int ii = 0; ii < p.transactions.size(); ++ii) {
				final InvestmentTxn t = p.transactions.get(ii);

				if (t.getShares() != null) {
					final BigDecimal shrbal = p.shrBalance.get(ii);

					System.out.println(String.format( //
							"  %-12s  %-10s  %s  %s", //
							Common.formatDate(t.getDate()), //
							t.action.toString(), //
							Common.formatAmount3(t.getShares()), //
							Common.formatAmount3(shrbal)));
				}
			}

			System.out.println();
		}

		System.out.println("----------------------------");
	}

	public static void reportGlobalPortfolio(SecurityPortfolio port) {
		for (final SecurityPosition p : port.positions) {
			System.out.println("Sec: " + p.security.getName());

			System.out.println(String.format( //
					"  %-12s  %-20s  %-10s  %10s  %10s", //
					"Date", "Account", "Action", "Shares", "Balance"));

			for (int ii = 0; ii < p.transactions.size(); ++ii) {
				final InvestmentTxn t = p.transactions.get(ii);

				if (t.getShares() != null) {
					final BigDecimal shrbal = p.shrBalance.get(ii);

					System.out.println(String.format( //
							"  %-12s  %-20s  %-10s  %s  %s", //
							Common.formatDate(t.getDate()), //
							QifDom.dom.getAccount(t.acctid).getName(), //
							t.action.toString(), //
							Common.formatAmount3(t.getShares()), //
							Common.formatAmount3(shrbal)));
				}
			}

			System.out.println();
		}

		System.out.println("----------------------------");
	}
}
