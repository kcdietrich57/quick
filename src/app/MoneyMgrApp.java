package app;

import java.util.Scanner;

import moneymgr.io.cvs.CSVImport;
import moneymgr.io.mm.Persistence;
import moneymgr.io.qif.QifDomReader;
import moneymgr.model.MoneyMgrModel;
import moneymgr.report.CashFlow;
import moneymgr.report.InvestmentPerformanceModel;
import moneymgr.ui.MainFrame;
import moneymgr.util.QDate;

/**
 * App for working with Quicken data (Windows QIF export, MacOS CSV export)
 */
public class MoneyMgrApp {
	public static final String WIN_QIF_MODEL_NAME = "Windows QIF";
	public static final String WIN_JSON_MODEL_NAME = "Windows JSON";
	public static Scanner scn;

	public static void main(String[] args) {
		MoneyMgrModel.changeModel(WIN_QIF_MODEL_NAME);

		MoneyMgrApp.scn = new Scanner(System.in);
		QifDomReader.loadDom(new String[] { "qif/DIETRICH.QIF" });

		MainFrame.createUI();

		// ----------------------------------------------------------

		boolean ENABLE_EXPERIMENTAL_CODE = false;

//		System.out.println("Mismatched quotes:");
//		for (Entry<String, Integer> entry : Security.dupQuotes.entrySet()) {
//			System.out.println("  " + entry.getKey() + ": " + entry.getValue());
//		}

		Persistence persistence = new Persistence("/tmp/dietrich.mm");
		persistence.saveJSON();
		persistence.buildModel(WIN_JSON_MODEL_NAME);
		MoneyMgrModel.compareModels(WIN_QIF_MODEL_NAME, WIN_JSON_MODEL_NAME);
		MoneyMgrModel.changeModel(WIN_QIF_MODEL_NAME);

		if (ENABLE_EXPERIMENTAL_CODE) {
			InvestmentPerformanceModel model = new InvestmentPerformanceModel( //
					new QDate(2018, 8, 1), //
					QDate.today());

			System.out.println(model.toString());

			runExperimentalCode();
		}
	}

	/** This function will run experimental code for the current data */
	private static void runExperimentalCode() {
		CashFlow.reportCashFlowForTrailingYear();

		CSVImport.testMacImport();
	}
}
