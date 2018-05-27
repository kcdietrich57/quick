package qif.report;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import qif.data.Account;
import qif.data.Common;
import qif.data.QifDom;
import qif.data.Statement;
import qif.report.StatusReporter.ReportStatusModel.AccountInfo;

public class StatusReporter {
	public static void reportStatus() {
		ReportStatusModel model = buildReportStatusModel();
		String s = generateReportStatus(model);
		System.out.println(s);
	}

	public static void reportStatus_old() {
		QifDom dom = QifDom.dom;

		final List<Account> ranking = new ArrayList<Account>();

		for (int acctid = 1; acctid <= dom.getNumAccounts(); ++acctid) {
			Account a = dom.getAccount(acctid);

			if (a != null) {
				ranking.add(a);
			}
		}

		final Comparator<Account> cmp = (o1, o2) -> {
			if (o1.statements.isEmpty()) {
				return (o2.statements.isEmpty()) ? 0 : -1;
			}
			return (o2.statements.isEmpty()) //
					? 1 //
					: o1.getLastStatementDate().compareTo(o2.getLastStatementDate());
		};

		Collections.sort(ranking, cmp);

		System.out.println();
		System.out.println("Overall");
		System.out.println();

		int unclracct_count = 0;
		int unclracct_utx_count = 0;
		int unclracct_tx_count = 0;

		int clracct_count = 0;
		int clracct_tx_count = 0;

		final Calendar cal = Calendar.getInstance();
		final Date today = cal.getTime();

		final long dayms = 1000L * 24 * 60 * 60;
		final long msCurrent = today.getTime();
		final Date minus30 = new Date(msCurrent - 30 * dayms);
		final Date minus60 = new Date(msCurrent - 60 * dayms);
		final Date minus90 = new Date(msCurrent - 90 * dayms);

		boolean nostat = false;
		boolean stat90 = false;
		boolean stat60 = false;
		boolean stat30 = false;
		boolean statcurrent = false;

		System.out.println(String.format("%3s   %-35s   %-8s  %-10s   %-5s %-5S      %-8s", //
				"N", "Account", "LastStmt", "Balance", "UncTx", "TotTx", "FirstUnc"));

		final int max = ranking.size();
		for (int ii = 0; ii < max; ++ii) {
			final Account a = ranking.get(ii);
			final Date laststatement = a.getLastStatementDate();

			if (laststatement == null) {
				if (!nostat) {
					System.out.println("### No statements");
					nostat = true;
				}
			} else if (laststatement.compareTo(minus90) < 0) {
				if (!stat90) {
					System.out.println("\n### More than 90 days");
					stat90 = true;
				}
			} else if (laststatement.compareTo(minus60) < 0) {
				if (!stat60) {
					System.out.println("\n### 60-90 days");
					stat60 = true;
				}
			} else if (laststatement.compareTo(minus30) < 0) {
				if (!stat30) {
					System.out.println("\n### 30-60 days");
					stat30 = true;
				}
			} else {
				if (!statcurrent) {
					System.out.println("\n### Less than 30 days");
					statcurrent = true;
				}
			}

			final int ucount = a.getUnclearedTransactionCount();
			final int tcount = a.transactions.size();

			if ((ucount > 0) || !a.isClosedAsOf(null)) {
				if (a.isClosedAsOf(null)) {
					System.out.println("Warning! Account " + a.getName() + " is closed!");
				}

				++unclracct_count;
				unclracct_utx_count += ucount;
				unclracct_tx_count += tcount;

				final String nam = a.getDisplayName(25);
				final Statement lStat = a.getLastStatement();
				final Date lStatDate = (lStat != null) ? lStat.date : null;

				System.out.println(String.format("%3d   %-35s : %8s  %s : %5d/%5d :    %8s", //
						unclracct_count, //
						nam, //
						Common.formatDate(lStatDate), //
						Common.formatAmount(a.balance), //
						ucount, //
						tcount, //
						Common.formatDate(a.getFirstUnclearedTransactionDate())));
			} else {
				++clracct_count;
				clracct_tx_count += tcount;
			}
		}

		System.out.println();
		System.out.println(String.format("   %5d / %5d uncleared tx in %4d open accounts", //
				unclracct_utx_count, unclracct_tx_count, unclracct_count));
		System.out.println(String.format("        %5d      cleared tx in %4d closed accounts", //
				clracct_tx_count, clracct_count));

		System.out.println();
	}

	public static class ReportStatusModel {
		public static class AccountInfo {
			public boolean isClosed;
			public Date lastStatementDate;
			public BigDecimal balance;
			public int ucount;
			public int tcount;
			public String name;
			public Statement lStat;
			public Date lStatDate;
			public Date firstUnclearedTxDate;

			public AccountInfo(Account a) {
				isClosed = !a.isOpenOn(null);
				lastStatementDate = a.getLastStatementDate();

				ucount = a.getUnclearedTransactionCount();
				tcount = a.transactions.size();

				this.name = a.getDisplayName(25);
				this.balance = a.balance;
				this.lStat = a.getLastStatement();
				this.lStatDate = (lStat != null) ? lStat.date : null;
				this.firstUnclearedTxDate = a.getFirstUnclearedTransactionDate();
			}
		}

		public Date today;
		private Date minus30;
		private Date minus60;
		private Date minus90;

		public int unclracct_count = 0;
		public int unclracct_utx_count = 0;
		public int unclracct_tx_count = 0;

		public int clracct_count = 0;
		public int clracct_tx_count = 0;

		public List<AccountInfo> accountsWithNoStatements = new ArrayList<AccountInfo>();
		public List<AccountInfo> accounts90 = new ArrayList<AccountInfo>();
		public List<AccountInfo> accounts60 = new ArrayList<AccountInfo>();
		public List<AccountInfo> accounts30 = new ArrayList<AccountInfo>();
		public List<AccountInfo> accounts0 = new ArrayList<AccountInfo>();

		public ReportStatusModel() {
			Calendar cal = Calendar.getInstance();
			today = cal.getTime();

			long dayms = 1000L * 24 * 60 * 60;
			long msCurrent = today.getTime();
			minus30 = new Date(msCurrent - 30 * dayms);
			minus60 = new Date(msCurrent - 60 * dayms);
			minus90 = new Date(msCurrent - 90 * dayms);
		}

		public void add(AccountInfo ainfo) {
			if (!ainfo.isClosed) {
				if (ainfo.lastStatementDate == null) {
					this.accountsWithNoStatements.add(ainfo);
				} else if (ainfo.lastStatementDate.compareTo(minus90) <= 0) {
					this.accounts90.add(ainfo);
				} else if (ainfo.lastStatementDate.compareTo(minus60) <= 0) {
					this.accounts60.add(ainfo);
				} else if (ainfo.lastStatementDate.compareTo(minus30) <= 0) {
					this.accounts30.add(ainfo);
				} else {
					this.accounts0.add(ainfo);
				}
			}

			if ((ainfo.ucount > 0))// || !a.isClosedAsOf(null))
			{
				// if (a.isClosedAsOf(null)) {
				// System.out.println("Warning! Account " + a.getName() + " is closed!");
				// }

				++this.unclracct_count;
				this.unclracct_utx_count += ainfo.ucount;
				this.unclracct_tx_count += ainfo.tcount;
			} else {
				++this.clracct_count;
				this.clracct_tx_count += ainfo.tcount;
			}
		}
	}

	public static ReportStatusModel buildReportStatusModel() {
		QifDom dom = QifDom.dom;

		ReportStatusModel model = new ReportStatusModel();

		final List<Account> ranking = new ArrayList<Account>();

		for (int acctid = 1; acctid <= dom.getNumAccounts(); ++acctid) {
			Account a = dom.getAccount(acctid);

			if (a != null) {
				ranking.add(a);
			}
		}

		final Comparator<Account> cmp = (o1, o2) -> {
			if (o1.statements.isEmpty()) {
				return (o2.statements.isEmpty()) ? 0 : -1;
			}
			return (o2.statements.isEmpty()) //
					? 1 //
					: o1.getLastStatementDate().compareTo(o2.getLastStatementDate());
		};

		Collections.sort(ranking, cmp);

		final int max = ranking.size();
		for (int ii = 0; ii < max; ++ii) {
			Account a = ranking.get(ii);

			AccountInfo ainfo = new AccountInfo(a);
			model.add(ainfo);
		}

		return model;
	}

	private static String formatAccountInfo(int anum, AccountInfo ainfo) {
		return String.format("%3d   %-35s : %8s  %s : %5d/%5d :    %8s", //
				anum, //
				ainfo.name, //
				Common.formatDate(ainfo.lStatDate), //
				Common.formatAmount(ainfo.balance), //
				ainfo.ucount, //
				ainfo.tcount, //
				Common.formatDate(ainfo.firstUnclearedTxDate));
	}

	private static int appendAccountSection(StringBuilder sb, //
			int anum, String header, List<AccountInfo> accts) {
		sb.append("\n");
		sb.append(header);
		sb.append("\n");

		for (AccountInfo ainfo : accts) {
			sb.append(formatAccountInfo(anum++, ainfo));
			sb.append("\n");
		}

		return anum;
	}

	public static String generateReportStatus(ReportStatusModel model) {
		StringBuilder sb = new StringBuilder();

		sb.append("\n");

		int anum = 1;

		sb.append(String.format("%3s   %-35s   %-8s  %-10s   %-5s %-5S      %-8s\n", //
				"N", "Account", "LastStmt", "Balance", "UncTx", "TotTx", "FirstUnc"));

		anum = appendAccountSection(sb, anum, "### No statements", model.accountsWithNoStatements);
		anum = appendAccountSection(sb, anum, "### More than 90 days", model.accounts90);
		anum = appendAccountSection(sb, anum, "### 60-90 days", model.accounts60);
		anum = appendAccountSection(sb, anum, "### 30-60 days", model.accounts30);
		anum = appendAccountSection(sb, anum, "### Less than 30 days", model.accounts0);

		sb.append("\n");
		sb.append(String.format("   %5d / %5d uncleared tx in %4d open accounts\n", //
				model.unclracct_utx_count, model.unclracct_tx_count, model.unclracct_count));
		sb.append(String.format("        %5d      cleared tx in %4d closed accounts\n", //
				model.clracct_tx_count, model.clracct_count));

		sb.append("\n");

		return sb.toString();
	}
}