package app;

import java.util.Date;
import java.util.Scanner;

import qif.data.Account;
import qif.data.Common;
import qif.data.QifDom;
import qif.data.QifDomReader;

public class QifLoader {
	public static Scanner scn;

	public static void main(String[] args) {
		scn = new Scanner(System.in);

		final QifDom dom = QifDomReader.loadDom(new String[] { //
				"qif/75to87.qif", //
				"qif/87ToNow.qif" });

		final Date firstTxDate = dom.getFirstTransactionDate();
		final Date lastTxDate = dom.getLastTransactionDate();

		for (;;) {
			System.out.println( //
					"First/last tx date (1): " + Common.getDateString(firstTxDate) //
							+ " " + Common.getDateString(lastTxDate));

			final String s = scn.nextLine();

			if (s.startsWith("q")) {
				break;
			}

			if (s.startsWith("a")) {
				final String aname = s.substring(1).trim();

				final Account a = dom.findAccount(aname);
				if (a != null) {
					a.reportStatus("m");
				}
			} else if (s.startsWith("s")) {
				dom.showStatistics();
			} else {
				final Date d = Common.parseDate(s);
				if (d != null) {
					dom.reportStatusForDate(d, true);
				}
			}
		}

		scn.close();
	}
}
