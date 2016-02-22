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
		for (Category c : fromdom.categories) {
			if ((c != null) && (todom.findCategory(c.name) == null)) {
				todom.addCategory(c);
			}
		}
	}
}
