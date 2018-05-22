package qif.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import qif.data.GenericTxn;
import qif.ui.model.TransactionTableModel;

public class TransactionPanel extends JScrollPane {
	private static final long serialVersionUID = 1L;

	TransactionTableModel transactionTableModel;
	JTable transactionTable;

	boolean showCleared = false;

	public TransactionPanel(boolean showCleared) {
		super(new JTable(new TransactionTableModel()));

		this.showCleared = showCleared;

		transactionTable = (JTable) getViewport().getView();
		this.transactionTableModel = (TransactionTableModel) transactionTable.getModel();
		transactionTable.setFillsViewportHeight(true);

		if (this.showCleared) {
			transactionTable.setDefaultRenderer(Object.class, new TransactionTableCellRenderer());
		}

		TableColumnModel tranColumnModel = transactionTable.getColumnModel();

		int twidths[] = { 70, 100, 90, 90, 110, 90 };
		for (int i = 0; i < twidths.length; i++) {
			switch (i) {
			case 0:
			case 2:
			case 5:
				tranColumnModel.getColumn(i).setMinWidth(twidths[i]);
				tranColumnModel.getColumn(i).setMaxWidth(twidths[i]);
				break;

			case 1:
			case 3:
			case 4:
				tranColumnModel.getColumn(i).setMinWidth(twidths[i]);
				break;

			default:
				tranColumnModel.getColumn(i).setPreferredWidth(twidths[i]);
				break;
			}
		}

		// Scroll to display the last (most recent) transaction
		transactionTable.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				Rectangle lastRow = transactionTable.getCellRect( //
						transactionTable.getRowCount() - 1, 0, true);
				transactionTable.scrollRectToVisible(lastRow);
			}
		});
	}
}

class TransactionTableCellRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = 1L;

	private static Font boldFont = new Font("Helvetica Bold", Font.PLAIN, 12);
	private static Font regularFont = new Font("Helvetica", Font.PLAIN, 12);

	private static Color gray1 = new Color(255, 255, 200);

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int col) {
		Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

		TransactionTableModel model = (TransactionTableModel) table.getModel();
		GenericTxn tx = model.getTransactionAt(row);

		boolean bold = (tx != null && tx.isCleared());

		if (bold) {
			c.setFont(boldFont);
			c.setForeground(Color.BLACK);
			c.setBackground(gray1);
		} else {
			c.setFont(regularFont);
			c.setForeground(Color.BLACK);
			c.setBackground(Color.WHITE);
		}

		return c;
	}
}