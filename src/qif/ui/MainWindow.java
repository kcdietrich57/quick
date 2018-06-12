package qif.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.internal.chartpart.Chart;

public class MainWindow extends JPanel {
	private static final long serialVersionUID = 1L;

	// Window contents:
	// MainWindow
	// - topTabs
	// - - dashboardPanel
	// - - accountView (Split)
	// - - - accountNavigationPanel
	// - - - accountPanel
	// - - - - accountInfo
	// - - - - accountTabs
	// - - - - - RegisterView (Transactions)
	// - - - - - StatementView (Split)
	// - - - - - - Statements
	// - - - - - - Transactions
	private JTabbedPane topTabs;
	private Dashboard dashboardPanel;
	private JSplitPane accountViewSplit;
	private AccountNavigationPanel accountNavigationPanel;
	private AccountPanel accountPanel;
	private JPanel chartView;

	public MainWindow() {
		super(new BorderLayout());

		dashboardPanel = new Dashboard();

		accountNavigationPanel = new AccountNavigationPanel();
		accountPanel = new AccountPanel();
		// AccountPanel responds to selections in the AccountList
		accountPanel.addAccountSelectionListeners(this.accountNavigationPanel.accountListPanel);

		accountViewSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, accountNavigationPanel, accountPanel);
		accountViewSplit.setPreferredSize(new Dimension(1200, 800));

		chartView = new JPanel(new BorderLayout());
		JTabbedPane chartTabs = new JTabbedPane();
		
		XYChart nwChart = NetWorthChart.createNetWorthChart();
		CategoryChart balChart = NetWorthChart.createBalancesChart();
		XYChart optChart = NetWorthChart.createISIOptionsChart();

		XChartPanel<Chart> nwChartPanel = new XChartPanel<Chart>(nwChart);
		XChartPanel<Chart> balChartPanel = new XChartPanel<Chart>(balChart);
		XChartPanel<Chart> optChartPanel = new XChartPanel<Chart>(optChart);
//		this.chartView.validate();
		
		chartTabs.addTab("Balances", balChartPanel);
		chartTabs.addTab("Net Worth", nwChartPanel);
		chartTabs.addTab("ISI Options", optChartPanel);

		this.chartView.add(chartTabs, BorderLayout.CENTER);

		topTabs = new JTabbedPane();
		topTabs.add("Accounts", accountViewSplit);
		topTabs.add("Dashboard", dashboardPanel);
		topTabs.add("Charts", chartView);

		add(new JButton("Toolbar Goes Here"), BorderLayout.NORTH);
		add(topTabs, BorderLayout.CENTER);
		add(new JButton("Status Bar Goes Here"), BorderLayout.SOUTH);
	}

	public void addAccountSelectionListener(AccountSelectionListener listener) {
		accountNavigationPanel.accountListPanel.addAccountSelectionListener(listener);
	}

	public void setSplitPosition() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				accountViewSplit.setDividerLocation(.25);
				accountPanel.statementViewSplit.setDividerLocation(.25);
			}
		});
	}
}
