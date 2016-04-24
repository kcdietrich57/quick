package app;

import java.util.Date;
import java.util.Scanner;

import qif.data.Common;
import qif.data.QifDom;
import qif.data.QifDomReader;

public class QifLoader {
//	private static QifDom loadDom(String qifFile) {
//		final QifDomReader rdr = new QifDomReader();
//		final QifDom dom = rdr.load(qifFile);
//
//		return dom;
//	}

	private static QifDom loadDom(String[] qifFiles) {
		final QifDomReader rdr = new QifDomReader();
		final QifDom dom = new QifDom();

		for (final String fn : qifFiles) {
			rdr.load(dom, fn);
		}

		return dom;
	}

	public static void main(String[] args) {
		final String file1 = "/Users/greg/qif/75to87.qif";
		final String file2 = "/Users/greg/qif/87ToNow.qif";

		// final QifDom dom1 = loadDom(file1);
		// final QifDom dom2 = loadDom(file2);
		final QifDom dom = loadDom(new String[] { file1, file2 });

		// QifReporter.reportDom(dom1);
		// System.out.println(dom1);

		final Date firstTxDate = dom.getFirstTransactionDate();
		final Date lastTxDate = dom.getLastTransactionDate();
		final Scanner scn = new Scanner(System.in);
		for (;;) {
			System.out.println( //
					"First/last tx date (1): " + Common.getDateString(firstTxDate) //
							+ " " + Common.getDateString(lastTxDate));
			// Date firstTxDate2 = dom2.getFirstTransactionDate();
			// Date lastTxDate2 = dom2.getLastTransactionDate();
			// System.out.println( //
			// "First/last tx date (2): " + Common.getDateString(firstTxDate2)
			// //
			// + " " + Common.getDateString(lastTxDate2));

			final String s = scn.nextLine();

			if (s.compareToIgnoreCase("quit") == 0) {
				break;
			}

			final Date d = Common.parseDate(s);
			if (d != null) {
				// dom1.reportStatusForDate(d, true);
				// dom2.reportStatusForDate(d, true);
				dom.reportStatusForDate(d, true);
			}
		}

		scn.close();
	}
}
