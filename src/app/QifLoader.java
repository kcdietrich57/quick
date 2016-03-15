package app;

import qif.data.QifDom;
import qif.data.QifDomReader;
import qif.data.QifReporter;

public class QifLoader {
	public static void main(String[] args) {
		String file = "/tmp/dietrich.qif";
		file = "/Users/greg/qif/87to16.qif";

		QifDomReader rdr = new QifDomReader();
		QifDom dom = rdr.load(file);

		QifReporter.reportAccounts(dom);

		// System.out.println(dom);
	}
}
