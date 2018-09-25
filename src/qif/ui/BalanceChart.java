package qif.ui;

import java.awt.Color;
import java.util.Arrays;

import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.StackedAreaRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DatasetUtilities;

import qif.data.QDate;

public class BalanceChart {
	private static final int RETIRE = 0;
	private static final int ASSET = 1;
	private static final int INVEST = 2;
	private static final int BANK = 3;
	private static final int CREDIT = 4;
	private static final int LOAN = 5;

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

		DefaultCategoryDataset catDataset = //
				(DefaultCategoryDataset) DatasetUtilities.createCategoryDataset( //
						"AccountType", "Value", balanceData.accountCategoryValues);

		CategoryDataset networthDataset = DatasetUtilities.createCategoryDataset( //
				"NetWorth", "Value", balanceData.netWorthValues);
		catDataset.setRowKeys(Arrays.asList(balanceData.accountCategoryNames));

		StackedAreaRenderer areaRenderer = new StackedAreaRenderer();
		areaRenderer.setSeriesPaint(CREDIT, Color.YELLOW);
		areaRenderer.setSeriesPaint(LOAN, Color.MAGENTA);
		areaRenderer.setSeriesPaint(ASSET, Color.GREEN);
		areaRenderer.setSeriesPaint(RETIRE, Color.DARK_GRAY);
		areaRenderer.setSeriesPaint(BANK, Color.RED);
		areaRenderer.setSeriesPaint(INVEST, Color.BLUE);

		LineAndShapeRenderer lineRenderer = new LineAndShapeRenderer();
		lineRenderer.setSeriesShapesVisible(0, false);
		lineRenderer.setSeriesPaint(0, Color.BLACK);

		CategoryAxis xAxis = new CategoryAxis("Type");
		xAxis.setLabel("Date");
		xAxis.tickLabels = balanceData.dates;
		xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		xAxis.setVisible(balanceData.dates.length <= 100);

		NumberAxis yAxis = new NumberAxis("Value");
		yAxis.setAutoRangeIncludesZero(false);
		yAxis.setLabel("$1000");
		yAxis.setAutoRange(true);

		CategoryPlot plot = new CategoryPlot(catDataset, xAxis, yAxis, areaRenderer);
		plot.setDataset(1, networthDataset);
		plot.setRenderer(1, lineRenderer);
		plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

		JFreeChart chart = new JFreeChart("Balances", JFreeChart.DEFAULT_TITLE_FONT, plot, true);

		return chart;
	}

	public void update() {
		QDate start = MainWindow.instance.getIntervalStart();
		QDate end = MainWindow.instance.getIntervalEnd();

		BalanceChartData balanceData = new BalanceChartData(start, end);

		DefaultCategoryDataset catDataset = //
				(DefaultCategoryDataset) DatasetUtilities.createCategoryDataset( //
						"AccountType", "Value", balanceData.accountCategoryValues);

		CategoryDataset networthDataset = DatasetUtilities.createCategoryDataset( //
				"NetWorth", "Value", balanceData.netWorthValues);
		catDataset.setRowKeys(Arrays.asList(balanceData.accountCategoryNames));

		CategoryPlot plot = this.chart.getCategoryPlot();
		plot.setDataset(0, catDataset);
		plot.setDataset(1, networthDataset);

		CategoryAxis xAxis = this.chart.getCategoryPlot().getDomainAxis();
		xAxis.tickLabels = balanceData.dates;
		xAxis.setVisible(balanceData.dates.length <= 100);
	}
}