package moneymgr.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
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
import javax.swing.JSpinner;
import javax.swing.SwingConstants;
import javax.swing.border.AbstractBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import moneymgr.model.MoneyMgrModel;
import moneymgr.util.QDate;

/**
 * Controls to shift the date for displaying data<br>
 * DatePanel | DateSlider
 */
@SuppressWarnings("serial")
public class TimeSliderPanel extends JPanel {
	private JComboBox<MainWindow.IntervalLength> periodCombo;
	private JComboBox<MainWindow.IntervalUnit> unitsCombo;
	private JSpinner daySpinner;
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

		this.periodCombo = new JComboBox<>(periods);
		this.periodCombo.setSelectedItem(MainWindow.instance.reportPeriod);

		this.daySpinner = new JSpinner();

		this.periodCombo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object o = TimeSliderPanel.this.periodCombo.getSelectedItem();
				if (MainWindow.instance.reportPeriod != (MainWindow.IntervalLength) o) {
					MainWindow.instance.reportPeriod = (MainWindow.IntervalLength) o;
					MainWindow.instance.updateChartPanel(true);
				}
			}
		});

		MainWindow.IntervalUnit[] units = new MainWindow.IntervalUnit[] { //
				MainWindow.IntervalUnit.Day, //
				MainWindow.IntervalUnit.Week, //
				MainWindow.IntervalUnit.Month, //
				MainWindow.IntervalUnit.Quarter, //
				MainWindow.IntervalUnit.Year //
		};

		this.unitsCombo = new JComboBox<>(units);
		this.unitsCombo.setSelectedItem(MainWindow.instance.reportUnit);

		this.unitsCombo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object o = TimeSliderPanel.this.unitsCombo.getSelectedItem();
				if (MainWindow.instance.reportUnit != (MainWindow.IntervalUnit) o) {
					MainWindow.instance.reportUnit = (MainWindow.IntervalUnit) o;
					MainWindow.instance.updateChartPanel(true);
				}
			}
		});

		this.daySpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				int day = ((Integer) ((JSpinner) e.getSource()).getValue()).intValue();

				QDate curdate = MainWindow.instance.asOfDate();

				if (curdate.getDay() != day) {
					if (day < 1) {
						int mday = curdate.getDay();
						curdate = curdate.addDays(-mday);
						day = curdate.getDay();
					} else if (day > curdate.getLastDayOfMonth().getDay()) {
						curdate = curdate.addDays(day - curdate.getLastDayOfMonth().getDay());
						day = curdate.getDay();
					}

					QDate newdate = new QDate(curdate.getYear(), curdate.getMonth(), day);

					MainWindow.instance.setAsOfDate(newdate);
					updateValues();

					TimeSliderPanel.this.daySpinner.setValue(new Integer(day));
				}
			}
		});

		this.asOfDateSliderLabel = new JLabel("---");
		this.asOfDateSliderLabel.setFont(new Font("Helvetica", Font.BOLD, 18));
		this.asOfDateSliderLabel.setForeground(Color.BLUE);
		this.asOfDateSliderLabel.setPreferredSize(new Dimension(100, 20));
		
		JPanel pan = new JPanel(new BorderLayout());
		pan.setBorder(new AbstractBorder() {
			public Insets getBorderInsets(Component c) {
				return new Insets(7, 20, 0, 0);
			}
		});

		pan.add(this.asOfDateSliderLabel, BorderLayout.CENTER);

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

		JPanel periodUnitsPanel = new JPanel(new BorderLayout());
		periodUnitsPanel.add(this.periodCombo, BorderLayout.NORTH);
		periodUnitsPanel.add(this.unitsCombo, BorderLayout.SOUTH);

		datePanel.add(periodUnitsPanel);

		JPanel sliderControlsPanel = new JPanel(new BorderLayout());
		sliderControlsPanel.add(pan, BorderLayout.NORTH);
		sliderControlsPanel.add(this.daySpinner, BorderLayout.SOUTH);

		datePanel.add(sliderControlsPanel);
		// datePanel.add(this.asOfDateLabel);

		return datePanel;
	}

	private void createDateSlider() {
		QDate start = MoneyMgrModel.getFirstTransactionDate();
		QDate end = MoneyMgrModel.getLastTransactionDate();
		int years = (end.getYear() - start.getYear()) + 1;
		int months = years * 12;

		BoundedRangeModel timeModel = new DefaultBoundedRangeModel(months, 0, 0, months + 1);

		this.asOfDateSlider = new JSlider(timeModel);
		this.asOfDateSlider.setOrientation(SwingConstants.HORIZONTAL);
		this.asOfDateSlider.setMajorTickSpacing(12);
		this.asOfDateSlider.setPaintTicks(true);
		this.asOfDateSlider.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2));
		this.asOfDateSlider.setFont(new Font(Font.DIALOG, Font.ITALIC, 5));

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
	}

	private void sliderPositionChanged() {
		int smonths = this.asOfDateSlider.getValue();

		this.sliderDate = convertMonthsToDate(smonths);

		if (!MainWindow.instance.asOfDate().equals(this.sliderDate)) {
			MainWindow.instance.setAsOfDate(this.sliderDate);
			this.daySpinner.setValue(new Integer(this.sliderDate.getDay()));

			updateValues();
		}
	}

	public void updateValues() {
		this.asOfDateSliderLabel.setText(this.sliderDate.toString());
	}

	public void setSliderPosition(QDate date) {
		this.asOfDateSlider.setValue(TimeSliderPanel.convertDateToMonths(date));
	}

	/** Convert months since start of history to the date */
	private QDate convertMonthsToDate(int months) {
		int startyear = MoneyMgrModel.getFirstTransactionDate().getYear();
		QDate date = new QDate(startyear, 1, 1);
		QDate sdate = date.addMonths(months);

		return sdate;
	}

	/** Convert a date to months since the start of our history */
	public static int convertDateToMonths(QDate date) {
		int months = 0;

		int year = MoneyMgrModel.getFirstTransactionDate().getYear();
		int y = date.getYear();

		while (year < y) {
			months += 12;
			++year;
		}

		months += date.getMonth();

		return months;
	}
}