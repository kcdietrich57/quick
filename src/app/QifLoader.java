package app;

import java.util.Date;
import java.util.Scanner;

import qif.data.Common;
import qif.data.QifDom;
import qif.data.QifDomReader;

public class QifLoader {
	public static void main(String[] args) {
		String file;
		file = "/Users/greg/qif/87to05.qif";
		file = "/Users/greg/qif/87to16.qif";

		file = "/Users/greg/qif/75to87.qif";
		file = "/Users/greg/qif/dietrich.qif";

		final QifDomReader rdr = new QifDomReader();
		final QifDom dom = rdr.load(file);

		// QifReporter.reportDom(dom);
		// System.out.println(dom);

		final Scanner scn = new Scanner(System.in);
		for (;;) {
			final String s = scn.nextLine();

			if (s.compareToIgnoreCase("quit") == 0) {
				break;
			}

			final Date d = Common.parseDate(s);
			if (d != null) {
				dom.reportStatusForDate(d, true);
			}
		}

		scn.close();
	}
}
