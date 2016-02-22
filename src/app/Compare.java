package app;

import data.QifMerger;
import qif.data.QifDom;
import qif.data.QifDomReader;
import qif.data.QifReporter;

public class Compare {

	public static void main(String[] args) {
		String file1 = "/tmp/qif/75to87.qif";
		String file2 = "/tmp/qif/87to16.qif";

		QifDomReader rdr = new QifDomReader();
		QifDom dom1 = rdr.load(file1);
		QifDom dom2 = rdr.load(dom1, file2);

		QifDom dom3 = QifMerger.merge(dom1, dom2);
		
		QifReporter.reportAccounts(dom3);
	}
}
