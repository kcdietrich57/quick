package qif.ui;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class MainFrame extends JFrame {
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

		// TODO testing chart stuff
//		NetWorthChart.showISIOptions();
//		NetWorthChart.showBalances();
//		NetWorthChart.showNetWorth();
	}

	public MainFrame(String name) {
		super(name);
	}
}
