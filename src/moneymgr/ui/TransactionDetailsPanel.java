package moneymgr.ui;

import java.awt.Font;

import javax.swing.JTextArea;

@SuppressWarnings("serial")
public class TransactionDetailsPanel //
		extends JTextArea {
	public TransactionDetailsPanel(int rows, int columns) {
		super(rows, columns);

		setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
	}
}