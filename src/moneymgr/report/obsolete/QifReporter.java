package moneymgr.report.obsolete;

import java.util.Date;

import moneymgr.model.Account;
import moneymgr.model.GenericTxn;
import moneymgr.model.MoneyMgrModel;

/** Various reporting functions - used by obsolete QifLoader */
public class QifReporter {
	public static void reportActivity(Date d1, Date d2) {
		// Auto-generated method stub
	}

	public static void showStatistics() {
		int nullt = 0;
		int reconciled = 0;
		int unreconciled = 0;

		for (GenericTxn t : MoneyMgrModel.getAllTransactions()) {
			if (t == null) {
				++nullt;
			} else if (t.stmtdate != null) {
				++reconciled;
			} else {
				++unreconciled;
			}
		}

		int total = (reconciled + unreconciled);
		final double pct = reconciled * 100.0 / total;

		System.out.println(String.format("%d of %d txns reconciled (%5.2f) nullTX: %d", //
				reconciled, total, pct, nullt));
	}

	public static void generateMonthlyStatements(Account a) {
		System.out.println("\n!Account");
		System.out.println("N" + a.name);
		System.out.println("^");
		System.out.println("!Statements");

		String lastMonthStr = "";
		int lastyear = -1;
		int lastmonth = -1;
		GenericTxn lasttx = null;
		boolean first = true;

		for (GenericTxn t : a.getTransactions()) {
			int thisyear = t.getDate().getYear();
			int thismonth = t.getDate().getMonth();
			String thisMonthStr = t.getDate().monthYearString;

			if (!lastMonthStr.isEmpty() && !lastMonthStr.equals(thisMonthStr)) {
				// End of month
				if (!first) {
					// Append balance to this year's line
					System.out.print(String.format(" %4.2f", lasttx.runningTotal));
				}

				// New year, or skipped month
				if ((lastyear != thisyear) || (lastmonth + 1 != thismonth)) {
					if (!first) {
						System.out.println();
					}

					System.out.print("M" + t.getDate().monthYearString);
				}

				lastyear = thisyear;
				lastmonth = thismonth;
				lastMonthStr = thisMonthStr;
			}

			first = false;
			lasttx = t;
		}

		System.out.println();
		System.out.println("^");
	}
}
