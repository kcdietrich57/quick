package moneymgr.ui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import moneymgr.model.MoneyMgrModel;

/** Top level application frame */
@SuppressWarnings("serial")
public class MainFrame extends JFrame {
	public static MainFrame appFrame;

	public MainWindow mainWindow;
	public MoneyMgrModel model;

	/** Create the GUI and show it. (Run in event-dispatching thread). */
	private static void createAndShowGUI(MoneyMgrModel model) {
		appFrame = new MainFrame("Money Manager", model);
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

	public static void createUI(MoneyMgrModel model) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI(model);
			}
		});
	}

	public MainFrame(String name, MoneyMgrModel model) {
		super(name);

		this.model = model;
	}

	public void setModel(MoneyMgrModel model) {
		this.model = model;

		// TODO refresh UI
	}
}
