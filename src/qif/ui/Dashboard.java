package qif.ui;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import qif.report.NetWorthReporter;
import qif.report.ReconcileStatusReporter;
import qif.report.ReconcileStatusReporter.ReconcileStatusModel;
import qif.report.StatusForDateModel;

/** This panel displays overall information */
@SuppressWarnings("serial")
public class Dashboard extends JPanel {
	private JTextArea balancesText;
	private JTextArea reconcileStatusText;

	public Dashboard() {
		super(new BorderLayout());

		this.balancesText = new JTextArea();
		balancesText.setFont(new Font("Courier", Font.PLAIN, 12));

		this.reconcileStatusText = new JTextArea();
		reconcileStatusText.setFont(new Font("Courier", Font.PLAIN, 12));

		JScrollPane scroller = new JScrollPane(this.balancesText);
		JScrollPane reconcileStatusScroller = new JScrollPane(this.reconcileStatusText);

		JPanel balancePane = new JPanel(new BorderLayout());

		balancePane.add(scroller, BorderLayout.CENTER);

		JTabbedPane tabs = new JTabbedPane();

		tabs.add("Balances", balancePane);
		tabs.add("Reconcile Status", reconcileStatusScroller);

		add(tabs, BorderLayout.CENTER);

		StatusForDateModel balancesModel = new StatusForDateModel(MainWindow.instance.asOfDate);
		this.balancesText.setText(NetWorthReporter.generateReportStatusForDate(balancesModel));

		ReconcileStatusModel reconcileStatusModel = ReconcileStatusReporter.buildReportStatusModel();
		this.reconcileStatusText.setText(ReconcileStatusReporter.generateReportStatus(reconcileStatusModel));
	}

	public void changeDate() {
		StatusForDateModel balancesModel = new StatusForDateModel(MainWindow.instance.asOfDate);
		this.balancesText.setText(NetWorthReporter.generateReportStatusForDate(balancesModel));
		this.balancesText.setCaretPosition(0);
	}
}