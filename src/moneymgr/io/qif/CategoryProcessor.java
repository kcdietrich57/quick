package moneymgr.io.qif;

import moneymgr.model.Category;
import moneymgr.util.Common;

/** Process input file details for a category */
public class CategoryProcessor {
	QifDomReader qrdr;

	public CategoryProcessor(QifDomReader qrdr) {
		this.qrdr = qrdr;
	}

	/** Load a section containing categories, creating category objects */
	public void loadCategories() {
		for (;;) {
			String s = this.qrdr.getFileReader().peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			if (!loadCategory()) {
				break;
			}
		}
	}

	private boolean loadCategory() {
		QFileReader.QLine qline = new QFileReader.QLine();

		String name = null;
		String desc = null;
		boolean isExpense = true;

		for (;;) {
			this.qrdr.getFileReader().nextCategoryLine(qline);

			switch (qline.type) {
			case EndOfSection:
				if (name == null) {
					return false;
				}

				Category cat = Category.findCategory(name);
				// TODO To be anal, we could check if cat.xxx matches input

				if (cat == null) {
					cat = new Category(name, desc, isExpense);
					Category.addCategory(cat);
				}

				return true;

			case CatName:
				name = qline.value;
				break;
			case CatDescription:
				desc = qline.value;
				break;
			case CatTaxRelated:
				// cat.taxRelated = Common.parseBoolean(qline.value);
				break;
			case CatIncomeCategory:
				isExpense = !Common.parseBoolean(qline.value);
				break;
			case CatExpenseCategory:
				isExpense = Common.parseBoolean(qline.value);
				break;
			case CatBudgetAmount:
				// cat.budgetAmount = Common.getDecimal(qline.value);
				break;
			case CatTaxSchedule:
				break;

			default:
				Common.reportError("syntax error");
			}
		}
	}
}