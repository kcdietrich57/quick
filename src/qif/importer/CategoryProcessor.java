package qif.importer;

import qif.data.Category;
import qif.data.Common;

class CategoryProcessor {
	QifDomReader qrdr;

	public CategoryProcessor(QifDomReader qrdr) {
		this.qrdr = qrdr;
	}

	public void loadCategories() {
		for (;;) {
			String s = this.qrdr.getFileReader().peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			Category cat = loadCategory();
			if (cat == null) {
				break;
			}

			Category existing = Category.findCategory(cat.name);

			if (existing == null) {
				Category.addCategory(cat);
			}
		}
	}

	private Category loadCategory() {
		final QFileReader.QLine qline = new QFileReader.QLine();

		final Category cat = new Category();

		for (;;) {
			this.qrdr.getFileReader().nextCategoryLine(qline);

			switch (qline.type) {
			case EndOfSection:
				return cat;

			case CatName:
				cat.name = qline.value;
				break;
			case CatDescription:
				cat.description = qline.value;
				break;
			case CatTaxRelated:
				// cat.taxRelated = Common.parseBoolean(qline.value);
				break;
			case CatIncomeCategory:
				cat.expenseCategory = !Common.parseBoolean(qline.value);
				// cat.incomeCategory = Common.parseBoolean(qline.value);
				break;
			case CatExpenseCategory:
				cat.expenseCategory = Common.parseBoolean(qline.value);
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