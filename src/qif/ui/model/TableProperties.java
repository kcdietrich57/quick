package qif.ui.model;

import java.util.ArrayList;
import java.util.List;

import qif.data.QifDom;

public class TableProperties {
	public class ColumnProperties {
		public String name;
		public int id;
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

	private List<ColumnProperties> columns = new ArrayList<ColumnProperties>();

	public TableProperties(String[] columnNames) {
		for (String cname : columnNames) {
			addColumn(cname, 100);
		}
	}

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

	public ColumnProperties getVisibleColumnProperties(int idx) {
		for (ColumnProperties cprop : this.columns) {
			if (cprop.visible && cprop.position == idx) {
				return cprop;
			}
		}

		return null;
	}

	public int getVisibleColumnWidth(int idx) {
		ColumnProperties cprop = getVisibleColumnProperties(idx);

		return (cprop != null) ? cprop.width : -1;
	}

	public void setVisibleColumnWidth(int ii, int width) {
		ColumnProperties cprop = getVisibleColumnProperties(ii);

		if (cprop != null) {
			cprop.width = width;
		}
	}

	public void addColumn(String name, int width) {
		int id = this.columns.size();

		ColumnProperties cprop = new ColumnProperties(name, id, id, width, true);

		this.columns.add(cprop);
	}

	public ColumnProperties getColumnProperties(int idx) {
		return ((idx >= 0) && (idx < this.columns.size())) //
				? this.columns.get(idx) //
				: null;
	}
}