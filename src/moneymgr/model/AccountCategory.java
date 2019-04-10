package moneymgr.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Defines account type categories for itemizing status output */
public enum AccountCategory {

	ASSET(true, "Asset", //
			new AccountType[] { AccountType.Asset }), //
	RETIREMENT(true, "Retirement", //
			new AccountType[] { AccountType.InvMutual, AccountType.Inv401k }), //
	INVESTMENT(true, "Investment", //
			new AccountType[] { AccountType.Invest, AccountType.InvPort }), //
	BANK(true, "Bank/Cash", //
			new AccountType[] { AccountType.Bank, AccountType.Cash }), //
	CREDITCARD(false, "Credit Card", //
			new AccountType[] { AccountType.CCard }), //
	LOAN(false, "Loan", //
			new AccountType[] { AccountType.Liability });

	/** Ordering (bottom to top) of categories in the itemized/stacked charts */
	public static final AccountCategory[] accountCategoryInfoForChart;
	public static final AccountCategory[] accountCategoryInfoForStatus;
	public static final List<String> accountCategoryLabelsForChart;
	public static final List<String> accountCategoryLabelsForStatus;

	/** Ordering (top to bottom) of account categories in the Accounts list */
	private static final List<AccountCategory> listOrder;

	static {
		accountCategoryInfoForChart = new AccountCategory[] { //
				ASSET, RETIREMENT, INVESTMENT, BANK, CREDITCARD, LOAN //
		};

		accountCategoryInfoForStatus = new AccountCategory[] { //
				BANK, CREDITCARD, RETIREMENT, INVESTMENT, ASSET, LOAN //
		};

		accountCategoryLabelsForChart = new ArrayList<>(accountCategoryInfoForChart.length);
		for (int idx = 0; idx < accountCategoryInfoForChart.length; ++idx) {
			accountCategoryLabelsForChart.add(idx, accountCategoryInfoForChart[idx].label);
		}

		accountCategoryLabelsForStatus = new ArrayList<>(accountCategoryInfoForStatus.length);
		for (int idx = 0; idx < accountCategoryInfoForStatus.length; ++idx) {
			accountCategoryLabelsForStatus.add(idx, accountCategoryInfoForStatus[idx].label);
		}

		listOrder = Arrays.asList(new AccountCategory[] { //
				BANK, CREDITCARD, INVESTMENT, RETIREMENT, ASSET, LOAN //
		});
	}

	public int getAccountListOrder() {
		return listOrder.indexOf(this);
	}

	public static int numCategories() {
		return accountCategoryInfoForChart.length;
	}

	public static AccountCategory forAccountType(AccountType type) {
		switch (type) {
		case Asset:
			return ASSET;

		case Bank:
		case Cash:
			return BANK;

		case CCard:
			return CREDITCARD;

		case Inv401k:
		case InvMutual:
			return RETIREMENT;

		case Invest:
		case InvPort:
			return INVESTMENT;

		case Liability:
			return LOAN;
		}

		return null;
	}

	public final boolean isAsset;
	public final String label;

	private AccountCategory(boolean isAsset, String label, AccountType[] atypes) {
		this.isAsset = isAsset;
		this.label = label;
	}

	public String toString() {
		switch (this) {
		case ASSET:
			return "Asset";
		case BANK:
			return "Bank";
		case CREDITCARD:
			return "Credit";
		case INVESTMENT:
			return "Investment";
		case LOAN:
			return "Loan";
		case RETIREMENT:
			return "Retirement";
		}

		return "UNKNOWN";
	}
}
