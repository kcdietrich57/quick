package qif.ui;

import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumnModel;

import qif.ui.model.TransactionTableModel;

public class TransactionPanel extends JScrollPane {
	private static final long serialVersionUID = 1L;

	TransactionTableModel transactionTableModel;
	JTable transactionTable;

	public TransactionPanel() {
		super(new JTable(new TransactionTableModel()));

		transactionTable = (JTable) getViewport().getView();
		this.transactionTableModel = (TransactionTableModel)transactionTable.getModel();
		transactionTable.setFillsViewportHeight(true);

		TableColumnModel tranColumnModel = transactionTable.getColumnModel();

		int twidths[] = { 30, 70, 200, 90, 90, 110, 90 };
		for (int i = 0; i < twidths.length; i++) {
			switch (i) {
			case 0:
			case 1:
			case 3:
			case 6:
				tranColumnModel.getColumn(i).setMinWidth(twidths[i]);
				tranColumnModel.getColumn(i).setMaxWidth(twidths[i]);
				break;

			case 2:
			case 4:
			case 5:
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