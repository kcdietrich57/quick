package qif.ui;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.style.CategoryStyler;
import org.knowm.xchart.style.Styler.LegendPosition;
import org.knowm.xchart.style.XYStyler;

import qif.data.Account;
import qif.data.QDate;
import qif.data.StockOption;
import qif.report.NetWorthReporter;
import qif.report.NetWorthReporter.Balances;
import qif.report.StatusForDateModel;
import qif.report.StatusForDateModel.Section;
import qif.report.StatusForDateModel.SectionInfo;

public class MainFrame extends JFrame {
	private static final long serialVersionUID = 1L;

	public MainWindow mainWindow;
	public static MainFrame frame;

	/** Create the GUI and show it. (Run in event-dispatching thread). */
	private static void createAndShowGUI() {
		frame = new MainFrame("Money Manager");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Create and set up the content pane.
		MainWindow mainWindow = new MainWindow();
		mainWindow.setOpaque(true);
		frame.setContentPane(mainWindow);
		frame.mainWindow = mainWindow;

		// Display the window.
		frame.pack();
		frame.setVisible(true);

		mainWindow.setSplitPosition();
	}

	public static void createUI() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});

		// TODO testing chart stuff
		List<StatusForDateModel> nwMonthly = NetWorthReporter.getMonthlyNetWorth();

		NetWorthChart.showISIOptions();
		NetWorthChart.showChart(nwMonthly);
		NetWorthChart.showChart2(nwMonthly);
	}

	public MainFrame(String name) {
		super(name);
	}
}

class NetWorthChart {

	public static void showISIOptions() {
		Account acct = Account.findAccount("ISI Options");

		QDate date = new QDate(1991, 1, 01);
		QDate end = new QDate(2003, 12, 31);

		List<QDate> dates = new ArrayList<QDate>();
		List<BigDecimal> high = new ArrayList<BigDecimal>();
		List<BigDecimal> low = new ArrayList<BigDecimal>();

		int curYear = 1991;
		int curMonth = 1;
		BigDecimal bdHigh = null;
		BigDecimal bdLow = null;

		while (date.compareTo(end) <= 0) {
			if ((date.getYear() != curYear) || (date.getMonth() != curMonth)) {
				dates.add(new QDate(curYear, curMonth, 1));
				high.add(bdHigh);
				low.add(bdLow);

				bdHigh = bdLow = null;
				curYear = date.getYear();
				curMonth = date.getMonth();
			}

			BigDecimal bal = BigDecimal.ZERO;

			List<StockOption> opts = StockOption.getOpenOptions(acct, date);
			for (StockOption opt : opts) {
				bal = bal.add(opt.getValueForDate(date));
			}

			if ((bdLow == null) || (bal.compareTo(bdLow) < 0)) {
				bdLow = bal;
			}
			if ((bdHigh == null) || (bal.compareTo(bdHigh) > 0)) {
				bdHigh = bal;
			}

			date = date.addDays(1);
		}

		double[] xData = new double[dates.size()];
		double[] yData = new double[dates.size()];

		for (int ii = 0; ii < dates.size(); ++ii) {
			xData[ii] = (double) ii;
			yData[ii] = Math.floor(high.get(ii).doubleValue() / 1000);
		}

		XYChart chart = QuickChart.getChart( //
				"ISI Options", "Month", "$K", "Value", xData, yData);

		new SwingWrapper<XYChart>(chart).displayChart();

	}

	public static void showChart(List<StatusForDateModel> balances) {

		if (balances.isEmpty()) {
			return;
		}

		SectionInfo[] sinfo = SectionInfo.sectionInfo;

		double[] xData = new double[balances.size()];
		double[][] yData = new double[sinfo.length][balances.size()];
		//double[][] yData = new double[sinfo.length + 1][balances.size()];

		for (int dateIndex = 0; dateIndex < balances.size(); ++dateIndex) {
			Section[] sections = balances.get(dateIndex).sections;

			xData[dateIndex] = (double) dateIndex;

			for (int sectionNum = 0; sectionNum < sections.length; ++sectionNum) {
				yData[sectionNum][dateIndex] = Math.floor(sections[sectionNum].subtotal.floatValue() / 1000);
			}

//			yData[sections.length][dateIndex] = //
//					Math.floor(balances.get(dateIndex).netWorth.floatValue() / 1000);
		}

		CategoryChart chart = new CategoryChartBuilder().width(800).height(600) //
				.title("Net Worth Chart") //
				.xAxisTitle("Month").yAxisTitle("K$").build();

		// Customize Chart
		CategoryStyler styler = chart.getStyler();
		styler.setLegendPosition(LegendPosition.InsideNW);
		styler.setAxisTitlesVisible(false);
		styler.setStacked(true);
		// styler.setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Area);

		// Series
		String[] categories = new String[] { //
				"Loan", "Credit Card", "Asset", "Retirement", "Investment", "Bank" //
		};

		for (int sectionNum = 0; sectionNum < sinfo.length; ++sectionNum) {
			for (int catNum = 0; catNum < sinfo.length; ++catNum) {
				if (sinfo[catNum].label.equals(categories[sectionNum])) {
					chart.addSeries(sinfo[catNum].label, xData, yData[catNum]);
				}
			}
		}

//		CategorySeries nwSeries = chart.addSeries("Net Worth", xData, yData[sinfo.length]);
//		nwSeries.setChartCategorySeriesRenderStyle(CategorySeriesRenderStyle.Line);

		new SwingWrapper<CategoryChart>(chart).displayChart();
	}

	public static void showChart1(List<Balances> balances) {

		double[] xData = new double[balances.size()];
		double[] y1Data = new double[balances.size()];
		double[] y2Data = new double[balances.size()];
		double[] y3Data = new double[balances.size()];
		double[] y4Data = new double[balances.size()];

		for (int ii = 0; ii < balances.size(); ++ii) {
			xData[ii] = (double) ii;
			y1Data[ii] = Math.floor(balances.get(ii).retirementAssets.doubleValue() / 1000);
			y2Data[ii] = Math.floor(balances.get(ii).investmentAssets.doubleValue() / 1000);
			y3Data[ii] = Math.floor(balances.get(ii).cashAssets.doubleValue() / 1000);
			y4Data[ii] = Math.floor(balances.get(ii).liabilities.doubleValue() / 1000);
		}

		XYChart chart = new XYChartBuilder().width(800).height(600) //
				.title("Net Worth Chart") //
				.xAxisTitle("Month").yAxisTitle("K$").build();

		// Customize Chart
		XYStyler styler = chart.getStyler();
		styler.setLegendPosition(LegendPosition.InsideNW);
		styler.setAxisTitlesVisible(false);
		styler.setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Area);

		// Series
		chart.addSeries("Retirement", xData, y1Data);
		chart.addSeries("Investments", xData, y2Data);
		chart.addSeries("Cash", xData, y3Data);
		chart.addSeries("Liabilities", xData, y4Data);

		new SwingWrapper<XYChart>(chart).displayChart();
	}

	public static void showChart2(List<StatusForDateModel> balances) {

		double[] xData = new double[balances.size()];
		double[] yData = new double[balances.size()];

		for (int ii = 0; ii < balances.size(); ++ii) {
			xData[ii] = (double) ii;
			yData[ii] = Math.floor(balances.get(ii).netWorth.doubleValue() / 1000);
		}

		XYChart chart = QuickChart.getChart( //
				"Monthly Net Worth", "Month", "$K", "Net Worth", xData, yData);

		new SwingWrapper<XYChart>(chart).displayChart();

	}
}
