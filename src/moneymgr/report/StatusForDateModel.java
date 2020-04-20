package moneymgr.report;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import moneymgr.model.Account;
import moneymgr.model.AccountCategory;
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.Security;
import moneymgr.model.SecurityPosition;
import moneymgr.model.StockOption;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/**
 * This captures a point in time snapshot of account values<br>
 * Used by various reports (e.g. NetWorth, StatusForDate, InvestmentPerformance)
 */
public class StatusForDateModel {
	/** Describes the holdings/value of a security at a point in time */
	public static class SecuritySummary {
		public String name;
		public BigDecimal shares = BigDecimal.ZERO;
		public BigDecimal price = BigDecimal.ZERO;
		public BigDecimal value = BigDecimal.ZERO;

		public String toString() {
			return String.format("%s: %s@%s %s", //
					this.name, //
					Common.formatAmount3(this.shares).trim(), //
					Common.formatAmount3(this.price).trim(), //
					Common.formatAmount3(this.value).trim());
		}
	};

	/** Describes the cash/security balance of an account at a point in time */
	public static class AccountSummary {
		public String name;
		public BigDecimal balance = BigDecimal.ZERO;
		public BigDecimal cashBalance = BigDecimal.ZERO;

		public List<StatusForDateModel.SecuritySummary> securities = new ArrayList<StatusForDateModel.SecuritySummary>();
	};

	/** Accumulates info for a section (account category) */
	public static class Section {
		public static Section[] getSections(boolean forChart) {
			int numcat = AccountCategory.numCategories();
			Section[] sections = new StatusForDateModel.Section[numcat];

			for (int ii = 0; ii < numcat; ++ii) {
				sections[ii] = new Section((forChart) //
						? AccountCategory.accountCategoryInfoForChart[ii]
						: AccountCategory.accountCategoryInfoForStatus[ii]);
			}

			return sections;
		}

		public final AccountCategory acctCategory;
		public BigDecimal subtotal = BigDecimal.ZERO;

		public List<StatusForDateModel.AccountSummary> accounts = new ArrayList<StatusForDateModel.AccountSummary>();

		public Section(AccountCategory category) {
			this.acctCategory = category;
		}
	};

	public final QDate date;
	public BigDecimal assets = BigDecimal.ZERO;
	public BigDecimal liabilities = BigDecimal.ZERO;
	public BigDecimal netWorth = BigDecimal.ZERO;
	public final StatusForDateModel.Section[] sections;

	/** Construct status for a particular date */
	public StatusForDateModel(QDate date) {
		this.date = date;
		this.sections = Section.getSections(true);

		build();
	}

	/** Determine which section an account belongs to */
	public StatusForDateModel.Section getSectionForAccount(Account acct) {
		for (StatusForDateModel.Section s : this.sections) {
			if (acct.acctCategory == s.acctCategory) {
				return s;
			}
		}

		return null;
	}

	/** Construct model from account info */
	private void build() {
		for (Account acct : MoneyMgrModel.getAccounts()) {
			if (!acct.isOpenOn(this.date)) {
				continue;
			}

			BigDecimal amt = acct.getValueForDate(this.date);

			if (Common.isEffectivelyZero(amt) //
					&& (acct.getFirstUnclearedTransaction() == null) //
					&& acct.securities.isEmptyForDate(this.date)) {
				continue;
			}

			StatusForDateModel.Section modelsect = getSectionForAccount(acct);

			StatusForDateModel.AccountSummary asummary = new StatusForDateModel.AccountSummary();
			modelsect.accounts.add(asummary);
			modelsect.subtotal = modelsect.subtotal.add(amt);

			asummary.name = acct.getDisplayName(36);
			asummary.balance = asummary.cashBalance = amt;

			List<StockOption> opts = StockOption.getOpenOptions(acct, this.date);
			if (!opts.isEmpty()) {
				StatusForDateModel.SecuritySummary ssummary = new StatusForDateModel.SecuritySummary();

				StockOption opt = opts.get(0);
				Security sec = MoneyMgrModel.getSecurity(opt.secid);

				ssummary.name = "Options:" + sec.getName();
				ssummary.shares = opt.getAvailableShares(true);
				ssummary.price = sec.getPriceForDate(this.date).getPrice();
				ssummary.value = opt.getValueForDate(this.date);

				asummary.securities.add(ssummary);
			}

			if (!acct.securities.isEmptyForDate(this.date)) {
				BigDecimal portValue = acct.getSecuritiesValueForDate(this.date);

				if (!Common.isEffectivelyZero(portValue)) {
					asummary.cashBalance = amt.subtract(portValue);

					for (SecurityPosition pos : acct.securities.positions) {
						BigDecimal posval = pos.getValueForDate(this.date);

						if (!Common.isEffectivelyZero(posval)) {
							StatusForDateModel.SecuritySummary ssummary = new StatusForDateModel.SecuritySummary();
							asummary.securities.add(ssummary);

							String nn = pos.security.getName();
							if (nn.length() > 34) {
								nn = nn.substring(0, 31) + "...";
							}

							ssummary.name = nn;
							ssummary.value = posval;
							ssummary.price = pos.security.getPriceForDate(this.date).getPrice();
							ssummary.shares = pos.getSharesForDate(this.date);
						}
					}
				}
			}

			modelsect.subtotal.add(amt);

			if (modelsect.acctCategory.isAsset) {
				this.assets = this.assets.add(amt);
			} else {
				this.liabilities = this.liabilities.add(amt);
			}

			this.netWorth = this.netWorth.add(amt);
		}
	}
}