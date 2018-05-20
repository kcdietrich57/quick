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

	private JSplitPane vsplit;
	private JSplitPane hsplit;

	AccountPanel accountPanel;
	StatementPanel statementPanel;
	TransactionPanel transactionPanel;
	StatementDetailsPanel statementDetails;

	public void setSplitPosition() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				hsplit.setDividerLocation(.3);
				vsplit.setDividerLocation(.25);
			}
		});
	}

	public MainWindow() {
		super(new BorderLayout());

		accountPanel = new AccountPanel();

		statementPanel = new StatementPanel();
		this.accountPanel.statementTableModel = statementPanel.statementTableModel;

		transactionPanel = new TransactionPanel();
		this.accountPanel.transactionTableModel = transactionPanel.transactionTableModel;

		statementDetails = new StatementDetailsPanel();
		this.statementPanel.statementDetails = statementDetails;
		this.statementPanel.statementTableModel.detailsPanel = statementDetails;

		JTabbedPane tabs = new JTabbedPane();

		vsplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, statementPanel, statementDetails);

		tabs.addTab("Register View", transactionPanel);
		tabs.add("Statement View", vsplit);

		hsplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, accountPanel, tabs);
		hsplit.setPreferredSize(new Dimension(1200, 800));

		add(hsplit, BorderLayout.CENTER);
	}
}
