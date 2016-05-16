package app;

import data.QifMerger;
import qif.data.QifDom;
import qif.data.QifDomReader;
import qif.data.QifReporter;

public class Compare {

	public static void main(String[] args) {
		final String file1 = "/tmp/qif/75to87.qif";
		final String file2 = "/tmp/qif/87to16.qif";

		final QifDomReader rdr = new QifDomReader(null);
		final QifDom dom1 = rdr.load(file1);
		final QifDom dom2 = rdr.load(dom1, file2);

		final QifDom dom3 = QifMerger.merge(dom1, dom2);

		QifReporter.reportDom(dom3);
	}
}
