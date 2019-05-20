package moneymgr.report;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import moneymgr.model.Account;
import moneymgr.model.GenericTxn;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/** This captures a point in time snapshot of account values */
public class CashFlowModel {

	public final QDate date;
	public final List<AcctInfo> acctinfoMonth;
	public final AcctInfo summaryMonth;
	public final List<AcctInfo> acctinfoYear;
	public final AcctInfo summaryYear;

	static class AcctInfo {
		public Account account;
		public String acctname;
		public List<GenericTxn> txns;
		public int numtxns;

		public QDate startDate;
		public QDate endDate;
		public BigDecimal startBal;
		public BigDecimal endBal;

		public BigDecimal startCashBal;
		public BigDecimal endCashBal;
		public BigDecimal startInvBal;
		public BigDecimal endInvBal;

		public BigDecimal calcBal;
		public boolean balMatch;

		public BigDecimal income = BigDecimal.ZERO;
		public BigDecimal expenses = BigDecimal.ZERO;
		public BigDecimal xferIn = BigDecimal.ZERO;
		public BigDecimal xferOut = BigDecimal.ZERO;
		public BigDecimal gains = BigDecimal.ZERO;

		public AcctInfo() {
			this.numtxns = 0;

			this.startBal = BigDecimal.ZERO;
			this.endBal = BigDecimal.ZERO;

			this.startInvBal = BigDecimal.ZERO;
			this.endInvBal = BigDecimal.ZERO;

			this.startCashBal = BigDecimal.ZERO;
			this.endCashBal = BigDecimal.ZERO;

			this.income = BigDecimal.ZERO;
			this.expenses = BigDecimal.ZERO;
			this.xferIn = BigDecimal.ZERO;
			this.xferOut = BigDecimal.ZERO;
			this.gains = BigDecimal.ZERO;
		}

		public void addInfo(AcctInfo other) {
			this.numtxns += other.numtxns;

			this.startBal = this.startBal.add(other.startBal);
			this.endBal = this.endBal.add(other.endBal);

			this.startInvBal = this.startInvBal.add(other.startInvBal);
			this.endInvBal = this.endInvBal.add(other.endInvBal);

			this.startCashBal = this.startCashBal.add(other.startCashBal);
			this.endCashBal = this.endCashBal.add(other.endCashBal);

			this.income = this.income.add(other.income);
			this.expenses = this.expenses.add(other.expenses);
			this.xferIn = this.xferIn.add(other.xferIn);
			this.xferOut = this.xferOut.add(other.xferOut);
			this.gains = this.gains.add(other.gains);
		}

		public BigDecimal getCalcBalance() {
			if (this.calcBal == null) {
				this.calcBal = this.startCashBal.add(this.income).add(this.expenses) //
						.add(this.xferIn).add(this.xferOut) //
						.add(this.endInvBal);
				this.balMatch = Common.isEffectivelyEqual(this.calcBal, this.endBal);
				this.balMatch = (this.calcBal.subtract(this.endBal).abs().compareTo(new BigDecimal("0.02")) < 0);
			}

			return this.calcBal;
		}

		public boolean balanceMatches() {
			if (this.calcBal == null) {
				getCalcBalance();
			}

			return this.balMatch;
		}

		public String toString() {
			String sb = "";

			sb += this.acctname //
					+ " " + this.startDate + " to " + this.endDate //
					+ " : " + this.numtxns + " transactions\n";

			sb += String.format("  Open:    %12s", Common.formatAmount(this.startBal).trim());
			sb += String.format("  Close:   %12s", Common.formatAmount(this.endBal).trim());

			BigDecimal bal = getCalcBalance();
			sb += String.format("  InvBal: %12s-%12s  CalcBal: %12s  [%12s]\n", //
					Common.formatAmount(this.startInvBal).trim(), //
					Common.formatAmount(this.endInvBal).trim(), //
					Common.formatAmount(bal).trim(), //
					Common.formatAmount(bal.subtract(this.endBal)).trim());
			sb += "\n";

			sb += String.format("  Income:  %12s", Common.formatAmount(this.income).trim());
			sb += String.format("  Expense: %12s\n", Common.formatAmount(this.expenses).trim());

			sb += String.format("  XferIn:  %12s", Common.formatAmount(this.xferIn).trim());
			sb += String.format("  XferOut: %12s\n", Common.formatAmount(this.xferOut).trim());

			sb += String.format("  GainLoss: %12s\n", Common.formatAmount(this.gains).trim());

			if ((this.txns != null) && !balanceMatches()) {
				for (GenericTxn txn : this.txns) {
					BigDecimal cash = txn.getCashAmount();
					BigDecimal xfer = txn.getXferAmount();
					BigDecimal gain = txn.getGain();
					if (xfer.signum() != 0) {
						cash = cash.subtract(xfer);
					}

					sb += txn.toString() + "\n";

					if (cash.signum() != 0) {
						sb += "  Cash: " + cash.toString();
					}
					if (xfer.signum() != 0) {
						sb += "  Xfer: " + xfer.toString();
					}
					if (gain.signum() != 0) {
						sb += "  Gain: " + gain.toString();
					}
					sb += "\n";
				}
			}

			return sb;
		}
	}

	public static List<CashFlowModel> goodModels = null;
	public static List<CashFlowModel> failModels = null;

	public List<CashFlowModel> checkCashFlow() {
		if (failModels == null) {
			goodModels = new ArrayList<CashFlowModel>();
			failModels = new ArrayList<CashFlowModel>();

			QDate dt = GenericTxn.getFirstTransactionDate();
			while (dt.compareTo(GenericTxn.getLastTransactionDate()) < 0) {
				CashFlowModel model = new CashFlowModel(dt);
				if (!model.balanceMatches()) {
					failModels.add(model);
					new CashFlowModel(dt);
				} else {
					goodModels.add(model);
				}

				dt = dt.getLastDayOfMonth().addDays(1);
			}
		}

		return goodModels;
	}

	public CashFlowModel(QDate date) {
		this.acctinfoMonth = new ArrayList<>();
		this.summaryMonth = new AcctInfo();
		this.summaryMonth.acctname = "Month Summary";
		this.acctinfoYear = new ArrayList<>();
		this.summaryYear = new AcctInfo();
		this.summaryYear.acctname = "12 Month Summary";

		if (failModels == null) {
			checkCashFlow();

			List<CashFlowModel> fails = failModels;
			List<CashFlowModel> succs = goodModels;
		}

		this.date = date.getLastDayOfMonth();

		QDate startMonth = this.date.getFirstDayOfMonth();
		build(startMonth, this.date, this.acctinfoMonth, this.summaryMonth);

		QDate startYear = this.date.addDays(-363).getFirstDayOfMonth();
		build(startYear, this.date, this.acctinfoYear, this.summaryYear);
	}

	public AcctInfo getSummary() {
		return this.summaryMonth;
	}

	public boolean balanceMatches() {
		for (AcctInfo info : this.acctinfoMonth) {
			if (!info.balanceMatches()) {
				return false;
			}
		}

		for (AcctInfo info : this.acctinfoYear) {
			if (!info.balanceMatches()) {
				return false;
			}
		}

		return true;
	}

	/** Construct model */
	private static void build(QDate start, QDate end, List<AcctInfo> accts, AcctInfo summary) {
		if (start.compareTo(GenericTxn.getFirstTransactionDate()) < 0) {
			start = GenericTxn.getFirstTransactionDate();
		}

		summary.startDate = start;
		summary.endDate = end;

		for (Account acct : Account.getAccounts()) {
			if (!acct.isOpenDuring(start, end)) {
				continue;
			}

			List<GenericTxn> txns = acct.getTransactions(start, end);
			if (txns.isEmpty()) {
				continue;
			}

			AcctInfo ainfo = new AcctInfo();
			ainfo.account = acct;
			ainfo.acctname = acct.name;
			ainfo.txns = txns;
			ainfo.numtxns = txns.size();
			ainfo.startDate = start;
			ainfo.endDate = end;

			ainfo.startBal = acct.getValueForDate(start.addDays(-1));
			ainfo.endBal = acct.getValueForDate(end);

			ainfo.startInvBal = acct.getSecuritiesValueForDate(start.addDays(-1));
			ainfo.endInvBal = acct.getSecuritiesValueForDate(end);

			ainfo.startCashBal = ainfo.startBal.subtract(ainfo.startInvBal);
			ainfo.endCashBal = ainfo.endBal.subtract(ainfo.endInvBal);

			ainfo.income = BigDecimal.ZERO;
			ainfo.expenses = BigDecimal.ZERO;
			ainfo.xferIn = BigDecimal.ZERO;
			ainfo.xferOut = BigDecimal.ZERO;
			ainfo.gains = ainfo.endInvBal.subtract(ainfo.startInvBal);

			for (GenericTxn txn : txns) {
				BigDecimal cash = txn.getCashAmount();
				BigDecimal xfer = txn.getXferAmount();
				BigDecimal gain = txn.getGain();
				if (xfer.signum() != 0) {
					cash = cash.subtract(xfer);
				}

				if (gain.signum() != 0) {
					ainfo.gains = ainfo.gains.add(gain);
				}

				if (xfer.signum() > 0) {
					ainfo.xferIn = ainfo.xferIn.add(xfer);
				} else if (xfer.signum() < 0) {
					ainfo.xferOut = ainfo.xferOut.add(xfer);
				}

				if (cash.signum() > 0) {
					ainfo.income = ainfo.income.add(cash);
				} else if (cash.signum() < 0) {
					ainfo.expenses = ainfo.expenses.add(cash);
				}
			}

			summary.addInfo(ainfo);

			for (int idx = 0; idx < accts.size(); ++idx) {
				AcctInfo info = accts.get(idx);
				if (info.endBal.compareTo(ainfo.endBal) < 0) {
					accts.add(idx, ainfo);
					ainfo = null;
					break;
				}
			}

			if (ainfo != null) {
				accts.add(ainfo);
			}
		}
	}

	public String toString() {
		return this.summaryMonth.toString() //
				+ "\n" + this.summaryYear.toString();
	}
}