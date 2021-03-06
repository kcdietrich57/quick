package moneymgr.report.obsolete;

import java.math.BigDecimal;
import java.util.List;

import app.QifDom;
import moneymgr.model.Account;
import moneymgr.model.GenericTxn;
import moneymgr.model.InvestmentTxn;
import moneymgr.model.SecurityPortfolio;
import moneymgr.model.SecurityPosition;
import moneymgr.ui.MainFrame;
import moneymgr.util.Common;

/** TODO unused */
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
		reportGlobalPortfolio();

		System.out.println("============================");
		System.out.println("Accounts");
		System.out.println("============================");

		for (Account a : MainFrame.appFrame.model.getAccounts()) {
			if (!a.isInvestmentAccount() || //
					Common.isEffectivelyZero(a.getBalance())) {
				continue;
			}

			if (compact) {
				System.out.println(a.name + " " + a.type + " " + a.getBalance());
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

		List<GenericTxn> txns = a.getTransactions();
		int ntran = a.getNumTransactions();
		// System.out.println(" " + ntran + " transactions");

		if (ntran > 0) {
			GenericTxn ft = txns.get(0);
			GenericTxn lt = txns.get(ntran - 1);

			System.out.println("    Date range: " //
					+ ft.getDate().toString() + " - " + lt.getDate().toString());

			if (includePseudoStatements) {
				System.out.println("----------------------------");
			}

			int curNumTxn = 0;
			String curMonth = "";
			BigDecimal bal = BigDecimal.ZERO;

			for (GenericTxn t : txns) {
				if (includePseudoStatements) {
					String txmonth = t.getDate().monthYearString;

					if (!curMonth.isEmpty() && !curMonth.equals(txmonth)) {
						System.out.println(curMonth //
								+ ": " + bal //
								+ " " + curNumTxn + " transactions");

						curNumTxn = 0;
						curMonth = txmonth;
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

		List<GenericTxn> txns = a.getTransactions();
		int ntran = a.getNumTransactions();
		System.out.println("" + ntran + " transactions");

		if (ntran > 0) {
			GenericTxn ft = txns.get(0);
			GenericTxn lt = txns.get(ntran - 1);

			System.out.println("Date range: " //
					+ ft.getDate().toString() + " - " + lt.getDate().toString());
			System.out.println("----------------------------");
		}

		reportPortfolio(a.securities);
	}

	private static void reportPortfolio(SecurityPortfolio port) {
		for (SecurityPosition p : port.getPositions()) {
			System.out.println("Sec: " + p.security.getName());

			System.out.println(String.format( //
					"  %-12s  %-10s  %10s  %10s", //
					"Date", "Action", "Shares", "Balance"));

			List<InvestmentTxn> txns = p.getTransactions();
			List<BigDecimal> shrbals = p.getShareBalances();

			for (int ii = 0; ii < txns.size(); ++ii) {
				InvestmentTxn t = txns.get(ii);

				if (t.getShares() != null) {
					BigDecimal shrbal = shrbals.get(ii);

					System.out.println(String.format( //
							"  %-12s  %-10s  %s  %s", //
							t.getDate().toString(), //
							t.getAction().toString(), //
							Common.formatAmount3(t.getShares()), //
							Common.formatAmount3(shrbal)));
				}
			}

			System.out.println();
		}

		System.out.println("----------------------------");
	}

	private static void reportGlobalPortfolio() {
		for (SecurityPosition p : MainFrame.appFrame.model.portfolio.getPositions()) {
			System.out.println("Sec: " + p.security.getName());

			System.out.println(String.format( //
					"  %-12s  %-20s  %-10s  %10s  %10s", //
					"Date", "Account", "Action", "Shares", "Balance"));

			List<InvestmentTxn> txns = p.getTransactions();
			List<BigDecimal> shrbals = p.getShareBalances();

			for (int ii = 0; ii < txns.size(); ++ii) {
				InvestmentTxn t = txns.get(ii);

				if (t.getShares() != null) {
					BigDecimal shrbal = shrbals.get(ii);

					System.out.println(String.format( //
							"  %-12s  %-20s  %-10s  %s  %s", //
							t.getDate().toString(), //
							MainFrame.appFrame.model.getAccountByID(t.getAccountID()).name, //
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