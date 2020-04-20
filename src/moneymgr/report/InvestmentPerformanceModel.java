package moneymgr.report;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import moneymgr.model.Account;
import moneymgr.model.AccountType;
import moneymgr.model.GenericTxn;
import moneymgr.model.InvestmentTxn;
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.Security;
import moneymgr.model.SecurityPortfolio;
import moneymgr.model.SecurityPosition;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/**
 * TODO experimental; Investment performance for a period.
 * 
 * Summary details are available for the entire portfolio, individual account,
 * account category, and individual security.
 */
public class InvestmentPerformanceModel {

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

	public static class Summary {
		BigDecimal startCash = BigDecimal.ZERO;
		BigDecimal endCash = BigDecimal.ZERO;
		BigDecimal startInv = BigDecimal.ZERO;
		BigDecimal endInv = BigDecimal.ZERO;

		BigDecimal contrib = BigDecimal.ZERO;
		BigDecimal match = BigDecimal.ZERO;
		BigDecimal div = BigDecimal.ZERO;

		public BigDecimal diffCash() {
			return this.endCash.subtract(this.startCash);
		}

		public BigDecimal diffInv() {
			return this.endInv.subtract(this.startInv);
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();

			String begin = String.format("%10s / %10s", //
					Common.formatAmount(this.startInv), //
					Common.formatAmount(this.startCash));
			String end = String.format("%10s / %10s", //
					Common.formatAmount(this.endInv), //
					Common.formatAmount(this.endCash));
			String diff = String.format("%10s / %10s", //
					Common.formatAmount(diffInv()), //
					Common.formatAmount(diffCash()));

			sb.append(String.format("%-30s: %34s | %34s | %34s", //
					"Summary", begin, end, diff));

			return sb.toString();
		}
	}

	/** Describes an account's performance for the period */
	public static class AccountSummary {
		public String name;

		public BigDecimal startCash = BigDecimal.ZERO;
		public BigDecimal startInv = BigDecimal.ZERO;
		public BigDecimal endCash = BigDecimal.ZERO;
		public BigDecimal endInv = BigDecimal.ZERO;

		public List<SecuritySummary> securities = new ArrayList<SecuritySummary>();

		public BigDecimal diffCash() {
			return this.endCash.subtract(this.startCash);
		}

		public BigDecimal diffInv() {
			return this.endInv.subtract(this.startInv);
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();

			String begin = String.format("%10s / %10s", //
					Common.formatAmount(this.startInv), //
					Common.formatAmount(this.startCash));
			String end = String.format("%10s / %10s", //
					Common.formatAmount(this.endInv), //
					Common.formatAmount(this.endCash));
			String diff = String.format("%10s / %10s", //
					Common.formatAmount(this.diffInv()), //
					Common.formatAmount(this.diffCash()));

			sb.append(String.format("%-30s: %34s | %34s | %34s", //
					this.name, begin, end, diff));

			return sb.toString();
		}
	}

	/**
	 * Describes a security's performance for the period (global or account)
	 */
	public static class SecuritySummary {
		public final String name;

		public final SecurityPosition.SecurityPerformance perf;

		public BigDecimal contributions = BigDecimal.ZERO;
		public BigDecimal matchingContributions = BigDecimal.ZERO;
		public BigDecimal dividendValue = BigDecimal.ZERO;
		public BigDecimal dividendShares = BigDecimal.ZERO;

		// Calculated as (value-costBasis) for holdings at end
		public BigDecimal gainLoss = BigDecimal.ZERO;

		public SecuritySummary(SecurityPosition pos, QDate start, QDate end) {
			this.name = pos.security.getName();
			this.perf = new SecurityPosition.SecurityPerformance(pos, start, end);
		}

		public BigDecimal diffShares() {
			return this.perf.endShares.subtract(this.perf.startShares);
		}

		public BigDecimal diffPrice() {
			return this.perf.getEndPrice().subtract(this.perf.getStartPrice());
		}

		public BigDecimal diffValue() {
			return this.perf.getEndValue().subtract(this.perf.getStartValue());
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();

			String start = String.format("%10s @%8s %10s", //
					Common.formatAmount3(this.perf.startShares).trim(), //
					Common.formatAmount3(this.perf.getStartPrice()).trim(), //
					Common.formatAmount(this.perf.getStartValue()).trim());

			String end = String.format("%10s @%8s %10s", //
					Common.formatAmount3(this.perf.endShares).trim(), //
					Common.formatAmount3(this.perf.getEndPrice()).trim(), //
					Common.formatAmount(this.perf.getEndValue()).trim());

			String diff = String.format("%10s @%8s %10s", //
					Common.formatAmount3(this.diffShares()).trim(), //
					Common.formatAmount3(this.diffPrice()).trim(), //
					Common.formatAmount(this.diffValue()).trim());

			String nn = (this.name.length() > 29) ? this.name.substring(0, 29) : this.name;
			sb.append(String.format("%-30s: %34s | %34s | %34s", nn, start, end, diff));

			return sb.toString();
		}
	}

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

	public StatusForDateModel startStatusModel;
	public StatusForDateModel endStatusModel;

	public final Summary summary;
	public final Section[] sections;
	public AccountSummary[] accounts;
	public SecuritySummary[] securities;

	public List<GenericTxn> txns;

	// TODO create appropriate summary info variables
	// Is this an accumulation of account info? or security info?
	// i.e. does it include cash positions in investment accounts?
	public BigDecimal assets = BigDecimal.ZERO;
	public BigDecimal liabilities = BigDecimal.ZERO;
	public BigDecimal netWorth = BigDecimal.ZERO;

	public InvestmentPerformanceModel(QDate startDate, QDate endDate) {
		this.summary = new Summary();
		this.sections = Section.getSections();

		this.startStatusModel = new StatusForDateModel(startDate);
		this.endStatusModel = new StatusForDateModel(endDate);

		this.txns = MoneyMgrModel.currModel.getInvestmentTransactions(startDate, endDate);

		build();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("\n");

		sb.append(String.format("Performance for period %s to %s", //
				this.startDate().toString(), //
				this.endDate().toString()));
		sb.append("\n");

		sb.append("\n");
		sb.append(this.summary.toString());
		sb.append("\n");

		sb.append("\n");
		sb.append("Accounts:");
		sb.append("\n");

		for (AccountSummary asum : this.accounts) {
			sb.append(asum.toString());
			sb.append("\n");
		}

		sb.append("\n");
		sb.append("Securities:");
		sb.append("\n");

		for (SecuritySummary ssum : this.securities) {
			if (Common.isEffectivelyZero(ssum.perf.startShares) //
					&& Common.isEffectivelyZero(ssum.perf.endShares)) {
				continue;
			}

			sb.append(ssum.toString());
			sb.append("\n");
		}

		sb.append("\n");

		for (SecuritySummary ssum : this.securities) {
			if (!Common.isEffectivelyZero(ssum.perf.startShares) //
					|| !Common.isEffectivelyZero(ssum.perf.endShares)) {
				continue;
			}

			sb.append(ssum.toString());
			sb.append("\n");
		}

		return sb.toString();
	}

	public Section getSectionForAccount(Account acct) {
		for (Section s : this.sections) {
			if (s.info.contains(acct.type)) {
				return s;
			}
		}

		return null;
	}

	private QDate startDate() {
		return this.startStatusModel.date;
	}

	private QDate endDate() {
		return this.endStatusModel.date;
	}

	public void build() {
		StatusForDateModel.Section sect = this.startStatusModel.getSectionForAccount(this.txns.get(0).getAccount());
		BigDecimal startbal = sect.subtotal;
		sect = this.endStatusModel.getSectionForAccount(this.txns.get(0).getAccount());
		BigDecimal endbal = sect.subtotal;

		BigDecimal netchange = endbal.subtract(startbal);

		for (GenericTxn gt : this.txns) {
			InvestmentTxn txn = (InvestmentTxn) gt;

		}

		List<AccountSummary> asums = new ArrayList<AccountSummary>();

		for (Account a : MoneyMgrModel.currModel.getAccounts()) {
			if (!a.isInvestmentAccount() //
					|| (a.getOpenDate().compareTo(endDate()) >= 0) //
					|| (a.closeDate != null && a.closeDate.compareTo(startDate()) <= 0)) {
				continue;
			}

			BigDecimal startAmt = a.getValueForDate(startDate());
			BigDecimal endAmt = a.getValueForDate(endDate());
			BigDecimal startInv = a.securities.getPortfolioValueForDate(startDate());
			BigDecimal endInv = a.securities.getPortfolioValueForDate(endDate());
			BigDecimal startCash = startAmt.subtract(startInv);
			BigDecimal endCash = endAmt.subtract(endInv);

			this.summary.startCash = this.summary.startCash.add(startCash);
			this.summary.startInv = this.summary.startInv.add(startInv);
			this.summary.endCash = this.summary.endCash.add(endCash);
			this.summary.endInv = this.summary.endInv.add(endInv);
//			this.summary.match
//			this.summary.div
//			this.summary.contrib

			// TODO check if account is active in the specified period
//			if (!a.isOpenOn(d) //
//					&& Common.isEffectivelyZero(startAmt) //
//					&& (a.getFirstUnclearedTransaction() == null) //
//					&& a.securities.isEmptyForDate(d)) {
//				continue;
//			}

			Section modelsect = getSectionForAccount(a);
			StatusForDateModel.Section startSect = startStatusModel.getSectionForAccount(a);
			StatusForDateModel.Section endSect = endStatusModel.getSectionForAccount(a);

			AccountSummary asummary = new AccountSummary();
			modelsect.accounts.add(asummary);
			asums.add(asummary);
			modelsect.subtotal = modelsect.subtotal.add(startAmt);

			asummary.name = a.getDisplayName(36);
			asummary.startInv = startInv;
			asummary.startCash = startCash;
			asummary.endInv = endInv;
			asummary.endCash = endCash;

			asummary.securities = null;

//			List<StockOption> opts = StockOption.getOpenOptions(a, d);
//			if (!opts.isEmpty()) {
//				StatusForDateModel.SecuritySummary ssummary = new StatusForDateModel.SecuritySummary();
//
//				StockOption opt = opts.get(0);
//				Security sec = Security.getSecurity(opt.secid);
//
//				ssummary.name = "Options:" + sec.getName();
//				ssummary.shares = opt.getAvailableShares(true);
//				ssummary.price = sec.getPriceForDate(d).getPrice();
//				ssummary.value = opt.getValueForDate(d);
//
//				asummary.securities.add(ssummary);
//			}

//			if (!a.securities.isEmptyForDate(d)) {
//				BigDecimal portValue = a.getSecuritiesValueForDate(d);
//
//				if (!Common.isEffectivelyZero(portValue)) {
//					asummary.cashBalance = startAmt.subtract(portValue);
//
//					for (SecurityPosition pos : a.securities.positions) {
//						BigDecimal posval = pos.getValueForDate(d);
//
//						if (!Common.isEffectivelyZero(posval)) {
//							StatusForDateModel.SecuritySummary ssummary = new StatusForDateModel.SecuritySummary();
//							asummary.securities.add(ssummary);
//
//							String nn = pos.security.getName();
//							if (nn.length() > 34) {
//								nn = nn.substring(0, 31) + "...";
//							}
//
//							ssummary.name = nn;
//							ssummary.value = posval;
//							ssummary.price = pos.security.getPriceForDate(d).getPrice();
//
//							int idx = pos.getTransactionIndexForDate(d);
//							if (idx >= 0) {
//								ssummary.shares = pos.shrBalance.get(idx);
//							}
//						}
//					}
//				}
//			}

			modelsect.subtotal.add(startAmt);

			this.netWorth = this.netWorth.add(startAmt);
		}

		Map<Security, SecuritySummary> ssums = new HashMap<Security, SecuritySummary>();

		for (SecurityPosition pos : SecurityPortfolio.portfolio.positions) {
			SecuritySummary sum = ssums.get(pos.security);
			if (sum == null) {
				sum = new SecuritySummary(pos, startDate(), endDate());
				ssums.put(pos.security, sum);
			}
		}

		this.accounts = asums.toArray(new AccountSummary[0]);
		this.securities = ssums.values().toArray(new SecuritySummary[0]);
	}
}