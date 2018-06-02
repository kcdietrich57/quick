package qif.report;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import qif.data.Account;
import qif.data.AccountType;
import qif.data.QDate;

public class StatusForDateModel {
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
				AccountType.Bank, AccountType.Cash, AccountType.Asset, //
				AccountType.Invest, AccountType.InvPort, //
				AccountType.InvMutual, AccountType.Inv401k, //
				AccountType.CCard, AccountType.Liability };

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

	public static class SecuritySummary {
		public String name;
		public BigDecimal value = BigDecimal.ZERO;
		public BigDecimal shares = BigDecimal.ZERO;
		public BigDecimal price = BigDecimal.ZERO;
	};

	public static class AccountSummary {
		public String name;
		public BigDecimal balance = BigDecimal.ZERO;
		public BigDecimal cashBalance = BigDecimal.ZERO;

		public List<StatusForDateModel.SecuritySummary> securities = new ArrayList<StatusForDateModel.SecuritySummary>();
	};

	public static class Section {
		public SectionInfo info;
		public BigDecimal subtotal = BigDecimal.ZERO;

		public List<StatusForDateModel.AccountSummary> accounts = new ArrayList<StatusForDateModel.AccountSummary>();
	};

	public QDate d;
	public BigDecimal assets = BigDecimal.ZERO;
	public BigDecimal liabilities = BigDecimal.ZERO;
	public BigDecimal netWorth = BigDecimal.ZERO;
	public StatusForDateModel.Section[] sections = new StatusForDateModel.Section[SectionInfo.sectionInfo.length];

	public StatusForDateModel() {
		for (int ii = 0; ii < SectionInfo.sectionInfo.length; ++ii) {
			StatusForDateModel.Section sect = new Section();
			this.sections[ii] = sect;

			sect.info = SectionInfo.sectionInfo[ii];
		}
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