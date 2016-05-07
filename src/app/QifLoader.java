package app;

import java.util.Date;
import java.util.Scanner;
import java.util.StringTokenizer;

import qif.data.Account;
import qif.data.Common;
import qif.data.QifDom;
import qif.data.QifDomReader;

public class QifLoader {
	public static void main(String[] args) {
		final QifDom dom = QifDomReader.loadDom(new String[] { //
				"qif/75to87.qif", //
				"qif/87ToNow.qif" });

		final Date firstTxDate = dom.getFirstTransactionDate();
		final Date lastTxDate = dom.getLastTransactionDate();
		final Scanner scn = new Scanner(System.in);

		for (;;) {
			System.out.println( //
					"First/last tx date (1): " + Common.getDateString(firstTxDate) //
							+ " " + Common.getDateString(lastTxDate));

			final String s = scn.nextLine();

			if (s.startsWith("q")) {
				break;
			}

			if (s.startsWith("a")) {
				final StringTokenizer toker = new StringTokenizer(s, " ");
				toker.nextToken();

				String aname = "";

				if (toker.hasMoreTokens()) {
					aname = toker.nextToken();
					final Account a = dom.findAccount(aname);
					if (a != null) {
						a.reportStatus("m");
					}
				}
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
