package qif.ui.chart;

import java.awt.Color;

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

import qif.data.QDate;
import qif.ui.MainWindow;

public class NetWorthChart {
	JFreeChart chart = null;

	public JPanel createChartPanel() {
		this.chart = createChart();

		ChartPanel chPanel = new ChartPanel(this.chart);
		// chPanel.setMouseWheelEnabled(true);

		return chPanel;
	}

	private JFreeChart createChart() {
		QDate start = MainWindow.instance.getIntervalStart();
		QDate end = MainWindow.instance.getIntervalEnd();

		BalanceChartData balanceData = new BalanceChartData(start, end);

		CategoryDataset networthDataset = DatasetUtilities.createCategoryDataset( //
				"NetWorth", "Value", balanceData.netWorthValues);

		LineAndShapeRenderer lineRenderer = new LineAndShapeRenderer();
		lineRenderer.setSeriesShapesVisible(0, false);
		lineRenderer.setSeriesPaint(0, Color.BLACK);

		CategoryAxis xAxis = new CategoryAxis("Type");
		xAxis.setLabel("Date");
		xAxis.tickLabels = balanceData.dates;
		xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		// xAxis.setVisible(balanceData.dates.length <= 100);

		NumberAxis yAxis = new NumberAxis("Value");
		yAxis.setAutoRangeIncludesZero(false);
		yAxis.setLabel("$1000");
		yAxis.setAutoRange(true);

		CategoryPlot plot = new CategoryPlot(networthDataset, xAxis, yAxis, lineRenderer);
		plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

		JFreeChart chart = new JFreeChart("Net Worth", JFreeChart.DEFAULT_TITLE_FONT, plot, true);

		return chart;
	}

	public void update() {
		QDate start = MainWindow.instance.getIntervalStart();
		QDate end = MainWindow.instance.getIntervalEnd();

		BalanceChartData balanceData = new BalanceChartData(start, end);

		CategoryDataset networthDataset = DatasetUtilities.createCategoryDataset( //
				"NetWorth", "Value", balanceData.netWorthValues);

		this.chart.getCategoryPlot().setDataset(networthDataset);

		CategoryAxis xAxis = this.chart.getCategoryPlot().getDomainAxis();
		xAxis.tickLabels = balanceData.dates;
		// xAxis.setVisible(balanceData.dates.length <= 100);
	}
}
