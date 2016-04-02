package qif.data;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

public class QifReporter {
	public static boolean compact = false;

	public static void reportDom(QifDom dom) {
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
				System.out.println(a.name + " " + a.type + " " + a.balance);
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
					+ Common.getDateString(ft.getDate()) //
					+ " - " + Common.getDateString(lt.getDate()));
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
						System.out.println(Common.getDateString(t.getDate()) //
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
					+ Common.getDateString(ft.getDate()) //
					+ " - " + Common.getDateString(lt.getDate()));
			System.out.println("----------------------------");
		}

		reportPortfolio(a.securities);
	}

	public static void reportPortfolio(SecurityPortfolio port) {
		for (final SecurityPosition p : port.positions) {
			System.out.println("Sec: " + p.security.name);

			System.out.println(String.format( //
					"  %-12s  %-10s  %10s  %10s", //
					"Date", "Action", "Shares", "Balance"));

			for (int ii = 0; ii < p.transactions.size(); ++ii) {
				final InvestmentTxn t = p.transactions.get(ii);

				if (t.quantity != null) {
					final BigDecimal shrbal = p.shrBalance.get(ii);

					System.out.println(String.format( //
							"  %-12s  %-10s  %10.3f  %10.3f", //
							Common.getDateString(t.getDate()), //
							t.action.toString(), //
							t.quantity, //
							shrbal));
				}
			}

			System.out.println();
		}

		System.out.println("----------------------------");
	}

	public static void reportGlobalPortfolio(SecurityPortfolio port) {
		for (final SecurityPosition p : port.positions) {
			System.out.println("Sec: " + p.security.name);

			System.out.println(String.format( //
					"  %-12s  %-20s  %-10s  %10s  %10s", //
					"Date", "Account", "Action", "Shares", "Balance"));

			for (int ii = 0; ii < p.transactions.size(); ++ii) {
				final InvestmentTxn t = p.transactions.get(ii);

				if (t.quantity != null) {
					final BigDecimal shrbal = p.shrBalance.get(ii);

					System.out.println(String.format( //
							"  %-12s  %-20s  %-10s  %10.3f  %10.3f", //
							Common.getDateString(t.getDate()), //
							QifDom.getDomById(t.domid).getAccount(t.acctid).name, //
							t.action.toString(), //
							t.quantity, //
							shrbal));
				}
			}

			System.out.println();
		}

		System.out.println("----------------------------");
	}
}
