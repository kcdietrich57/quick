package qif.data;

import java.util.Arrays;
import java.util.List;

/** Defines account categories for itemized status */
public enum AccountCategory {

	ASSET(0, true, "Asset", //
			new AccountType[] { AccountType.Asset }), //
	RETIRE(1, true, "Retirement", //
			new AccountType[] { AccountType.InvMutual, AccountType.Inv401k }), //
	INVEST(2, true, "Investment", //
			new AccountType[] { AccountType.Invest, AccountType.InvPort }), //
	BANK(3, true, "Bank", //
			new AccountType[] { AccountType.Bank, AccountType.Cash }), //
	CREDIT(4, false, "Credit Card", //
			new AccountType[] { AccountType.CCard }), //
	LOAN(5, false, "Loan", //
			new AccountType[] { AccountType.Liability });

	public static final AccountCategory[] accountCategoryInfo = { //
			ASSET, RETIRE, INVEST, BANK, CREDIT, LOAN //
	};

	private static List<AccountCategory> listOrder = //
			Arrays.asList(new AccountCategory[] { //
					BANK, CREDIT, INVEST, RETIRE, ASSET, LOAN //
			});

	public int getAccountListOrder() {
		return listOrder.indexOf(this);
	}

	public static int numCategories() {
		return accountCategoryInfo.length;
	}

	public static final String[] accountCategoryNames = new String[] { //
			"Retirement", "Asset", "Investment", "Bank", "Credit Card", "Loan" //
	};

	public static AccountCategory forAccountType(AccountType type) {
		switch (type) {
		case Asset:
			return ASSET;

		case Bank:
		case Cash:
			return BANK;

		case CCard:
			return CREDIT;

		case Inv401k:
		case InvMutual:
			return RETIRE;

		case Invest:
		case InvPort:
			return INVEST;

		case Liability:
			return LOAN;
		}

		return null;
	}

	public final int index;
	public final boolean isAsset;
	public final String label;

	private AccountCategory(int val, boolean isAsset, String label, //
			AccountType[] atypes) {
		this.index = val;
		this.isAsset = isAsset;
		this.label = label;
	}

	public int index() {
		return this.index;
	}

	public String toString() {
		switch (this) {
		case ASSET:
			return "Asset";
		case BANK:
			return "Bank";
		case CREDIT:
			return "Credit";
		case INVEST:
			return "Investment";
		case LOAN:
			return "Loan";
		case RETIRE:
			return "Retirement";
		}

		return "UNKNOWN";
	}

	public boolean contains(AccountType at) {
		return this == forAccountType(at);
	}
}
