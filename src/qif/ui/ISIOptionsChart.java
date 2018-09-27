package qif.ui;

import java.awt.Color;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetUtilities;

import qif.data.Account;
import qif.data.QDate;
import qif.data.StockOption;
import qif.ui.MainWindow.IntervalUnit;

class StatusForOptionsModel {
	public QDate date;
	public double value;

	public StatusForOptionsModel(QDate date) {
		this.date = date;
	}
}

class ISIOptionsChartData {
	public QDate[] dates;
	public double[][] optionsValues;

//TODO no, no, no
	QDate start, end;

	public ISIOptionsChartData(QDate start, QDate end) {
		this(start, end, MainWindow.instance.reportUnit);
	}

	public ISIOptionsChartData(QDate start, QDate end, IntervalUnit units) {
		this.start = start;
		this.end = end;
		List<StatusForOptionsModel> optionsData = getOptionsData(start, end, units);

		getData(optionsData);
	}

	private void getData(List<StatusForOptionsModel> optionsData) {
		Account optionsAccount = Account.findAccount("ISI Options");

		this.dates = new QDate[optionsData.size()];
		this.optionsValues = new double[1][optionsData.size()];

		for (int ii = 0; ii < optionsData.size(); ++ii) {
			this.dates[ii] = optionsData.get(ii).date;
			QDate curDate = this.dates[ii];

			BigDecimal bal = BigDecimal.ZERO;

			List<StockOption> opts = StockOption.getOpenOptions(optionsAccount, curDate);
			for (StockOption opt : opts) {
				bal = bal.add(opt.getValueForDate(curDate));
			}

			this.optionsValues[0][ii] = bal.doubleValue();
		}
	}

	/**
	 * Construct a skeleton list of values of the necessary size to be filled in
	 * later
	 */
	public static List<StatusForOptionsModel> getOptionsData() {
		return getOptionsData(null, MainWindow.instance.asOfDate, //
				MainWindow.instance.reportUnit);
	}

	/**
	 * Construct a skeleton list of values of the necessary size to be filled in
	 * later
	 */
	static List<StatusForOptionsModel> getOptionsData(QDate start, QDate end) {
		return getOptionsData(start, end, MainWindow.instance.reportUnit);
	}

	/**
	 * Construct a skeleton list of values of the necessary size to be filled in
	 * later
	 */
	static List<StatusForOptionsModel> getOptionsData( //
			QDate start, QDate end, MainWindow.IntervalUnit unit) {
		List<StatusForOptionsModel> balances = new ArrayList<StatusForOptionsModel>();

		QDate d = (start != null) ? start : QDate.today(); // getFirstTransactionDate();
		QDate lastTxDate = (end != null) ? end : QDate.today(); // getLastTransactionDate();

		int year = d.getYear();
		int month = d.getMonth();
		d = QDate.getDateForEndOfMonth(year, month);

		do {
			StatusForOptionsModel b = new StatusForOptionsModel(d);

			balances.add(b);

			d = unit.nextDate(d);
		} while (d.compareTo(lastTxDate) <= 0);

		return balances;
	}
}

public class ISIOptionsChart {
	JFreeChart chart = null;

	public JPanel createChartPanel() {
		this.chart = createChart();

		ChartPanel chPanel = new ChartPanel(this.chart);
		chPanel.setMouseWheelEnabled(true);

		return chPanel;
	}

	private JFreeChart createChart() {
		QDate start = MainWindow.instance.getIntervalStart();
		QDate end = MainWindow.instance.getIntervalEnd();

		ISIOptionsChartData optionsData = new ISIOptionsChartData(start, end);

		CategoryDataset optionsDataset = DatasetUtilities.createCategoryDataset( //
				"ISI Options", "Value", optionsData.optionsValues);

		LineAndShapeRenderer lineRenderer = new LineAndShapeRenderer();

		CategoryAxis xAxis = new CategoryAxis("Type");

		NumberAxis yAxis = new NumberAxis("Value");
		yAxis.setAutoRangeIncludesZero(false);

		CategoryPlot plot = new CategoryPlot(optionsDataset, xAxis, yAxis, lineRenderer);
		plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

		JFreeChart chart = new JFreeChart("ISI Options", JFreeChart.DEFAULT_TITLE_FONT, plot, true);

		lineRenderer.setSeriesShapesVisible(0, false);

		// X-Axis Labels will be inclined at 45degree
		xAxis.setLabel("Date");
		xAxis.tickLabels = optionsData.dates;
		xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		//xAxis.setVisible(optionsData.dates.length <= 100);

		// Y-Axis range will be set automatically based on the supplied data
		yAxis.setLabel("$1000");
		yAxis.setAutoRange(true);

		lineRenderer.setSeriesPaint(0, new Color(0, 0, 0));

		return chart;
	}

	public void update() {
		QDate start = MainWindow.instance.getIntervalStart();
		QDate end = MainWindow.instance.getIntervalEnd();

		ISIOptionsChartData optionsData = new ISIOptionsChartData(start, end);
		CategoryDataset optionsDataset = DatasetUtilities.createCategoryDataset( //
				"ISI Options", "Value", optionsData.optionsValues);

		this.chart.getCategoryPlot().setDataset(optionsDataset);

		CategoryAxis xAxis = this.chart.getCategoryPlot().getDomainAxis();
		xAxis.tickLabels = optionsData.dates;
		//xAxis.setVisible(optionsData.dates.length <= 100);
	}
}