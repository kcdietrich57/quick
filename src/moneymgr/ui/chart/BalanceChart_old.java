package moneymgr.ui.chart;

import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.style.CategoryStyler;
import org.knowm.xchart.style.Styler.LegendPosition;

import moneymgr.ui.MainWindow;
import moneymgr.util.QDate;

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