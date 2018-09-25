package qif.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.internal.chartpart.Chart;

import qif.data.GenericTxn;
import qif.data.QDate;
import qif.data.QifDom;

@SuppressWarnings("serial")
public class MainWindow extends JPanel {
	public static MainWindow instance;

	public JTabbedPane contentPanel;
	// content->account
	public AccountPanel accountPanel;
	// content->account->accountNav
	public AccountNavigationPanel accountNavigationPanel;
	// content->account->accountNav->summary
	public SummaryPanel summaryPanel;
	// content->account->accountNav->accounts
	public AccountListPanel accountListPanel;
	// content->account->account->accountInfo
	public AccountInfoPanel acctInfoPanel;
	// content->account->transactions
	public TransactionPanel registerTransactionPanel;
	// content->account->statements
	public StatementPanel statementPanel;
	// content->account->statementDetails
	public StatementDetailsPanel statementDetailsPanel;
	// content->account->statements->statementDetails
	public TransactionPanel statementTransactionPanel;
	// content->account->reconcile
	public static ReconcilePanel reconcilePanel;
	// content->account->reconcile->reconcileStatus
	public ReconcileStatusPanel reconcileStatusPanel;
	// content->account->reconcile->reconcileTransactions
	public ReconcileTransactionsPanel reconcileTransactionsPanel;
	// AsOfDatePanel
	public TimeSliderPanel asOfDatePanel;

	public enum IntervalLength {
		All, TenYear, FiveYear, OneYear, Quarter, Month, Week, Day
	}

	public enum IntervalUnit {
		Year, Quarter, Month, Week, Day;

		public QDate nextDate(QDate d) {
			int year = d.getYear();
			int month = d.getMonth();
			int day = d.getDay();

			switch (this) {
			case Year:
				++year;
				break;
			case Quarter:
				month += 3;
				break;
			case Month:
				++month;
				break;
			case Week:
				day += 7;
				break;
			case Day:
				++day;
				break;
			}

			if (month > 12) {
				++year;
				month -= 12;
			}

			QDate dd = new QDate(year, month, 1).getLastDayOfMonth();
			if (day > dd.getDay()) {
				if (this != Week && this != Day) {
					day = dd.getDay();
				} else {
					++month;
					day -= dd.getDay();

					if (month > 12) {
						++year;
						month -= 12;
					}
				}
			}

			return new QDate(year, month, day);
		}
	}

	public QDate asOfDate = QDate.today();
	public IntervalLength reportPeriod = IntervalLength.All;
	public IntervalUnit reportUnit = IntervalUnit.Year;

	public Dashboard dashboardPanel;
	public JPanel chartPanel;
	private BalanceChart balChart;
	private NetWorthChart nwChart;
	private ISIOptionsChart optChart;
	private JSplitPane accountViewSplit;

	private BalanceChart_old balChartXCHART;
	private NetWorthChart_old nwChartXCHART;
	private ISIOptionsChart_old optChartXCHART;

	public MainWindow() {
		super(new BorderLayout());

		instance = this;

		loadProperties();

		createTimeSlider();
		createContentPanel();

		add(contentPanel, BorderLayout.CENTER);
		add(this.asOfDatePanel, BorderLayout.SOUTH);
		// add(new JButton("Status Bar Goes Here"), BorderLayout.SOUTH);
	}

	public QDate getIntervalStart() {
		QDate first = this.asOfDate;

		switch (this.reportPeriod) {
		case Day:
			first = first.addDays(-1);
			break;
		case Week:
			first = first.addDays(-7);
			break;
		case Month:
			first = first.addMonths(-1);
			break;
		case Quarter:
			first = first.addMonths(-3);
			break;
		case OneYear:
			first = first.addMonths(-12);
			break;
		case FiveYear:
			first = first.addMonths(-60);
			break;
		case TenYear:
			first = first.addMonths(-120);
			break;

		case All:
			return GenericTxn.getFirstTransactionDate();
		}

		if (first.compareTo(GenericTxn.getFirstTransactionDate()) < 0) {
			first = GenericTxn.getFirstTransactionDate();
		}

		return first;
	}

	public QDate getIntervalEnd() {
		QDate last = getIntervalStart();

		switch (this.reportPeriod) {
		case Day:
			last = last.addDays(1);
			break;
		case Week:
			last = last.addDays(7);
			break;
		case Month:
			last = last.addMonths(1);
			break;
		case Quarter:
			last = last.addMonths(3);
			break;
		case OneYear:
			last = last.addMonths(12);
			break;
		case FiveYear:
			last = last.addMonths(60);
			break;
		case TenYear:
			last = last.addMonths(120);
			break;

		case All:
			return GenericTxn.getLastTransactionDate();
		}

		if (last.compareTo(GenericTxn.getLastTransactionDate()) > 0) {
			last = GenericTxn.getLastTransactionDate();
		}

		return last;
	}

	public void loadProperties() {
		File propfile = new File(QifDom.qifDir, "properties");

		Properties p = new Properties();

		if (propfile.isFile() && propfile.canRead()) {
			try {
				p.load(new FileInputStream(propfile));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		QifDom.qifProperties = p;
	}

	public void saveProperties() {
		if (QifDom.qifProperties == null) {
			QifDom.qifProperties = new Properties();
		}

		// TODO give statment transactions table its own model
		// this.statementTransactionPanel.updateQifProperties();
		this.reconcileTransactionsPanel.updateQifProperties();
		this.registerTransactionPanel.updateQifProperties();

		File cwfile = new File(QifDom.qifDir, "properties");

		try {
			QifDom.qifProperties.store(new FileOutputStream(cwfile), "QIF properties");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void createTimeSlider() {
		this.asOfDatePanel = new TimeSliderPanel();
	}

	public void setSliderPosition(QDate date) {
		this.asOfDatePanel.setSliderPosition(date);
	}

	public void setAsOfDate(QDate date) {
		if (date.compareTo(GenericTxn.getFirstTransactionDate()) < 0) {
			date = GenericTxn.getFirstTransactionDate();
		}
		if (date.compareTo(QDate.today()) > 0) {
			date = QDate.today();
		}

		if (!date.equals(asOfDate)) {
			this.asOfDate = date;

			// TODO use AsOfDateListeners to update UI
			this.dashboardPanel.changeDate();
			this.accountNavigationPanel.refreshAccountList();
			this.summaryPanel.updateValues();

			updateChartPanel();
		}
	}

	private void createContentPanel() {
		dashboardPanel = new Dashboard();
		createAccountsPanel();
		createChartsPanel();

		contentPanel = new JTabbedPane();

		contentPanel.add("Accounts", accountViewSplit);
		contentPanel.add("Investments", new JButton("Investments go here"));
		contentPanel.add("Dashboard", dashboardPanel);
		contentPanel.add("Charts", chartPanel);
	}

	private void createAccountsPanel() {
		accountNavigationPanel = new AccountNavigationPanel();
		accountPanel = new AccountPanel();

		accountViewSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, //
				accountNavigationPanel, accountPanel);
		accountViewSplit.setPreferredSize(new Dimension(1200, 800));

		this.accountNavigationPanel.addAccountSelectionListener(this.accountPanel);
	}

	@SuppressWarnings("rawtypes")
	private void createChartsPanel() {
		this.chartPanel = new JPanel(new BorderLayout());
		JTabbedPane chartTabs = new JTabbedPane();

		this.nwChartXCHART = new NetWorthChart_old();
		this.nwChartXCHART.create();
		this.balChartXCHART = new BalanceChart_old();
		this.balChartXCHART.create();
		this.optChartXCHART = new ISIOptionsChart_old();
		this.optChartXCHART.create();

		this.balChart = new BalanceChart();
		this.nwChart = new NetWorthChart();
		this.optChart = new ISIOptionsChart();

		XChartPanel<Chart> nwChartPanel_old = new XChartPanel<Chart>(nwChartXCHART.chart);
		XChartPanel<Chart> balChartPanel_old = new XChartPanel<Chart>(balChartXCHART.chart);
		XChartPanel<Chart> optChartPanel = new XChartPanel<Chart>(optChartXCHART.chart);
		// this.chartView.validate();

		chartTabs.addTab("Balances", this.balChart.createChartPanel());
		chartTabs.addTab("Net Worth", this.nwChart.createChartPanel());
		chartTabs.addTab("ISI Options", this.optChart.createChartPanel());

//		chartTabs.addTab("Balances(old)", balChartPanel_old);
//		chartTabs.addTab("Net Worth(old)", nwChartPanel_old);
//		chartTabs.addTab("ISI Options(old)", optChartPanel);

		this.chartPanel.add(chartTabs, BorderLayout.CENTER);
	}

	public void updateChartPanel() {
		this.balChart.update();
		this.nwChart.update();
		this.optChart.update();

		this.balChartXCHART.update();
		this.nwChartXCHART.update();
		this.optChartXCHART.update();

		this.chartPanel.repaint();
	}

	public void addAccountSelectionListener(AccountSelectionListener listener) {
		accountNavigationPanel.addAccountSelectionListener(listener);
	}

	public void setSplitPosition() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				accountViewSplit.setDividerLocation(.25);
				accountPanel.setSplitPosition();
			}
		});
	}
}
