package moneymgr.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import moneymgr.model.Account;
import moneymgr.model.SecurityPortfolio;
import moneymgr.model.SecurityPosition;
import moneymgr.model.Statement;
import moneymgr.ui.model.StatementTableModel;
import moneymgr.util.Common;
import moneymgr.util.QDate;

// TODO Holdings table is very lame
/**
 * This displays statements for the current account and supports selection<br>
 * Title<br>
 * Statements | Holdings
 */
@SuppressWarnings("serial")
public class AccountInfoStatementPanel extends JPanel //
		implements AccountSelectionListener {

	private JPanel titlePanel;
	private JTable statementTable;
	private StatementTableModel statementTableModel;
	private JScrollPane scroller;
	private JTable statementHoldingsTable;
	private StatementHoldingsTableModel statementHoldingsTableModel;

	private List<StatementSelectionListener> stmtSelListeners;

	public AccountInfoStatementPanel() {
		setLayout(new BorderLayout());

		this.stmtSelListeners = new ArrayList<>();

		this.titlePanel = new JPanel(new GridBagLayout());
		this.titlePanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.DARK_GRAY));

		JLabel title = new JLabel("Statements");
		title.setFont(new Font("Helvetica", Font.BOLD, 14));
		title.setForeground(Color.DARK_GRAY);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(3, 3, 3, 3);
		this.titlePanel.add(title, gbc);

		this.statementHoldingsTableModel = new StatementHoldingsTableModel();
		this.statementHoldingsTable = new JTable(this.statementHoldingsTableModel);
//		this.statementHoldingsTable.setDefaultRenderer( //
//				Object.class, new StatementHoldingsTableCellRenderer());

		JScrollPane statementHoldingsTableScroller = //
				new JScrollPane(this.statementHoldingsTable);
		this.statementHoldingsTable.setMinimumSize(new Dimension(200, 50));
		this.statementHoldingsTable.setMaximumSize(new Dimension(500, 75));
		this.statementHoldingsTable.setPreferredScrollableViewportSize(new Dimension(200, 100));

		this.statementTableModel = new StatementTableModel();
		this.statementTable = new JTable(this.statementTableModel);
		this.scroller = new JScrollPane(this.statementTable);
		this.statementTable.setFillsViewportHeight(true);

		this.statementTable.setDefaultRenderer(Object.class, //
				new StatementTableCellRenderer());

		add(this.titlePanel, BorderLayout.NORTH);
		add(statementHoldingsTableScroller, BorderLayout.EAST);
		add(this.scroller, BorderLayout.CENTER);

		TableColumnModel statColumnModel = this.statementTable.getColumnModel();

		int swidths[] = { 30, 70, 80, 80, 80, 80, 80, 40 };
		for (int i = 0; i < swidths.length; i++) {
			statColumnModel.getColumn(i).setMinWidth(swidths[i]);
			statColumnModel.getColumn(i).setMaxWidth(swidths[i]);
		}

		ListSelectionModel statementSelectionModel = this.statementTable.getSelectionModel();
		statementSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		statementSelectionModel.addListSelectionListener( //
				new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent e) {
						if (e.getValueIsAdjusting()) {
							return;
						}

						selectStatementHandler();
					}
				});

		setActions();
		addContextMenu();
	}

	public void addStatementSelectionListener(StatementSelectionListener listener) {
		this.stmtSelListeners.add(listener);
	}

	public void accountSelected(Account account, boolean update) {
		this.statementTableModel.accountSelected(account, update);
	}

	private void setActions() {
		this.statementTableModel.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				selectStatementHandler();
			}
		});

		// Scroll to display the last (most recent) transaction
		this.statementTable.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				Rectangle lastRow = AccountInfoStatementPanel.this.statementTable.getCellRect( //
						AccountInfoStatementPanel.this.statementTable.getRowCount() - 1, 0, true);
				AccountInfoStatementPanel.this.statementTable.scrollRectToVisible(lastRow);
			}
		});
	}

	int getFirstVisibleStatementRow() {
		JViewport viewport = this.scroller.getViewport();
		Point p = viewport.getViewPosition();
		int rowIndex = this.statementTable.rowAtPoint(p);

		return rowIndex;
	}

	private Statement getSelectedStatement() {
		Statement stmt = null;

		if (this.statementTable.getSelectedRowCount() > 0) {
			int[] selidx = this.statementTable.getSelectedRows();
			stmt = this.statementTableModel.getStatementAt(selidx[0]);
		}

		return stmt;
	}

	protected void selectStatementHandler() {
		Statement stmt = getSelectedStatement();

		this.statementHoldingsTableModel.setStatement(stmt);
		this.statementHoldingsTableModel.fireTableDataChanged();

		for (StatementSelectionListener l : this.stmtSelListeners) {
			l.statementSelected(stmt);
		}
	}

	private void addContextMenu() {
		final JPopupMenu statPopupMenu = new JPopupMenu();
		JMenuItem chooseStatItem = new JMenuItem("Choose Statement");

		chooseStatItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int idx = AccountInfoStatementPanel.this.statementTable.getSelectionModel().getMinSelectionIndex();
				Object a = AccountInfoStatementPanel.this.statementTableModel.getValueAt(idx, 0);

				JOptionPane.showMessageDialog(MainFrame.appFrame, //
						"You chose statement " + a.toString());
			}
		});

		statPopupMenu.add(chooseStatItem);
		this.statementTable.setComponentPopupMenu(statPopupMenu);

		statPopupMenu.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						int rowAtPoint = AccountInfoStatementPanel.this.statementTable.rowAtPoint( //
								SwingUtilities.convertPoint(statPopupMenu, new Point(0, 0),
										AccountInfoStatementPanel.this.statementTable));

						if (rowAtPoint > -1) {
							AccountInfoStatementPanel.this.statementTable.setRowSelectionInterval(rowAtPoint,
									rowAtPoint);
						}
					}
				});
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				// Auto-generated method stub
			}

			public void popupMenuCanceled(PopupMenuEvent e) {
				// Auto-generated method stub
			}
		});
	}
}

@SuppressWarnings("serial")
class StatementTableCellRenderer extends DefaultTableCellRenderer {
	private static Font regularFont = new Font("Helvetica", Font.BOLD, 12);
	private static Color regularColor = Color.BLACK;
	private static Color regularBackground = UIConstants.LIGHT_BLUE;

	private static Font unclearedFont = new Font("Helvetica", Font.PLAIN, 12);
	private static Color unclearedColor = Color.BLUE;
	private static Color unclearedBackground = Color.WHITE;

	private static Font futureFont = new Font("Helvetica", Font.ITALIC, 12);
	private static Color futureColor = Color.GRAY;
	private static Color futureBackground = Color.WHITE;

	public Component getTableCellRendererComponent( //
			JTable table, Object value, boolean isSelected, boolean hasFocus, //
			int row, int col) {
		Component c = super.getTableCellRendererComponent( //
				table, value, isSelected, hasFocus, row, col);

		StatementTableModel model = (StatementTableModel) table.getModel();
		Statement stmt = model.getStatementAt(row);

		boolean future = !stmt.isBalanced() && //
				(stmt.date.compareTo(QDate.today()) > 0);

		if (stmt.isBalanced()) {
			c.setFont(regularFont);
			c.setForeground(regularColor);
			c.setBackground(regularBackground);
		} else if (future) {
			c.setFont(futureFont);
			c.setForeground(futureColor);
			c.setBackground(futureBackground);
		} else {
			c.setFont(unclearedFont);
			c.setForeground(unclearedColor);
			c.setBackground(unclearedBackground);
		}

		if (isSelected) {
			c.setBackground(UIConstants.LIGHT_YELLOW);
		}

		switch (col) {
		case 2:
		case 3:
		case 5:
		case 6:
		case 7:
			setHorizontalAlignment(SwingConstants.RIGHT);
			break;
		}

		return c;
	}
}

@SuppressWarnings("serial")
class StatementHoldingsTableModel extends AbstractTableModel {
	private static final String headers[] = { "Security", "Start", "End" };

	private Statement stmt;

	public SecurityPortfolio.HoldingsComparison holdingsComparision;

	public void setStatement(Statement stmt) {
		this.stmt = stmt;
	}

	public int getRowCount() {
		return ((this.stmt != null) && (this.stmt.holdings != null)) //
				? this.stmt.holdings.size() //
				: 0;
	}

	public int getColumnCount() {
		return 3;
	}

	public String getColumnName(int col) {
		return headers[col];
	}

	public Object getValueAt(int row, int col) {
		SecurityPosition pos = this.stmt.holdings.getPositions().get(row);

		BigDecimal shares;
		QDate open = stmt.getOpeningDate();

		switch (col) {
		case 0:
			return pos.security.getName();
		case 1:
			shares = pos.getSharesForDate(open);
			return Common.formatAmount3(shares);
		case 2:
			shares = pos.getSharesForDate(stmt.date);
			return Common.formatAmount3(shares);
		}

		return "N/A";
	}
}

//@SuppressWarnings("serial")
//class StatementHoldingsTableCellRenderer extends DefaultTableCellRenderer {
//	private static final Font BALANCED_FONT = new Font("Helvetica", Font.PLAIN, 12);
//	private static final Color BALANCED_COLOR = Color.BLACK;
//	private static final Font UNBALANCED_FONT = new Font("Helvetica", Font.BOLD, 14);
//	private static final Color UNBALANCED_COLOR = Color.RED;
//
//	public Component getTableCellRendererComponent( //
//			JTable table, Object value, boolean isSelected, boolean hasFocus, //
//			int row, int column) {
//		Component c = super.getTableCellRendererComponent( //
//				table, value, isSelected, hasFocus, row, column);
//
//		StatementHoldingsTableModel model = (StatementHoldingsTableModel) table.getModel();
//
//		if (model.getValueAt(row, 1).equals(model.getValueAt(row, 2))) {
//			c.setFont(BALANCED_FONT);
//			c.setForeground(BALANCED_COLOR);
//		} else {
//			c.setFont(UNBALANCED_FONT);
//			c.setForeground(UNBALANCED_COLOR);
//		}
//
//		return c;
//	}
//}
