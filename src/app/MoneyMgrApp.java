package app;

import java.util.Scanner;

import moneymgr.io.cvs.CSVImport;
import moneymgr.io.mm.Persistence;
import moneymgr.io.qif.QifDomReader;
import moneymgr.model.MoneyMgrModel;
import moneymgr.report.CashFlow;
import moneymgr.report.InvestmentPerformanceModel;
import moneymgr.ui.MainFrame;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/**
 * App for working with Quicken data (Windows QIF export, MacOS CSV export)
 */
public class MoneyMgrApp {
	public static final String WIN_QIF_MODEL_NAME = "Windows QIF";
	public static final String WIN_JSON_MODEL_NAME = "Windows JSON";
	public static Scanner scn;

	private static long startupTime;
	private static long lapTime;

	public static String elapsedTime() {
		long now = System.currentTimeMillis();
		long elapsed = now - startupTime;
		long lap = now - lapTime;

		lapTime = now;

		String ret = String.format("%1.1fs - elapsed %1.1fs", //
				elapsed / 1000.0, //
				lap / 1000.0);
		
		return ret;
	}

	public static void main(String[] args) {
		Common.reportInfo("Starting MoneyManager");
		startupTime = System.currentTimeMillis();
		lapTime = startupTime;

		MoneyMgrModel.changeModel(WIN_QIF_MODEL_NAME);

		Common.reportInfo("Loading data");
		MoneyMgrApp.scn = new Scanner(System.in);
		QifDomReader.loadDom(new String[] { "qif/DIETRICH.QIF" });

		// ----------------------------------------------------------

		boolean ENABLE_EXPERIMENTAL_CODE = false;

//		System.out.println("Mismatched quotes:");
//		for (Entry<String, Integer> entry : Security.dupQuotes.entrySet()) {
//			System.out.println("  " + entry.getKey() + ": " + entry.getValue());
//		}

		String jsonFilename = "qif/DIETRICH.json";
		
		Common.reportInfo(String.format("Load complete: %s\nSaving JSON", elapsedTime()));
		Persistence persistence = new Persistence();
		persistence.saveJSON(jsonFilename);

		Common.reportInfo(String.format("JSON saved: %s\nLoading JSON", elapsedTime()));
		persistence.loadJSON(WIN_JSON_MODEL_NAME, jsonFilename);

		Common.reportInfo(String.format("JSON loaded: %s\nComparing models", elapsedTime()));
		MoneyMgrModel.compareModels(WIN_QIF_MODEL_NAME, WIN_JSON_MODEL_NAME);

		if (ENABLE_EXPERIMENTAL_CODE) {
			InvestmentPerformanceModel model = new InvestmentPerformanceModel( //
					new QDate(2018, 8, 1), //
					QDate.today());

			System.out.println(model.toString());

			runExperimentalCode();
		}

		Common.reportInfo(String.format("Load complete: %s\nBuilding UI", elapsedTime()));
		MoneyMgrModel.changeModel(WIN_QIF_MODEL_NAME);
		//MoneyMgrModel.changeModel(WIN_JSON_MODEL_NAME);
		MainFrame.createUI(MoneyMgrModel.currModel);

		Common.reportInfo(String.format("Startup complete: %s", elapsedTime()));
	}

	/** This function will run experimental code for the current data */
	private static void runExperimentalCode() {
		CashFlow.reportCashFlowForTrailingYear();

		CSVImport.testMacImport();
	}
}
