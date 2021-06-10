package moneymgr.report;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import moneymgr.model.Account;
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.SimpleTxn;
import moneymgr.ui.MainFrame;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/**
 * EXPERIMENTAL: Collect cashflow statistics for an account over a period of
 * time.<br>
 * Cash in/out (total and itemized by account)
 */
class CashFlowNode {
	public final MoneyMgrModel model;
	public Account acct;
	public QDate start;
	public QDate end;

	public BigDecimal inflowTotal = BigDecimal.ZERO;
	public BigDecimal outflowTotal = BigDecimal.ZERO;

	public BigDecimal inxTotal = BigDecimal.ZERO;
	public BigDecimal outxTotal = BigDecimal.ZERO;
	public Map<Account, BigDecimal> inxTotalForAccount = new HashMap<>();
	public Map<Account, BigDecimal> outxTotalForAccount = new HashMap<>();

	int incount = 0;
	int outcount = 0;
	int inxcount = 0;
	int outxcount = 0;
	int neutralcount = 0;

	/** Construct an empty Node */
	public CashFlowNode(Account acct, QDate start, QDate end) {
		this.model = acct.model;
		this.acct = acct;
		this.start = start;
		this.end = end;

		gatherInfo();
	}

	/** Process all transactions for the time period */
	public void gatherInfo() {
		QDate startDate = this.start;
		QDate endDate = this.end;

		List<SimpleTxn> txns = this.acct.getTransactions(startDate, endDate);
		flatten(txns);

		for (SimpleTxn txn : txns) {
			addTransaction(txn);
		}
	}

	/** Process a transaction's cash effects */
	public void addTransaction(SimpleTxn txn) {
		BigDecimal cash = txn.getCashAmount();
		BigDecimal xfer = txn.getCashTransferAmount();

		List<SimpleTxn> transfers = txn.getCashTransfers();
		if (transfers.isEmpty()) {
			return;
		}

		// TODO handle multiple transfer splits
		int xacctid = -transfers.get(0).getCatid();

		Account xacct = this.model.getAccountByID(xacctid);

		// Save transfer info
		if (xfer.signum() > 0) {
			this.inxTotal = this.inxTotal.add(xfer);
			BigDecimal inxtot = this.inxTotalForAccount.get(xacct);
			inxtot = (inxtot == null) ? xfer : inxtot.add(xfer);
			this.inxTotalForAccount.put(xacct, inxtot);
			++this.inxcount;
		} else if (xfer.signum() < 0) {
			this.outxTotal = this.outxTotal.add(xfer);
			BigDecimal outxtot = this.outxTotalForAccount.get(xacct);
			outxtot = (outxtot == null) ? xfer : outxtot.add(xfer);
			this.outxTotalForAccount.put(xacct, outxtot);
			++this.outxcount;
		}

		// Remove transfer amount from total cash leaving in/outflow
		if (xfer.signum() != 0) {
			cash = cash.subtract(xfer);
		}

		// Save inflow/outflow info
		if (cash.signum() > 0) {
			this.inflowTotal = this.inflowTotal.add(cash);
			++this.incount;
		} else if (cash.signum() < 0) {
			this.outflowTotal = this.outflowTotal.add(cash);
			++this.outcount;
		}

		if (((xfer.signum() == 0) && (cash.signum() == 0)) //
				|| xfer.equals(cash)) {
			++this.neutralcount;
		}
	}

	/** Return the net cash change over the time period */
	public BigDecimal getNetAmount() {
		return this.inflowTotal.add(this.outflowTotal).add(this.inxTotal).add(this.outxTotal);
	}

	/** Format string containing transaction count and value */
	private String formatAmount(int count, BigDecimal value) {
		String countstr = (count > 0) ? Integer.toString(count) : "";
		String valuestr = (value.signum() != 0) ? Common.formatAmount(value).trim() : "";
		return String.format("%3s %10s", countstr, valuestr);
	}

	/** Format string with summary of cashflow statistics */
	public String summaryString() {
		if (this.inflowTotal.signum() == 0 //
				&& this.outflowTotal.signum() == 0 //
				&& this.inxTotal.signum() == 0 //
				&& this.outxTotal.signum() == 0) {
			return "";
		}

		String fmt = "  %8s to %8s  %14s  %14s  %14s  %14s  %3s  %12s";

		String ret = String.format(fmt, //
				this.start.toString(), this.end.toString(), //
				formatAmount(this.incount, this.inflowTotal), //
				formatAmount(this.outcount, this.outflowTotal), //
				formatAmount(this.inxcount, this.inxTotal), //
				formatAmount(this.outxcount, this.outxTotal), //
				this.neutralcount, //
				Common.formatAmount(getNetAmount()).trim());

		return ret;
	}

	/** Construct a string listing total transfers in/out by account */
	public String transfersString() {
		String ret = "";
		boolean first = true;

		for (Account xacct : this.model.getAccounts()) {
			BigDecimal inx = this.inxTotalForAccount.get(xacct);
			BigDecimal outx = this.outxTotalForAccount.get(xacct);

			if (inx != null || outx != null) {
				if (!first) {
					ret += "\n";
				} else {
					first = false;
				}

				String inxstr = (inx != null && inx.signum() != 0) //
						? Common.formatAmount(inx).trim()
						: "";
				String outxstr = (outx != null && outx.signum() != 0) //
						? Common.formatAmount(outx).trim()
						: "";

				ret += String.format("       XFER %30s: IN %10s  OUT %10s", //
						xacct.name, inxstr, outxstr);
			}
		}

		return ret;
	}

	public String toString() {
		String summary = summaryString();
		String transfers = transfersString();

		return (!summary.isEmpty() || !transfers.isEmpty()) //
				? (summary + transfers) //
				: "";
	}

	/** For a list of transactions, replace any split txns with their splits */
	private static void flatten(List<SimpleTxn> txns) {
		for (int ii = 0; ii < txns.size(); ++ii) {
			SimpleTxn stx = txns.get(ii);

			if (stx.hasSplits()) {
				txns.remove(ii);
				txns.addAll(ii, stx.getSplits());
			}
		}
	}
}

/**
 * EXPERIMENTAL - Analyze cash flow for a period of time.<br>
 * This builds a graph following cash as it enters/exits the system and flows
 * between accounts.
 */
public class CashFlow {
	public final MoneyMgrModel model;
	public QDate start;
	public QDate end;

	/** Lists places where cash enters the system (e.g. income) */
	Map<Account, List<CashFlowNode>> terminalInputs = new HashMap<>();
	/** Lists places where cash leaves the system (e.g. purchases) */
	Map<Account, List<CashFlowNode>> terminalOutputs = new HashMap<>();
	/** Lists places where cash is transferred between accounts */
	Map<Account, List<CashFlowNode>> intermediaries = new HashMap<>();

	public CashFlow(MoneyMgrModel model) {
		this.model = model;
	}

	private void addNode(CashFlowNode node, Map<Account, List<CashFlowNode>> nodes) {
		List<CashFlowNode> list = terminalInputs.get(node.acct);
		if (list == null) {
			list = new ArrayList<>();
		}

		list.add(node);
	}

	public void addNode(CashFlowNode node) {
//			if (node.isInput()) {
//				addNode(node, this.terminalInputs);
//			}
//			if (node.isOutput()) {
//				addNode(node, this.terminalOutputs);
//			}
//			if (!(node.isInput() || node.isOutput())) {
//				addNode(node, this.intermediaries);
//			}
	}

	public void buildGraph() {

	}

	private static void buildCashFlowNode( //
			Map<Account, List<CashFlowNode>> nodes, //
			Account acct, QDate start) {
		List<CashFlowNode> anodes = nodes.get(acct);
		if (anodes == null) {
			anodes = new ArrayList<>();
			nodes.put(acct, anodes);
		}

		QDate totStart = start.getFirstDayOfMonth();
		QDate totEnd = totStart;

		boolean showresults = false;

		// Get Monthly stats for last year
		for (int ii = 0; ii < 12; ++ii) {
			start = start.getFirstDayOfMonth();
			QDate end = start.getLastDayOfMonth();
			totEnd = end;

			CashFlowNode node = new CashFlowNode(acct, start, end);

			String ss = node.toString();
			if (!ss.isEmpty()) {
				anodes.add(node);
				showresults = true;
			}

			start = end.addDays(1);
		}

		// Get stats for last year
		if (showresults) {
			anodes.add(new CashFlowNode(acct, totStart, totEnd));
		}
	}

	/** Report cash flow for last 12 months */
	public void reportCashFlowForTrailingYear() {
		QDate d = QDate.today();
		QDate start = d.getFirstDayOfMonth().addMonths(-12).getFirstDayOfMonth();

		Map<Account, List<CashFlowNode>> nodes = new HashMap<>();

		for (Account acct : MainFrame.appFrame.model.getAccounts()) {
			if (acct.isOpenDuring(start, QDate.today())) {
				buildCashFlowNode(nodes, acct, start);
			}
		}

		for (Account acct : this.model.getAccounts()) {
			List<CashFlowNode> anodes = nodes.get(acct);
			if (anodes == null) {
				continue;
			}

			String s = "";

			s += "\nCash flow for " + acct.name + "\n";

			String hdr = "  %-20s  %-14s  %-14s  %-14s  %-14s  %-3s  %-12s\n";

			s += String.format(hdr, //
					"Date Range", "Inflows", "Outflows", //
					"Transfer In", "Transfer Out", "NEU", "Net Cash Flow");
			s += String.format(hdr, //
					"====================", "=== ==========", "=== =========", //
					"=== ==========", "=== ==========", "===", "============");
			s += "\n";

			System.out.println(s);

			for (CashFlowNode node : anodes) {
				System.out.println(node.summaryString());
			}
		}

		System.out.println();

		for (Account acct : this.model.getAccounts()) {
			List<CashFlowNode> anodes = nodes.get(acct);
			if (anodes == null) {
				continue;
			}

			String ss = "";

			for (CashFlowNode node : anodes) {
				String sss = node.transfersString();
				if (!sss.isEmpty()) {
					ss += "     " + node.start.toString() + " to " + node.end.toString() + "\n";
					ss += sss + "\n";
				}
			}

			if (!ss.isEmpty()) {
				System.out.println();
				System.out.println("Transfers for " + acct.name);

				System.out.println(ss);
			}
		}
	}
}