package qif.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

class MainFrame extends JFrame {
	private static final long serialVersionUID = 1L;

	public MainWindow mainWindow;
	public static MainFrame frame;

	/** Create the GUI and show it. (Run in event-dispatching thread). */
	private static void createAndShowGUI() {
		frame = new MainFrame("Money Manager");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Create and set up the content pane.
		MainWindow mainWindow = new MainWindow();
		mainWindow.setOpaque(true);
		frame.setContentPane(mainWindow);
		frame.mainWindow = mainWindow;

		// Display the window.
		frame.pack();
		frame.setVisible(true);

		mainWindow.setSplitPosition();
	}

	public static void createUI() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}

	public MainFrame(String name) {
		super(name);
	}
}

public class MainWindow extends JPanel {
	private static final long serialVersionUID = 1L;

	private JSplitPane statementViewSplit;
	private JSplitPane accountViewSplit;

	JButton dashboardPanel;
	AccountNavigationPanel accountNavigationPanel;
	StatementPanel statementPanel;
	TransactionPanel transactionPanel;
	StatementDetailsPanel statementDetails;
	JButton acctInfoPanel;

	public void setSplitPosition() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				accountViewSplit.setDividerLocation(.25);
				statementViewSplit.setDividerLocation(.25);
			}
		});
	}

	public MainWindow() {
		super(new BorderLayout());

		accountNavigationPanel = new AccountNavigationPanel();

		statementPanel = new StatementPanel();
		this.accountNavigationPanel.accountPanel.statementTableModel = statementPanel.statementTableModel;

		transactionPanel = new TransactionPanel(true);
		this.accountNavigationPanel.accountPanel.transactionTableModel = transactionPanel.transactionTableModel;

		statementDetails = new StatementDetailsPanel();
		this.statementPanel.statementDetails = statementDetails;
		this.statementPanel.statementTableModel.detailsPanel = statementDetails;

		dashboardPanel = new JButton("Dashboard goes here");

		JTabbedPane acctTabs = new JTabbedPane();

		statementViewSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, statementPanel, statementDetails);

		acctTabs.addTab("Register View", transactionPanel);
		acctTabs.add("Statement View", statementViewSplit);

		JPanel acctPanel = new JPanel(new BorderLayout());
		acctInfoPanel = new JButton("---");
		acctInfoPanel.setFont(new Font("Helvetica", Font.BOLD, 20));
		acctPanel.add(acctInfoPanel, BorderLayout.NORTH);
		acctPanel.add(acctTabs, BorderLayout.CENTER);
		this.accountNavigationPanel.accountPanel.acctInfoPanel = this.acctInfoPanel;
		
		accountViewSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, accountNavigationPanel, acctPanel);
		accountViewSplit.setPreferredSize(new Dimension(1200, 800));

		JTabbedPane topTabs = new JTabbedPane();
		topTabs.add("Dashboard", dashboardPanel);
		topTabs.add("Accounts", accountViewSplit);

		add(topTabs, BorderLayout.CENTER);
	}
}
