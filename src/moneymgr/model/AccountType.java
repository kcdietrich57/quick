package moneymgr.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import moneymgr.util.Common;

/** Account type (e.g. banking vs hard asset vs investment) */
public enum AccountType { //
	Bank(1, "BNK", "Bank", true, true, false), //
	CCard(2, "CCD", "CCard", false, false, false), //
	Cash(3, "CSH", "Cash", true, true, false), //
	Asset(4, "AST", "Oth A", false, true, false), //
	Liability(5, "LIA", "Oth L", false, false, false), //
	Invest(6, "INV", "Invst", false, true, true), //
	InvPort(7, "INV", "Port", false, true, true), //
	Inv401k(8, "RET", "401(k)/403(b)", false, true, true), //
	InvMutual(9, "INV", "Mutual", false, true, true);

	public final int id;
	private final String qname;
	public final String name;
	public final boolean isAsset;
	public final boolean isInvestment;
	public final boolean isCash;

	private static final Map<String, AccountType> quickenAccountType = new HashMap<String, AccountType>();
	private static final List<AccountType> accountTypes = new ArrayList<AccountType>(9);

	public static final AccountType[] getAccountTypes() {
		return new AccountType[] { //
				Bank, CCard, Cash, Asset, Liability, //
				Invest, InvPort, Inv401k, InvMutual //
		};
	}

	static {
		for (AccountType at : AccountType.values()) {
			AccountType.quickenAccountType.put(at.qname, at);

			while (AccountType.accountTypes.size() < at.id) {
				AccountType.accountTypes.add(null);
			}
			AccountType.accountTypes.add(at.id, at);
		}
	}

	private AccountType(int id, String name, String qname, //
			boolean isCash, boolean isAsset, boolean isInvestment) {
		this.id = id;
		this.name = name;
		this.qname = qname;
		this.isCash = isCash;
		this.isAsset = isAsset;
		this.isInvestment = isInvestment;
	}

	/** Return account type for name from QIF input file */
	public static AccountType parseAccountType(String qname) {
		AccountType at = quickenAccountType.get(qname);

		if (at != null) {
			return at;
		}

		Common.reportError(String.format("Unknown account type: '%s'", qname));
		return AccountType.Bank;
	}

	public static AccountType byId(int id) {
		return accountTypes.get(id);
	}

	public boolean isLiability() {
		return !this.isAsset;
	}

	public boolean isAsset() {
		return this.isAsset;
	}

	public boolean isCash() {
		return this.isCash;
	}

	public boolean isInvestment() {
		return this.isInvestment;
	}

	public boolean isNonInvestment() {
		return !this.isInvestment;
	}

	public String toString() {
		return this.name;
	}
}