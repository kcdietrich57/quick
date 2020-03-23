package moneymgr.report.obsolete;

import moneymgr.model.Account;
import moneymgr.model.GenericTxn;
import moneymgr.model.Statement;
import moneymgr.util.Common;

/** Report account info - used by obsolete QifLoader */
public class AccountReporter {

	public static void reportStatus(Account acct, String interval) {
		if (acct.statements.isEmpty()) {
			System.out.println("No statements for " + acct.getDisplayName(36));
		} else {
			System.out.println();
			System.out.println("-------------------------------------\n" //
					+ acct.getDisplayName(36));
			System.out.println(String.format("%d Statements, %d Transactions", //
					acct.statements.size(), acct.getNumTransactions()));
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
						s.date.toString(), s.transactions.size(), //
						Common.formatAmount(s.closingBalance)));
			}

			System.out.println();

			System.out.println("Uncleared transactions as of last statement:");

			for (GenericTxn t : acct.statements.get(acct.statements.size() - 1).unclearedTransactions) {
				System.out.println(String.format("  %s  %s  %s", //
						t.getDate().toString(), //
						Common.formatAmount(t.getAmount()), //
						t.getPayee()));
			}

			int unclearedCount = 0;

			for (GenericTxn t : acct.getTransactions()) {
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
}