package qif.ui;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.style.XYStyler;
import org.knowm.xchart.style.Styler.LegendPosition;

import qif.data.Account;
import qif.data.QDate;
import qif.data.StockOption;
import qif.report.NetWorthReporter;
import qif.report.StatusForDateModel;
import qif.report.NetWorthReporter.Balances;

public class NetWorthChart {

	static final BalanceChart balanceChart = new BalanceChart();

	public static void showBalances() {
		createBalancesChart();
		balanceChart.display();
	}

	public static void updateBalancesChart(CategoryChart chart) {
		QDate end = MainWindow.instance.asOfDate;
		QDate start = end.addDays(-3650);

		balanceChart.update(start, end);
	}

	public static void showISIOptions() {
		XYChart chart = createISIOptionsChart();

		new SwingWrapper<XYChart>(chart).displayChart();
	}

	public static void showNetWorth() {
		XYChart chart = createNetWorthChart();

		new SwingWrapper<XYChart>(chart).displayChart();
	}

	public static XYChart createISIOptionsChart() {
		QDate start = new QDate(1991, 1, 01);
		QDate end = new QDate(2003, 12, 31);

		return createISIOptionsChart(start, end);
	}

	public static XYChart createISIOptionsChart(QDate start, QDate end) {
		if (start == null) {
			start = new QDate(1991, 1, 01);
		}
		if (end == null) {
			end = new QDate(2003, 12, 31);
		}

		Account acct = Account.findAccount("ISI Options");

		List<QDate> dates = new ArrayList<QDate>();
		List<BigDecimal> high = new ArrayList<BigDecimal>();
		List<BigDecimal> low = new ArrayList<BigDecimal>();

		int curYear = 1991;
		int curMonth = 1;
		BigDecimal bdHigh = null;
		BigDecimal bdLow = null;

		while (start.compareTo(end) <= 0) {
			if ((start.getYear() != curYear) || (start.getMonth() != curMonth)) {
				dates.add(new QDate(curYear, curMonth, 1));
				high.add(bdHigh);
				low.add(bdLow);

				bdHigh = bdLow = null;
				curYear = start.getYear();
				curMonth = start.getMonth();
			}

			BigDecimal bal = BigDecimal.ZERO;

			List<StockOption> opts = StockOption.getOpenOptions(acct, start);
			for (StockOption opt : opts) {
				bal = bal.add(opt.getValueForDate(start));
			}

			if ((bdLow == null) || (bal.compareTo(bdLow) < 0)) {
				bdLow = bal;
			}
			if ((bdHigh == null) || (bal.compareTo(bdHigh) > 0)) {
				bdHigh = bal;
			}

			start = start.addDays(1);
		}

		double[] xData = new double[dates.size()];
		double[] yData = new double[dates.size()];

		for (int ii = 0; ii < dates.size(); ++ii) {
			xData[ii] = (double) ii;
			yData[ii] = Math.floor(high.get(ii).doubleValue() / 1000);
		}

		XYChart chart = QuickChart.getChart( //
				"ISI Options", "Month", "$K", "Value", xData, yData);

		return chart;
	}

	public static CategoryChart createBalancesChart() {
		QDate end = MainWindow.instance.asOfDate;
		QDate start = end.addDays(-3650);

		balanceChart.create(start, end);

		return balanceChart.chart;
	}

	public static XYChart createNetWorthChart() {
		QDate endDate = MainWindow.instance.asOfDate;
		QDate startDate = endDate.addDays(-365);

		List<StatusForDateModel> balances = NetWorthReporter.getMonthlyNetWorth(startDate, endDate);

		double[] xData = new double[balances.size()];
		double[] yData = new double[balances.size()];

		for (int ii = 0; ii < balances.size(); ++ii) {
			xData[ii] = (double) ii;
			yData[ii] = Math.floor(balances.get(ii).netWorth.doubleValue() / 1000);
		}

		XYChart chart = QuickChart.getChart( //
				"Monthly Net Worth", "Month", "$K", "Net Worth", xData, yData);

		return chart;
	}

	private static void showChart1(List<Balances> balances) {

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
}