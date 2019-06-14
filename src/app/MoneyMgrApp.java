package app;

import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;

import moneymgr.io.cvs.CSVImport;
import moneymgr.io.qif.QifDomReader;
import moneymgr.model.SimpleTxn;
import moneymgr.report.InvestmentPerformanceModel;
import moneymgr.ui.MainFrame;
import moneymgr.util.QDate;

/** Test program for importing output from Quicken on Mac */
public class MoneyMgrApp {
	public static Scanner scn;

	public static void importCSV(String filename) {
		CSVImport csvimp = new CSVImport(filename);
		csvimp.importFile();

		Collections.sort(csvimp.nomatch, new Comparator<SimpleTxn>() {
			public int compare(SimpleTxn tx1, SimpleTxn tx2) {
				return tx1.getDate().compareTo(tx2.getDate());
			}
		});
		for (SimpleTxn txn : csvimp.nomatch) {
			System.out.println(txn.toString());
		}

		System.out.println("\nSummary for : " + filename);
		System.out.println(" Exact matches: " + csvimp.match.size());
		// System.out.println(" Multi matches: " + csvimp.multimatch.size());
		System.out.println(" Unmatched:     " + csvimp.nomatch.size());
		System.out.println(" Unmatched zero:" + csvimp.nomatchZero.size());
		System.out.println(" All zero:" + csvimp.allzero.size());
		System.out.println(" Total:         " + csvimp.totaltx);
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

		System.out.println("Processing csv export file");

		String importDir = "/Users/greg/Documents/workspace/Quicken/qif/";

//		try {
//			Thread.sleep(5000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		// importCSV(importDir + "import20180630.csv");
		// importCSV(importDir + "export-20171231.csv");
		// importCSV(importDir + "export-20180815.csv");
		// importCSV(importDir + "DIETRICH_all-2019061.csv");
		importCSV(importDir + "DIETRICH-export-2019-06-12.csv");
	}
}
