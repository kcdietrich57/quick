package moneymgr.ui.chart;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.knowm.xchart.QuickChart;
import org.knowm.xchart.XYChart;

import moneymgr.model.Account;
import moneymgr.model.StockOption;
import moneymgr.ui.MainFrame;
import moneymgr.ui.MainWindow;
import moneymgr.util.QDate;

public class ISIOptionsChart_old {
	private class ISIOptionsChartData {
		private double[] xData;
		private double[] yData;

		public ISIOptionsChartData(QDate start, QDate end) {
			Account acct = MainFrame.appFrame.model.findAccount("ISI Options");

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

		Account acct = MainFrame.appFrame.model.findAccount("ISI Options");

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