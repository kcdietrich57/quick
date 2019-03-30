package moneymgr.ui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Top level application frame */
@SuppressWarnings("serial")
public class MainFrame extends JFrame {
	public static MainFrame appFrame;

	public MainWindow mainWindow;

	/** Create the GUI and show it. (Run in event-dispatching thread). */
	private static void createAndShowGUI() {
		appFrame = new MainFrame("Money Manager");
		appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		MainWindow mainWindow = new MainWindow();
		mainWindow.setOpaque(true);
		appFrame.setContentPane(mainWindow);
		appFrame.mainWindow = mainWindow;

		appFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent winEvt) {
				mainWindow.saveProperties();

				System.exit(0);
			}
		});

		appFrame.pack();
		appFrame.setVisible(true);

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
