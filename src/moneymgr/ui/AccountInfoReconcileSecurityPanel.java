package moneymgr.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import moneymgr.model.SecurityPosition;
import moneymgr.model.Statement;
import moneymgr.util.Common;

@SuppressWarnings("serial")
public class AccountInfoReconcileSecurityPanel //
		extends JPanel //
		implements StatementSelectionListener {
	private JTable holdingsTable;
	private HoldingsTableModel holdingsTableModel;
	private Statement stmt;

	public AccountInfoReconcileSecurityPanel() {
		this.holdingsTableModel = new HoldingsTableModel();
		this.holdingsTable = new JTable(this.holdingsTableModel);
		this.holdingsTable.setDefaultRenderer(Object.class, new HoldingsTableCellRenderer());

		JScrollPane holdingsTableScroller = new JScrollPane(this.holdingsTable);

		add(holdingsTableScroller, BorderLayout.CENTER);
	}

	public void statementSelected(Statement stmt) {
		if (stmt != this.stmt) {
			this.stmt = stmt;

			updateValues();
		}
	}

	public void updateValues() {
		if ((this.stmt != null) && (this.stmt.holdings != null)) {
			this.holdingsTableModel.setStatement(this.stmt);
		}

		this.holdingsTableModel.fireTableDataChanged();
	}
}

@SuppressWarnings("serial")
class HoldingsTableModel extends AbstractTableModel {
	private static final String headers[] = { "Security", "Desired", "Actual" };

	private Statement stmt;

	public void setStatement(Statement stmt) {
		if (this.stmt != stmt) {
			this.stmt = stmt;
		}
	}

	public int getRowCount() {
		return (this.stmt != null) //
				? this.stmt.holdings.positions.size() //
				: 0;
	}

	public int getColumnCount() {
		return 3;
	}

	public String getColumnName(int col) {
		return headers[col];
	}

	public Object getValueAt(int row, int col) {
		SecurityPosition pos = this.stmt.holdings.positions.get(row);

		switch (col) {
		case 0:
			return pos.security.getName();
		case 1:
			return Common.formatAmount3(pos.getExpectedEndingShares());
		case 2:
			return Common.formatAmount3(pos.getEndingShares());
		}

		return "N/A";
	}
}

@SuppressWarnings("serial")
class HoldingsTableCellRenderer extends DefaultTableCellRenderer {
	// TODO put colors into UIConstants for consistency
	private static final Font BALANCED_FONT = new Font("Helvetica", Font.PLAIN, 12);
	private static final Color BALANCED_COLOR = Color.BLACK;
	private static final Font UNBALANCED_FONT = new Font("Helvetica", Font.BOLD, 14);
	private static final Color UNBALANCED_COLOR = Color.RED;

	public Component getTableCellRendererComponent( //
			JTable table, Object value, boolean isSelected, boolean hasFocus, //
			int row, int column) {
		Component c = super.getTableCellRendererComponent( //
				table, value, isSelected, hasFocus, row, column);

		HoldingsTableModel model = (HoldingsTableModel) table.getModel();

		if (model.getValueAt(row, 1).equals(model.getValueAt(row, 2))) {
			c.setFont(BALANCED_FONT);
			c.setForeground(BALANCED_COLOR);
		} else {
			c.setFont(UNBALANCED_FONT);
			c.setForeground(UNBALANCED_COLOR);
		}

		return c;
	}
}
