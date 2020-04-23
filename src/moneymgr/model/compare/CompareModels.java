package moneymgr.model.compare;

import java.util.List;

import moneymgr.model.Account;
import moneymgr.model.Category;
import moneymgr.model.GenericTxn;
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.Security;
import moneymgr.model.Statement;

public class CompareModels {
	public static void compareModels(MoneyMgrModel m1, MoneyMgrModel m2) {
		/**
		 * 1. Compare categories <br>
		 * 2. Compare securities <br>
		 * 3. Compare account types<br>
		 * 4. Compare account categories<br>
		 * 5. Compare accounts<br>
		 * 6. Compare transactions<br>
		 * 7. Compare lots<br>
		 * 8. Compare options<br>
		 * 9. Compare statements<br>
		 */
		System.out.println("Comparing JSON model");

		compareCategories(m1, m2);
		compareSecurities(m1, m2);
		compareAccounts(m1, m2);
		compareTransactions(m1, m2);
		compareStatements(m1, m2);

		System.out.println("Compare complete");
	}

	static int txct = 0;

	private static void compareTransactions(MoneyMgrModel m1, MoneyMgrModel m2) {
		List<GenericTxn> txns1 = m1.getAllTransactions();
		List<GenericTxn> txns2 = m2.getAllTransactions();

		if (txns1.size() != txns2.size()) {
			System.out.println("Transaction count different");
		}

		for (int ii = 0; ii < txns1.size() && ii < txns2.size(); ++ii) {
			GenericTxn t1 = txns1.get(ii);
			GenericTxn t2 = txns2.get(ii);

			if (t1 != null && t2 != null) {
				if (!t1.matches(t2)) {
					++txct;
					t1.matches(t2);
					System.out.println(t1.toString());
					System.out.println("Transaction mismatch " + txct);
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
				if (!a1.matches(a2)) {
					System.out.println("Account mismatch");
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
				if (!s1.matches(s2)) {
					System.out.println("Security mismatch");
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
				if (!s1.matches(s2)) {
					System.out.println("Statement mismatch");
				}
			} else if ((s1 == null) != (s2 == null)) {
				System.out.println("Statement missing");
			}
		}
	}
}
