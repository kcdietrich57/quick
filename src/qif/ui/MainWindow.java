package qif.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.internal.chartpart.Chart;

import qif.data.GenericTxn;
import qif.data.QDate;

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

	public QDate asOfDate = QDate.today();

	public Dashboard dashboardPanel;
	public JPanel chartPanel;

	private JSplitPane accountViewSplit;

	public MainWindow() {
		super(new BorderLayout());

		instance = this;

		createTimeSlider();
		createContentPanel();

		add(contentPanel, BorderLayout.CENTER);
		add(this.asOfDatePanel, BorderLayout.SOUTH);
		// add(new JButton("Status Bar Goes Here"), BorderLayout.SOUTH);
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
			this.accountNavigationPanel.refreshAccountList();
			this.summaryPanel.updateValues();
		}
	}

	private void createContentPanel() {
		dashboardPanel = new Dashboard();
		createAccountsPanel();
		createChartsPanel();

		contentPanel = new JTabbedPane();

		contentPanel.add("Accounts", accountViewSplit);
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
		chartPanel = new JPanel(new BorderLayout());
		JTabbedPane chartTabs = new JTabbedPane();

		XYChart nwChart = NetWorthChart.createNetWorthChart();
		CategoryChart balChart = NetWorthChart.createBalancesChart();
		XYChart optChart = NetWorthChart.createISIOptionsChart();

		XChartPanel<Chart> nwChartPanel = new XChartPanel<Chart>(nwChart);
		XChartPanel<Chart> balChartPanel = new XChartPanel<Chart>(balChart);
		XChartPanel<Chart> optChartPanel = new XChartPanel<Chart>(optChart);
		// this.chartView.validate();

		chartTabs.addTab("Balances", balChartPanel);
		chartTabs.addTab("Net Worth", nwChartPanel);
		chartTabs.addTab("ISI Options", optChartPanel);

		this.chartPanel.add(chartTabs, BorderLayout.CENTER);
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
