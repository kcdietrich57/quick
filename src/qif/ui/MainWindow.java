package qif.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;

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

	public MainWindow() {
		super(new BorderLayout());

		dashboardPanel = new Dashboard();

		accountNavigationPanel = new AccountNavigationPanel();
		accountPanel = new AccountPanel();
		// AccountPanel responds to selections in the AccountList
		accountPanel.addAccountSelectionListeners(this.accountNavigationPanel.accountListPanel);

		accountViewSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, accountNavigationPanel, accountPanel);
		accountViewSplit.setPreferredSize(new Dimension(1200, 800));

		topTabs = new JTabbedPane();
		topTabs.add("Dashboard", dashboardPanel);
		topTabs.add("Accounts", accountViewSplit);

		add(topTabs, BorderLayout.CENTER);
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
