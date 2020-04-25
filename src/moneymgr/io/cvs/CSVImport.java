package moneymgr.io.cvs;

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

	private LineNumberReader rdr;

	/** Map account name to transaction tuples */
	public Map<String, List<TransactionInfo>> transactionsMap = new HashMap<>();

	/** When assembling splits, this holds the current main transaction */
	public GenericTxn lasttxn = null;

	/**
	 * Matches come in several forms<br>
	 * Simple - no splits on either side Split - tx exists as split in both versions
	 * PseudoSplit - non-split matches split in other version
	 */

	/** Process a CVS file exported from MacOs */
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

	/** TESTING: Import CSV file and compare to QIF version */
	public static void testMacImport() {
		String importDir = "/Users/greg/qif/";

		System.out.println("Processing csv file");

		// TODO import into separate MoneyManagerModel instead
		System.out.println(String.format("There are %d transactions from DIETRICH.QIF", //
				MoneyMgrModel.currModel.getAllTransactions().size()));

		CSVImport.importCSV(importDir + "DIETRICH.csv");

		System.out.println(String.format("After import, there are now %d transactions from DIETRICH.QIF", //
				MoneyMgrModel.currModel.getAllTransactions().size()));
//		Collections.sort(MoneyMgrModel.currModel.alternateTransactions, new Comparator<GenericTxn>() {
//			public int compare(GenericTxn o1, GenericTxn o2) {
//				return o1.getDate().compareTo(o2.getDate());
//			}
//		});
//
//		List<GenericTxn> txns = MoneyMgrModel.currModel.alternateTransactions;
//		System.out.println(String.format("There are %d transactions from MAC export", //
//				MoneyMgrModel.currModel.alternateTransactions.size()));
	}

	/** Map MAC tx to WIN tx */
	public List<TransactionInfo> nomatch = new ArrayList<>();
	public List<TransactionInfo> allzero = new ArrayList<>();
	public List<TransactionInfo> nomatchZero = new ArrayList<>();
	public int totaltx = 0;

	private CSVImport(String filename) {
		try {
			this.rdr = new LineNumberReader(new FileReader(filename));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void importFile() {
		if (!readFieldNames()) {
			return;
		}

		String[] msgs = { "Multiple Matches", "Inexact Matches", "Action Mismatches" };

		importCSVRecords();

		try {
			this.rdr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

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

		System.out.println("Done importing CSV file");
	}

	private void processCategories(PrintStream out) {
		Set<String> categoryNameSet = new HashSet<>();

		for (List<TransactionInfo> tinfos : this.transactionsMap.values()) {
			for (TransactionInfo tinfo : tinfos) {
				String cat = tinfo.value(TransactionInfo.CATEGORY_IDX);

				if (!cat.isEmpty() && !cat.startsWith("Transfer:[")) {
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

				processTuple(tuple);
			}
		}
	}

	private void processTuple(TransactionInfo tuple) {
		SimpleTxn txn = createTransaction(tuple);
		if (txn != null) {
			++this.totaltx;
			tuple.macTxn = txn;

			matchTransaction(tuple);
		}
	}

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

		Account acct = MoneyMgrModel.currModel.getAccountByID(mactxn.getAccountID());
		List<SimpleTxn> txns = MoneyMgrModel.currModel.findMatchingTransactions(acct, mactxn);

		if (txns.isEmpty()) {
			txns = MoneyMgrModel.currModel.findMatchingTransactions(acct, mactxn);
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

		if (tuple.winTxn == null) {
			// TODO System.out.println("xyzzy NO WIN TXN");
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
			tuple.addInexactMessage("   WIN " + wintxn.toString());
		}
	}

	private SimpleTxn createTransaction(TransactionInfo tuple) {
		SimpleTxn txn = null;

		try {
			tuple.processValues();

			Account acct = tuple.account;
			if (acct == null) {
				Common.reportError("Account not found");
				return null;
			}

			String payee = tuple.value(TransactionInfo.PAYEE_IDX);
			BigDecimal amount = Common.getDecimal(tuple.value(TransactionInfo.AMOUNT_IDX));

			String split = tuple.value(TransactionInfo.SPLIT_IDX);
			String memo = tuple.value(TransactionInfo.MEMO_IDX);
			String cknum = tuple.value(TransactionInfo.CHECKNUM_IDX);
			String cat = tuple.value(TransactionInfo.CATEGORY_IDX);
			String desc = tuple.value(TransactionInfo.DESCRIPTION_IDX);
			String type = tuple.value(TransactionInfo.TYPE_IDX);
			String sec = tuple.value(TransactionInfo.SECURITY_IDX);
			String fees = tuple.value(TransactionInfo.FEES_IDX);
			String shares = tuple.value(TransactionInfo.SHARES_IDX);
			String action = tuple.value(TransactionInfo.ACTION_IDX);
			String sharesIn = tuple.value(TransactionInfo.SHARESIN_IDX);
			String sharesOut = tuple.value(TransactionInfo.SHARESOUT_IDX);
			String inflow = tuple.value(TransactionInfo.INFLOW_IDX);
			String outflow = tuple.value(TransactionInfo.OUTFLOW_IDX);

			Account xferAcct = null;
			Category c = null;
			int catid;

			if (cat.startsWith("Transfer:[")) {
				// TODO account names different in mac file
				cat = cat.substring(10, cat.length() - 1);
				if (cat.contentEquals("Tesla Model 3")) {
					cat = "Tesla";
				} else if (cat.contentEquals("Tesla Loan")) {
					cat = "TeslaLoan";
				}
				xferAcct = MoneyMgrModel.currModel.findAccount(cat);
				catid = (xferAcct != null) ? -xferAcct.acctid : 0;
			} else {
				c = (!cat.isEmpty()) ? MoneyMgrModel.currModel.findCategory(cat) : null;
				catid = (c != null) ? c.catid : 0;
			}

			if (split.equals("S")) {
				if (this.lasttxn != null) {
					if (!this.lasttxn.getDate().equals(tuple.getDate()) //
							|| !this.lasttxn.hasSplits() //
							|| this.lasttxn.chkNumber != tuple.value(TransactionInfo.CHECKNUM_IDX)) {
						// TODO at some point, validate splits
						this.lasttxn = null;
					}
				}

				if (this.lasttxn == null) {
					this.lasttxn = (acct.isInvestmentAccount()) //
							? new InvestmentTxn(acct.acctid) //
							: new NonInvestmentTxn(acct.acctid);
					if (!cknum.isEmpty()) {
						this.lasttxn.setCheckNumber(cknum);
					}
				}

				if (this.lasttxn instanceof InvestmentTxn) {
					Common.reportWarning("Can't add split to investment txn");
				}
				txn = new SplitTxn(this.lasttxn);
				MoneyMgrModel.currModel.addTransaction(txn);
				this.lasttxn.addSplit((SplitTxn) txn);
			} else if (acct.isNonInvestmentAccount()) {
				txn = new NonInvestmentTxn(acct.acctid);
			} else if (acct.isInvestmentAccount()) {
				txn = new InvestmentTxn(acct.acctid);
			}

			if (!split.equals("S")) {
				txn.setDate(tuple.getDate());
			} else if (this.lasttxn.getDate() == null) {
				this.lasttxn.setDate(tuple.getDate());
			}

			txn.setAmount(amount);
			txn.setMemo(memo);
			txn.setCashTransferTxn(null);
			txn.setCatid(catid);

			if (txn instanceof GenericTxn) {
				GenericTxn gtxn = (GenericTxn) txn;
				gtxn.setPayee(payee);
			}

			if (txn instanceof NonInvestmentTxn) {
				NonInvestmentTxn nitxn = (NonInvestmentTxn) txn;
				nitxn.chkNumber = cknum;
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
					itxn.accountForTransfer = MoneyMgrModel.currModel.getAccountByID(itxn.getCashTransferAcctid()).name;
					itxn.setAction((inflow.isEmpty()) ? TxAction.XOUT : TxAction.XIN);
					itxn.cashTransferred = amount;
				} else {
					itxn.setAction(TxAction.OTHER);
					itxn.cashTransferred = BigDecimal.ZERO;
					itxn.accountForTransfer = null;
				}
				itxn.commission = (fees.isEmpty()) //
						? BigDecimal.ZERO //
						: Common.getDecimal(fees);
				itxn.price = BigDecimal.ZERO;
				if (!sec.isEmpty()) {
					itxn.setSecurity(MoneyMgrModel.currModel.findSecurity(sec));
				}

				// 0.42 shares @ 1.00
				if (!desc.isEmpty()) {
					int sharesidx = desc.indexOf(' ');
					int priceidx = desc.indexOf('@');
					if (priceidx >= 0) {
						try {
							String pricestr = desc.substring(priceidx + 1).trim();
							String quantitystr = desc.substring(0, sharesidx).trim();

							itxn.setQuantity(Common.getDecimal(quantitystr));
							itxn.price = Common.getDecimal(pricestr);
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
				TransactionInfo.setFieldNames(record.toArray(new String[0]));

				return true;
			}
		}

		return false;
	}
}
