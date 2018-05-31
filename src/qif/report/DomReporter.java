package qif.report;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import qif.data.Account;
import qif.data.Common;
import qif.data.GenericTxn;
import qif.data.InvestmentTxn;
import qif.data.QifDom;
import qif.data.SecurityPortfolio;
import qif.data.SecurityPosition;

public class DomReporter {

	public static boolean compact = false;

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

		for (Account a : dom.getAccounts()) {
			if (!a.isInvestmentAccount() || //
					Common.isEffectivelyZero(a.balance)) {
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
							t.getAction().toString(), //
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
							QifDom.dom.getAccountByID(t.acctid).getName(), //
							t.getAction().toString(), //
							Common.formatAmount3(t.getShares()), //
							Common.formatAmount3(shrbal)));
				}
			}

			System.out.println();
		}

		System.out.println("----------------------------");
	}

}