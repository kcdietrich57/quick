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
					"First/last tx date (1): " + Common.formatDate(firstTxDate) //
							+ " " + Common.formatDate(lastTxDate));

			dom.showStatistics();

			final String s = scn.nextLine();

			if (s.startsWith("q")) {
				break;
			}

			if (s.startsWith("accts")) {
				dom.reportAllAccountStatus();
			} else if (s.startsWith("mnw")) {
				dom.reportMonthlyNetWorth();
			} else if (s.startsWith("relog")) {
				dom.rewriteStatementLogFile();
			} else if (s.startsWith("ys")) {
				dom.reportYearlyStatus();
			} else if (s.startsWith("g")) {
				final String aname = s.substring(1).trim();

				final Account a = dom.findAccount(aname);
				if (a != null) {
					a.generateMonthlyStatements();
				}
			} else if (s.startsWith("a")) {
				final String aname = s.substring(1).trim();

				final Account a = dom.findAccount(aname);
				if (a != null) {
					a.reportStatus("m");
				}
			} else if (s.startsWith("s")) {
				dom.reportStatistics();
			} else {
				final Date d = Common.parseDate(s);
				if (d != null) {
					dom.reportStatusForDate(d);
				}
			}
		}

		scn.close();
	}
}
