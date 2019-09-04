package moneymgr.ui;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import moneymgr.report.CashFlowModel;
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
	private JTextArea cashFlowText;
	private JTextArea reconcileStatusText;
	private JPanel balancePanel;

	public Dashboard() {
		super(new BorderLayout());

		this.balancesText = new JTextArea();
		this.balancesText.setFont(new Font("Courier", Font.PLAIN, 12));

		this.cashFlowText = new JTextArea();
		this.cashFlowText.setFont(new Font("Courier", Font.PLAIN, 12));

		this.reconcileStatusText = new JTextArea();
		this.reconcileStatusText.setFont(new Font("Courier", Font.PLAIN, 12));

		JScrollPane balancesScroller = new JScrollPane(this.balancesText);
		JScrollPane cashFlowScroller = new JScrollPane(this.cashFlowText);
		JScrollPane reconcileStatusScroller = new JScrollPane(this.reconcileStatusText);

		this.balancePanel = new JPanel(new BorderLayout());

		this.balancePanel.add(balancesScroller, BorderLayout.CENTER);

		JTabbedPane tabs = new JTabbedPane();

		tabs.add("Reconcile Status", reconcileStatusScroller);
		tabs.add("Cash Flow", cashFlowScroller);
		tabs.add("Balances", this.balancePanel);

		add(tabs, BorderLayout.CENTER);

		StatusForDateModel balancesModel = new StatusForDateModel(MainWindow.instance.asOfDate());
		this.balancesText.setText(NetWorthReporter.generateReportStatusForDate(balancesModel));

		CashFlowModel cashFlowModel = new CashFlowModel(MainWindow.instance.asOfDate());
		this.cashFlowText.setText(NetWorthReporter.generateReportStatusForDate(cashFlowModel));

		ReconcileStatusModel reconcileStatusModel = ReconcileStatusReporter.buildReportStatusModel();
		this.reconcileStatusText.setText(ReconcileStatusReporter.generateReportStatus(reconcileStatusModel));
	}

	public void changeDate() {
		StatusForDateModel balancesModel = new StatusForDateModel(MainWindow.instance.asOfDate());
		this.balancesText.setText(NetWorthReporter.generateReportStatusForDate(balancesModel));
		this.balancesText.setCaretPosition(0);

		CashFlowModel cashFlowModel = new CashFlowModel(MainWindow.instance.asOfDate());
		this.cashFlowText.setText(NetWorthReporter.generateReportStatusForDate(cashFlowModel));
		this.cashFlowText.setCaretPosition(0);

		// TODO reset this when we reconcile a statement instead
		ReconcileStatusModel reconcileStatusModel = ReconcileStatusReporter.buildReportStatusModel();
		this.reconcileStatusText.setText(ReconcileStatusReporter.generateReportStatus(reconcileStatusModel));
	}
}