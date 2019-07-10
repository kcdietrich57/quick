package app;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import moneymgr.io.TransactionInfo;
import moneymgr.io.cvs.CSVImport;
import moneymgr.io.qif.QifDomReader;
import moneymgr.model.Account;
import moneymgr.model.GenericTxn;
import moneymgr.model.SimpleTxn;
import moneymgr.report.InvestmentPerformanceModel;
import moneymgr.ui.MainFrame;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/** Test program for importing output from Quicken on Mac */
public class MoneyMgrApp {
	public static Scanner scn;

	public static void importCSV(String filename) {
		CSVImport csvimp = new CSVImport(filename);
		csvimp.importFile();

		Comparator<TransactionInfo> comp = new Comparator<TransactionInfo>() {
			public int compare(TransactionInfo tx1, TransactionInfo tx2) {
				return tx1.date.compareTo(tx2.date);
			}
		};
		Collections.sort(csvimp.nomatch, comp);

//		int mac_nomatch = csvimp.nomatch.size() + csvimp.nomatchZero.size();
//		int mac_total = csvimp.totaltx;
//		int win_unmatch = 0;
//		int win_total = 0;
//
//		// int nn = 1;
////		PrintStream out = null;
////		try {
////			out = new PrintStream("/Users/greg/qif/output.txt");
////
////			int totalmac = 0;
////			int totalwin = 0;
////			int nomatchmac = 0;
////			int nomatchwin = 0;
////
////			for (List<TupleInfo> tuples : csvimp.transactionsMap.values()) {
////				for (TupleInfo tuple : tuples) {
////					++totalmac;
////					if (tuple.winTxnMatches.isEmpty()) {
////						SimpleTxn mactxn = tuple.macTxn;
////						out.print("No match for mactxn:\n    " + mactxn);
////
////						out.println();
////
////						++nomatchmac;
////
////						if (mactxn.getAmount().signum() != 0) {
////							Account acct = mactxn.getAccount();
////							acct.findMatchingTransactions(mactxn);
////						}
////					}
////				}
////			}
//
//			for (GenericTxn wintxn : GenericTxn.getAllTransactions()) {
//				++totalwin;
////				if (wintxn != null && !csvimp.matchInfoForWinTxn.containsKey(wintxn)) {
////					out.println("No match for wintxn:\n    " + wintxn);
////					++nomatchwin;
////				}
//			}
//
//			out.println("Total unmatched mac=" + nomatchmac + "/" + totalmac //
//					+ " win=" + nomatchwin + "/" + totalwin);
//
//			out.println("\nSummary for : " + filename);
//			out.println("MAC tot=" + mac_total + " match=" + mac_nomatch);
//			out.println("WIN tot=" + win_total + " match=" + (win_total - win_unmatch));
//
//			out.println(" Unmatched:     " + csvimp.nomatch.size());
//			out.println(" Unmatched zero:" + csvimp.nomatchZero.size());
//			out.println(" All zero:" + csvimp.allzero.size());
//			out.println(" Total:         " + csvimp.totaltx);
//
//			out.close();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}

	public static void main(String[] args) {
		MoneyMgrApp.scn = new Scanner(System.in);
		QifDomReader.loadDom(new String[] { "qif/DIETRICH.QIF" });

		// TODO experimental code
		InvestmentPerformanceModel model = new InvestmentPerformanceModel( //
				new QDate(2018, 8, 1), //
				QDate.today());

		System.out.println(model.toString());

		MainFrame.createUI();

		runExperimentalCode();
	}

	static boolean runex = true;

	private static void runExperimentalCode() {
		if (!runex) {
			return;
		}

		reportCashFlow();
//		testMacImport();
	}

	static class CashFlow {
		public QDate start;
		public QDate end;

		Map<Account, List<CashFlowNode>> terminalInputs = new HashMap<>();
		Map<Account, List<CashFlowNode>> terminalOutputs = new HashMap<>();
		Map<Account, List<CashFlowNode>> intermediaries = new HashMap<>();

		public CashFlow() {

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
	}

	static class CashFlowNode {
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

		public CashFlowNode(Account acct, QDate start, QDate end) {
			this.acct = acct;
			this.start = start;
			this.end = end;

			gatherInfo();
		}

		public void gatherInfo() {
			QDate startDate = this.start;
			QDate endDate = this.end;

			List<SimpleTxn> txns = this.acct.getTransactions(startDate, endDate);
			flatten(txns);

			for (SimpleTxn txn : txns) {
				addTransaction(txn);
			}
		}

		public void addTransaction(SimpleTxn txn) {
			BigDecimal cash = txn.getCashAmount();
			BigDecimal xfer = txn.getXferAmount();
			Account xacct = Account.getAccountByID(txn.getXferAcctid());
			SimpleTxn xtxn = txn.getXtxn();

			if (cash.signum() != 0 && xfer.signum() != 0) {
				txn = null;
			}

			// Save transfer info
			BigDecimal xtot = this.inxTotalForAccount.get(xacct);

			if (xfer.signum() > 0) {
				this.inxTotal = this.inxTotal.add(xfer);
				xtot = (xtot == null) ? xfer : xtot.add(xfer);
				this.inxTotalForAccount.put(xacct, xtot);
				++this.inxcount;
			} else if (xfer.signum() < 0) {
				this.outxTotal = this.outxTotal.add(xfer);
				xtot = (xtot == null) ? xfer : xtot.add(xfer);
				this.inxTotalForAccount.put(xacct, xtot);
				++this.outxcount;
			}

			if (xfer.signum() != 0) {
				cash = cash.subtract(xfer);
			}

			// Save inflow/outflow info
			if (cash.signum() > 0) {
				this.inflowTotal = this.inflowTotal.add(cash);
				++incount;
			} else if (cash.signum() < 0) {
				this.outflowTotal = this.outflowTotal.add(cash);
				++this.outcount;
			}

			if (((xfer.signum() == 0) && (cash.signum() == 0)) //
					|| xfer.equals(cash)) {
				++this.neutralcount;
			}
		}

		public BigDecimal getNetAmount() {
			return this.inflowTotal.add(this.outflowTotal).add(this.inxTotal).add(this.outxTotal);
		}

		String formatAmount(int count, BigDecimal value) {
			String countstr = (count > 0) ? Integer.toString(count) : "";
			String valuestr = (value.signum() != 0) ? Common.formatAmount(value).trim() : "";
			return String.format("%3s %10s", countstr, valuestr);
		}

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

		public String transfersString() {
			String ret = "";
			boolean first = true;

			for (Account xacct : Account.getAccounts()) {
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
	}

	private static void flatten(List<SimpleTxn> txns) {
		for (int ii = 0; ii < txns.size(); ++ii) {
			SimpleTxn stx = txns.get(ii);

			if (stx.hasSplits()) {
				txns.remove(ii);
				txns.addAll(ii, stx.getSplits());
			}
		}
	}

	private static void reportCashFlow() {
		QDate d = QDate.today();
		QDate start = d.getFirstDayOfMonth().addMonths(-12).getFirstDayOfMonth();

		Map<Account, List<CashFlowNode>> nodes = new HashMap<>();

		for (Account acct : Account.getAccounts()) {
			if (acct.isOpenDuring(start, QDate.today())) {
				buildCashFlowNode(nodes, acct, start);
			}
		}

		for (Account acct : Account.getAccounts()) {
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

		for (Account acct : Account.getAccounts()) {
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

	private static void testMacImport() {
		String importDir = "/Users/greg/Documents/workspace/Quicken/qif/";

		System.out.println("Processing csv file");

		GenericTxn.rememberTransactions = false;

		System.out.println(String.format("There are %d transactions from DIETRICH.QIF", //
				GenericTxn.getAllTransactions().size()));

		importCSV(importDir + "DIETRICH.csv");

		System.out.println(String.format("After import, there are now %d transactions from DIETRICH.QIF", //
				GenericTxn.getAllTransactions().size()));
		Collections.sort(GenericTxn.alternateTransactions, new Comparator<GenericTxn>() {
			public int compare(GenericTxn o1, GenericTxn o2) {
				return o1.getDate().compareTo(o2.getDate());
			}
		});

		List<GenericTxn> txns = GenericTxn.alternateTransactions;
		System.out.println(String.format("There are %d transactions from MAC export", //
				GenericTxn.alternateTransactions.size()));
	}
}
