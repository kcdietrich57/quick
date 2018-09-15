package qif.ui;

import java.util.List;

import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.style.CategoryStyler;
import org.knowm.xchart.style.Styler.LegendPosition;

import qif.data.QDate;
import qif.report.NetWorthReporter;
import qif.report.StatusForDateModel;
import qif.report.StatusForDateModel.Section;
import qif.report.StatusForDateModel.SectionInfo;

public class BalanceChart {

	private class BalanceChartData {
		final SectionInfo[] sectionInfo = SectionInfo.sectionInfo;

		final String[] category = new String[] { //
				"Loan", "Credit Card", "Asset", "Retirement", "Investment", "Bank" //
		};

		double[] xData;
		double[][] yData;

		public BalanceChartData(QDate start, QDate end) {
			List<StatusForDateModel> balances = NetWorthReporter.getMonthlyNetWorth(start, end);

			setModel(balances);
		}

		private void setModel(List<StatusForDateModel> balances) {
			this.xData = new double[balances.size()];
			this.yData = new double[this.sectionInfo.length][balances.size()];

			for (int dateIndex = 0; dateIndex < balances.size(); ++dateIndex) {
				Section[] sections = balances.get(dateIndex).sections;

				this.xData[dateIndex] = (double) dateIndex;

				for (int sectionNum = 0; sectionNum < sections.length; ++sectionNum) {
					this.yData[sectionNum][dateIndex] = //
							Math.floor(sections[sectionNum].subtotal.floatValue() / 1000);
				}

				// yData[sections.length][dateIndex] = //
				// Math.floor(balances.get(dateIndex).netWorth.floatValue() / 1000);
			}
		}
	}

	private BalanceChartData balanceData = null;
	public CategoryChart chart = null;

	public void display() {
		new SwingWrapper<CategoryChart>(this.chart).displayChart();
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

		for (int sectionNum = 0; sectionNum < balanceData.sectionInfo.length; ++sectionNum) {
			for (int catNum = 0; catNum < balanceData.sectionInfo.length; ++catNum) {
				if (balanceData.sectionInfo[catNum].label.equals(balanceData.category[sectionNum])) {
					chart.addSeries(balanceData.sectionInfo[catNum].label, //
							balanceData.xData, balanceData.yData[catNum]);
				}
			}
		}
	}

	public void update(QDate start, QDate end) {
		this.balanceData = new BalanceChartData(start, end);

		for (int sectionNum = 0; sectionNum < balanceData.sectionInfo.length; ++sectionNum) {
			for (int catNum = 0; catNum < balanceData.sectionInfo.length; ++catNum) {
				if (balanceData.sectionInfo[catNum].label.equals(balanceData.category[sectionNum])) {
					chart.updateCategorySeries(balanceData.sectionInfo[catNum].label, //
							balanceData.xData, balanceData.yData[catNum], null);
				}
			}
		}
	}
}