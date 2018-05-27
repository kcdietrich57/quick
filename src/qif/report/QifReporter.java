package qif.report;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import qif.data.Account;
import qif.data.AccountPosition;
import qif.data.Common;
import qif.data.GenericTxn;
import qif.data.InvestmentTxn;
import qif.data.QifDom;
import qif.data.SecurityPortfolio;
import qif.data.SecurityPosition;
import qif.data.Statement;
import qif.data.QifDom.Balances;

public class QifReporter {
	public static boolean compact = false;

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

	// ===============================================================

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

	// ===============================================================

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

	// ===============================================================

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

			NetWorthReporter.reportNetWorthForDate(d);

			++year;
		} while (d.compareTo(lastTxDate) < 0);
	}

	// ===============================================================

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

	private static void reportInvestmentAccount(Account a) {
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

	private static void reportPortfolio(SecurityPortfolio port) {
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

	private static void reportGlobalPortfolio(SecurityPortfolio port) {
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
