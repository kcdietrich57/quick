package app;

import java.util.Scanner;

import qif.importer.CSVImport;
import qif.importer.QifDomReader;
import qif.ui.MainFrame;

public class MoneyMgrApp {
	public static Scanner scn;

	public static void importCSV(String filename) {
		CSVImport csvimp = new CSVImport(filename);
		csvimp.importFile();

		System.out.println("\nSummary for : " + filename);
		System.out.println(" Exact matches: " + csvimp.match.size());
		System.out.println(" Multi matches: " + csvimp.multimatch.size());
		System.out.println(" Unmatched:     " + csvimp.nomatch.size());
		System.out.println(" Unmatched zero:" + csvimp.zero.size());
		System.out.println(" Total:         " + csvimp.totaltx);
	}

	public static void main(String[] args) {
		MoneyMgrApp.scn = new Scanner(System.in);
		QifDomReader.loadDom(new String[] { "qif/DIETRICH.QIF" });

		MainFrame.createUI();

		String importDir = "/Users/greg/Documents/workspace/Quicken/qif/";

		// importCSV(importDir + "import20180630.csv");
		importCSV(importDir + "export-20171231.csv");
		importCSV(importDir + "export-20180815.csv");
	}
}
