package qif.report;

import java.util.Calendar;
import java.util.Date;

import qif.data.Account;
import qif.data.Common;
import qif.data.GenericTxn;

public class QifReporter {
	public static void reportActivity(Date d1, Date d2) {
		// TODO Auto-generated method stub
	}

	public static void showStatistics() {
		int nullt = 0;
		int reconciled = 0;
		int unreconciled = 0;

		for (GenericTxn t : GenericTxn.getAllTransactions()) {
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
		int lastyear = -1;
		int lastmonth = -1;
		GenericTxn lasttx = null;
		boolean first = true;

		System.out.println("\n!Account");
		System.out.println("N" + a.getName());
		System.out.println("^");
		System.out.println("!Statements");

		for (final GenericTxn t : a.transactions) {
			final Calendar cal = Calendar.getInstance();
			cal.setTime(t.getDate());

			final int thisyear = cal.get(Calendar.YEAR);
			final int thismonth = cal.get(Calendar.MONTH);

			if ((lastyear != thisyear) || (lastmonth != thismonth)) {
				if (!first) {
					System.out.print(String.format(" %4.2f", lasttx.runningTotal));
				}

				if ((lastyear != thisyear) || (lastmonth + 1 != thismonth)) {
					if (!first) {
						System.out.println();
					}

					System.out.print("M" + Common.formatDateMonthYear(t.getDate()));
				}

				lastyear = thisyear;
				lastmonth = thismonth;
			}

			first = false;
			lasttx = t;
		}

		System.out.println();
		System.out.println("^");
	}
}
