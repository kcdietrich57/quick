package qif.data;

// TODO this should be an enum
/** Defines account categories for itemized status */
public class AccountCategory {
	public static final int RETIRE = 0;
	public static final int ASSET = 1;
	public static final int INVEST = 2;
	public static final int BANK = 3;
	public static final int CREDIT = 4;
	public static final int LOAN = 5;

	public static final String[] accountCategoryNames = new String[] { //
			"Bank", "Asset", "Investment", "Retirement", "Credit Card", "Loan" //
	};

	public static final AccountCategory[] accountCategoryInfo = {
			new AccountCategory("Bank", new AccountType[] { AccountType.Bank, AccountType.Cash }, true), //
			new AccountCategory("Asset", new AccountType[] { AccountType.Asset }, true), //
			new AccountCategory("Investment", new AccountType[] { AccountType.Invest, AccountType.InvPort }, true), //
			new AccountCategory("Retirement", new AccountType[] { AccountType.InvMutual, AccountType.Inv401k },
					true), //
			new AccountCategory("Credit Card", new AccountType[] { AccountType.CCard }, false), //
			new AccountCategory("Loan", new AccountType[] { AccountType.Liability }, false) //
	};

	private static AccountType[] allAcctTypes = { //
			AccountType.Bank, AccountType.Cash, //
			AccountType.Asset, //
			AccountType.Invest, AccountType.InvPort, //
			AccountType.InvMutual, AccountType.Inv401k, //
			AccountType.CCard, //
			AccountType.Liability };

	private AccountType[] accountTypes;
	public final String label;
	public boolean isAsset;

	public AccountCategory(String label, AccountType[] atypes, boolean isAsset) {
		this.label = label;
		this.accountTypes = atypes;
		this.isAsset = isAsset;
	}

	public static AccountCategory getSectionInfoForAccount(Account a) {
		for (AccountCategory sinfo : accountCategoryInfo) {
			if (sinfo.contains(a.type)) {
				return sinfo;
			}
		}

		return null;
	}

	public static AccountType[] getAccountTypes() {
		return allAcctTypes;
	}

	public boolean contains(AccountType at) {
		for (final AccountType myat : this.accountTypes) {
			if (myat == at) {
				return true;
			}
		}

		return false;
	}
}
