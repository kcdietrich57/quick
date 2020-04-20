package moneymgr.model;

/** Transaction category for tracking/budgeting */
public class Category {
	public final int catid;
	public final String name;
	public final String description;
	public final boolean isExpense;

	public Category(String name, String desc, boolean isExpense) {
		this.catid = MoneyMgrModel.currModel.nextCategoryID();
		this.name = name;
		this.description = desc;
		this.isExpense = isExpense;
	}

	public String toString() {
		final String s = "Category" + this.catid + ": " + this.name //
				+ " desc=" + this.description //
				+ " expense=" + this.isExpense //
				+ "\n";

		return s;
	}
}
