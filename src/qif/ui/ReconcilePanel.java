package qif.ui;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

public class ReconcilePanel extends JPanel {
	private static final long serialVersionUID = 1L;

	public ReconcilePanel() {
		super(new BorderLayout());

		JButton details = new JButton("Reconcile details go here");

		add(details, BorderLayout.NORTH);
		
		add(new JButton("Reconcile panel goes here"), BorderLayout.CENTER);
	}
}