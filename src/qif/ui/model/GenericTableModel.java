package qif.ui.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import qif.ui.model.TableProperties.ColumnProperties;

@SuppressWarnings("serial")
abstract class GenericTableModel extends AbstractTableModel {

	public class ColumnWidthListener implements PropertyChangeListener {
		private final GenericTableModel tableModel;
		private final TableColumnModel columnModel;

		public ColumnWidthListener( //
				GenericTableModel tableModel, //
				TableColumnModel columnModel) {
			this.tableModel = tableModel;
			this.columnModel = columnModel;
		}

		public void propertyChange(PropertyChangeEvent e) {
			if (e.getPropertyName().equals("preferredWidth")) {
				TableColumn tableColumn = (TableColumn) e.getSource();
				int index = columnModel.getColumnIndex(tableColumn.getHeaderValue());

				int newval = ((Integer) e.getNewValue()).intValue();
				System.out.println(propertyName + ": setting width " + index + " to " + newval);
				this.tableModel.setColumnWidth(index, newval);
			}
		}
	};

	private String propertyName;
	private TableProperties properties = null;

	public GenericTableModel(String propName) {
		this.propertyName = propName;

		if (properties == null) {
			properties = new TableProperties(new String[] { //
					"Date", "Type", "Payee", "Amount", //
					"Category", "Memo", "Shares", "Cash Balance" //
			});

			properties.load(this.propertyName);
		}
	}

	public void addColumnWidthListeners(JTable table) {
		PropertyChangeListener colWidthListener = //
				new ColumnWidthListener(this, table.getColumnModel());

		for (int colnum = 0; colnum < table.getColumnCount(); ++colnum) {
			TableColumn col = table.getColumnModel().getColumn(colnum);
			col.addPropertyChangeListener(colWidthListener);
		}
	}

	public void updateQifColumnProperties() {
		properties.save(this.propertyName);
	}

	public void setColumnWidths(TableColumnModel tranColumnModel) {
		for (int idx = 0; idx < properties.getNumVisibleColumns(); ++idx) {
			ColumnProperties cprop = properties.getVisibleColumnProperties(idx);

			tranColumnModel.getColumn(cprop.position).setPreferredWidth(cprop.width);
		}
	}

	public void setColumnWidth(int idx, int value) {
		ColumnProperties cprop = properties.getColumnProperties(idx);

		if (cprop != null) {
			cprop.width = value;
		}
	}

	public int getColumnCount() {
		return properties.getNumVisibleColumns();
	}

	public String getColumnName(int col) {
		ColumnProperties cprop = properties.getVisibleColumnProperties(col);

		return (cprop != null) ? cprop.name : "???";
	}
}