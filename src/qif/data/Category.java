package qif.data;

import java.util.ArrayList;
import java.util.List;

/** Transaction category for tracking/budgeting */
public class Category {
	private static final List<Category> categories = new ArrayList<Category>();

	public static int getNextCategoryID() {
		return (categories.isEmpty()) ? 1 : categories.size();
	}

	public static Category getCategory(int catid) {
		return ((catid > 0) && catid < categories.size()) //
				? categories.get(catid) //
				: null;
	}

	public static Category findCategory(String name) {
		if (!name.isEmpty()) {
			for (Category cat : categories) {
				if ((cat != null) && cat.name.equals(name)) {
					return cat;
				}
			}
		}

		return null;
	}

	public static void addCategory(Category cat) {
		assert (findCategory(cat.name) == null);

		while (categories.size() <= cat.catid) {
			categories.add(null);
		}

		categories.set(cat.catid, cat);
	}

	public final int catid;
	public final String name;
	public final String description;
	public final boolean isExpense;

	public Category(String name, String desc, boolean isExpense) {
		this.catid = getNextCategoryID();
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
