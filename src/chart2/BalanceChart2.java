package chart2;

import java.awt.Color;
import java.util.List;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
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
import qif.ui.MainWindow;

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

class BalanceChartData2 {
	final SectionInfo[] sectionInfo = SectionInfo.sectionInfo;

	final String[] category = new String[] { //
			"Loan", "Credit Card", "Asset", "Retirement", "Investment", "Bank" //
	};

	double[] xData;
	double[][] yData;

	public BalanceChartData2(QDate start, QDate end) {
		List<StatusForDateModel> balances = NetWorthReporter.getNetWorthData( //
				start, end, MainWindow.instance.reportUnit);

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

public class BalanceChart2 {
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
		this.chart = createChart2();

		ChartPanel chPanel = new ChartPanel(this.chart);
		chPanel.setMouseWheelEnabled(true);

		return chPanel;
	}

	public void update() {
		QDate start = MainWindow.instance.getIntervalStart();
		QDate end = MainWindow.instance.getIntervalEnd();

		BalanceChartData2 balanceData = new BalanceChartData2(start, end);

		CategoryDataset dataset = DatasetUtilities.createCategoryDataset( //
				"Series ", "Type ", balanceData.yData);
		this.chart.getCategoryPlot().setDataset(dataset);
	}

	private JFreeChart createChart() {
		QDate start = MainWindow.instance.getIntervalStart();
		QDate end = MainWindow.instance.getIntervalEnd();

		BalanceChartData2 balanceData = new BalanceChartData2(start, end);

		CategoryDataset dataset = DatasetUtilities.createCategoryDataset( //
				"Series ", "Type ", balanceData.yData);

		// Plot type: Stacked Bar Graph.
		JFreeChart chart = ChartFactory.createStackedBarChart(//
				"Title", "", "", dataset, PlotOrientation.VERTICAL, true, true, false);

		// Set current date as the title
		DateTitle dateTitle = new DateTitle();
		chart.addSubtitle(dateTitle);

		CategoryPlot plot = chart.getCategoryPlot();

		// 4 series to show. (Approved, Negotiaion, Cancelled, Completed)
		plot.getRenderer().setSeriesPaint(0, new Color(0, 255, 0));
		plot.getRenderer().setSeriesPaint(1, new Color(0, 0, 255));
		plot.getRenderer().setSeriesPaint(2, new Color(255, 0, 0));
		plot.getRenderer().setSeriesPaint(3, new Color(255, 255, 0));

		// X-Axis Labels will be inclined at 45degree
		CategoryAxis xAxis = plot.getDomainAxis();
		xAxis.setLabel("Date");
		xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

		// Y-Axis range will be set automatically based on the supplied data
		ValueAxis rangeAxis = plot.getRangeAxis();
		rangeAxis.setLabel("$");
		rangeAxis.setAutoRange(true);

		// if there is only one bar, it does not occupy the entire width
		BarRenderer renderer = (BarRenderer) plot.getRenderer();
		renderer.setMaximumBarWidth(.1);

		renderer.setBaseItemLabelGenerator(new CategoryLabelGenerator());
		renderer.setBaseItemLabelsVisible(true);

		ItemLabelPosition p = new ItemLabelPosition(ItemLabelAnchor.CENTER, TextAnchor.BOTTOM_CENTER);
		renderer.setPositiveItemLabelPositionFallback(p);

		return chart;
	}

	private JFreeChart createChart2() {
		QDate start = MainWindow.instance.getIntervalStart();
		QDate end = MainWindow.instance.getIntervalEnd();

		BalanceChartData2 balanceData = new BalanceChartData2(start, end);

		CategoryDataset dataset = DatasetUtilities.createCategoryDataset( //
				"Series ", "Type ", balanceData.yData);

		StackedBarRenderer barRenderer = new StackedBarRenderer();
		LineAndShapeRenderer lineRenderer = new LineAndShapeRenderer();

		CategoryAxis xAxis = new CategoryAxis("Type");
		NumberAxis yAxis = new NumberAxis("Value");
		yAxis.setAutoRangeIncludesZero(false);

		CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, barRenderer);
		plot.setDataset(1, dataset);
		plot.setRenderer(1, lineRenderer);
		plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

		JFreeChart chart = new JFreeChart("Test", JFreeChart.DEFAULT_TITLE_FONT, plot, true);

//		CategoryAxis xAxis = new CategoryAxis("Type");
//		NumberAxis yAxis = new NumberAxis("Value");
//
//		CategoryPlot plot = new CategoryPlot( //
//				dataset, //
//				new CategoryAxis("$"), //
//				new ValueAxis("Date", ), //
//				rend);
//		IntervalXYDataset data1 = this.createDataset1();
//		XYItemRenderer renderer1 = new VerticalXYBarRenderer(0.20);
//		renderer1.setToolTipGenerator(new TimeSeriesToolTipGenerator("d-MMM-yyyy", "0.00"));
//		XYPlot subplot1 = new XYPlot(data1, null, null, renderer1);
//		// create subplot 2...
//		XYDataset data2 = this.createDataset2();
//		XYItemRenderer renderer2 = new StandardXYItemRenderer();
//		renderer2.setToolTipGenerator(new TimeSeriesToolTipGenerator("d-MMM-yyyy", "0.00"));
//		XYPlot subplot2 = new XYPlot(data2, null, null, renderer2);

		// Plot type: Stacked Bar Graph.
//		JFreeChart chart = ChartFactory.createStackedBarChart(//
//				"Title", "", "", dataset, PlotOrientation.VERTICAL, true, true, false);

		// Set current date as the title
		DateTitle dateTitle = new DateTitle();
		chart.addSubtitle(dateTitle);

		// CategoryPlot plot = chart.getCategoryPlot();

		// 4 series to show. (Approved, Negotiaion, Cancelled, Completed)
		barRenderer.setSeriesPaint(0, new Color(0, 255, 0));
		barRenderer.setSeriesPaint(1, new Color(0, 0, 255));
		barRenderer.setSeriesPaint(2, new Color(255, 0, 0));
		barRenderer.setSeriesPaint(3, new Color(255, 255, 0));

		// X-Axis Labels will be inclined at 45degree
		// CategoryAxis xAxis = plot.getDomainAxis();
		xAxis.setLabel("Date");
		xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

		// Y-Axis range will be set automatically based on the supplied data
		// ValueAxis rangeAxis = plot.getRangeAxis();
		yAxis.setLabel("$");
		yAxis.setAutoRange(true);

		// if there is only one bar, it does not occupy the entire width
		// BarRenderer renderer = (BarRenderer) plot.getRenderer();
		barRenderer.setMaximumBarWidth(.1);

		barRenderer.setBaseItemLabelGenerator(new CategoryLabelGenerator());
		barRenderer.setBaseItemLabelsVisible(true);

		ItemLabelPosition p = new ItemLabelPosition(ItemLabelAnchor.CENTER, TextAnchor.BOTTOM_CENTER);
		barRenderer.setPositiveItemLabelPositionFallback(p);

		return chart;
	}
}