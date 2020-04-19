package moneymgr.model;

import java.util.ArrayList;
import java.util.List;

/** Class comprising the complete MoneyManager data model */
public class MoneyMgrModel {
	private static final List<Category> categories = new ArrayList<>();

	public static int getNextCategoryID() {
		return (categories.isEmpty()) ? 1 : categories.size();
	}

	public static void addCategory(Category cat) {
		assert (findCategory(cat.name) == null);

		while (categories.size() <= cat.catid) {
			categories.add(null);
		}

		categories.set(cat.catid, cat);
	}

	public static Category getCategory(int catid) {
		return ((catid > 0) && (catid < categories.size())) //
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
}