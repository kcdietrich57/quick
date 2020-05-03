package moneymgr.model.compare;

import java.util.List;

import app.MoneyMgrApp;
import moneymgr.model.Account;
import moneymgr.model.Category;
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.Security;
import moneymgr.model.SimpleTxn;
import moneymgr.model.Statement;
import moneymgr.util.Common;

public class CompareModels {
	public static void compareModels(MoneyMgrModel m1, MoneyMgrModel m2) {
		Common.reportInfo(String.format("Categories: %s", MoneyMgrApp.elapsedTime()));
		compareCategories(m1, m2);

		Common.reportInfo(String.format("Securities: %s", MoneyMgrApp.elapsedTime()));
		compareSecurities(m1, m2);

		Common.reportInfo(String.format("Account Types: %s", MoneyMgrApp.elapsedTime()));

		Common.reportInfo(String.format("Account Categories: %s", MoneyMgrApp.elapsedTime()));

		Common.reportInfo(String.format("Accounts: %s", MoneyMgrApp.elapsedTime()));
		compareAccounts(m1, m2);

		Common.reportInfo(String.format("Transactions: %s", MoneyMgrApp.elapsedTime()));
		compareTransactions(m1, m2);

		Common.reportInfo(String.format("Portfolio: %s", MoneyMgrApp.elapsedTime()));

		Common.reportInfo(String.format("Lots: %s", MoneyMgrApp.elapsedTime()));

		Common.reportInfo(String.format("Options: %s", MoneyMgrApp.elapsedTime()));

		Common.reportInfo(String.format("Statements: %s", MoneyMgrApp.elapsedTime()));
		compareStatements(m1, m2);

		Common.reportInfo(String.format("Complete: %s", MoneyMgrApp.elapsedTime()));
	}

	static int txct = 0;

	private static void compareTransactions(MoneyMgrModel m1, MoneyMgrModel m2) {
		List<SimpleTxn> txns1 = m1.getAllTransactions();
		List<SimpleTxn> txns2 = m2.getAllTransactions();

		if (txns1.size() != txns2.size()) {
			System.out.println("Transaction count different");
		}

		for (int ii = 0; ii < txns1.size() && ii < txns2.size(); ++ii) {
			SimpleTxn t1 = txns1.get(ii);
			SimpleTxn t2 = txns2.get(ii);

			if (t1 != null && t2 != null) {
				String res = t1.matches(t2);
				if (res != null) {
					++txct;
					t1.matches(t2);
					System.out.println(t1.toString());
					System.out.println("Tx:" + res + ":" + txct);
				}
			} else if ((t1 == null) != (t2 == null)) {
				System.out.println("Transaction missing");
			}
		}
	}

	private static void compareAccounts(MoneyMgrModel m1, MoneyMgrModel m2) {
		List<Account> accts1 = m1.getAccountsById();
		List<Account> accts2 = m2.getAccountsById();

		if (accts1.size() != accts2.size()) {
			System.out.println("Account count different");
		}

		for (int ii = 0; ii < accts1.size() && ii < accts2.size(); ++ii) {
			Account a1 = accts1.get(ii);
			Account a2 = accts2.get(ii);

			if (a1 != null && a2 != null) {
				String res = a1.matches(a2);
				if (res != null) {
					System.out.println("Account mismatch: " + res);
				}
			} else if ((a1 == null) != (a2 == null)) {
				System.out.println("Account missing");
			}
		}
	}

	private static void compareSecurities(MoneyMgrModel m1, MoneyMgrModel m2) {
		List<Security> secs1 = m1.getSecuritiesById();
		List<Security> secs2 = m2.getSecuritiesById();

		if (secs1.size() != secs2.size()) {
			System.out.println("Security count different");
		}

		for (int ii = 0; ii < secs1.size() && ii < secs2.size(); ++ii) {
			Security s1 = secs1.get(ii);
			Security s2 = secs2.get(ii);

			if (s1 != null && s2 != null) {
				String res = s1.matches(s2);

				if (res != null) {
					System.out.println("Security mismatch: " + res);
					s1.matches(s2);
				}
			} else if ((s1 == null) != (s2 == null)) {
				System.out.println("Security missing");
			}
		}
	}

	private static void compareCategories(MoneyMgrModel m1, MoneyMgrModel m2) {
		List<Category> cats1 = m1.getCategories();
		List<Category> cats2 = m2.getCategories();

		if (cats1.size() != cats2.size()) {
			System.out.println("Category count different");
		}

		for (int ii = 0; ii < cats1.size() && ii < cats2.size(); ++ii) {
			Category c1 = cats1.get(ii);
			Category c2 = cats2.get(ii);

			if (c1 != null && c2 != null) {
				if (!c1.matches(c2)) {
					System.out.println("Category mismatch");
				}
			} else if ((c1 == null) != (c2 == null)) {
				System.out.println("Category missing");
			}
		}
	}

	private static void compareStatements(MoneyMgrModel m1, MoneyMgrModel m2) {
		for (Account a1 : m1.getAccounts()) {
			Account a2 = m2.getAccountByID(a1.acctid);

			if (a2 != null) {
				compareStatements(m1, m2, a1, a2);
			} else {
				System.out.println("Account missing for statements");
			}
		}
	}

	private static void compareStatements(MoneyMgrModel m1, MoneyMgrModel m2, //
			Account a1, Account a2) {
		List<Statement> stats1 = a1.getStatements();
		List<Statement> stats2 = a2.getStatements();

		if (stats1.size() != stats2.size()) {
			System.out.println("Statement count different");
		}

		for (int ii = 0; ii < stats1.size() && ii < stats2.size(); ++ii) {
			Statement s1 = stats1.get(ii);
			Statement s2 = stats2.get(ii);

			if (s1 != null && s2 != null) {
				String res = s1.matches(s2);
				if (res != null) {
					System.out.println("Statement mismatch:" + res);
				}
			} else if ((s1 == null) != (s2 == null)) {
				System.out.println("Statement missing");
			}
		}
	}
}
