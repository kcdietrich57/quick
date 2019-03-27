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
import org.jfree.chart.renderer.category.StackedAreaRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DatasetUtilities;

import qif.data.AccountCategory;
import qif.data.QDate;
import qif.ui.MainWindow;
import qif.ui.UIConstants;

public class BalanceChart {
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
		catDataset.setRowKeys(balanceData.accountCategoryLabels);

		StackedAreaRenderer areaRenderer = new StackedAreaRenderer();
		setSeriesColor(areaRenderer);

		LineAndShapeRenderer lineRenderer = new LineAndShapeRenderer();
		lineRenderer.setSeriesShapesVisible(0, false);
		lineRenderer.setSeriesPaint(0, Color.WHITE);

		CategoryAxis xAxis = new CategoryAxis("Type");
		xAxis.setLabel("Date");
		xAxis.tickLabels = balanceData.dates;
		xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		// xAxis.setVisible(balanceData.dates.length <= 100);

		NumberAxis yAxis = new NumberAxis("Value");
		yAxis.setAutoRangeIncludesZero(false);
		yAxis.setLabel("$1000");
		yAxis.setAutoRange(true);

		CategoryPlot plot = new CategoryPlot(catDataset, xAxis, yAxis, areaRenderer);
		plot.setDataset(1, networthDataset);
		plot.setRenderer(1, lineRenderer);
		plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
		plot.setBackgroundPaint(new Color(220, 220, 220));

		JFreeChart chart = new JFreeChart("Balances", JFreeChart.DEFAULT_TITLE_FONT, plot, true);

		return chart;
	}

	private void setSeriesColor(StackedAreaRenderer r) {
		for (int idx = 0; idx < AccountCategory.numCategories(); ++idx) {
			AccountCategory acat = AccountCategory.values()[idx];
			Color c = UIConstants.acctCategoryColor.get(acat);

			r.setSeriesPaint(idx, c);
		}
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
		catDataset.setRowKeys(balanceData.accountCategoryLabels);

		CategoryPlot plot = this.chart.getCategoryPlot();
		plot.setDataset(0, catDataset);
		plot.setDataset(1, networthDataset);

		CategoryAxis xAxis = this.chart.getCategoryPlot().getDomainAxis();
		xAxis.tickLabels = balanceData.dates;
		// xAxis.setVisible(true); //balanceData.dates.length <= 100);
	}
}