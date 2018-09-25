package qif.ui;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.style.CategoryStyler;
import org.knowm.xchart.style.Styler.LegendPosition;

import qif.data.Account;
import qif.data.QDate;
import qif.data.StockOption;

public class BalanceChart_old {

//	private static class BalanceChartData {
//		final AccountCategoryInfo[] sectionInfo = AccountCategoryInfo.accountCategoryInfo;
//
//		final String[] category = new String[] { //
//				"Loan", "Credit Card", "Asset", "Retirement", "Investment", "Bank" //
//		};
//
//		double[] xData;
//		double[][] yData;
//
//		public BalanceChartData(QDate start, QDate end) {
//			List<StatusForDateModel> balances = //
//					NetWorthReporter.getNetWorthData(start, end);
//
//			setModel(balances);
//		}
//
//		private void setModel(List<StatusForDateModel> balances) {
//			this.xData = new double[balances.size()];
//			this.yData = new double[this.sectionInfo.length][balances.size()];
//
//			for (int dateIndex = 0; dateIndex < balances.size(); ++dateIndex) {
//				Section[] sections = balances.get(dateIndex).sections;
//
//				this.xData[dateIndex] = (double) dateIndex;
//
//				for (int sectionNum = 0; sectionNum < sections.length; ++sectionNum) {
//					this.yData[sectionNum][dateIndex] = //
//							Math.floor(sections[sectionNum].subtotal.floatValue() / 1000);
//				}
//
//				// yData[sections.length][dateIndex] = //
//				// Math.floor(balances.get(dateIndex).netWorth.floatValue() / 1000);
//			}
//		}
//	}

	private BalanceChartData balanceData = null;
	public CategoryChart chart = null;

	public void create() {
		QDate start = MainWindow.instance.getIntervalStart();
		QDate end = MainWindow.instance.getIntervalEnd();

		create(start, end);
	}

	public void update() {
		QDate start = MainWindow.instance.getIntervalStart();
		QDate end = MainWindow.instance.getIntervalEnd();

		update(start, end);
	}

	public void create(QDate start, QDate end) {
		this.balanceData = new BalanceChartData(start, end);

		this.chart = new CategoryChartBuilder().width(800).height(600) //
				.title("Net Worth Chart") //
				.xAxisTitle("Month").yAxisTitle("K$").build();

		// Customize Chart
		CategoryStyler styler = chart.getStyler();
		styler.setLegendPosition(LegendPosition.InsideNW);
		styler.setAxisTitlesVisible(false);
		styler.setStacked(true);

//		for (int sectionNum = 0; sectionNum < balanceData.accountCategoryNames.length; ++sectionNum) {
//			for (int catNum = 0; catNum < balanceData.accountCategoryNames.length; ++catNum) {
//				if (AccountCategoryInfo.accountCategoryInfo[catNum].label.equals( //
//						balanceData.accountCategoryNames[sectionNum])) {
//					chart.addSeries(AccountCategoryInfo.accountCategoryInfo[catNum].label, //
//							balanceData.dates, balanceData.accountCategoryValues[catNum]);
//				}
//			}
//		}
	}

	public void update(QDate start, QDate end) {
		this.balanceData = new BalanceChartData(start, end);

//		for (int sectionNum = 0; sectionNum < balanceData.sectionInfo.length; ++sectionNum) {
//			for (int catNum = 0; catNum < balanceData.sectionInfo.length; ++catNum) {
//				if (balanceData.sectionInfo[catNum].label.equals(balanceData.category[sectionNum])) {
//					chart.updateCategorySeries(balanceData.sectionInfo[catNum].label, //
//							balanceData.xData, balanceData.yData[catNum], null);
//				}
//			}
//		}
	}
}

class ISIOptionsChart_old {
	private class ISIOptionsChartData {
		private double[] xData;
		private double[] yData;

		public ISIOptionsChartData(QDate start, QDate end) {
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

			this.xData = new double[dates.size()];
			this.yData = new double[dates.size()];

			for (int ii = 0; ii < dates.size(); ++ii) {
				xData[ii] = (double) ii;
				BigDecimal bd = high.get(ii);
				double d = (bd != null) ? bd.doubleValue() : 0.0;
				yData[ii] = Math.floor(d / 1000);
			}
		}
	}

	private ISIOptionsChartData netWorthData = null;
	public XYChart chart = null;

	public void create() {
		QDate start = MainWindow.instance.getIntervalStart();
		QDate end = MainWindow.instance.getIntervalEnd();

		create(start, end);
	}

	public void create(QDate start, QDate end) {
		this.netWorthData = new ISIOptionsChartData(start, end);

		this.chart = QuickChart.getChart( //
				"ISI Options", "Month", "$K", "Value", //
				this.netWorthData.xData, this.netWorthData.yData);
	}

	public void update() {
		QDate start = MainWindow.instance.getIntervalStart();
		QDate end = MainWindow.instance.getIntervalEnd();

		update(start, end);
	}

	public void update(QDate start, QDate end) {
		this.netWorthData = new ISIOptionsChartData(start, end);

		this.chart.updateXYSeries("Value", //
				this.netWorthData.xData, this.netWorthData.yData, null);
	}

	public XYChart createISIOptionsChart(QDate start, QDate end) {
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
}