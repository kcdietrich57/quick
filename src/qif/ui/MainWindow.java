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

@SuppressWarnings("serial")
public class MainWindow extends JPanel {
	private JTabbedPane contentPanel;

	private Dashboard dashboardPanel;
	private AccountPanel accountPanel;
	private JPanel chartPanel;

	private JSplitPane accountViewSplit;
	private AccountNavigationPanel accountNavigationPanel;

	public MainWindow() {
		super(new BorderLayout());

		createContentPanel();

		add(new JButton("Toolbar Goes Here"), BorderLayout.NORTH);
		add(contentPanel, BorderLayout.CENTER);
		add(new JButton("Status Bar Goes Here"), BorderLayout.SOUTH);
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
