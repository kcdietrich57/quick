package qif.ui.model;

import java.util.ArrayList;
import java.util.List;

import qif.data.QifDom;

/**
 * Persistable properties for a displayed table.<br>
 * (e.g. column width/positions/visibility)
 */
public class TableProperties {
	/** Properties of one column */
	public class ColumnProperties {
		public final String name;
		public final int id;
		public int position;
		public int width;
		public boolean visible;

		public ColumnProperties(String name, int id, int position, int width, boolean visible) {
			this.id = id;
			this.name = name;
			this.position = position;
			this.width = width;
			this.visible = visible;
		}
	}

	private final List<ColumnProperties> columns = new ArrayList<ColumnProperties>();

	/** Construct with default column properties */
	public TableProperties(String[] columnNames) {
		for (String cname : columnNames) {
			addColumn(cname, 100);
		}
	}

	/** Load settings from property archive */
	public void load(String keyroot) {
		for (ColumnProperties cprop : columns) {
			String key = String.format("%s.%s", keyroot, cprop.name);

			String propstr = QifDom.qifProperties.getProperty(key);

			if (propstr != null) {
				String[] props = propstr.split(",");

				cprop.position = Integer.parseInt(props[1]);
				cprop.width = Integer.parseInt(props[2]);
				cprop.visible = props[3].charAt(0) == 'y';
			}
		}
	}

	/** Save settings in property archive */
	public void save(String keyroot) {
		for (ColumnProperties cprop : columns) {
			String key = String.format("%s.%s", keyroot, cprop.name);

			String propstr = String.format("%d,%d,%d,%s", //
					cprop.id, cprop.position, cprop.width, //
					((cprop.visible) ? "y" : "n"));

			QifDom.qifProperties.setProperty(key, propstr);
		}
	}

	public int getNumColumns() {
		return this.columns.size();
	}

	public int getNumVisibleColumns() {
		int numvis = 0;

		for (ColumnProperties cprop : this.columns) {
			if (cprop.visible) {
				++numvis;
			}
		}

		return numvis;
	}

	/**
	 * Get properties of a column, only considering visible columns.
	 * 
	 * @param idx Visible column index (skipping hidden columns)
	 */
	public ColumnProperties getVisibleColumnProperties(int idx) {
		for (ColumnProperties cprop : this.columns) {
			if (cprop.visible && cprop.position == idx) {
				return cprop;
			}
		}

		return null;
	}

	/**
	 * Get width of a column, only considering visible columns.
	 * 
	 * @param idx Visible column index (skipping hidden columns)
	 */
	public int getVisibleColumnWidth(int idx) {
		ColumnProperties cprop = getVisibleColumnProperties(idx);

		return (cprop != null) ? cprop.width : -1;
	}

	/**
	 * Set width of a column, only considering visible columns.
	 * 
	 * @param idx Visible column index (skipping hidden columns)
	 */
	public void setVisibleColumnWidth(int idx, int width) {
		ColumnProperties cprop = getVisibleColumnProperties(idx);

		if (cprop != null) {
			cprop.width = width;
		}
	}

	/** Add a column with a specified name and width */
	public void addColumn(String name, int width) {
		int id = this.columns.size();

		ColumnProperties cprop = new ColumnProperties(name, id, id, width, true);

		this.columns.add(cprop);
	}

	/** Get properties of a column at the given position */
	public ColumnProperties getColumnProperties(int idx) {
		return ((idx >= 0) && (idx < this.columns.size())) //
				? this.columns.get(idx) //
				: null;
	}
}