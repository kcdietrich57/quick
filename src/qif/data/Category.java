package qif.data;

import java.util.ArrayList;
import java.util.List;

public class Category {
	private static final List<Category> categories = new ArrayList<Category>();

	public static int getNextCategoryID() {
		return (categories.isEmpty()) ? 1 : categories.size();
	}

	public static Category getCategory(int catid) {
		if (catid < 1 || catid >= categories.size()) {
			return null;
		}

		return categories.get(catid);
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

		cat.catid = getNextCategoryID();

		while (categories.size() <= cat.catid) {
			categories.add(null);
		}

		categories.set(cat.catid, cat);
	}

	public int catid;
	public String name;
	public String description;
	public boolean expenseCategory;

	public Category() {
		this.catid = (short) 0;
	}

	public String toString() {
		final String s = "Category" + this.catid + ": " + this.name //
				+ " desc=" + this.description //
				+ " expcat=" + this.expenseCategory //
				+ "\n";

		return s;
	}
}
