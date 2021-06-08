package moneymgr.io.csv;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.QifDom;
import moneymgr.io.TransactionInfo;
import moneymgr.io.qif.QifDomReader;
import moneymgr.io.qif.TransactionCleaner;
import moneymgr.model.Account;
import moneymgr.model.Category;
import moneymgr.model.GenericTxn;
import moneymgr.model.InvestmentTxn;
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.NonInvestmentTxn;
import moneymgr.model.Security;
import moneymgr.model.SimpleTxn;
import moneymgr.model.SplitTxn;
import moneymgr.model.TxAction;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/** EXPERIMENTAL Import data from CSV file exported from MacOS Quicken */
public class CSVImport {

	private static void infoMessage(String msg) {
		if (QifDom.verbose) {
			infoMessageNoln(msg + "\n");
		}
	}

	private static void infoMessageNoln(String msg) {
		if (QifDom.verbose) {
			System.out.print(msg);
		}
	}

	/** TESTING: Import CSV file and compare to QIF version */
	public static void testCsvImport() {
		MoneyMgrModel sourceModel = MoneyMgrModel.getModel(MoneyMgrModel.WIN_QIF_MODEL_NAME);

		printMessage("Processing csv file");
		printMessage(String.format("Source model has %d transactions", //
				sourceModel.getAllTransactions().size()));

		String importDir = "/Users/greg/qif/";
		importCSV(importDir + "DIETRICH.csv");

		printMessage(String.format("Imported %d transactions", //
				MoneyMgrModel.currModel.getAllTransactions().size()));
	}

	/** Process a CSV file exported from MacOs */
	public static void importCSV(String filename) {
		CSVImport csvimp = new CSVImport(filename);

		csvimp.cloneSourceModelInfo();
		csvimp.importFile();

		QifDomReader rdr = new QifDomReader(new File("/Users/greg/qif"));
		rdr.postLoad();

		TransactionCleaner.cleanUpTransactionsFromCsv();
	}

	private LineNumberReader rdr;

	/** Map account name to transaction tuples */
	public Map<String, List<TransactionInfo>> transactionsMap = new HashMap<>();

	private MoneyMgrModel sourceModel;
	private MoneyMgrModel csvModel;

	/** Map MAC tx to WIN tx */
	public List<TransactionInfo> nomatch = new ArrayList<>();
	public List<TransactionInfo> allzero = new ArrayList<>();
	public List<TransactionInfo> nomatchZero = new ArrayList<>();
	public int totaltx = 0;

	private CSVImport(String filename) {
		try {
			this.sourceModel = MoneyMgrModel.getModel(MoneyMgrModel.WIN_QIF_MODEL_NAME);
			this.csvModel = MoneyMgrModel.changeModel(MoneyMgrModel.MAC_CSV_MODEL_NAME);

			this.rdr = new LineNumberReader(new FileReader(filename));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void importFile() {
		if (!readFieldNames()) {
			return;
		}

		importCSVRecords();

		try {
			this.rdr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		createTransactions();
		analyzeResults();

//		analyzeResults2();
	}

	private void createTransactions() {
		for (List<TransactionInfo> txinfos : transactionsMap.values()) {
			for (TransactionInfo txinfo : txinfos) {
				txinfo.processValues(this.sourceModel);

				SimpleTxn txn = createTransaction(txinfo);
				if (txn != null) {
					++this.totaltx;
					txinfo.setMacTransaction(txn);

					matchTransaction(txinfo);
					if (txinfo.winTxn() == null) {
						// printMessage("No match for\n" + txinfo.toString());
						txinfo.setWinTransaction(null);
						matchTransaction(txinfo);
					}
				}
			}
		}
	}

	static PrintStream ps = null;

	private static void printMessage(String msg) {
		if (ps == null) {
			try {
				ps = new PrintStream(new File("/tmp/fixme.txt"));
			} catch (Exception e) {
			}
		}

		// System.out.println(msg);
		ps.println(msg);
	}

	private void analyzeResults() {
		SimpleTxn mac2win[] = new SimpleTxn[this.csvModel.getLastTransactionId() + 1];
		SimpleTxn win2mac[] = new SimpleTxn[this.sourceModel.getLastTransactionId() + 1];

		for (String key : transactionsMap.keySet()) {
			List<TransactionInfo> txinfos = transactionsMap.get(key);

			for (TransactionInfo txinfo : txinfos) {
				if (txinfo.macTxn() != null && txinfo.winTxn() != null) {
					SimpleTxn mactx = txinfo.macTxn();
					int macid = mactx.getTxid();
					SimpleTxn wintx = txinfo.winTxn();
					int winid = wintx.getTxid();

					if (macid >= mac2win.length || winid >= win2mac.length) {
						Common.reportError("Bad txid - should be impossible");
					} else if (mac2win[macid] != null || win2mac[winid] != null) {
						Common.reportWarning("tx matched twice!");
					} else {
						mac2win[macid] = wintx;
						win2mac[winid] = mactx;
					}
				}
			}
		}

		List<GenericTxn> unmatchedWin = new ArrayList<>();
		List<GenericTxn> unmatchedMac = new ArrayList<>();
		int totalWin = 0;
		int totalMac = 0;

		for (int winid = 1; winid < win2mac.length; ++winid) {
			GenericTxn tx = sourceModel.getTransaction(winid);

			if (tx != null) {
				++totalWin;

				if (win2mac[winid] == null) {
					unmatchedWin.add(tx);
				}
			}
		}

		for (int macid = 1; macid < mac2win.length; ++macid) {
			GenericTxn tx = this.csvModel.getTransaction(macid);

			if (tx != null) {
				++totalMac;

				if (mac2win[macid] == null) {
					unmatchedMac.add(tx);
				}
			}
		}

		Comparator<GenericTxn> comp = new Comparator<GenericTxn>() {
			public int compare(GenericTxn o1, GenericTxn o2) {
//				int diff = o1.getAccount().name.compareTo(o2.getAccount().name);
//				if (diff != 0) {
//					// return diff;
//				}

				return o1.getDate().compareTo(o2.getDate());
			}
		};

		List<GenericTxn> uwintmp = new ArrayList<>(unmatchedWin);
		List<GenericTxn> umactmp = new ArrayList<>(unmatchedMac);

		Collections.sort(uwintmp, comp);
		Collections.sort(umactmp, comp);

		int winmax = uwintmp.size();
		int macmax = umactmp.size();
		int winNewmatch = 0;
		int macNewmatch = 0;

		try {
			printMessage(String.format("Unmatched: win=%d/%d mac=%d/%d", //
					winmax, totalWin, macmax, totalMac));

			printMessage("\nUnmatched transactions by date:");

			for (int winidx = 0, macidx = 0; winidx < winmax || macidx < macmax;) {
				QDate curdate = null;
				QDate windate = (winidx < winmax) ? uwintmp.get(winidx).getDate() : null;
				QDate macdate = (macidx < macmax) ? umactmp.get(macidx).getDate() : null;

				if (windate != null //
						&& (curdate == null || curdate.compareTo(windate) > 0)) {
					curdate = windate;
				}
				if (macdate != null //
						&& (curdate == null || curdate.compareTo(macdate) > 0)) {
					curdate = macdate;
				}

				// ======================================
				// Try to match up transactions that are splits
				// in the QIF data but separate transactions in the CSV data
				// Super simple method - all tx must be on the same day
				// ======================================

				List<GenericTxn> todayWin = new ArrayList<>();
				List<GenericTxn> todayMac = new ArrayList<>();

				while (winidx < winmax //
						&& uwintmp.get(winidx).getDate().compareTo(curdate) <= 0) {
					todayWin.add(uwintmp.get(winidx));
					++winidx;
				}
				while (macidx < macmax //
						&& umactmp.get(macidx).getDate().compareTo(curdate) <= 0) {
					todayMac.add(umactmp.get(macidx));
					++macidx;
				}

				for (Iterator<GenericTxn> iter = todayWin.iterator(); iter.hasNext();) {
					GenericTxn wintx = iter.next();
					if (Common.isEffectivelyZero(wintx.getAmount())) {
						// continue;
					}

					boolean matchFound = false;
					List<GenericTxn> macMatches = new ArrayList<>();

					List<GenericTxn> tmpmac = new ArrayList<>();
					BigDecimal tot = BigDecimal.ZERO;
					for (GenericTxn mactx : todayMac) {
						if (wintx.getAccount().name.equals(mactx.getAccount().name)) {
							tot = tot.add(mactx.getAmount());
							tmpmac.add(mactx);
						}
					}

					if (Common.isEffectivelyEqual(wintx.getAmount(), tot)) {
//						printMessage("Looks like these are a match:\n-----");
//						printMessage("  " + wintx.toString());
//
//						for (GenericTxn t : tmpmac) {
//							printMessage("      " + t.toString());
//						}
//
//						printMessage("-----");

						if (tmpmac.size() == 1) {
							SimpleTxn tx = tmpmac.get(0);
							tx.info.setWinTransaction(wintx);
						} else {
							// TODO fix up the transactions - combine the separate
							// transactions into a single tx with splits
						}

						matchFound = true;
						macMatches.addAll(tmpmac);

						++winNewmatch;
						macNewmatch += tmpmac.size();
					}

					if (fixBuyx(wintx, tmpmac, macMatches)) {
						matchFound = true;
						// TODO delete/disregard extra mac xfer tx altogether
					}

					if (fixZeroTransfer(wintx, tmpmac, macMatches)) {
						matchFound = true;
					}

					if (matchFound) {
						iter.remove();
						unmatchedWin.remove(wintx);

						todayMac.removeAll(tmpmac);
						unmatchedMac.removeAll(tmpmac);
					} else {
						if (wintx.getDate().toString().equals("4/30/93")) {
							// System.out.println("xyzzy");
						}
					}
				}

				if (!todayWin.isEmpty() || !todayMac.isEmpty()) {
					printMessage("");

					for (GenericTxn t : todayWin) {
						printMessage("  " + t.toString());
					}
					for (GenericTxn t : todayMac) {
						printMessage("      " + t.toString());
					}
				}
			}

			winmax -= winNewmatch;
			macmax -= macNewmatch;

			printMessage(String.format("Unmatched: win=%d(-%d) mac=%d(-%d)", //
					winmax, winNewmatch, macmax, macNewmatch));

			printMessage("\nUnmatched transactions in source model:");
			int count = 1;
			for (GenericTxn tx : unmatchedWin) {
				printMessage(String.format(" %d: %s", count, tx.toString()));
				++count;
			}

			printMessage("\nUnmatched transactions in new model:");
			count = 1;
			for (GenericTxn tx : unmatchedMac) {
				printMessage(String.format(" %d: %s", count, tx.toString()));
				++count;
			}

			// ps.close();
			// ps = null;
		} catch (Exception e) {
		}
	}

	private boolean fixBuyx(GenericTxn wintx, //
			List<GenericTxn> tmpmac, //
			List<GenericTxn> macToRemove) {
		if (!(wintx instanceof InvestmentTxn //
				&& (wintx.getAction() == TxAction.BUYX //
						|| wintx.getAction() == TxAction.SELLX) //
				&& tmpmac.size() == 2 //
				&& tmpmac.get(0) instanceof InvestmentTxn //
				&& tmpmac.get(1) instanceof InvestmentTxn)) {
			return false;
		}

		TxAction buysell;
		TxAction buysellx;
		TxAction inout;
		BigDecimal amount;
		if (wintx.getAction() == TxAction.BUYX) {
			buysell = TxAction.BUY;
			buysellx = TxAction.BUYX;
			inout = TxAction.XIN;
			amount = wintx.getCashTransferAmount();
		} else {
			buysell = TxAction.SELL;
			buysellx = TxAction.SELLX;
			inout = TxAction.XOUT;
			amount = wintx.getCashTransferAmount().negate();
		}

		InvestmentTxn macx = (InvestmentTxn) tmpmac.get(0);
		InvestmentTxn macbs = (InvestmentTxn) ((macx.getAction() == inout) //
				? tmpmac.get(1) //
				: macx);
		if (macbs == macx) {
			macx = (InvestmentTxn) tmpmac.get(1);
		}

		if (!(macx.getAction() == inout //
				&& macbs.getAction() == buysell //
				&& Common.isEffectivelyEqual(wintx.getAmount(), macbs.getAmount()) //
				&& Common.isEffectivelyEqual(macx.getAmount(), macbs.getAmount()))) {
			return false;
		}

		macbs.setAction(buysellx);
		macbs.setCatid(macx.getCatid());
		// TODO deal with multiple xfer splits
		List<SimpleTxn> transfers = macx.getCashTransfers();
		Account xacct = (transfers.isEmpty()) ? null : transfers.get(0).getAccount();
		int xacctid = (xacct != null) ? xacct.acctid : 0;
		macbs.setCashTransferAcctid(xacctid);
		macbs.setAccountForTransfer(String.format("[%s]", macx.getAccountForTransfer()));
		macbs.setCashTransferred(amount);

		macbs.info.setWinTransaction(wintx);
		macToRemove.add(macx);

		return true;
	}

	private boolean fixZeroTransfer(GenericTxn wintx, //
			List<GenericTxn> tmpmac, //
			List<GenericTxn> macToRemove) {
		if (!((wintx.getAction() == TxAction.XIN //
				|| wintx.getAction() == TxAction.XOUT) //
				&& Common.isEffectivelyZero(wintx.getAmount()))) {
			return false;
		}

		for (GenericTxn mactx : tmpmac) {
			if ((mactx.getAction() == TxAction.XIN //
					|| mactx.getAction() == TxAction.XOUT) //
					&& Common.isEffectivelyZero(mactx.getAmount())) {
				mactx.info.setWinTransaction(wintx);

				macToRemove.add(mactx);
				return true;
			}
		}

		return false;
	}

	private void analyzeResults2() {
		String[] msgs = { "Multiple Matches", "Inexact Matches", "Action Mismatches" };

		try {
			PrintStream out = new PrintStream("/Users/greg/qif/tupleinfo.out");

			processCategories(out);
			processAccounts(out);
			processSecurities(out);

			// TODO security price history

			int cleantuples = 0;
			int dirtytuples = 0;
			int datefixed = 0;
			int actionfixed = 0;
			int multi = 0;
			int inexact = 0;
			int action = 0;

			for (int ii = 0; ii < 3; ++ii) {

				String msg = "\n" + msgs[ii] + "\n-------------------------------------";
				out.println(msg);

				for (Account acct : MoneyMgrModel.currModel.getAccounts()) {
					List<TransactionInfo> tuples = this.transactionsMap.get(acct.name);
					if (tuples == null) {
						continue;
					}

					String amsg = "\n" + acct.name + "\n-------------------------------------\n";

					for (TransactionInfo tuple : tuples) {
//						out.println(((tuple.datemismatch) ? " fixdate" : "") //
//								+ ((tuple.fixaction) ? " fixaction" : ""));
						if (!tuple.multipleMessages.isEmpty()) {
							if (ii == 0) {
								++dirtytuples;
								++multi;
								out.print(amsg);
								amsg = "";
								out.println("  MAC" + tuple.macTxn().toString());
								out.print(tuple.multipleMessages);
							}
						} else if (!tuple.inexactMessages.isEmpty()) {
							if (ii == 1) {
								++dirtytuples;
								++inexact;
								out.print(amsg);
								amsg = "";
								out.println("  MAC" + tuple.macTxn().toString());
								out.print(tuple.inexactMessages);
							}
						} else if (!tuple.actionMessages.isEmpty()) {
							if (ii == 2) {
								++dirtytuples;
								++action;
								out.print(amsg);
								amsg = "";
								out.println("  MAC" + tuple.macTxn().toString());
								out.print(tuple.actionMessages);
							}
						} else if (ii == 0) {
							++cleantuples;
							if (tuple.datemismatch) {
								++datefixed;
							}
							if (tuple.fixaction) {
								++actionfixed;
							}
						}
					}
				}
			}

			String gmsg = "\nUnmatched Windows Transactions\n-------------------------------------";
			out.println(gmsg);

			int unmatchedWindowsTxns = 0;

			for (SimpleTxn stx : MoneyMgrModel.currModel.getAllTransactions()) {
				if (!isWinTxnMatched(stx, true)) {
//					out.println(gtx.toString());
					++unmatchedWindowsTxns;
				}
			}

			out.println("\nClean tuples: " + cleantuples);
			out.println("Clean but fixed date: " + datefixed + " action: " + actionfixed);
			out.println("Dirty tuples: " + dirtytuples);
			out.println("  Multi match: " + multi);
			out.println("  Inexact match: " + inexact);
			out.println("  Bad Action: " + action);
			out.println("  Unmatched Windows Txns: " + unmatchedWindowsTxns);

			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		Comparator<TransactionInfo> comp = new Comparator<TransactionInfo>() {
			public int compare(TransactionInfo tx1, TransactionInfo tx2) {
				return tx1.date.compareTo(tx2.date);
			}
		};
		Collections.sort(this.nomatch, comp);

		System.out.println("Done importing CSV file");
	}

	/**
	 * Matches come in several forms<br>
	 * Simple - no splits on either side<br>
	 * Split - tx exists as split in both versions<br>
	 * PseudoSplit - non-split matches split in other version
	 */

	private Account cloneAccount(Account acct) {
		Account a = csvModel.findAccount(acct.name);
		if (a != null && a.name.equals(acct.name)) {
			return a;
		}

		Common.debugInfo( //
				"Cloning source account '" + acct.name + "'" + //
						"(" + acct.acctid + ")");

		a = new Account( //
				acct.acctid, acct.name, acct.description, acct.type, //
				acct.getStatementFrequency(), acct.getStatementDay());

		csvModel.addAccount(a);

		return a;
	}

	private Category cloneCategory(Category cat) {
		if (cat == null) {
			return null;
		}

		Category c = csvModel.findCategory(cat.name);
		if (c != null) {
			return c;
		}

		Common.debugInfo( //
				"Cloning source category '" + cat.name + "'" + //
						"(" + cat.catid + ")");

		c = new Category( //
				cat.catid, cat.name, cat.description, cat.isExpense);

		csvModel.addCategory(c);

		return c;
	}

	private Security cloneSecurity(Security sec) {
		Security s = csvModel.findSecurityBySymbol(sec.symbol);
		if (s != null) {
			return s;
		}

		Common.debugInfo( //
				"Cloning source security '" + sec.symbol + "'" + //
						"(" + sec.secid + ")");

		s = new Security( //
				sec.secid, sec.symbol, sec.getName(), sec.type, sec.goal);
		s.names.clear();
		s.names.addAll(sec.names);

		csvModel.addSecurity(s);

		return s;
	}

	private void cloneSourceModelInfo() {
		for (Account acct : sourceModel.getAccounts()) {
			cloneAccount(acct);
		}

		for (Category cat : sourceModel.getCategories()) {
			cloneCategory(cat);
		}

		for (Security sec : sourceModel.getSecurities()) {
			cloneSecurity(sec);
		}

		Security tbm = sourceModel.findSecurityByName("Total Bond Market  1");
		if (tbm != null) {
			tbm.names.add("Total Bond Market 1");
		}
		tbm = csvModel.findSecurityByName("Total Bond Market  1");
		if (tbm != null) {
			tbm.names.add("Total Bond Market 1");
		}

		Security kdhax = sourceModel.findSecurityByName("Scudder Dreman High Return A");
		if (kdhax != null) {
			kdhax.names.add("Scudder Dreman High Return A (KDHAX)");
		}
		kdhax = csvModel.findSecurityByName("Scudder Dreman High Return A");
		if (kdhax != null) {
			kdhax.names.add("Scudder Dreman High Return A (KDHAX)");
		}
	}

	private void processCategories(PrintStream out) {
		Set<String> categoryNameSet = new HashSet<>();

		for (List<TransactionInfo> tinfos : this.transactionsMap.values()) {
			for (TransactionInfo tinfo : tinfos) {
				String cat = tinfo.value(TransactionInfo.CATEGORY_IDX);

				if (!cat.isEmpty() //
						&& !cat.startsWith("Transfer:[") //
						&& !cat.startsWith("[") //
				) {
					categoryNameSet.add(cat);
				}
			}
		}

		List<String> cats = new ArrayList<String>(categoryNameSet);
		Collections.sort(cats, new Comparator<String>() {
			public int compare(String s1, String s2) {
				return s1.compareToIgnoreCase(s2);
			}
		});

		out.println("\nUnrecognized Categories\n=====================");
		for (String catName : cats) {
			Category cat = MoneyMgrModel.currModel.findCategory(catName);

			if (cat == null) {
				out.println(catName);
			}
		}
	}

	private void processAccounts(PrintStream out) {
		Set<String> accountNameSet = new HashSet<>();

		for (List<TransactionInfo> tinfos : this.transactionsMap.values()) {
			for (TransactionInfo tinfo : tinfos) {
				String acctname = tinfo.value(TransactionInfo.ACCOUNT_IDX);

				if (!acctname.isEmpty()) {
					accountNameSet.add(acctname);
				}
			}
		}

		List<String> accts = new ArrayList<String>(accountNameSet);
		Collections.sort(accts, new Comparator<String>() {
			public int compare(String s1, String s2) {
				return s1.compareToIgnoreCase(s2);
			}
		});

		out.println("\nUnrecognized Accounts\n=====================");
		for (String acctName : accts) {
			Account acct = MoneyMgrModel.currModel.findAccount(acctName);

			if (acct == null) {
				out.println(acctName);
			}
		}
	}

	private void processSecurities(PrintStream out) {
		Set<String> secNameSet = new HashSet<>();

		for (List<TransactionInfo> tinfos : this.transactionsMap.values()) {
			for (TransactionInfo tinfo : tinfos) {
				String secName = tinfo.value(TransactionInfo.SECURITY_IDX);

				if (!secName.isEmpty()) {
					secNameSet.add(secName);
				}
			}
		}

		List<String> secNames = new ArrayList<String>(secNameSet);
		Collections.sort(secNames, new Comparator<String>() {
			public int compare(String s1, String s2) {
				return s1.compareToIgnoreCase(s2);
			}
		});

		out.println("\nUnrecognized Security\n=====================");
		for (String secName : secNames) {
			Security sec = MoneyMgrModel.currModel.findSecurity(secName);

			if (sec == null) {
				out.println(secName);
			}
		}
	}

	private boolean isWinTxnMatched(SimpleTxn wintxn, boolean processSplits) {
		if ((wintxn != null) //
				&& (wintxn.getAmount() != null) //
				&& !Common.isEffectivelyZero(wintxn.getAmount()) //
				&& !isMatched(wintxn)) {
			if (wintxn instanceof SplitTxn) {
				return isWinTxnMatched(((SplitTxn) wintxn).getContainingTxn(), false);
			}

			if (!wintxn.hasSplits() || !processSplits) {
				return false;
			}

			for (SplitTxn stxn : wintxn.getSplits()) {
				if (!isWinTxnMatched(stxn, true)) {
					return false;
				}
			}
		}

		return true;
	}

	private void importCSVRecords() {
		int tupleCount = 0;
		TransactionInfo currentSplitParent = null;

		for (;;) {
			TransactionInfo tuple = readRecord();
			if (tuple == null) {
				break;
			}

			++tupleCount;

			String f0 = tuple.value(0);

			if (f0.trim().isEmpty()) {
				String acctname = tuple.value(TransactionInfo.ACCOUNT_IDX);
				String payee = tuple.value(TransactionInfo.PAYEE_IDX);
				String date = tuple.value(TransactionInfo.DATE_IDX);
				String cknum = tuple.value(TransactionInfo.CHECKNUM_IDX);
				List<TransactionInfo> accttxns = this.transactionsMap.get(acctname);

				if (accttxns == null) {
					accttxns = new ArrayList<>();
					this.transactionsMap.put(acctname, accttxns);
				}

				if (tuple.isSplit()) {
					if (currentSplitParent != null) {
						String splitacctname = currentSplitParent.value(TransactionInfo.ACCOUNT_IDX);
						String splitpayee = currentSplitParent.value(TransactionInfo.PAYEE_IDX);
						String splitdate = currentSplitParent.value(TransactionInfo.DATE_IDX);
						String splitcknum = currentSplitParent.value(TransactionInfo.CHECKNUM_IDX);

						if (!(splitacctname.equals(acctname) //
								&& splitpayee.equals(payee) //
								&& splitdate.equals(date) //
								&& splitcknum.equals(cknum))) {
							currentSplitParent = null;
						}
					}

					if (currentSplitParent == null) {
						currentSplitParent = new TransactionInfo(new ArrayList<String>());

						currentSplitParent.setValue(TransactionInfo.ACCOUNT_IDX, acctname);
						currentSplitParent.setValue(TransactionInfo.DATE_IDX, date);
						currentSplitParent.setValue(TransactionInfo.PAYEE_IDX, payee);
						currentSplitParent.setValue(TransactionInfo.CHECKNUM_IDX, cknum);
						currentSplitParent.payee = payee;

						// TODO action amount inflow outflow ???

						accttxns.add(currentSplitParent);
					}

					TransactionInfo newsplit = tuple;

					String cat = tuple.value(TransactionInfo.CATEGORY_IDX);
					if (cat != null && !cat.isEmpty()) {
						for (TransactionInfo existing : currentSplitParent.splits) {
							String scat = existing.value(TransactionInfo.CATEGORY_IDX);

							if (existing.hasSplits()) {
								scat = existing.splits.get(0).value(TransactionInfo.CATEGORY_IDX);

								if (cat.equals(scat)) {
									existing.addSplit(tuple);
									newsplit = existing;
									break;
								}

							} else if (cat.equals(scat)) {
								newsplit = new TransactionInfo(new ArrayList<>());

								newsplit.setValue(TransactionInfo.ACCOUNT_IDX, acctname);
								newsplit.setValue(TransactionInfo.DATE_IDX, date);
								newsplit.setValue(TransactionInfo.PAYEE_IDX, payee);
								newsplit.payee = payee;

								currentSplitParent.splits.remove(existing);
								newsplit.addSplit(existing);
								newsplit.addSplit(tuple);
								break;
							}
						}
					}

					if (!currentSplitParent.splits.contains(newsplit)) {
						currentSplitParent.addSplit(newsplit);
					}
				} else {
					currentSplitParent = null;

					accttxns.add(tuple);
				}
			}
		}

		printMessage("Transactions loaded:");

		for (Map.Entry<String, List<TransactionInfo>> entry : transactionsMap.entrySet()) {
			printMessage(String.format("  %s: %d", entry.getKey(), entry.getValue().size()));
		}

		printMessage(String.format("Total: %d transactions", tupleCount));
	}

	private boolean isMatched(SimpleTxn txn) {
		if (txn == null || txn.getAccount() == null || txn.getAccount().name == null) {
			return false;
		}
		List<TransactionInfo> tuples = this.transactionsMap.get(txn.getAccount().name);

		if (tuples != null) {
			for (TransactionInfo tuple : tuples) {
				if (tuple.winTxn() == txn) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * @param tuple Tupleinfo for the txn
	 */
	private void matchTransaction(TransactionInfo tuple) {
		SimpleTxn mactxn = tuple.macTxn();

		infoMessage(mactxn.toString());

		Account acct = this.sourceModel.getAccountByID(mactxn.getAccountID());
		List<SimpleTxn> txns = this.sourceModel.findMatchingTransactions(acct, mactxn);

		if (txns.isEmpty()) {
			txns = this.sourceModel.findMatchingTransactions(acct, mactxn);
		}
		for (Iterator<SimpleTxn> iter = txns.iterator(); iter.hasNext();) {
			if (isMatched(iter.next())) {
				iter.remove();
			}
		}

		boolean iszero = Common.isEffectivelyZero(mactxn.getAmount());
		if (iszero) {
			this.allzero.add(tuple);
		}

		if (!txns.isEmpty()) {
			processMatchesFound(tuple, txns);
		} else if (iszero) {
			this.nomatchZero.add(tuple);
		} else {
			this.nomatch.add(tuple);
		}
	}

	private void processMatchesFound(TransactionInfo tuple, List<SimpleTxn> txns) {
		SimpleTxn mactxn = tuple.macTxn();
		boolean iszero = Common.isEffectivelyZero(mactxn.getAmount());

		if (!iszero && (txns.size() > 1)) {
			tuple.winTxnMatches.addAll(txns);
			for (SimpleTxn tx : txns) {
				tuple.addMultipleMessage("     WIN " + tx.toString());
			}
		}

		SimpleTxn wintxn = null;
		int lastdiff = 0;

		// TODO this matching sucks
		for (SimpleTxn tx : txns) {
			SimpleTxn mtxn = tx;// TODO Account.getMatchTx(mactxn, tx);

			int diff = Math.abs(mactxn.compareWith(tuple, mtxn));

			if (wintxn == null || diff > lastdiff) {
				wintxn = tx;
				lastdiff = diff;
			}
		}

		tuple.setWinTransaction(wintxn);

		if (!iszero && (lastdiff != 0)) {
			wintxn.verifyAllSplit();

			tuple.addInexactMessage("   WIN " + wintxn.toString());

			printMessage(String.format("\ninexact match for:\n  %s\n  %s", // TODO xyzzy
					mactxn.toString(), wintxn.toString()));

			mactxn.compareWith(tuple, wintxn);
		}
	}

	private SimpleTxn createTransaction(TransactionInfo tuple) {
		MoneyMgrModel model = MoneyMgrModel.currModel;

		// TODO xyzzy defunct test here
		if (!model.name.equals(MoneyMgrModel.MAC_CSV_MODEL_NAME)) {
			System.out.println(String.format( //
					"Creating CSV transaction in wrong model '%s'", model.name));
		}
		GenericTxn txn = null;

		try {
			Account acct = tuple.account;
			if (acct == null) {
				Common.reportError("Account not found");
				return null;
			}

			if (acct.isNonInvestmentAccount()) {
				txn = new NonInvestmentTxn(acct.acctid);
			} else if (acct.isInvestmentAccount()) {
				txn = new InvestmentTxn(acct.acctid);
			} else {
				Common.reportError("crazy account?");
			}

			txn.setDate(tuple.date);

			txn.setAmount(tuple.amount);
			txn.setMemo(tuple.memo);
			txn.setCashTransferTxn(null);
			txn.setCatid((tuple.category != null) ? tuple.category.catid : 0);
			txn.setCheckNumber(tuple.cknum);

			if (tuple.xaccount != null) {
				txn.setCashTransferAcctid(tuple.xaccount.acctid);
			}

			if (txn instanceof GenericTxn) {
				GenericTxn gtxn = (GenericTxn) txn;
				gtxn.setPayee(tuple.payee);
			}

			if (txn instanceof InvestmentTxn) {
				InvestmentTxn itxn = (InvestmentTxn) txn;

				// TODO payee, acctForTransfer, amountTransferred,
				// TODO lots

				// TODO action for non-transfers (payment, income, buy/sell, buyx/sellx, etc)
				if (itxn.getCatid() < 0) {
					itxn.setCashTransferAcctid(-itxn.getCatid());
				}

				// TODO deal with multiple xfer splits
				int xacctid = 0; // itxn.getCashTransferAcctid();
				if (xacctid > 0) {
					itxn.setAccountForTransfer(model.getAccountByID(xacctid).name);
					itxn.setAction((tuple.inflow != null && tuple.inflow.signum() > 0) //
							? TxAction.XIN //
							: TxAction.XOUT);
					itxn.setCashTransferred(tuple.amount);
				} else {
					TxAction act = TxAction.parseAction(tuple.value(TransactionInfo.TYPE_IDX));
					if (act == null) {
						act = TxAction.OTHER;
					}
					itxn.setAction(act);

					itxn.setCashTransferred(BigDecimal.ZERO);
					itxn.setAccountForTransfer(null);
				}

				itxn.setCommission((tuple.fees != null) ? tuple.fees : BigDecimal.ZERO);
				itxn.setQuantity(tuple.shares);
				itxn.setPrice(tuple.price); // BigDecimal.ZERO;
				if (tuple.security != null) {
					itxn.setSecurity(tuple.security);
				}

				// TODO redundant??? 0.42 shares @ 1.00
				if (tuple.description != null && !tuple.description.isEmpty()) {
					int sharesidx = tuple.description.indexOf(' ');
					int priceidx = tuple.description.indexOf('@');
					if (priceidx >= 0) {
						try {
							String pricestr = tuple.description.substring(priceidx + 1).trim();
							String quantitystr = tuple.description.substring(0, sharesidx).trim();

							itxn.setQuantity(Common.getDecimal(quantitystr));
							itxn.setPrice(Common.getDecimal(pricestr));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}

				// Calculate missing amount for e.g. reinvest (p*q)
				if (Common.isEffectivelyZero(itxn.getAmount())) {
					BigDecimal p = itxn.getPrice();
					BigDecimal q = itxn.getQuantity();

					if (p != null && q != null) {
						BigDecimal amt = itxn.getQuantity().multiply(itxn.getPrice());
						if (!Common.isEffectivelyZero(amt)) {
							itxn.setAmount(amt);
						}
					}
				}
			}

			if (tuple.hasSplits()) {
				for (TransactionInfo splitinfo : tuple.splits) {
					SplitTxn splittx = new SplitTxn(txn);

					splittx.setAmount(splitinfo.amount);
					splittx.setMemo(splitinfo.memo);
					splittx.setCatid( //
							(splitinfo.category != null) ? splitinfo.category.catid : 0);

					txn.addSplit(splittx);
					// NB split txns are not automatically added to model
					model.addTransaction(splittx);
				}
			}

			acct.addTransaction((GenericTxn) txn);

//			List<SimpleTxn> matches = acct.findPotentialMatchingTransactions(txn);
//			matches.add(txn);
//			System.out.println("" + matches.size() + " potential matches found");

//		public int xacctid;
//		public int catid; // >0: CategoryID; <0 AccountID
//		public SimpleTxn xtxn;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return txn;
	}

	private List<String> readRawRecord() {
		List<String> fields = new ArrayList<>();

		try {
			String line = this.rdr.readLine();
			if (line == null) {
				return null;
			}

			int startidx = 0;
			int endidx = line.length();

			// process each field
			while (startidx < endidx) {
				int ch = line.charAt(startidx);
				if (ch == ',') {
					fields.add("");
					++startidx;

					continue;
				}

				boolean inquote = false;

				if (ch == '"') {
					inquote = true;
					++startidx;
				}

				int tokenstart = startidx;
				int tokenend = -1;

				while ((startidx < endidx) && (tokenend < 0)) {
					ch = line.charAt(startidx);

					if (inquote && (ch == '"')) {
						if (startidx < (endidx - 1)) {
							if (line.charAt(startidx + 1) == '"') {
								startidx += 2;
								continue;
							}
						}

						inquote = false;
						tokenend = startidx;
						++startidx;
					} else if (!inquote && (ch == ',')) {
						tokenend = startidx;
					} else {
						++startidx;
					}
				}

				if (inquote) {
					Common.reportError("Unmatched quote in CSV: " + line);
				}

				if (tokenend < 0) {
					tokenend = endidx;
				}

				String field = line.substring(tokenstart, tokenend);
				fields.add(field);

				if ((startidx < endidx) && (line.charAt(startidx) != ',')) {
					Common.reportError("Missing comma at " + startidx + " in CSV: " + line);
				}

				++startidx;
			}
		} catch (Exception e) {
			e.printStackTrace();

			return null;
		}

		String f0 = (!fields.isEmpty()) ? fields.get(0) : "";
		if ((f0.length() == 1) && (f0.charAt(0) == 65279)) {
			fields.remove(0);
			fields.add(0, "");
		}

		return fields;
	}

	private TransactionInfo readRecord() {
		List<String> fields = readRawRecord();
		return (fields != null) ? new TransactionInfo(fields) : null;
	}

	private boolean readFieldNames() {
		for (;;) {
			List<String> record = readRawRecord();
			if (record == null) {
				break;
			}

			if (record.contains("Account") && record.contains("Date")) {
				TransactionInfo.setFieldOrder(record.toArray(new String[0]));

				return true;
			}
		}

		return false;
	}
}

//int mac_nomatch = csvimp.nomatch.size() + csvimp.nomatchZero.size();
//int mac_total = csvimp.totaltx;
//int win_unmatch = 0;
//int win_total = 0;
//
//// int nn = 1;
////PrintStream out = null;
////try {
////	out = new PrintStream("/Users/greg/qif/output.txt");
////
////	int totalmac = 0;
////	int totalwin = 0;
////	int nomatchmac = 0;
////	int nomatchwin = 0;
////
////	for (List<TupleInfo> tuples : csvimp.transactionsMap.values()) {
////		for (TupleInfo tuple : tuples) {
////			++totalmac;
////			if (tuple.winTxnMatches.isEmpty()) {
////				SimpleTxn mactxn = tuple.macTxn;
////				out.print("No match for mactxn:\n    " + mactxn);
////
////				out.println();
////
////				++nomatchmac;
////
////				if (mactxn.getAmount().signum() != 0) {
////					Account acct = mactxn.getAccount();
////					acct.findMatchingTransactions(mactxn);
////				}
////			}
////		}
////	}
//
//	for (GenericTxn wintxn : GenericTxn.getAllTransactions()) {
//		++totalwin;
////		if (wintxn != null && !csvimp.matchInfoForWinTxn.containsKey(wintxn)) {
////			out.println("No match for wintxn:\n    " + wintxn);
////			++nomatchwin;
////		}
//	}
//
//	out.println("Total unmatched mac=" + nomatchmac + "/" + totalmac //
//			+ " win=" + nomatchwin + "/" + totalwin);
//
//	out.println("\nSummary for : " + filename);
//	out.println("MAC tot=" + mac_total + " match=" + mac_nomatch);
//	out.println("WIN tot=" + win_total + " match=" + (win_total - win_unmatch));
//
//	out.println(" Unmatched:     " + csvimp.nomatch.size());
//	out.println(" Unmatched zero:" + csvimp.nomatchZero.size());
//	out.println(" All zero:" + csvimp.allzero.size());
//	out.println(" Total:         " + csvimp.totaltx);
//
//	out.close();
//} catch (Exception e) {
//	e.printStackTrace();
//}
