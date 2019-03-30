package demo;

import java.awt.EventQueue;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

class AxisTest {

	private static final int N = 5;
	private static final double FEET_PER_METER = 3.28084;

	private static XYDataset createDataset() {
		XYSeriesCollection data = new XYSeriesCollection();

		final XYSeries series = new XYSeries("Data");
		for (int i = -N; i < N * N; i++) {
			series.add(i, i);
		}

		data.addSeries(series);

		return data;
	}

	private JFreeChart createChart(XYDataset dataset) {
		NumberAxis meters = new NumberAxis("Meters");
		NumberAxis feet = new NumberAxis("Feet");
		ValueAxis domain = new NumberAxis();
		XYItemRenderer renderer = new XYLineAndShapeRenderer();

		XYPlot plot = new XYPlot(dataset, domain, meters, renderer);
		plot.setRangeAxis(1, feet);
		plot.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_LEFT);

		List<Integer> axes = Arrays.asList(0, 1);
		plot.mapDatasetToRangeAxes(0, axes);

		scaleRange(feet, meters);

		meters.addChangeListener((AxisChangeEvent event) -> {
			EventQueue.invokeLater(() -> {
				scaleRange(feet, meters);
			});
		});

		JFreeChart chart = new JFreeChart("Axis Test", JFreeChart.DEFAULT_TITLE_FONT, plot, true);

		return chart;
	}

	private void scaleRange(NumberAxis feet, NumberAxis meters) {
		feet.setRange(meters.getLowerBound() * FEET_PER_METER, meters.getUpperBound() * FEET_PER_METER);
	}

	private void display() {
		JFrame f = new JFrame("AxisTest");

		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.add(new ChartPanel(createChart(createDataset())));
		f.pack();
		f.setLocationRelativeTo(null);

		f.setVisible(true);
	}

	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			new AxisTest().display();
		});
	}
}