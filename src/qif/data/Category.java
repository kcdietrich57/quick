
package qif.data;

import java.math.BigDecimal;

public class Category {
	public int catid;

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
		this.catid = (short) 0;
	}

	public Category(int id) {
		this.catid = id;
	}

	public Category(Category other) {
		this(other.catid);

		this.name = other.name;
		this.description = other.description;
		this.taxRelated = other.taxRelated;
		this.incomeCategory = other.incomeCategory;
		this.expenseCategory = other.expenseCategory;
		this.budgetAmount = other.budgetAmount;
	}

	public String toString() {
		final String s = "Category" + this.catid + ": " + this.name //
				+ " desc=" + this.description //
				+ " tax=" + this.taxRelated //
				+ " inccat=" + this.incomeCategory //
				+ " expcat=" + this.expenseCategory //
				+ " bud=" + this.budgetAmount //
				+ "\n";

		return s;
	}
};
