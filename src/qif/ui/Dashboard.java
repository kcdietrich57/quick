package qif.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import org.jdatepicker.JDatePicker;

import qif.data.QDate;
import qif.report.NetWorthReporter;
import qif.report.StatusForDateModel;
import qif.report.StatusReporter;
import qif.report.StatusReporter.ReportStatusModel;

/** This panel displays overall information */
@SuppressWarnings("serial")
public class Dashboard extends JPanel {
	private QDate displayDate = MainWindow.instance.asOfDate;

	private JTextArea textarea;

	public Dashboard() {
		super(new BorderLayout());

		JTabbedPane tabs = new JTabbedPane();

		JPanel balancePane = new JPanel(new BorderLayout());

		JDatePicker datePicker = new JDatePicker(displayDate.toDate());
		JPanel datePane = new JPanel(new BorderLayout());

		this.textarea = new JTextArea();
		textarea.setFont(new Font("Courier", Font.PLAIN, 12));
		JScrollPane scroller = new JScrollPane(this.textarea);

		datePane.add(datePicker, BorderLayout.WEST);

		balancePane.add(datePane, BorderLayout.NORTH);
		balancePane.add(scroller, BorderLayout.CENTER);

		tabs.add("Balances", balancePane);

		JTextArea textarea2 = new JTextArea();
		textarea2.setFont(new Font("Courier", Font.PLAIN, 12));
		JScrollPane scroller2 = new JScrollPane(textarea2);

		tabs.add("Reconcile Status", scroller2);

		add(tabs, BorderLayout.CENTER);

		datePicker.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object selObj = datePicker.getModel().getValue();
				if (selObj instanceof Date) {
					setDate(new QDate((Date) selObj));
				}
			}
		});

		StatusForDateModel model = NetWorthReporter.buildReportStatusForDate(displayDate);
		textarea.setText(NetWorthReporter.generateReportStatusForDate(model));

		ReportStatusModel model2 = StatusReporter.buildReportStatusModel();
		textarea2.setText(StatusReporter.generateReportStatus(model2));
	}

	public void setDate(QDate date) {
		if (!this.displayDate.equals(date)) {
			this.displayDate = date;
			StatusForDateModel model = NetWorthReporter.buildReportStatusForDate(this.displayDate);
			textarea.setText(NetWorthReporter.generateReportStatusForDate(model));

			MainWindow.instance.setAsOfDate(date);
		}
	}
}