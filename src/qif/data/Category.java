
package qif.data;

public class Category {
	public int catid;

	public String name;
	public String description;
	public boolean expenseCategory;

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
		this.expenseCategory = other.expenseCategory;
	}

	public String toString() {
		final String s = "Category" + this.catid + ": " + this.name //
				+ " desc=" + this.description //
				+ " expcat=" + this.expenseCategory //
				+ "\n";

		return s;
	}
};
