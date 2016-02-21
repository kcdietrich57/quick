
package qif.data;

import java.math.BigDecimal;

// !Type:Cat
class Category {
	private static short nextid = 1;
	public final short id;

	public String name;
	public String description;
	public boolean taxRelated;
	public boolean incomeCategory;
	public boolean expenseCategory;
	public BigDecimal budgetAmount;

	// public string TaxSchedule {
	// CategoryName = "";
	// Description = "";
	// TaxSchedule = "";
	// }

	public Category() {
		this.id = nextid++;
	}

	public static Category load(QFileReader reader) {
		QFileReader.QLine qline = new QFileReader.QLine();

		Category cat = new Category();

		for (;;) {
			reader.nextCategoryLine(qline);

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
				cat.taxRelated = Common.GetBoolean(qline.value);
				break;
			case CatIncomeCategory:
				cat.incomeCategory = Common.GetBoolean(qline.value);
				break;
			case CatExpenseCategory:
				cat.expenseCategory = Common.GetBoolean(qline.value);
				break;
			case CatBudgetAmount:
				cat.budgetAmount = Common.getDecimal(qline.value);
				break;
			case CatTaxSchedule:
				break;

			default:
				Common.reportError("syntax error");
			}
		}
	}

	public String toString() {
		String s = "Category" + this.id + ": " + this.name //
				+ " desc=" + this.description //
				+ " tax=" + this.taxRelated //
				+ " inccat=" + this.incomeCategory //
				+ " expcat=" + this.expenseCategory //
				+ " bud=" + this.budgetAmount //
				+ "\n";

		return s;
	}

	// static void Export(StreamWriter writer, List<CategoryListTransaction>
	// list) {
	// if ((list == null) || (list.Count == 0)) {
	// return;
	// }
	//
	// writer.WriteLine(Headers.CategoryList);
	//
	// foreach (CategoryListTransaction item in list) {
	// writer.WriteLine(BudgetAmount +
	// item.BudgetAmount.ToString(CultureInfo.CurrentCulture));
	// writeIfSet(CategoryName, item.CategoryName);
	// writeIfSet(Description, item.Description);
	// writer.WriteLine(ExpenseCategory + item.ExpenseCategory.ToString());
	// writer.WriteLine(IncomeCategory + item.IncomeCategory.ToString());
	// writer.WriteLine(TaxRelated + item.TaxRelated.ToString());
	// writeIfSet(TaxSchedule, item.TaxSchedule);
	// writer.WriteLine(EndOfEntry);
	// }
	// }
};
