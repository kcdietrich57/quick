package app;

import java.util.Scanner;

import moneymgr.io.csv.CSVImport;
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
		MoneyMgrModel qifModel = null;
		MoneyMgrModel jsonModel = null;
		MoneyMgrModel csvModel = null;

		boolean ENABLE_EXPERIMENTAL_CODE = false;
		boolean ENABLE_CSV_IMPORT = false;

		boolean savejson = false;
		boolean comparejson = false;
		boolean usejson = false;

		boolean loadwin = true;
		boolean loadjson = false;

		if (usejson) {
			loadjson = true;
		} else {
			loadwin = true;
		}

		if (savejson) {
			loadwin = true;
		}

		if (comparejson) {
			loadwin = true;
			loadjson = true;
		}

		Common.reportInfo("Starting MoneyManager");
		startupTime = System.currentTimeMillis();
		lapTime = startupTime;

		if (loadwin) {
			qifModel = MoneyMgrModel.changeModel(MoneyMgrModel.WIN_QIF_MODEL_NAME);

			Common.reportInfo("Loading QIF data");
			MoneyMgrApp.scn = new Scanner(System.in);
			QifDomReader.loadDom(new String[] { "qif/DIETRICH.QIF" });
			Common.reportInfo(String.format("Load complete: %s", elapsedTime()));
		}

		// ----------------------------------------------------------

//		System.out.println("Mismatched quotes:");
//		for (Entry<String, Integer> entry : Security.dupQuotes.entrySet()) {
//			System.out.println("  " + entry.getKey() + ": " + entry.getValue());
//		}

		if (ENABLE_EXPERIMENTAL_CODE) {
			Common.reportInfo(String.format("Running experimental code"));
			InvestmentPerformanceModel investmentPerformanceModel = new InvestmentPerformanceModel( //
					null, //
					new QDate(2018, 8, 1), //
					QDate.today());

			System.out.println(investmentPerformanceModel.toString());

			runExperimentalCode(qifModel);
			Common.reportInfo(String.format("Experimental code complete: %s", elapsedTime()));
		}

		// ----------------------------------------------------------

		String jsonFilename = "qif/DIETRICH.json";

		Persistence persistence = new Persistence();

		if (savejson) {
			Common.reportInfo(String.format("Saving QIF model as JSON"));
			persistence.saveJSON(qifModel, jsonFilename);
			Common.reportInfo(String.format("JSON saved: %s", elapsedTime()));
		}

		if (loadjson) {
			Common.reportInfo(String.format("Loading JSON"));
			jsonModel = persistence.loadJSON(MoneyMgrModel.WIN_JSON_MODEL_NAME, jsonFilename);
			Common.reportInfo(String.format("JSON loaded: %s", elapsedTime()));
		}

		if (comparejson) {
			Common.reportInfo(String.format("Comparing QIF/JSON models"));
			MoneyMgrModel.compareModels(MoneyMgrModel.WIN_QIF_MODEL_NAME, //
					MoneyMgrModel.WIN_JSON_MODEL_NAME);
			Common.reportInfo(String.format("Compare complete: %s", elapsedTime()));
		}

		if (ENABLE_CSV_IMPORT) {
			CSVImport.testCsvImport();
			csvModel = MoneyMgrModel.getModel(MoneyMgrModel.MAC_CSV_MODEL_NAME);

//			Common.reportInfo(String.format("Comparing QIF/CSV models"));
//			MoneyMgrModel.compareModels(MoneyMgrModel.WIN_QIF_MODEL_NAME, //
//					MoneyMgrModel.MAC_CSV_MODEL_NAME);
//			Common.reportInfo(String.format("Compare complete: %s", elapsedTime()));
		}

		MoneyMgrModel model;

		if (ENABLE_CSV_IMPORT) {
			model = MoneyMgrModel.changeModel(MoneyMgrModel.MAC_CSV_MODEL_NAME);
		} else if (usejson) {
			model = MoneyMgrModel.changeModel(MoneyMgrModel.WIN_JSON_MODEL_NAME);
		} else {
			model = MoneyMgrModel.changeModel(MoneyMgrModel.WIN_QIF_MODEL_NAME);
		}

		Common.reportInfo(String.format("Building UI"));
		MainFrame.createUI(model);

		Common.reportInfo(String.format("Startup complete: %s", elapsedTime()));
	}

	/** This function will run experimental code for the current data */
	private static void runExperimentalCode(MoneyMgrModel model) {
		CashFlow cashflow = new CashFlow(model);
		cashflow.reportCashFlowForTrailingYear();
	}
}
