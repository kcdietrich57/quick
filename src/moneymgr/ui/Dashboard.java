package moneymgr.ui;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import moneymgr.model.MoneyMgrModel;
import moneymgr.report.CashFlowModel;
import moneymgr.report.NetWorthReporter;
import moneymgr.report.ReconcileStatusReporter;
import moneymgr.report.ReconcileStatusReporter.ReconcileStatusModel;
import moneymgr.report.StatusForDateModel;
import moneymgr.util.QDate;

/**
 * This panel displays overall information<br>
 * Balances | ReconcileStatus
 */
@SuppressWarnings("serial")
public class Dashboard extends JPanel {
	public final MoneyMgrModel model;

	private JTextArea balancesText;
	private JTextArea cashFlowText;
	private JTextArea reconcileStatusText;
	private JPanel balancePanel;

	private final ReconcileStatusReporter reconcileStatusReporter;
	private final NetWorthReporter netWorthReporter;

	public Dashboard(MoneyMgrModel model) {
		super(new BorderLayout());

		this.model = model;

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

		QDate aod = MainWindow.instance.getAsOfDate();

		this.netWorthReporter = new NetWorthReporter(model);
		;
		StatusForDateModel balancesModel = new StatusForDateModel(model, aod);
		this.balancesText.setText(this.netWorthReporter.generateReportStatusForDate(balancesModel));

		CashFlowModel cashFlowModel = new CashFlowModel(aod);
		this.cashFlowText.setText(this.netWorthReporter.generateReportStatusForDate(cashFlowModel));

		this.reconcileStatusReporter = new ReconcileStatusReporter(model);

		ReconcileStatusModel reconcileStatusModel = this.reconcileStatusReporter.buildReportStatusModel();
		this.reconcileStatusText.setText(this.reconcileStatusReporter.generateReportStatus(reconcileStatusModel));
	}

	public void changeDate() {
		QDate aod = MainWindow.instance.getAsOfDate();

		StatusForDateModel balancesModel = new StatusForDateModel(this.model, aod);
		this.balancesText.setText(this.netWorthReporter.generateReportStatusForDate(balancesModel));
		this.balancesText.setCaretPosition(0);

		CashFlowModel cashFlowModel = new CashFlowModel(aod);
		this.cashFlowText.setText(this.netWorthReporter.generateReportStatusForDate(cashFlowModel));
		this.cashFlowText.setCaretPosition(0);

		// TODO reset this when we reconcile a statement instead
		ReconcileStatusModel reconcileStatusModel = this.reconcileStatusReporter.buildReportStatusModel();
		this.reconcileStatusText.setText(this.reconcileStatusReporter.generateReportStatus(reconcileStatusModel));
	}
}