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
import org.knowm.xchart.XYChart;
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

	public QDate asOfDate = QDate.today();

	public Dashboard dashboardPanel;
	public JPanel chartPanel;
	public BalanceChart balChart;
	private JSplitPane accountViewSplit;

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

		XYChart nwChart = NetWorthChart.createNetWorthChart();
		this.balChart = new BalanceChart();
		this.balChart.create();
		XYChart optChart = NetWorthChart.createISIOptionsChart();

		XChartPanel<Chart> nwChartPanel = new XChartPanel<Chart>(nwChart);
		XChartPanel<Chart> balChartPanel = new XChartPanel<Chart>(balChart.chart);
		XChartPanel<Chart> optChartPanel = new XChartPanel<Chart>(optChart);
		// this.chartView.validate();

		chartTabs.addTab("Balances", balChartPanel);
		chartTabs.addTab("Net Worth", nwChartPanel);
		chartTabs.addTab("ISI Options", optChartPanel);

		this.chartPanel.add(chartTabs, BorderLayout.CENTER);
	}
	
	private void updateChartPanel() {
		this.balChart.update();
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
