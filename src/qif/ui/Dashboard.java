package qif.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Date;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import qif.report.NetWorthReporter;
import qif.report.StatusForDateModel;
import qif.report.StatusReporter;
import qif.report.StatusReporter.ReportStatusModel;

public class Dashboard extends JPanel {
	private static final long serialVersionUID = 1L;

	public Dashboard() {
		super(new BorderLayout());

		JTabbedPane tabs = new JTabbedPane();

		JTextArea textarea = new JTextArea();
		textarea.setFont(new Font("Courier", Font.PLAIN, 12));
		JScrollPane scroller = new JScrollPane(textarea);

		tabs.add("Net Worth", scroller);

		JTextArea textarea2 = new JTextArea();
		textarea2.setFont(new Font("Courier", Font.PLAIN, 12));
		JScrollPane scroller2 = new JScrollPane(textarea2);

		tabs.add("Reconcile Status", scroller2);

		add(tabs, BorderLayout.CENTER);

		StatusForDateModel model = NetWorthReporter.buildReportStatusForDate(new Date());
		textarea.setText(NetWorthReporter.generateReportStatusForDate(model));

		ReportStatusModel model2 = StatusReporter.buildReportStatusModel();
		textarea2.setText(StatusReporter.generateReportStatus(model2));
	}
}