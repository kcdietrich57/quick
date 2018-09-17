package qif.ui;

import java.awt.Color;
import java.util.List;

import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.StackedAreaRenderer;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.title.DateTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.ui.TextAnchor;

import qif.data.QDate;
import qif.report.NetWorthReporter;
import qif.report.StatusForDateModel;
import qif.report.StatusForDateModel.Section;
import qif.report.StatusForDateModel.SectionInfo;

class CategoryLabelGenerator implements CategoryItemLabelGenerator {
	public String generateRowLabel(CategoryDataset dataset, int row) {
		return "";
	}

	public String generateColumnLabel(CategoryDataset dataset, int column) {
		return "";
	}

	public String generateLabel(CategoryDataset dataset, int row, int column) {
		Number d = dataset.getValue(row, column);

		if (d.intValue() == 0) {
			return "";
		} else {
			return d.toString();
		}
	}
}

class BalanceChartData {
	final SectionInfo[] sectionInfo = SectionInfo.sectionInfo;

	final String[] category = new String[] { //
			"Loan", "Credit Card", "Asset", "Investment", "Retirement", "Bank" //
	};
	final int[] catOrder = { 4, 2, 3, 5, 0, 1 };

	double[] xData;
	double[][] yData;
	double[][] yDataNetWorth;

	public BalanceChartData(QDate start, QDate end) {
		List<StatusForDateModel> balances = NetWorthReporter.getNetWorthData( //
				start, end, MainWindow.instance.reportUnit);

		setModel(balances);
	}

	private void setModel(List<StatusForDateModel> balances) {
		this.xData = new double[balances.size()];
		this.yData = new double[this.sectionInfo.length][balances.size()];
		this.yDataNetWorth = new double[1][balances.size()];

		for (int dateIndex = 0; dateIndex < balances.size(); ++dateIndex) {
			Section[] sections = balances.get(dateIndex).sections;

			this.xData[dateIndex] = (double) dateIndex;
			this.yDataNetWorth[0][dateIndex] = 0.0;

			for (int sectionNum = 0; sectionNum < sections.length; ++sectionNum) {
				int idx = catOrder[sectionNum];

				double val = Math.floor(sections[idx].subtotal.floatValue() / 1000);

				this.yData[idx][dateIndex] = val;
				this.yDataNetWorth[0][dateIndex] += val;
			}

			// yData[sections.length][dateIndex] = //
			// Math.floor(balances.get(dateIndex).netWorth.floatValue() / 1000);
		}
	}
}

public class BalanceChart {
	static double[][] data = new double[][] { //
			{ 1.0, 4.0, 3.0, 5.0, 5.0, 7.0, 7.0, 8.0 }, //
			{ 5.0, 7.0, 6.0, 8.0, 4.0, 4.0, 2.0, 1.0 }, //
			{ 4.0, 3.0, 2.0, 3.0, 6.0, 3.0, 4.0, 3.0 } //
	};

	static double[] average;
	static {
		average = new double[data[0].length];

		for (int ii = 0; ii < average.length; ++ii) {
			average[ii] = (data[0][ii] + data[1][ii] + data[2][ii]) / 3.0;
		}
	}

	JFreeChart chart = null;

	public JPanel createChartPanel() {
		this.chart = createChart();

		ChartPanel chPanel = new ChartPanel(this.chart);
		chPanel.setMouseWheelEnabled(true);

		return chPanel;
	}

	public void update() {
		QDate start = MainWindow.instance.getIntervalStart();
		QDate end = MainWindow.instance.getIntervalEnd();

		BalanceChartData balanceData = new BalanceChartData(start, end);

		CategoryDataset catDataset = DatasetUtilities.createCategoryDataset( //
				"Series ", "Type ", balanceData.yData);
		CategoryDataset networthDataset = DatasetUtilities.createCategoryDataset( //
				"SeriesNW ", "TypeNW ", balanceData.yDataNetWorth);

		this.chart.getCategoryPlot().setDataset(0, catDataset);
		this.chart.getCategoryPlot().setDataset(1, networthDataset);
	}

	private JFreeChart createChart() {
		QDate start = MainWindow.instance.getIntervalStart();
		QDate end = MainWindow.instance.getIntervalEnd();

		BalanceChartData balanceData = new BalanceChartData(start, end);

		CategoryDataset catDataset = DatasetUtilities.createCategoryDataset( //
				"Series ", "Type ", balanceData.yData);
		CategoryDataset networthDataset = DatasetUtilities.createCategoryDataset( //
				"SeriesNW ", "TypeNW ", balanceData.yDataNetWorth);

		StackedAreaRenderer areaRenderer = new StackedAreaRenderer();
		StackedBarRenderer barRenderer = new StackedBarRenderer();
		LineAndShapeRenderer lineRenderer = new LineAndShapeRenderer();

		CategoryAxis xAxis = new CategoryAxis("Type");
		NumberAxis yAxis = new NumberAxis("Value");
		yAxis.setAutoRangeIncludesZero(false);

		CategoryPlot plot = new CategoryPlot(catDataset, xAxis, yAxis, areaRenderer);
		plot.setDataset(1, networthDataset);
		plot.setRenderer(1, lineRenderer);
		plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

		JFreeChart chart = new JFreeChart("Balances", JFreeChart.DEFAULT_TITLE_FONT, plot, true);

		// Set current date as the title
		DateTitle dateTitle = new DateTitle();
		dateTitle.setText(MainWindow.instance.asOfDate.toString());
		chart.addSubtitle(dateTitle);

		lineRenderer.setSeriesShapesVisible(0, false);

		// 4 series to show. (Approved, Negotiaion, Cancelled, Completed)
//		areaRenderer.setSeriesPaint(0, new Color(0, 255, 0));
//		areaRenderer.setSeriesPaint(1, new Color(40, 0, 0));
//		areaRenderer.setSeriesPaint(2, new Color(80, 0, 0));
//		areaRenderer.setSeriesPaint(3, new Color(120, 0, 0));
//		areaRenderer.setSeriesPaint(4, new Color(160, 0, 0));
//		areaRenderer.setSeriesPaint(5, new Color(200, 0, 0));
//		areaRenderer.setSeriesPaint(6, new Color(0, 0, 255));

		// X-Axis Labels will be inclined at 45degree
		xAxis.setLabel("Date");
		xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

		// Y-Axis range will be set automatically based on the supplied data
		yAxis.setLabel("$");
		yAxis.setAutoRange(true);

		barRenderer.setSeriesPaint(0, new Color(0, 255, 0));
		barRenderer.setSeriesPaint(1, new Color(40, 0, 0));
		barRenderer.setSeriesPaint(2, new Color(80, 0, 0));
		barRenderer.setSeriesPaint(3, new Color(120, 0, 0));
		barRenderer.setSeriesPaint(4, new Color(160, 255, 0));
		barRenderer.setSeriesPaint(5, new Color(200, 255, 0));
		barRenderer.setSeriesPaint(6, new Color(0, 0, 255));

		// if there is only one bar, it does not occupy the entire width
		barRenderer.setMaximumBarWidth(.1);

		barRenderer.setBaseItemLabelGenerator(new CategoryLabelGenerator());
		barRenderer.setBaseItemLabelsVisible(true);

		ItemLabelPosition p = new ItemLabelPosition(ItemLabelAnchor.CENTER, TextAnchor.BOTTOM_CENTER);
		barRenderer.setPositiveItemLabelPositionFallback(p);

		return chart;
	}
}