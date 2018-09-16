package chart2;

import java.awt.Color;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.title.DateTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.ui.TextAnchor;

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

public class BalanceChart2 {
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

	public JPanel createChartPanel() {
		JFreeChart chart = createChart();

		ChartPanel chPanel = new ChartPanel(chart);

		// chPanel.setPreferredSize(new Dimension(785, 440)); // size according to my
		// window
		chPanel.setMouseWheelEnabled(true);

//		JPanel panel = new JPanel();
//		panel.add(chPanel); // add the chart viewer to the JPanel

		return chPanel;
	}

	public JFreeChart createChart() {
		// DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		// fillDataSetWithData(dataset);
		CategoryDataset dataset = DatasetUtilities.createCategoryDataset("Series ", "Type ", data);

		// Plot type: Stacked Bar Graph.
		JFreeChart chart = ChartFactory.createStackedBarChart3D(//
				"Title", "", "", dataset, PlotOrientation.VERTICAL, true, true, false);

		DateTitle dateTitle = new DateTitle(); // Set current date as the title
		chart.addSubtitle(dateTitle);

		CategoryPlot plot = chart.getCategoryPlot();

		// 4 series to show. (Approved, Negotiaion, Cancelled, Completed)
		plot.getRenderer().setSeriesPaint(0, new Color(0, 255, 0));
		plot.getRenderer().setSeriesPaint(1, new Color(0, 0, 255));
		plot.getRenderer().setSeriesPaint(2, new Color(255, 0, 0));
		plot.getRenderer().setSeriesPaint(3, new Color(255, 255, 0));

		CategoryAxis xAxis = plot.getDomainAxis();
		// X-Axis Labels will be inclined at 45degree
		xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		xAxis.setLabel("X Axis Label");

		ValueAxis rangeAxis = plot.getRangeAxis();
		// Y-Axis range will be set automatically based on the supplied data
		rangeAxis.setAutoRange(true);
		rangeAxis.setLabel("Y Axis Label");

		BarRenderer renderer = (BarRenderer) plot.getRenderer();
		// if there is only one bar, it does not occupy the entire width
		renderer.setMaximumBarWidth(.1);

		renderer.setBaseItemLabelGenerator(new CategoryLabelGenerator());
		renderer.setBaseItemLabelsVisible(true);

		ItemLabelPosition p = new ItemLabelPosition(ItemLabelAnchor.CENTER, TextAnchor.BOTTOM_CENTER);
		renderer.setPositiveItemLabelPositionFallback(p);

		return chart;
	}
}