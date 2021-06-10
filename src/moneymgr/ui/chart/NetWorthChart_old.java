package moneymgr.ui.chart;

import java.util.List;

import org.knowm.xchart.QuickChart;
import org.knowm.xchart.XYChart;

import moneymgr.model.MoneyMgrModel;
import moneymgr.report.StatusForDateModel;
import moneymgr.ui.MainWindow;
import moneymgr.util.QDate;

public class NetWorthChart_old {

	private static class NetWorthChartData {
		private double[] xData;
		private double[] yData;

		public NetWorthChartData(MoneyMgrModel model, QDate start, QDate end) {
			BalanceChartData balanceChartData = new BalanceChartData(model, start, end);

			List<StatusForDateModel> balances = //
					balanceChartData.getNetWorthData(start, end);

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

	public final MoneyMgrModel model;
	private NetWorthChartData netWorthData = null;
	public XYChart chart = null;

	public NetWorthChart_old(MoneyMgrModel model) {
		this.model = model;
	}

	public void create() {
		QDate start = MainWindow.instance.getIntervalStart();
		QDate end = MainWindow.instance.getIntervalEnd();

		create(start, end);
	}

	public void create(QDate start, QDate end) {
		this.netWorthData = new NetWorthChartData(this.model, start, end);

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
		this.netWorthData = new NetWorthChartData(this.model, start, end);

		this.chart.updateXYSeries("Net Worth", this.netWorthData.xData, this.netWorthData.yData, null);
	}
}
