package moneymgr.report;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import moneymgr.model.Account;
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.Statement;
import moneymgr.report.ReconcileStatusReporter.ReconcileStatusModel.AccountInfo;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/** Generate report on reconciliation status for all accounts */
public class ReconcileStatusReporter {
	public final MoneyMgrModel model;

	public ReconcileStatusReporter(MoneyMgrModel model) {
		this.model = model;
	}

	/** Comparator for ranking accounts by most recent statement date */
	private static final Comparator<Account> compareLastBalancedStatementDate = (o1, o2) -> {
		if ((o1 == null) || (o2 == null)) {
			return (o1 == null) ? ((o2 == null) ? 0 : 1) : -1;
		}

		if (o1.getNumStatements() == 0) {
			return (o2.getNumStatements() == 0) ? 0 : -1;
		}

		QDate d1 = o1.getLastBalancedStatementDate();
		QDate d2 = o2.getLastBalancedStatementDate();

		if ((d1 == null) || (d2 == null)) {
			return (d1 == null) ? ((d2 == null) ? 0 : 1) : -1;
		}

		// TODO I don't see how o2 could have no stmts here
		return (o2.getNumStatements() == 0) ? 1 : d1.compareTo(d2);
	};

	/** Print reconcile status report - obsolete QifLoader only */
	public void reportStatus() {
		ReconcileStatusModel model = buildReportStatusModel();
		String s = generateReportStatus(model);
		System.out.println(s);
	}

	/** Model the reconcile status for all accounts */
	public static class ReconcileStatusModel {
		/** Model the reconcile status for an account */
		public static class AccountInfo {
			public boolean isClosed;
			public QDate nextStatementDate;
			public QDate lastStatementDate;
			public BigDecimal balance;
			public int ucount;
			public int tcount;
			public String name;
			public Statement lStat;
			private QDate lStatDate;
			public QDate firstUnclearedTxDate;

			public AccountInfo(Account a) {
				this.isClosed = !a.isOpenOn(null);

				this.lastStatementDate = a.getLastBalancedStatementDate();
				this.nextStatementDate = a.getNextStatementDate();

				this.ucount = a.getUnclearedTransactionCount();
				this.tcount = a.getNumTransactions();

				this.name = a.getDisplayName(25);
				this.balance = a.getBalance();
				this.lStat = a.getLastStatement();
				this.lStatDate = (this.lStat != null) ? this.lStat.date : null;
				this.firstUnclearedTxDate = a.getFirstUnclearedTransactionDate();
			}
		}

		public int unclracct_count = 0;
		public int unclracct_utx_count = 0;
		public int unclracct_tx_count = 0;

		public int clracct_count = 0;
		public int clracct_tx_count = 0;

		public List<AccountInfo> accountsWithNoStatements = new ArrayList<>();
		public List<AccountInfo> accounts60 = new ArrayList<>();
		public List<AccountInfo> accounts30 = new ArrayList<>();
		public List<AccountInfo> accounts0 = new ArrayList<>();
		public List<AccountInfo> accounts_duesoon = new ArrayList<>();
		public List<AccountInfo> accounts_notdue = new ArrayList<>();

		/** Add account info to list sorted by next expected statement date */
		public void insertAccountInfo(List<AccountInfo> list, AccountInfo info) {
			if (list.isEmpty()) {
				list.add(info);
			} else {
				int idx = 0;
				while ((idx < list.size()) //
						&& list.get(idx).nextStatementDate.compareTo(info.nextStatementDate) < 0) {
					++idx;
				}

				list.add(idx, info);
			}
		}

		/** Add account into the appropriate list based on next statement date */
		public void add(AccountInfo ainfo) {
			if (!ainfo.isClosed) {
				if (ainfo.lastStatementDate == null) {
					this.accountsWithNoStatements.add(ainfo);
				} else {
					int diff = QDate.today().subtract(ainfo.nextStatementDate);

					if (diff >= 60) {
						insertAccountInfo(this.accounts60, ainfo);
					} else if (diff >= 30) {
						insertAccountInfo(this.accounts30, ainfo);
					} else if (diff >= 0) {
						insertAccountInfo(this.accounts0, ainfo);
					} else if (diff >= -7) {
						insertAccountInfo(this.accounts_duesoon, ainfo);
					} else {
						insertAccountInfo(this.accounts_notdue, ainfo);
					}
				}
			}

			if ((ainfo.ucount > 0)) {
				++this.unclracct_count;
				this.unclracct_utx_count += ainfo.ucount;
				this.unclracct_tx_count += ainfo.tcount;
			} else {
				++this.clracct_count;
				this.clracct_tx_count += ainfo.tcount;
			}
		}
	}

	/** Create a model with current reconciliation information */
	public ReconcileStatusModel buildReportStatusModel() {
		ReconcileStatusModel model = new ReconcileStatusModel();

		List<Account> accountsByLastStatement = new ArrayList<>(this.model.getAccounts());

		Collections.sort(accountsByLastStatement, compareLastBalancedStatementDate);

		for (Account a : accountsByLastStatement) {
			model.add(new AccountInfo(a));
		}

		return model;
	}

	/** Format one account's information */
	private String formatAccountInfo(int anum, AccountInfo ainfo) {
		return String.format("%3d   %-35s : %8s  %8s  %s : %5d/%5d :    %8s", //
				anum, //
				ainfo.name, //
				((ainfo.lastStatementDate != null) ? ainfo.lStatDate.toString() : "N/A"), //
				ainfo.nextStatementDate.toString(), //
				Common.formatAmount(ainfo.balance), //
				ainfo.ucount, //
				ainfo.tcount, //
				((ainfo.firstUnclearedTxDate != null) ? ainfo.firstUnclearedTxDate.toString() : "N/A"));
	}

	/** Output a section for accounts with a range of dates since the last stmt */
	private int appendAccountSection(StringBuilder sb, //
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

	/** Produce a text report with accounts organized by next statement date */
	public String generateReportStatus(ReconcileStatusModel model) {
		StringBuilder sb = new StringBuilder();

		sb.append("\n");

		int anum = 1;

		sb.append(String.format("%3s   %-35s   %-8s  %-8s  %-10s   %-5s %-5S      %-8s\n", //
				"N", "Account", "LastStmt", "NextStmt", "Balance", "UncTx", "TotTx", "FirstUnc"));

		anum = appendAccountSection(sb, anum, "### No statements", model.accountsWithNoStatements);
		anum = appendAccountSection(sb, anum, "### Overdue 60+ days", model.accounts60);
		anum = appendAccountSection(sb, anum, "### Overdue 30-60 days", model.accounts30);
		anum = appendAccountSection(sb, anum, "### Due <30 days", model.accounts0);
		anum = appendAccountSection(sb, anum, "### Due soon", model.accounts_duesoon);
		anum = appendAccountSection(sb, anum, "### Not due", model.accounts_notdue);

		sb.append("\n");
		sb.append(String.format("   %5d / %5d uncleared tx in %4d open accounts\n", //
				model.unclracct_utx_count, model.unclracct_tx_count, model.unclracct_count));
		sb.append(String.format("        %5d      cleared tx in %4d closed accounts\n", //
				model.clracct_tx_count, model.clracct_count));

		sb.append("\n");

		return sb.toString();
	}
}