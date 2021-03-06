package demo;

import java.awt.Color;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

/**
 * A stacked area chart using data from a {@link CategoryDataset}.
 */
@SuppressWarnings("serial")
public class StackedAreaChartDemo extends ApplicationFrame {
	static final double[][] data = new double[][] { //
			{ 1.0, 4.0, 3.0, 5.0, 5.0, 7.0, 7.0, 8.0 }, //
			{ 5.0, 7.0, 6.0, 8.0, 4.0, 4.0, 2.0, 1.0 }, //
			{ 4.0, 3.0, 2.0, 3.0, 6.0, 3.0, 4.0, 3.0 } //
	};

	static final double[] average;
	static {
		average = new double[data[0].length];

		for (int ii = 0; ii < average.length; ++ii) {
			average[ii] = (data[0][ii] + data[1][ii] + data[2][ii]) / 3.0;
		}
	}

	public StackedAreaChartDemo(String title) {
		super(title);

		CategoryDataset dataset = createDataset();
		JFreeChart chart = createChart(dataset);

		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));

		setContentPane(chartPanel);
	}

	public CategoryDataset createDataset() {
		CategoryDataset dataset = DatasetUtilities.createCategoryDataset("Series ", "Type ", data);

		return dataset;
	}

	public JFreeChart createChart(CategoryDataset dataset) {

		JFreeChart chart = ChartFactory.createStackedAreaChart( //
				"Stacked Area Chart", // chart title
				"Category", // domain axis label
				"Value", // range axis label
				dataset, // data
				PlotOrientation.VERTICAL, // orientation
				false, // include legend
				true, // tooltips
				false); // urls

		chart.setBackgroundPaint(Color.white);

		CategoryPlot plot = (CategoryPlot) chart.getPlot();
		plot.setForegroundAlpha(0.5f);
		plot.setBackgroundPaint(Color.lightGray);
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);

		CategoryAxis domainAxis = plot.getDomainAxis();
		domainAxis.setLowerMargin(0.0);
		domainAxis.setUpperMargin(0.0);

		// change the auto tick unit selection to integer units only...
		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

		// CategoryItemRenderer renderer = plot.getRenderer();
		// renderer.setItemLabelsVisible(true);

		return chart;
	}

	public static void main(final String[] args) {
		StackedAreaChartDemo demo = new StackedAreaChartDemo("Stacked Area Chart Demo");
		demo.pack();

		RefineryUtilities.centerFrameOnScreen(demo);

		demo.setVisible(true);
	}
}