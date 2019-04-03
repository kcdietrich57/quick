package moneymgr.ui;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import moneymgr.report.NetWorthReporter;
import moneymgr.report.ReconcileStatusReporter;
import moneymgr.report.ReconcileStatusReporter.ReconcileStatusModel;
import moneymgr.report.StatusForDateModel;

/**
 * This panel displays overall information<br>
 * Balances | ReconcileStatus
 */
@SuppressWarnings("serial")
public class Dashboard extends JPanel {
	private JTextArea balancesText;
	private JTextArea reconcileStatusText;
	private JPanel balancePanel;

	public Dashboard() {
		super(new BorderLayout());

		this.balancesText = new JTextArea();
		this.balancesText.setFont(new Font("Courier", Font.PLAIN, 12));

		this.reconcileStatusText = new JTextArea();
		this.reconcileStatusText.setFont(new Font("Courier", Font.PLAIN, 12));

		JScrollPane balancesScroller = new JScrollPane(this.balancesText);
		JScrollPane reconcileStatusScroller = new JScrollPane(this.reconcileStatusText);

		this.balancePanel = new JPanel(new BorderLayout());

		this.balancePanel.add(balancesScroller, BorderLayout.CENTER);

		JTabbedPane tabs = new JTabbedPane();

		tabs.add("Balances", this.balancePanel);
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

		// TODO reset this when we reconcile a statement instead
		ReconcileStatusModel reconcileStatusModel = ReconcileStatusReporter.buildReportStatusModel();
		this.reconcileStatusText.setText(ReconcileStatusReporter.generateReportStatus(reconcileStatusModel));
	}
}