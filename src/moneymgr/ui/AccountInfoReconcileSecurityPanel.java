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

import moneymgr.model.SecurityPortfolio;
import moneymgr.model.Statement;
import moneymgr.util.Common;

@SuppressWarnings("serial")
public class AccountInfoReconcileSecurityPanel //
		extends JPanel //
		implements StatementSelectionListener {
	private AccountInfoReconcilePanel reconcilePanel;
	private JTable holdingsTable;
	private HoldingsTableModel holdingsTableModel;
	private Statement stmt;
	public boolean holdingsMatch = false;

	public AccountInfoReconcileSecurityPanel( //
			AccountInfoReconcilePanel reconcilePanel) {
		this.reconcilePanel = reconcilePanel;

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
			SecurityPortfolio.HoldingsComparison comparison = //
					this.stmt.holdings.comparisonTo(reconcilePanel.getPortfolioDelta());
			this.holdingsTableModel.holdingsComparision = comparison;
			holdingsMatch = comparison.holdingsMatch();
		}

		this.holdingsTableModel.fireTableDataChanged();
	}
}

@SuppressWarnings("serial")
class HoldingsTableModel extends AbstractTableModel {
	private static final String headers[] = { "Security", "Desired", "Actual" };

	public SecurityPortfolio.HoldingsComparison holdingsComparision;

	public int getRowCount() {
		return (this.holdingsComparision != null) //
				? this.holdingsComparision.desiredPositions.size() //
				: 0;
	}

	public int getColumnCount() {
		return 3;
	}

	public String getColumnName(int col) {
		return headers[col];
	}

	public Object getValueAt(int row, int col) {
		switch (col) {
		case 0:
			return this.holdingsComparision.getSecurityName(row);
		case 1:
			return Common.formatAmount3(this.holdingsComparision.getDesiredShares(row));
		case 2:
			return Common.formatAmount3(this.holdingsComparision.getActualShares(row));
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
