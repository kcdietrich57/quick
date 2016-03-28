package qif.data;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import qif.data.SimpleTxn.Action;

public class QifReporter {
	public static boolean compact = false;

	public static void reportAccounts(QifDom dom) {
		System.out.println("============================");
		System.out.println("Accounts");
		System.out.println("============================");

		for (int idx = 0; idx < dom.getNumAccounts(); ++idx) {
			Account a = dom.getAccountByTime(idx);

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
		int ntran = a.transactions.size();

		// System.out.println(" " + ntran + " transactions");

		if (ntran > 0) {
			GenericTxn ft = a.transactions.get(0);
			GenericTxn lt = a.transactions.get(ntran - 1);

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
			Calendar cal = Calendar.getInstance();

			for (GenericTxn t : a.transactions) {
				Date d = t.getDate();
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
		int ntran = a.transactions.size();

		System.out.println("" + ntran + " transactions");

		if (ntran > 0) {
			GenericTxn ft = a.transactions.get(0);
			GenericTxn lt = a.transactions.get(ntran - 1);

			System.out.println("Date range: " //
					+ Common.getDateString(ft.getDate()) //
					+ " - " + Common.getDateString(lt.getDate()));
			System.out.println("----------------------------");
		}

		for (SecurityPosition p : a.securities.positions) {
			BigDecimal shrbal = BigDecimal.ZERO;

			System.out.println("Sec: " + p.security.name);

			for (InvestmentTxn t : p.transactions) {
				if (t.getAction() == Action.ActionStockSplit) {
					shrbal = shrbal.multiply(t.quantity);
					shrbal = shrbal.divide(BigDecimal.TEN);
				} else if (t.quantity != null) {
					shrbal = shrbal.add(t.quantity);
				}

				if (t.quantity != null) {
					System.out.println(String.format( //
							"  %-12s  %-16s Shares: %10.3f  Bal: %10.3f", //
							Common.getDateString(t.getDate()), //
							t.action.toString(), //
							t.quantity, //
							shrbal));
				}
			}
		}

		System.out.println("----------------------------");
	}
}
