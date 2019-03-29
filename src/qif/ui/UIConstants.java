package qif.ui;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import qif.data.AccountCategory;

public class UIConstants {
	
	/** Colors for account categories in graphs */
	public static Map<AccountCategory, Color> acctCategoryColor = new HashMap<>();

	static {
		acctCategoryColor.put(AccountCategory.CREDITCARD, Color.YELLOW);
		acctCategoryColor.put(AccountCategory.LOAN, Color.MAGENTA);
		acctCategoryColor.put(AccountCategory.ASSET, new Color(127, 127, 127));
		acctCategoryColor.put(AccountCategory.RETIREMENT, Color.GREEN);
		acctCategoryColor.put(AccountCategory.BANK, Color.RED);
		acctCategoryColor.put(AccountCategory.INVESTMENT, Color.BLUE);
	}

	public static final Color LIGHT_BLUE = new Color(245, 245, 255);
	public static final Color LIGHT_YELLOW = new Color(250, 250, 220);
	public static final Color LIGHT_GRAY = new Color(240, 240, 240);
	public static final Color DARK_GRAY = new Color(16, 16, 16);
	
}
