package qif.data;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

public class QifReporter {
	public static void reportAccounts(QifDom dom) {
		System.out.println("============================");
		System.out.println("Accounts");
		System.out.println("============================");

		for (Account a : dom.accounts_bytime) {
			if (a == null) {
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
		System.out.println(a.name + " " + a.type + " " + a.description);
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
			BigDecimal bal = new BigDecimal(0);
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

	private static void reportInvestmentAccount(Account a) {
		System.out.println("----------------------------");
		System.out.println(a.name + " " + a.type + " " + a.description);
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

		System.out.println("----------------------------");
	}
}
