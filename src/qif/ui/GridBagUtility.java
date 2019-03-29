package qif.ui;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Helper class for creating fields for GridBag layout forms */
class GridBagUtility {
	public static Font bold12 = new Font("Helvetica", Font.BOLD, 12);
	public static Font plain12 = new Font("Helvetica", Font.PLAIN, 12);
	public static Font bold16 = new Font("Helvetica", Font.BOLD, 16);
	public static Font plain16 = new Font("Helvetica", Font.PLAIN, 16);
	public static Font bold20 = new Font("Helvetica", Font.BOLD, 16);

	/** Create a named label with constraints and text */
	public static JLabel addLabeledValue(JPanel panel, GridBagConstraints gbc, //
			int row, int col, String text) {
		return addLabeledValue(panel, gbc, row, col, text, bold12, plain12);
	}

	/** Create an unnamed label with constraints */
	public static JLabel addValue(JPanel panel, GridBagConstraints gbc, //
			int row, int col) {
		return addValue(panel, gbc, row, col, plain12);
	}

	/** Create an text input field with constraints */
	public static JTextField addTextField(JPanel panel, GridBagConstraints gbc, //
			int row, int col) {
		return addTextField(panel, gbc, row, col, plain12);
	}

	/** Create a named label with constraints and text with font size */
	public static JLabel addLabeledValue(JPanel panel, GridBagConstraints gbc, //
			int row, int col, String text, int fontsize) {
		Font lf = new Font("Helvetica", Font.BOLD, fontsize);
		Font vf = new Font("Helvetica", Font.PLAIN, fontsize);

		return addLabeledValue(panel, gbc, row, col, text, lf, vf);
	}

	/** Create an unnamed label with constraints and font size */
	public static JLabel addValue(JPanel panel, GridBagConstraints gbc, //
			int row, int col, int fontsize) {
		Font vf = new Font("Helvetica", Font.PLAIN, fontsize);

		return addValue(panel, gbc, row, col, vf);
	}

	/** Create a named label with constraints and text with fonts */
	public static JLabel addLabeledValue(JPanel panel, GridBagConstraints gbc, //
			int row, int col, String text, Font labelFont, Font valueFont) {
		GridLayout layout = new GridLayout(1, 2);
		JPanel innerPanel = new JPanel(layout);

		layout.setHgap(10);
		innerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		JLabel label = new JLabel(text);
		label.setFont(labelFont);
		innerPanel.add(label);

		label = new JLabel("---");
		label.setFont(valueFont);
		innerPanel.add(label);

		gbc.gridy = row;
		gbc.gridx = col;
		panel.add(innerPanel, gbc);

		return label;
	}

	/** Create an unnamed label with constraints and font */
	public static JLabel addValue(JPanel panel, GridBagConstraints gbc, //
			int row, int col, Font valueFont) {
		JLabel value = new JLabel("---");
		value.setFont(valueFont);

		gbc.gridy = row;
		gbc.gridx = col;
		panel.add(value, gbc);

		return value;
	}

	/** Create an text input field with constraints and font */
	public static JTextField addTextField(JPanel panel, GridBagConstraints gbc, //
			int row, int col, Font valueFont) {
		JTextField value = new JTextField("---");
		value.setFont(valueFont);

		gbc.gridy = row;
		gbc.gridx = col;
		panel.add(value, gbc);

		return value;
	}
}