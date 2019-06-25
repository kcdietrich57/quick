package app;

import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;

import moneymgr.io.cvs.CSVImport;
import moneymgr.io.cvs.CSVImport.TupleInfo;
import moneymgr.io.qif.QifDomReader;
import moneymgr.model.GenericTxn;
import moneymgr.report.InvestmentPerformanceModel;
import moneymgr.ui.MainFrame;
import moneymgr.util.QDate;

/** Test program for importing output from Quicken on Mac */
public class MoneyMgrApp {
	public static Scanner scn;

	public static void importCSV(String filename) {
		CSVImport csvimp = new CSVImport(filename);
		csvimp.importFile();

		Comparator<TupleInfo> comp = new Comparator<TupleInfo>() {
			public int compare(TupleInfo tx1, TupleInfo tx2) {
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

		testMacImport();
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
		System.out.println(String.format("There are %d transactions from MAC export", //
				GenericTxn.alternateTransactions.size()));
	}
}
