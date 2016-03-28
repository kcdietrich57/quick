package data;

import qif.data.Category;
import qif.data.QifDom;

public class QifMerger {

	public static QifDom merge(QifDom dom1, QifDom dom2) {
		QifDom newdom = new QifDom();

		mergeCategories(newdom, dom1);
		mergeCategories(newdom, dom2);

		return newdom;
	}

	private static void mergeCategories(QifDom todom, QifDom fromdom) {
		for (int ii = 1; ii <= fromdom.getNumCategories(); ++ii) {
			Category c = fromdom.getCategory(ii);
			todom.addCategory(c);
		}
	}
}
