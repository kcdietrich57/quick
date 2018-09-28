package qif.report;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import qif.data.Account;
import qif.data.AccountCategory;
import qif.data.Common;
import qif.data.QDate;
import qif.data.Security;
import qif.data.SecurityPosition;
import qif.data.StockOption;

/** This captures a point in time snapshot of account values */
public class StatusForDateModel {
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
			Section[] sections = new StatusForDateModel.Section[AccountCategory.accountCategoryInfo.length];

			for (int ii = 0; ii < AccountCategory.accountCategoryInfo.length; ++ii) {
				sections[ii] = new Section(AccountCategory.accountCategoryInfo[ii]);
			}

			return sections;
		}

		public final AccountCategory info;
		public BigDecimal subtotal = BigDecimal.ZERO;

		public List<StatusForDateModel.AccountSummary> accounts = new ArrayList<StatusForDateModel.AccountSummary>();

		public Section(AccountCategory info) {
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

		build();
	}

	public StatusForDateModel.Section getSectionForAccount(Account acct) {
		for (StatusForDateModel.Section s : this.sections) {
			if (s.info.contains(acct.type)) {
				return s;
			}
		}

		return null;
	}

	private void build() {
		for (Account a : Account.getAccounts()) {
			if (!a.isOpenAsOf(this.date)) {
				continue;
			}

			BigDecimal amt = a.getValueForDate(this.date);

			if (!a.isOpenOn(this.date) //
					|| (Common.isEffectivelyZero(amt) //
							&& (a.getFirstUnclearedTransaction() == null) //
							&& a.securities.isEmptyForDate(this.date))) {
				continue;
			}

			StatusForDateModel.Section modelsect = getSectionForAccount(a);

			StatusForDateModel.AccountSummary asummary = new StatusForDateModel.AccountSummary();
			modelsect.accounts.add(asummary);
			modelsect.subtotal = modelsect.subtotal.add(amt);

			asummary.name = a.getDisplayName(36);
			asummary.balance = asummary.cashBalance = amt;

			List<StockOption> opts = StockOption.getOpenOptions(a, this.date);
			if (!opts.isEmpty()) {
				StatusForDateModel.SecuritySummary ssummary = new StatusForDateModel.SecuritySummary();

				StockOption opt = opts.get(0);
				Security sec = Security.getSecurity(opt.secid);

				ssummary.name = "Options:" + sec.getName();
				ssummary.shares = opt.getAvailableShares(true);
				ssummary.price = sec.getPriceForDate(this.date).getPrice();
				ssummary.value = opt.getValueForDate(this.date);

				asummary.securities.add(ssummary);
			}

			if (!a.securities.isEmptyForDate(this.date)) {
				BigDecimal portValue = a.getSecuritiesValueForDate(this.date);

				if (!Common.isEffectivelyZero(portValue)) {
					asummary.cashBalance = amt.subtract(portValue);

					for (SecurityPosition pos : a.securities.positions) {
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

			if (modelsect.info.isAsset) {
				this.assets = this.assets.add(amt);
			} else {
				this.liabilities = this.liabilities.add(amt);
			}

			this.netWorth = this.netWorth.add(amt);
		}
	}
}