package qif.report;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import qif.data.Account;
import qif.data.AccountType;
import qif.data.Common;
import qif.data.QDate;

/** This captures a point in time snapshot of account values */
public class StatusForDateModel {

	/** Defines account categories for itemized status */
	public static class SectionInfo {
		public static final SectionInfo[] sectionInfo = {
				new SectionInfo("Bank", new AccountType[] { AccountType.Bank, AccountType.Cash }, true), //
				new SectionInfo("Asset", new AccountType[] { AccountType.Asset }, true), //
				new SectionInfo("Investment", new AccountType[] { AccountType.Invest, AccountType.InvPort }, true), //
				new SectionInfo("Retirement", new AccountType[] { AccountType.InvMutual, AccountType.Inv401k }, true), //
				new SectionInfo("Credit Card", new AccountType[] { AccountType.CCard }, false), //
				new SectionInfo("Loan", new AccountType[] { AccountType.Liability }, false) //
		};

		private static AccountType[] allAcctTypes = { //
				AccountType.Bank, AccountType.Cash, //
				AccountType.Asset, //
				AccountType.Invest, AccountType.InvPort, //
				AccountType.InvMutual, AccountType.Inv401k, //
				AccountType.CCard, //
				AccountType.Liability };

		public AccountType[] atypes;
		public String label;
		public boolean isAsset;

		public SectionInfo(String label, AccountType[] atypes, boolean isAsset) {
			this.label = label;
			this.atypes = atypes;
			this.isAsset = isAsset;
		}

		public static SectionInfo getSectionInfoForAccount(Account a) {
			for (SectionInfo sinfo : sectionInfo) {
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
			for (final AccountType myat : this.atypes) {
				if (myat == at) {
					return true;
				}
			}

			return false;
		}
	};

	/** Describes the holdings/value of a security at a point in time */
	public static class SecuritySummary {
		public String name;
		public BigDecimal value = BigDecimal.ZERO;
		public BigDecimal shares = BigDecimal.ZERO;
		public BigDecimal price = BigDecimal.ZERO;

		public String toString() {
			return String.format("%s: %s@%s %s", //
					this.name, //
					Common.formatAmount3(this.shares).trim(), //
					Common.formatAmount3(this.price).trim(), //
					Common.formatAmount3(this.value).trim());
		}
	};

	/** Describes the cash/total balance of an account at a point in time */
	public static class AccountSummary {
		public String name;
		public BigDecimal balance = BigDecimal.ZERO;
		public BigDecimal cashBalance = BigDecimal.ZERO;

		public List<StatusForDateModel.SecuritySummary> securities = new ArrayList<StatusForDateModel.SecuritySummary>();
	};

	/** Accumulates info for a section (account category) */
	public static class Section {
		public static Section[] getSections() {
			Section[] sections = new StatusForDateModel.Section[SectionInfo.sectionInfo.length];

			for (int ii = 0; ii < SectionInfo.sectionInfo.length; ++ii) {
				sections[ii] = new Section(SectionInfo.sectionInfo[ii]);
			}

			return sections;
		}

		public final SectionInfo info;
		public BigDecimal subtotal = BigDecimal.ZERO;

		public List<StatusForDateModel.AccountSummary> accounts = new ArrayList<StatusForDateModel.AccountSummary>();

		public Section(SectionInfo info) {
			this.info = info;
		}
	};

	public final QDate date;
	public BigDecimal assets = BigDecimal.ZERO;
	public BigDecimal liabilities = BigDecimal.ZERO;
	public BigDecimal netWorth = BigDecimal.ZERO;
	public final StatusForDateModel.Section[] sections;

	public StatusForDateModel(QDate date) {
		this.date = date;
		this.sections = Section.getSections();
	}

	public StatusForDateModel.Section getSectionForAccount(Account acct) {
		for (StatusForDateModel.Section s : this.sections) {
			if (s.info.contains(acct.type)) {
				return s;
			}
		}

		return null;
	}
}

/**
 * This models investment performance for a period.
 * 
 * Summary details are available for the entire portfolio, individual account,
 * account category, and individual security.
 */
class InvestmentPerformanceModel {

	/** Defines account categories for itemized status */
	public static class SectionInfo {
		public static final SectionInfo[] sectionInfo = {
				new SectionInfo("Investment", new AccountType[] { AccountType.Invest, AccountType.InvPort }, true), //
				new SectionInfo("Retirement", new AccountType[] { AccountType.InvMutual, AccountType.Inv401k }, true) //
		};

		private static AccountType[] allAcctTypes = { //
				AccountType.Invest, AccountType.InvPort, //
				AccountType.InvMutual, AccountType.Inv401k //
		};

		public AccountType[] atypes;
		public String label;

		public SectionInfo(String label, AccountType[] atypes, boolean isAsset) {
			this.label = label;
			this.atypes = atypes;
		}

		public static SectionInfo getSectionInfoForAccount(Account a) {
			for (SectionInfo sinfo : sectionInfo) {
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
			for (final AccountType myat : this.atypes) {
				if (myat == at) {
					return true;
				}
			}

			return false;
		}
	};

	/**
	 * Describes a security's performance for the period (global or account)
	 */
	public static class SecuritySummary {
		public String name;
		public BigDecimal startValue = BigDecimal.ZERO;
		public BigDecimal startShares = BigDecimal.ZERO;
		public BigDecimal startPrice = BigDecimal.ZERO;
		public BigDecimal endValue = BigDecimal.ZERO;
		public BigDecimal endShares = BigDecimal.ZERO;
		public BigDecimal endPrice = BigDecimal.ZERO;

		public BigDecimal contributions = BigDecimal.ZERO;
		public BigDecimal matchingContributions = BigDecimal.ZERO;
		public BigDecimal dividendValue = BigDecimal.ZERO;
		public BigDecimal dividendShares = BigDecimal.ZERO;

		// Calculated as (value-costBasis) for holdings at end
		public BigDecimal gainLoss = BigDecimal.ZERO;

		public String toString() {
			return String.format("%s: %s@%s %s", //
					this.name, //
					Common.formatAmount3(this.endShares).trim(), //
					Common.formatAmount3(this.endPrice).trim(), //
					Common.formatAmount3(this.endValue).trim());
		}
	};

	/** Describes an account's performance for the period */
	public static class AccountSummary {
		public String name;

		public BigDecimal startBalance = BigDecimal.ZERO;
		public BigDecimal startCashBalance = BigDecimal.ZERO;
		public BigDecimal endBalance = BigDecimal.ZERO;
		public BigDecimal endCashBalance = BigDecimal.ZERO;

		public List<SecuritySummary> securities = new ArrayList<SecuritySummary>();
	};

	/** Accumulates info for a section (account category) */
	public static class Section {
		public static Section[] getSections() {
			Section[] sections = new Section[SectionInfo.sectionInfo.length];

			for (int ii = 0; ii < SectionInfo.sectionInfo.length; ++ii) {
				sections[ii] = new Section(SectionInfo.sectionInfo[ii]);
			}

			return sections;
		}

		public final SectionInfo info;
		public BigDecimal subtotal = BigDecimal.ZERO;

		public List<AccountSummary> accounts = new ArrayList<AccountSummary>();

		public Section(SectionInfo info) {
			this.info = info;
		}
	};

	public final QDate startDate;
	public final QDate endDate;

	public final Section[] sections;
	public AccountSummary[] accounts;
	public SecuritySummary[] securities;

	// TODO create appropriate summary info variables
	// Is this an accumulation of account info? or security info?
	// i.e. does it include cash positions in investment accounts?
	public BigDecimal assets = BigDecimal.ZERO;
	public BigDecimal liabilities = BigDecimal.ZERO;
	public BigDecimal netWorth = BigDecimal.ZERO;

	public InvestmentPerformanceModel(QDate startDate, QDate endDate) {
		this.startDate = startDate;
		this.endDate = endDate;
		this.sections = Section.getSections();
	}

	public Section getSectionForAccount(Account acct) {
		for (Section s : this.sections) {
			if (s.info.contains(acct.type)) {
				return s;
			}
		}

		return null;
	}
}