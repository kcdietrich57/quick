package moneymgr.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import moneymgr.model.GenericTxn;
import moneymgr.util.QDate;

/**
 * Controls to shift the date for displaying data<br>
 * DatePanel | DateSlider
 */
@SuppressWarnings("serial")
public class TimeSliderPanel extends JPanel {
	private JLabel asOfDateLabel;
	private JLabel asOfDateSliderLabel;
	private JSlider asOfDateSlider;

	private QDate sliderDate;

	public TimeSliderPanel() {
		super(new BorderLayout());

		this.sliderDate = MainWindow.instance.asOfDate();

		createDateSlider();
		JPanel datePanel = createDatePanel();

		add(datePanel, BorderLayout.WEST);
		add(this.asOfDateSlider, BorderLayout.CENTER);

		updateValues();
	}
	
	private JPanel createDatePanel() {
		JPanel datePanel = new JPanel(new GridLayout(1, 3));
		datePanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2));

		this.asOfDateLabel = new JLabel("---");
		this.asOfDateLabel.setFont(new Font("Helvetica", Font.BOLD, 12));
		this.asOfDateLabel.setForeground(Color.GRAY);

		MainWindow.IntervalLength[] periods = new MainWindow.IntervalLength[] { //
				MainWindow.IntervalLength.Day, //
				MainWindow.IntervalLength.Week, //
				MainWindow.IntervalLength.Month, //
				MainWindow.IntervalLength.Quarter, //
				MainWindow.IntervalLength.OneYear, //
				MainWindow.IntervalLength.FiveYear, //
				MainWindow.IntervalLength.TenYear, //
				MainWindow.IntervalLength.All //
		};

		JComboBox<MainWindow.IntervalLength> periodCombo = new JComboBox<>(periods);
		periodCombo.setSelectedItem(MainWindow.instance.reportPeriod);

		periodCombo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object o = periodCombo.getSelectedItem();
				if (MainWindow.instance.reportPeriod != (MainWindow.IntervalLength) o) {
					MainWindow.instance.reportPeriod = (MainWindow.IntervalLength) o;
					MainWindow.instance.updateChartPanel(true);
				}
			}
		});

		datePanel.add(periodCombo);

		MainWindow.IntervalUnit[] units = new MainWindow.IntervalUnit[] { //
				MainWindow.IntervalUnit.Day, //
				MainWindow.IntervalUnit.Week, //
				MainWindow.IntervalUnit.Month, //
				MainWindow.IntervalUnit.Quarter, //
				MainWindow.IntervalUnit.Year //
		};

		JComboBox<MainWindow.IntervalUnit> unitsCombo = new JComboBox<>(units);
		unitsCombo.setSelectedItem(MainWindow.instance.reportUnit);

		unitsCombo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object o = unitsCombo.getSelectedItem();
				if (MainWindow.instance.reportUnit != (MainWindow.IntervalUnit) o) {
					MainWindow.instance.reportUnit = (MainWindow.IntervalUnit) o;
					MainWindow.instance.updateChartPanel(true);
				}
			}
		});

		datePanel.add(unitsCombo);

		datePanel.add(this.asOfDateLabel);
		datePanel.add(this.asOfDateSliderLabel);
		
		return datePanel;
	}

	private void createDateSlider() {
		QDate start = GenericTxn.getFirstTransactionDate();
		QDate end = GenericTxn.getLastTransactionDate();
		int years = (end.getYear() - start.getYear()) + 1;
		int months = years * 12;

		BoundedRangeModel timeModel = new DefaultBoundedRangeModel(months, 0, 0, months + 1);

		this.asOfDateSlider = new JSlider(timeModel);
		this.asOfDateSlider.setOrientation(SwingConstants.HORIZONTAL);
		this.asOfDateSlider.setMajorTickSpacing(12);
		this.asOfDateSlider.setPaintTicks(true);
		this.asOfDateSlider.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2));

		this.asOfDateSlider.addChangeListener(e -> {
			if (this.asOfDateSlider.getValueIsAdjusting()) {
				return;
			}

			sliderPositionChanged();
		});

		Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
		int m = 0;
		for (int y = start.getYear(); y <= end.getYear(); ++y, m += 12) {
			labelTable.put(new Integer(m), new JLabel(String.format("%02d", y % 100)));
		}

		this.asOfDateSlider.setLabelTable(labelTable);

		this.asOfDateSlider.setPaintLabels(true);

		this.asOfDateSliderLabel = new JLabel("---");
		this.asOfDateSliderLabel.setFont(new Font("Helvetica", Font.BOLD, 12));
		this.asOfDateSliderLabel.setForeground(Color.BLUE);
		this.asOfDateSliderLabel.setPreferredSize(new Dimension(100, 20));
		
//		this.asOfDateSliderLabel.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				JDatePicker dp = new JDatePicker();
//				dp.setMainWindow.instance.asOfDate().toDate());
//				JFormattedTextField tf = dp.getFormattedTextField();
//				String s = tf.getText();
//				System.out.println(s);
//				
//				dp.setVisible(true);
//			}
//		});
	}

	private void sliderPositionChanged() {
		int smonths = this.asOfDateSlider.getValue();

		this.sliderDate = convertMonthsToDate(smonths);

		if (!MainWindow.instance.asOfDate().equals(this.sliderDate)) {
			MainWindow.instance.setAsOfDate(this.sliderDate);

			updateValues();
		}
	}

	public void updateValues() {
		this.asOfDateSliderLabel.setText(this.sliderDate.toString());

		if (!this.sliderDate.equals(MainWindow.instance.asOfDate())) {
			this.asOfDateLabel.setText(MainWindow.instance.asOfDate().toString());
		} else {
			this.asOfDateLabel.setText("");
		}
	}

	public void setSliderPosition(QDate date) {
		this.asOfDateSlider.setValue(TimeSliderPanel.convertDateToMonths(date));
	}

	/** Convert months since start of history to the date */
	private QDate convertMonthsToDate(int months) {
		int startyear = GenericTxn.getFirstTransactionDate().getYear();
		QDate date = new QDate(startyear, 1, 1);
		QDate sdate = date.addMonths(months);

		return sdate;
	}

	/** Convert a date to months since the start of our history */
	public static int convertDateToMonths(QDate date) {
		int months = 0;

		int year = GenericTxn.getFirstTransactionDate().getYear();
		int y = date.getYear();

		while (year < y) {
			months += 12;
			++year;
		}

		months += date.getMonth();

		return months;
	}
}