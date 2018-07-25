package qif.ui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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

		MainWindow mainWindow = new MainWindow();
		mainWindow.setOpaque(true);
		frame.setContentPane(mainWindow);
		frame.mainWindow = mainWindow;

		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent winEvt) {
				mainWindow.saveProperties();

				System.exit(0);
			}
		});

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
