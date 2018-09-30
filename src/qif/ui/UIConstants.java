package qif.ui;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import qif.data.AccountCategory;

public class UIConstants {
	public static Map<AccountCategory, Color> acctCategoryColor = //
			new HashMap<AccountCategory, Color>();

	static {
		acctCategoryColor.put(AccountCategory.CREDIT, Color.YELLOW);
		acctCategoryColor.put(AccountCategory.LOAN, Color.MAGENTA);
		acctCategoryColor.put(AccountCategory.ASSET, Color.GREEN);
		acctCategoryColor.put(AccountCategory.RETIRE, Color.DARK_GRAY);
		acctCategoryColor.put(AccountCategory.BANK, Color.RED);
		acctCategoryColor.put(AccountCategory.INVEST, Color.BLUE);
	}

}
