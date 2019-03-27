package qif.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
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

import qif.data.GenericTxn;
import qif.data.QDate;
import qif.data.QifDom;
import qif.ui.chart.BalanceChart;
import qif.ui.chart.BalanceChart_old;
import qif.ui.chart.ISIOptionsChart;
import qif.ui.chart.ISIOptionsChart_old;
import qif.ui.chart.NetWorthChart;
import qif.ui.chart.NetWorthChart_old;

@SuppressWarnings("serial")
public class MainWindow extends JPanel {

	public static MainWindow instance;

	/** Time span for charts */
	public enum IntervalLength {
		All, TenYear, FiveYear, OneYear, Quarter, Month, Week, Day
	}

	/** Resolution for charts */
	public enum IntervalUnit {
		Year, Quarter, Month, Week, Day;

		/** Calculate the next date for this date resolution value */
		public QDate nextDate(QDate d) {
			int year = d.getYear();
			int month = d.getMonth();
			int day = d.getDay();

			// TODO this is messy
			switch (this) {
			case Year:
				if ((month < 12) || (day < 31)) {
					// End of current year
					month = 12;
					day = 31;
				} else {
					++year;
				}
				break;

			case Quarter:
				// Pick month/year
				if ((month % 3) != 0) {
					// Ending month of current quarter
					month = (month + 3) - (month % 3);
				} else if (day == QDate.getDateForEndOfMonth(year, month).getDay()) {
					month += 3;
					if (month > 12) {
						++year;
						month -= 12;
					}
				}

				day = QDate.getDateForEndOfMonth(year, month).getDay();
				break;

			case Month:
				int eomday = QDate.getDateForEndOfMonth(year, month).getDay();

				if (day < eomday) {
					// End of current month
					day = eomday;
				} else {
					if (++month > 12) {
						month = 1;
						++year;
					}

					day = QDate.getDateForEndOfMonth(year, month).getDay();
				}
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

			QDate dd = QDate.getDateForEndOfMonth(year, month);
			if (day > dd.getDay()) {
				if ((this != Week) && (this != Day)) {
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

	// UI Organization:
	// -----------------------------------------------------
	// content->account
	// content->account->accountNav
	// content->account->accountNav->summary
	// content->account->accountNav->accounts
	// content->account->account->accountInfo
	// content->account->transactions
	// content->account->statements
	// content->account->statementDetails
	// content->account->statements->statementDetails
	// content->account->reconcile
	// content->account->reconcile->reconcileStatus
	// content->account->reconcile->reconcileTransactions
	// AsOfDatePanel
	// -----------------------------------------------------

	public JTabbedPane contentPanel;
	public AccountInfoPanel accountPanel;
	public AccountNavigationPanel accountNavigationPanel;
	public AccountNavigationSummaryPanel summaryPanel;
	public AccountNavigationListPanel accountListPanel;
	public AccountInfoHeaderPanel acctInfoPanel;
	public TransactionPanel registerTransactionPanel;
	public AccountInfoStatementPanel statementPanel;
	public AccountInfoStatementDetailsPanel statementDetailsPanel;
	public TransactionPanel statementTransactionPanel;
	public static AccountInfoReconcilePanel reconcilePanel;
	public AccountInfoReconcileStatusPanel reconcileStatusPanel;
	public AccountInfoReconcileTransactionsPanel reconcileTransactionsPanel;
	public TimeSliderPanel asOfDatePanel;

	private JSplitPane accountViewSplit;
	public Dashboard dashboardPanel;

	/** Chart parameters */
	public QDate startAsOfDate;
	public QDate asOfDate;
	public IntervalLength reportPeriod;
	public IntervalUnit reportUnit;

	private boolean chartNeedsRefresh = true;
	private boolean chartsVisible = false;
	private boolean nwVisible = false;
	private boolean balVisible = false;
	private boolean optVisible = false;

	public JPanel chartPanel;
	private BalanceChart balChart;
	private NetWorthChart nwChart;
	private ISIOptionsChart optChart;

	private BalanceChart_old balChartXCHART;
	private NetWorthChart_old nwChartXCHART;
	private ISIOptionsChart_old optChartXCHART;

	public MainWindow() {
		super(new BorderLayout());

		instance = this;

		updateAsOfDate(QDate.today());

		this.reportPeriod = IntervalLength.All;
		this.reportUnit = IntervalUnit.Month;

		loadProperties();

		createTimeSlider();
		createContentPanel();

		add(this.contentPanel, BorderLayout.CENTER);
		add(this.asOfDatePanel, BorderLayout.SOUTH);
		// add(new JButton("Status Bar Goes Here"), BorderLayout.SOUTH);
	}

	/** Calculate the starting date given the end (asOfDate-period) */
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

	/** Calculate the ending date given the start (start+period) */
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

	/** Load persistent UI properties from file */
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

	/** Write persistent UI properties to file */
	public void saveProperties() {
		if (QifDom.qifProperties == null) {
			QifDom.qifProperties = new Properties();
		}

		// TODO give statement transactions table its own model
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

	// TODO time slider manipulation - seems these could be TimeSliderPanel methods
	private void createTimeSlider() {
		this.asOfDatePanel = new TimeSliderPanel();
	}

	/** Move the time slider to a new position */
	public void setSliderPosition(QDate date) {
		this.asOfDatePanel.setSliderPosition(date);
	}

	/** Change current display date */
	private void updateAsOfDate(QDate date) {
		this.asOfDate = date;

		int year = this.asOfDate.getYear();
		int lastMonth = this.asOfDate.getMonth() - 1;
		if (lastMonth < 1) {
			lastMonth += 12;
			--year;
		}

		this.startAsOfDate = new QDate(year, lastMonth, 1).getLastDayOfMonth();
	}

	/** Set current display date */
	public void setAsOfDate(QDate date) {
		if (date.compareTo(GenericTxn.getFirstTransactionDate()) < 0) {
			date = GenericTxn.getFirstTransactionDate();
		}
		if (date.compareTo(GenericTxn.getLastTransactionDate()) > 0) {
			date = GenericTxn.getLastTransactionDate();
		}

		if (!date.equals(this.asOfDate)) {
			updateAsOfDate(date);

			// TODO use AsOfDateListeners to update UI
			this.dashboardPanel.changeDate();
			this.accountNavigationPanel.refreshAccountList();
			this.summaryPanel.updateValues();

			if (this.reportPeriod != IntervalLength.All) {
				updateChartPanel(true);
			}
		}
	}

	private void createContentPanel() {
		this.dashboardPanel = new Dashboard();
		createAccountsPanel();
		createChartsPanel();

		this.contentPanel = new JTabbedPane();

		this.contentPanel.add("Accounts", this.accountViewSplit);
		this.contentPanel.add("Investments", new JButton("Investments go here"));
		this.contentPanel.add("Dashboard", this.dashboardPanel);
		this.contentPanel.add("Charts", this.chartPanel);
	}

	private void createAccountsPanel() {
		this.accountNavigationPanel = new AccountNavigationPanel();
		this.accountPanel = new AccountInfoPanel();

		this.accountViewSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, //
				this.accountNavigationPanel, this.accountPanel);
		this.accountViewSplit.setPreferredSize(new Dimension(1200, 800));

		this.accountNavigationPanel.addAccountSelectionListener(this.accountPanel);
	}

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

		// XChartPanel<Chart> nwChartPanel_old = new
		// XChartPanel<Chart>(nwChartXCHART.chart);
		// XChartPanel<Chart> balChartPanel_old = new
		// XChartPanel<Chart>(balChartXCHART.chart);
		// XChartPanel<Chart> optChartPanel = new
		// XChartPanel<Chart>(optChartXCHART.chart);
		// this.chartView.validate();

		JPanel balPanel = this.balChart.createChartPanel();
		JPanel nwPanel = this.nwChart.createChartPanel();
		JPanel optPanel = this.optChart.createChartPanel();

		chartTabs.addTab("Balances", balPanel);
		chartTabs.addTab("Net Worth", nwPanel);
		chartTabs.addTab("ISI Options", optPanel);

//		chartTabs.addTab("Balances(old)", balChartPanel_old);
//		chartTabs.addTab("Net Worth(old)", nwChartPanel_old);
//		chartTabs.addTab("ISI Options(old)", optChartPanel);

		this.chartPanel.add(chartTabs, BorderLayout.CENTER);

		ComponentListener chartListener = new ComponentAdapter() {
			public void componentShown(ComponentEvent e) {
				if (e.getComponent() == MainWindow.this.chartPanel) {
					MainWindow.this.chartsVisible = true;
				} else if (e.getComponent() == balPanel) {
					MainWindow.this.balVisible = true;
				} else if (e.getComponent() == nwPanel) {
					MainWindow.this.nwVisible = true;
				} else if (e.getComponent() == optPanel) {
					MainWindow.this.optVisible = true;
				}

				MainWindow.this.chartNeedsRefresh = true;
				updateChartPanel(false);
			}

			public void componentHidden(ComponentEvent e) {
				if (e.getComponent() == MainWindow.this.chartPanel) {
					MainWindow.this.chartsVisible = false;
				} else if (e.getComponent() == balPanel) {
					MainWindow.this.balVisible = false;
				} else if (e.getComponent() == nwPanel) {
					MainWindow.this.nwVisible = false;
				} else if (e.getComponent() == optPanel) {
					MainWindow.this.optVisible = false;
				}
			}
		};

		this.chartPanel.addComponentListener(chartListener);

		balPanel.addComponentListener(chartListener);
		nwPanel.addComponentListener(chartListener);
		optPanel.addComponentListener(chartListener);
	}

	public void updateChartPanel(boolean refresh) {
		if (!this.chartsVisible) {
			this.chartNeedsRefresh = true;
			return;
		} else if (!refresh && !this.chartNeedsRefresh) {
			return;
		}

		this.chartNeedsRefresh = false;

		if (this.balVisible) {
			this.balChart.update();
			this.balChartXCHART.update();
		} else if (this.nwVisible) {
			this.nwChart.update();
			this.nwChartXCHART.update();
		} else if (this.optVisible) {
			this.optChart.update();
			this.optChartXCHART.update();
		}

		this.chartPanel.repaint();
	}

	public void addAccountSelectionListener(AccountSelectionListener listener) {
		this.accountNavigationPanel.addAccountSelectionListener(listener);
	}

	public void setSplitPosition() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				MainWindow.this.accountViewSplit.setDividerLocation(.25);
				MainWindow.this.accountPanel.setSplitPosition();
			}
		});
	}
}
