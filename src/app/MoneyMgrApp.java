package app;

import java.util.Scanner;

import moneymgr.io.cvs.CSVImport;
import moneymgr.io.mm.Persistence;
import moneymgr.io.qif.QifDomReader;
import moneymgr.report.CashFlow;
import moneymgr.report.InvestmentPerformanceModel;
import moneymgr.ui.MainFrame;
import moneymgr.util.QDate;

/**
 * App for working with Quicken data (Windows QIF export, MacOS CSV export)
 */
public class MoneyMgrApp {
	// TODO Optionally run experimental code
	static boolean ENABLE_EXPERIMENTAL_CODE = false;

	public static Scanner scn;

	public static void main(String[] args) {
		MoneyMgrApp.scn = new Scanner(System.in);
		QifDomReader.loadDom(new String[] { "qif/DIETRICH.QIF" });

//		System.out.println("Mismatched quotes:");
//		for (Entry<String, Integer> entry : Security.dupQuotes.entrySet()) {
//			System.out.println("  " + entry.getKey() + ": " + entry.getValue());
//		}
		Persistence persistence = new Persistence("/tmp/dietrich.mm");
		persistence.save();

		if (ENABLE_EXPERIMENTAL_CODE) {
			InvestmentPerformanceModel model = new InvestmentPerformanceModel( //
					new QDate(2018, 8, 1), //
					QDate.today());

			System.out.println(model.toString());
		}

		MainFrame.createUI();

		if (MoneyMgrApp.ENABLE_EXPERIMENTAL_CODE) {
			runExperimentalCode();
		}
	}

	/** This function will run experimental code for the current data */
	private static void runExperimentalCode() {
		CashFlow.reportCashFlowForTrailingYear();

		CSVImport.testMacImport();
	}
}
