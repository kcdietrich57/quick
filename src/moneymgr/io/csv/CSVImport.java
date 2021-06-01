package moneymgr.io.csv;

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

		String importDir = "/Users/greg/qif/";

		System.out.println("Processing csv file");

		System.out.println(String.format("Source model has %d transactions", //
				sourceModel.getAllTransactions().size()));

		importCSV(importDir + "DIETRICH.csv");

		System.out.println(String.format("Imported %d transactions", //
				MoneyMgrModel.currModel.getAllTransactions().size()));
	}

	/** Process a CSV file exported from MacOs */
	public static void importCSV(String filename) {
		CSVImport csvimp = new CSVImport(filename);

		csvimp.cloneSourceModelInfo();
		csvimp.importFile();
	}

	private LineNumberReader rdr;

	/** Map account name to transaction tuples */
	public Map<String, List<TransactionInfo>> transactionsMap = new HashMap<>();

	/** When assembling splits, this holds the current main transaction */
	public GenericTxn lasttxn = null;

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

		System.exit(1);

		analyzeResults2();
	}

	private void createTransactions() {
		for (List<TransactionInfo> txinfos : transactionsMap.values()) {
			for (TransactionInfo txinfo : txinfos) {
				txinfo.processValues(this.sourceModel);

				SimpleTxn txn = createTransaction(txinfo);
				if (txn != null) {
					++this.totaltx;
					txinfo.macTxn = txn;

					matchTransaction(txinfo);
				}
			}
		}
	}

	private void analyzeResults() {
		SimpleTxn mac2win[] = new SimpleTxn[this.csvModel.getLastTransactionId() + 1];
		SimpleTxn win2mac[] = new SimpleTxn[this.sourceModel.getLastTransactionId() + 1];

		for (List<TransactionInfo> txinfos : transactionsMap.values()) {
			for (TransactionInfo txinfo : txinfos) {
				if (txinfo.macTxn != null && txinfo.winTxn != null) {
					SimpleTxn mactx = txinfo.macTxn;
					int macid = mactx.getTxid();
					SimpleTxn wintx = txinfo.winTxn;
					int winid = wintx.getTxid();

					if (macid >= mac2win.length || winid >= win2mac.length) {
						Common.reportWarning("Bad txid - should be impossible");
					} else if (mac2win[macid] != null) {
						Common.reportWarning("mac tx matched twice!");
					} else if (win2mac[winid] != null) {
						Common.reportWarning("win tx matched twice!");
					} else {
						mac2win[macid] = wintx;
						win2mac[winid] = mactx;
					}
				}
			}
		}

		System.out.println("Unmatched transactions in source model:");
		for (int winid = 1; winid < win2mac.length; ++winid) {
			if (win2mac[winid] == null //
					&& sourceModel.getTransaction(winid) != null) {
				GenericTxn tx = sourceModel.getTransaction(winid);
				System.out.println("  " + tx.toString());
			}
		}

		System.out.println("Unmatched transactions in new model:");
		for (int macid = 1; macid < win2mac.length; ++macid) {
			if (mac2win[macid] == null //
					&& this.csvModel.getTransaction(macid) != null) {
				GenericTxn tx = this.csvModel.getTransaction(macid);
				System.out.println("  " + tx.toString());
			}
		}
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
								out.println("  MAC" + tuple.macTxn.toString());
								out.print(tuple.multipleMessages);
							}
						} else if (!tuple.inexactMessages.isEmpty()) {
							if (ii == 1) {
								++dirtytuples;
								++inexact;
								out.print(amsg);
								amsg = "";
								out.println("  MAC" + tuple.macTxn.toString());
								out.print(tuple.inexactMessages);
							}
						} else if (!tuple.actionMessages.isEmpty()) {
							if (ii == 2) {
								++dirtytuples;
								++action;
								out.print(amsg);
								amsg = "";
								out.println("  MAC" + tuple.macTxn.toString());
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
		if (a != null) {
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
		for (;;) {
			TransactionInfo tuple = readRecord();
			if (tuple == null) {
				break;
			}

			String f0 = tuple.value(0);

			if (f0.trim().isEmpty()) {
				String acctname = tuple.value(TransactionInfo.ACCOUNT_IDX);
				List<TransactionInfo> accttxns = this.transactionsMap.get(acctname);

				if (accttxns == null) {
					accttxns = new ArrayList<>();
					this.transactionsMap.put(acctname, accttxns);
				}

				accttxns.add(tuple);

				// TODO not here processTuple(tuple);
			}
		}

		System.out.println("Transactions loaded:");

		int total = 0;

		for (Map.Entry<String, List<TransactionInfo>> entry : transactionsMap.entrySet()) {
			String acctName = entry.getKey();
			List<TransactionInfo> infos = entry.getValue();

			System.out.println("  " + acctName + ": " + infos.size());

			total += infos.size();

//			for (TransactionInfo info : infos) {
//				// TODO not yet matchTransaction(info);
//			}
		}

		System.out.println("Total: " + total + " transactions");
	}

//	private void processTuple(TransactionInfo tuple) {
//		SimpleTxn txn = createTransaction(tuple);
//
//		if (txn != null) {
//			++this.totaltx;
//			tuple.macTxn = txn;
//
//			// TODO matchTransaction(tuple);
//		}
//	}

	private boolean isMatched(SimpleTxn txn) {
		if (txn == null || txn.getAccount() == null || txn.getAccount().name == null) {
			return false;
		}
		List<TransactionInfo> tuples = this.transactionsMap.get(txn.getAccount().name);

		if (tuples != null) {
			for (TransactionInfo tuple : tuples) {
				if (tuple.winTxn == txn) {
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
		SimpleTxn mactxn = tuple.macTxn;

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
		SimpleTxn mactxn = tuple.macTxn;
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

		tuple.winTxn = wintxn;

		if (!iszero && (lastdiff != 0)) {
			wintxn.verifyAllSplit();

			tuple.addInexactMessage("   WIN " + wintxn.toString());

			System.out.println( // TODO xyzzy
					"\ninexact match for:\n  " + mactxn.toString() //
							+ "\n  " + wintxn.toString());

			mactxn.compareWith(tuple, wintxn);
		}
	}

	private SimpleTxn createTransaction(TransactionInfo tuple) {
		SimpleTxn txn = null;

		try {
			Account acct = tuple.account;
			if (acct == null) {
				Common.reportError("Account not found");
				return null;
			}

			if (tuple.isSplit) {
				if (this.lasttxn != null) {
					if (!this.lasttxn.getDate().equals(tuple.date) //
							|| !this.lasttxn.hasSplits() //
							|| !this.lasttxn.getCheckNumberString().equals(tuple.cknum)) {
						// TODO at some point, validate splits
						this.lasttxn = null;
					}
				}

				if (this.lasttxn == null) {
					this.lasttxn = (acct.isInvestmentAccount()) //
							? new InvestmentTxn(acct.acctid) //
							: new NonInvestmentTxn(acct.acctid);
					if (tuple.cknum != null && !tuple.cknum.isEmpty()) {
						this.lasttxn.setCheckNumber(tuple.cknum);
					}
				}

				if (this.lasttxn instanceof InvestmentTxn) {
					// TODO xyzzy Common.reportWarning("Adding split to investment txn");
				}
				txn = new SplitTxn(this.lasttxn);
				MoneyMgrModel.currModel.addTransaction(txn);
				this.lasttxn.addSplit((SplitTxn) txn);
			} else if (acct.isNonInvestmentAccount()) {
				txn = new NonInvestmentTxn(acct.acctid);
			} else if (acct.isInvestmentAccount()) {
				txn = new InvestmentTxn(acct.acctid);
			}

			if (!tuple.isSplit) {
				txn.setDate(tuple.date);
			} else if (this.lasttxn.getDate() == null) {
				this.lasttxn.setDate(tuple.date);
			}

			txn.setAmount(tuple.amount);
			txn.setMemo(tuple.memo);
			txn.setCashTransferTxn(null);
			txn.setCatid((tuple.category != null) ? tuple.category.catid : 0);

			if (tuple.xaccount != null) {
				txn.setCashTransferAcctid(tuple.xaccount.acctid);
			}

			if (txn instanceof GenericTxn) {
				GenericTxn gtxn = (GenericTxn) txn;
				gtxn.setPayee(tuple.payee);
			}

			if (txn instanceof NonInvestmentTxn) {
				NonInvestmentTxn nitxn = (NonInvestmentTxn) txn;
				nitxn.setCheckNumber(tuple.cknum);
			}
// TODO payee, acctForTransfer, amountTransferred, 
// TODO lots
			if (txn instanceof InvestmentTxn) {
				InvestmentTxn itxn = (InvestmentTxn) txn;
// TODO action for non-transfers (payment, income, buy/sell, buyx/sellx, etc)
				if (itxn.getCatid() < 0) {
					itxn.setCashTransferAcctid(-itxn.getCatid());
				}

				if (itxn.getCashTransferAcctid() > 0) {
					itxn.setAccountForTransfer(
							MoneyMgrModel.currModel.getAccountByID(itxn.getCashTransferAcctid()).name);
					itxn.setAction((tuple.inflow != null && tuple.inflow.signum() > 0) //
							? TxAction.XIN //
							: TxAction.XOUT);
					itxn.setCashTransferred(tuple.amount);
				} else {
					itxn.setAction(TxAction.OTHER);
					itxn.setCashTransferred(BigDecimal.ZERO);
					itxn.setAccountForTransfer(null);
				}

				itxn.setCommission((tuple.fees != null) ? tuple.fees : BigDecimal.ZERO);
				itxn.setQuantity(tuple.shares);
				itxn.setPrice(tuple.price); // BigDecimal.ZERO;
				if (tuple.security != null) {
					itxn.setSecurity(tuple.security);
				}

				// 0.42 shares @ 1.00
				if (!tuple.description.isEmpty()) {
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
			}

//			List<SimpleTxn> matches = acct.findPotentialMatchingTransactions(txn);
//			matches.add(txn);
//			System.out.println("" + matches.size() + " potential matches found");

//		public int xacctid;
//		public int catid; // >0: CategoryID; <0 AccountID
//		public SimpleTxn xtxn;
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (txn instanceof GenericTxn) {
			this.lasttxn = (GenericTxn) txn;
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
