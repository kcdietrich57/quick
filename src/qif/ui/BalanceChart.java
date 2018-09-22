package qif.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.StackedAreaRenderer;
import org.jfree.chart.title.DateTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DatasetUtilities;

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
	public final SectionInfo[] sectionInfo = SectionInfo.sectionInfo;

	private final String[] category = new String[] { //
			"Bank", "Asset", "Investment", "Retirement", "Credit Card", "Loan" //
	};
	 int[] catOrder = { 3, 1, 2, 0, 5, 4 };

	public List<String> categoryNames;
	public double[] xData;
	public QDate[] xDates;
	public double[][] yData;
	public double[][] yDataNetWorth;

	public BalanceChartData(QDate start, QDate end) {
		List<StatusForDateModel> balances = NetWorthReporter.getNetWorthData( //
				start, end, MainWindow.instance.reportUnit);

		setModel(balances);
	}

	private void setModel(List<StatusForDateModel> balances) {
		this.xData = new double[balances.size()];
		this.xDates = new QDate[balances.size()];
		this.yData = new double[this.sectionInfo.length][balances.size()];
		this.yDataNetWorth = new double[1][balances.size()];

		this.categoryNames = new ArrayList<String>(catOrder.length);
		for (int ii = 0; ii < 6; ++ii) {
			int idx = catOrder[ii];
			while (this.categoryNames.size() < idx + 1) { this.categoryNames.add(""); }
			this.categoryNames.set(ii, category[idx]);
		}

		for (int dateIndex = 0; dateIndex < balances.size(); ++dateIndex) {
			Section[] sections = balances.get(dateIndex).sections;

			this.xDates[dateIndex] = balances.get(dateIndex).date;
			this.xData[dateIndex] = (double) dateIndex;
			this.yDataNetWorth[0][dateIndex] = 0.0;

			for (int sectionNum = 0; sectionNum < sections.length; ++sectionNum) {
				int idx = catOrder[sectionNum];

				double val = Math.floor(sections[idx].subtotal.floatValue() / 1000);

				this.yData[sectionNum][dateIndex] = val;
				this.yDataNetWorth[0][dateIndex] += val;
			}

			// yData[sections.length][dateIndex] = //
			// Math.floor(balances.get(dateIndex).netWorth.floatValue() / 1000);
		}
	}
}

public class BalanceChart {
	static double[][] data;
	static double[] average;

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

		BalanceChartData balanceData = new BalanceChartData(start, end);

		DefaultCategoryDataset catDataset = //
				(DefaultCategoryDataset)DatasetUtilities.createCategoryDataset( //
				"AccountType", "Value", balanceData.yData);
		CategoryDataset networthDataset = DatasetUtilities.createCategoryDataset( //
				"NetWorth", "Value", balanceData.yDataNetWorth);

		List<String> categoryNames = balanceData.categoryNames;
		catDataset.setRowKeys(categoryNames);
		
		StackedAreaRenderer areaRenderer = new StackedAreaRenderer();
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

		// X-Axis Labels will be inclined at 45degree
		xAxis.setLabel("Date");
		xAxis.tickLabels = balanceData.xDates;
		xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

		// Y-Axis range will be set automatically based on the supplied data
		yAxis.setLabel("$1000");
		yAxis.setAutoRange(true);

		lineRenderer.setSeriesPaint(0, new Color(0, 0, 0));

		areaRenderer.setSeriesPaint(0, new Color(0, 0, 255));
		areaRenderer.setSeriesPaint(1, new Color(0, 255, 0));
		areaRenderer.setSeriesPaint(2, new Color(0, 255, 255));
		areaRenderer.setSeriesPaint(3, new Color(255, 0, 0));
		areaRenderer.setSeriesPaint(4, new Color(255, 0, 255));
		areaRenderer.setSeriesPaint(5, new Color(255, 255, 0));
//		areaRenderer.setSeriesPaint(6, new Color(255, 255, 255));

		return chart;
	}

	public void update() {
		QDate start = MainWindow.instance.getIntervalStart();
		QDate end = MainWindow.instance.getIntervalEnd();

		BalanceChartData balanceData = new BalanceChartData(start, end);

		DefaultCategoryDataset catDataset = //
				(DefaultCategoryDataset)DatasetUtilities.createCategoryDataset( //
				"AccountType", "Value", balanceData.yData);
		CategoryDataset networthDataset = DatasetUtilities.createCategoryDataset( //
				"NetWorth", "Value", balanceData.yDataNetWorth);

		List<String> categoryNames = balanceData.categoryNames;
		catDataset.setRowKeys(categoryNames);

		this.chart.getCategoryPlot().setDataset(0, catDataset);
		this.chart.getCategoryPlot().setDataset(1, networthDataset);
		
		CategoryAxis xAxis = this.chart.getCategoryPlot().getDomainAxis();
		xAxis.tickLabels = balanceData.xDates;
	}

//	/* Day of the year values for month end days. */
//	public static final Integer[] MONTH_LENGTHS = { 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
//	public static final String[] MONTH_NAMES = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct",
//			"Nov", "Dec" };
//
//	class DateAxis extends CategoryAxis {
//		protected AxisState drawTickMarksAndLabels(Graphics2D g2, double cursor, Rectangle2D plotArea,
//				Rectangle2D dataArea, RectangleEdge edge) {
//			AxisState state = new AxisState(cursor);
//
//			g2.setFont(getTickLabelFont());
//
//			double ol = getTickMarkOutsideLength();
//			double il = getTickMarkInsideLength();
//			int y = (int) (Math.round(cursor - ol));
//			LineMetrics lineMetrics = g2.getFont().getLineMetrics("Ápr", g2.getFontRenderContext());
//			int h = (int) (lineMetrics.getHeight() + 6);
//
//			List<ValueTick> ticks = refreshTicks(g2, state, dataArea, edge);
//			state.setTicks(ticks);
//
//			/* Last x point */
//			ValueTick tick = ticks.get(ticks.size() - 1);
//			float[] prevAnchorPoint = calculateAnchorPoint(tick, cursor, dataArea, edge);
//			double xmax = prevAnchorPoint[0];
//			double max_day = tick.getValue();
//
//			/* First x point */
//			tick = ticks.get(0);
//			prevAnchorPoint = calculateAnchorPoint(tick, cursor, dataArea, edge);
//			double xmin = Math.round(prevAnchorPoint[0]);
//			double min_day = tick.getValue();
//			double days_visible = max_day - min_day + 1;
//			/* 0.1 day horizontal gap. */
//			double gap = 0.1 * (xmax - xmin) / days_visible;
//
//			System.out.println("min_day " + min_day + " max_day" + max_day);
//
//			g2.setFont(getTickLabelFont());
//			g2.setColor(Color.BLACK);
//			int start_day = 0;
//
//			for (int month = 0; month < 12; month++) {
//				int end_day = start_day + MONTH_LENGTHS[month] - 1;
//				System.out.println("start-end " + start_day + " " + end_day);
//
//				if ((start_day >= min_day) && (start_day <= max_day) && (end_day >= min_day) && (end_day <= max_day)) {
//					double factor_x1 = (start_day - min_day) / days_visible;
//					double x1 = xmin + (xmax - xmin) * factor_x1;
//					double factor_x2 = (end_day - min_day) / days_visible;
//					double x2 = xmin + (xmax - xmin) * factor_x2;
//					System.out.println("month=" + month + ", start_day=" + start_day + " end_day=" + end_day + " x1="
//							+ x1 + " x2=" + x2);
//					g2.setColor(Color.LIGHT_GRAY);
//					g2.fill3DRect((int) (x1 + gap), y, (int) (x2 - x1 - 2 * gap), h, true);
//					g2.setColor(Color.BLACK);
//					TextUtilities.drawAlignedString(MONTH_NAMES[month], g2, (float) ((x1 + x2) / 2), (float) (y + ol),
//							TextAnchor.TOP_CENTER);
//				}
//
//				start_day += MONTH_LENGTHS[month];
//			}
//
//			return state;
//		}
//	}
}