package qif.data;

/** Defines account categories for itemized status */
public enum AccountCategory {

	RETIRE(0, true, "Retirement", //
			new AccountType[] { AccountType.InvMutual, AccountType.Inv401k }), //
	ASSET(1, true, "Asset", //
			new AccountType[] { AccountType.Asset }), //
	INVEST(2, true, "Investment", //
			new AccountType[] { AccountType.Invest, AccountType.InvPort }), //
	BANK(3, true, "Bank", //
			new AccountType[] { AccountType.Bank, AccountType.Cash }), //
	CREDIT(4, false, "Credit Card", //
			new AccountType[] { AccountType.CCard }), //
	LOAN(5, false, "Loan", //
			new AccountType[] { AccountType.Liability });

	public static final int RETIRE_IDX = 0;
	public static final int ASSET_IDX = 1;
	public static final int INVEST_IDX = 2;
	public static final int BANK_IDX = 3;
	public static final int CREDIT_IDX = 4;
	public static final int LOAN_IDX = 5;

	public static final AccountCategory[] accountCategoryInfo = { //
			RETIRE, ASSET, INVEST, BANK, CREDIT, LOAN };

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
