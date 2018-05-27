package qif.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Date;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import qif.report.QifReporter;
import qif.report.StatusForDateModel;

public class Dashboard extends JPanel {
	private static final long serialVersionUID = 1L;

	public Dashboard() {
		super(new BorderLayout());
		
		JTextArea textarea = new JTextArea();
		textarea.setFont(new Font("Courier", Font.PLAIN, 12));
		JScrollPane scroller = new JScrollPane(textarea);
		
		add(scroller, BorderLayout.CENTER);

		StatusForDateModel model = QifReporter.buildReportStatusForDate(new Date());
		textarea.setText(QifReporter.generateReportStatusForDate(model));
	}
}