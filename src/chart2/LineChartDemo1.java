package chart2;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

/**
 * A line chart using data from a {@link CategoryDataset}.
 */
@SuppressWarnings("serial")
public class LineChartDemo1 extends ApplicationFrame {

	public LineChartDemo1(String title) {
		super(title);

		CategoryDataset dataset = createDataset();
		JFreeChart chart = createChart(dataset);

		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new Dimension(500, 270));

		setContentPane(chartPanel);
	}

	private CategoryDataset createDataset() {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		String[] seriesKeys = new String[] { //
				"First", "Second", "Third" //
		};

		String[] columnKeys = new String[] { //
				"Type 1", "Type 2", "Type 3", "Type 4", //
				"Type 5", "Type 6", "Type 7", "Type 8" //
		};

		double[][] values = new double[][] { //
				{ 1, 4, 3, 5, 5, 7, 7, 8 }, //
				{ 5, 7, 6, 8, 4, 4, 2, 1 }, //
				{ 4, 3, 2, 3, 6, 3, 4, 3 } };

		for (int seriesNum = 0; seriesNum < seriesKeys.length; ++seriesNum) {
			String seriesKey = seriesKeys[seriesNum];

			for (int valueNum = 0; valueNum < values[seriesNum].length; ++valueNum) {
				double value = values[seriesNum][valueNum];

				dataset.addValue(value, seriesKey, columnKeys[valueNum]);
			}
		}

		return dataset;
	}

	private JFreeChart createChart(CategoryDataset dataset) {

		JFreeChart chart = ChartFactory.createLineChart("Line Chart Demo 1", // chart title
				"Type", // domain axis label
				"Value", // range axis label
				dataset, // data
				PlotOrientation.VERTICAL, // orientation
				true, // include legend
				true, // tooltips
				false // urls
		);

		// NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
		// StandardLegend legend = (StandardLegend) chart.getLegend();
		// legend.setDisplaySeriesShapes(true);
		// legend.setShapeScaleX(1.5);
		// legend.setShapeScaleY(1.5);
		// legend.setDisplaySeriesLines(true);

		chart.setBackgroundPaint(Color.white);

		CategoryPlot plot = (CategoryPlot) chart.getPlot();
		plot.setBackgroundPaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.white);

		// customise the range axis...
		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		rangeAxis.setAutoRangeIncludesZero(true);

		// customise the renderer...
		LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
//        renderer.setDrawShapes(true);

		renderer.setSeriesStroke(0, new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f,
				new float[] { 10.0f, 6.0f }, 0.0f));
		renderer.setSeriesStroke(1, new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f,
				new float[] { 6.0f, 6.0f }, 0.0f));
		renderer.setSeriesStroke(2, new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f,
				new float[] { 2.0f, 6.0f }, 0.0f));
		// OPTIONAL CUSTOMISATION COMPLETED.

		return chart;
	}

	public static void main(String[] args) {
		LineChartDemo1 demo = new LineChartDemo1("Line Chart Demo");
		demo.pack();
		RefineryUtilities.centerFrameOnScreen(demo);
		demo.setVisible(true);
	}
}
