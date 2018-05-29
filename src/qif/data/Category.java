package qif.data;

import java.util.ArrayList;
import java.util.List;

public class Category {
	public static final List<Category> categories = new ArrayList<Category>();

	public static int getNumCategories() {
		return categories.size() - 1;
	}

	public static int getNextCategoryID() {
		return (categories.isEmpty()) ? 1 : categories.size();
	}

	public static Category getCategory(int catid) {
		return categories.get(catid);
	}

	public static int findCategoryID(String s) {
		if (s.startsWith("[")) {
			s = s.substring(1, s.length() - 1).trim();

			Account acct = QifDom.dom.findAccount(s);

			return (short) ((acct != null) ? (-acct.acctid) : 0);
		}

		final int slash = s.indexOf('/');
		if (slash >= 0) {
			// Throw away tag
			s = s.substring(slash + 1);
		}

		Category cat = findCategory(s);

		return (cat != null) ? (cat.catid) : 0;
	}

	public static Category findCategory(String name) {
		for (Category cat : categories) {
			if ((cat != null) && cat.name.equals(name)) {
				return cat;
			}
		}

		return null;
	}

	public static void addCategory(Category cat) {
		Category existing = findCategory(cat.name);

		if (existing != null) {
			Common.reportError("Adding duplicate category");
		}

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
