package moneymgr.model;

import moneymgr.util.Common;

/** Transaction category for tracking/budgeting */
public class Category {
	public final MoneyMgrModel model;
	public final int catid;
	public final String name;
	public final String description;
	public final boolean isExpense;

	public Category(int catid, String name, String desc, boolean isExpense) {
		this.model = MoneyMgrModel.currModel;
		this.catid = (catid > 0) ? catid : this.model.nextCategoryID();
		this.name = name;
		this.description = desc;
		this.isExpense = isExpense;
	}

	public Category(String name, String desc, boolean isExpense) {
		this(0, name, desc, isExpense);
	}

	public String toString() {
		final String s = "Category" + this.catid + ": " + this.name //
				+ " desc=" + this.description //
				+ " expense=" + this.isExpense //
				+ "\n";

		return s;
	}

	public boolean matches(Category other) {
		return (this.catid == other.catid) //
				&& Common.safeEquals(this.name, other.name) //
				&& Common.safeEquals(this.description, other.description) //
				&& (this.isExpense == other.isExpense);
	}
}
