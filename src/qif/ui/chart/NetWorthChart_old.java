package qif.ui.chart;

import java.util.List;

import org.knowm.xchart.QuickChart;
import org.knowm.xchart.XYChart;

import qif.data.QDate;
import qif.report.StatusForDateModel;
import qif.ui.MainWindow;

public class NetWorthChart_old {

	private static class NetWorthChartData {
		private double[] xData;
		private double[] yData;

		public NetWorthChartData(QDate start, QDate end) {
			List<StatusForDateModel> balances = //
					BalanceChartData.getNetWorthData(start, end);

			setModel(balances);
		}

		private void setModel(List<StatusForDateModel> balances) {
			this.xData = new double[balances.size()];
			this.yData = new double[balances.size()];

			for (int ii = 0; ii < balances.size(); ++ii) {
				this.xData[ii] = (double) ii;
				this.yData[ii] = Math.floor(balances.get(ii).netWorth.doubleValue() / 1000);
			}
		}
	}

	private NetWorthChartData netWorthData = null;
	public XYChart chart = null;

	public void create() {
		QDate start = MainWindow.instance.getIntervalStart();
		QDate end = MainWindow.instance.getIntervalEnd();

		create(start, end);
	}

	public void create(QDate start, QDate end) {
		this.netWorthData = new NetWorthChartData(start, end);

		this.chart = QuickChart.getChart( //
				"Net Worth", "Month", "$K", "Net Worth", //
				this.netWorthData.xData, this.netWorthData.yData);
	}

	public void update() {
		QDate start = MainWindow.instance.getIntervalStart();
		QDate end = MainWindow.instance.getIntervalEnd();

		update(start, end);
	}

	public void update(QDate start, QDate end) {
		this.netWorthData = new NetWorthChartData(start, end);

		this.chart.updateXYSeries("Net Worth", this.netWorthData.xData, this.netWorthData.yData, null);
	}
}
