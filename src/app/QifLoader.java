package app;

import qif.data.QifDom;
import qif.data.QifDomReader;
import qif.data.QifReporter;

public class QifLoader {
	public static void main(String[] args) {
		final String file = "/Users/greg/qif/dietrich.qif";
		// file = "/Users/greg/qif/87to16.qif";

		final QifDomReader rdr = new QifDomReader();
		final QifDom dom = rdr.load(file);

		QifReporter.reportDom(dom);

		// System.out.println(dom);
	}
}
