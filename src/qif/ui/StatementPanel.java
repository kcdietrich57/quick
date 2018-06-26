package qif.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import qif.data.Account;
import qif.data.Statement;
import qif.ui.model.StatementTableModel;

/**
 * This panel displays statements for the current account and supports selection
 */
@SuppressWarnings("serial")
public class StatementPanel //
		extends JPanel //
		implements AccountSelectionListener {

	private JTable statementTable;
	private StatementTableModel statementTableModel;
	private JScrollPane scroller;

	private List<StatementSelectionListener> stmtSelListeners;

	public StatementPanel() {
		setLayout(new BorderLayout());

		stmtSelListeners = new ArrayList<StatementSelectionListener>();

		JPanel titlePanel = new JPanel(new GridBagLayout());
		titlePanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.DARK_GRAY));

		JLabel title = new JLabel("Statements");
		title.setFont(new Font("Helvetica", Font.BOLD, 14));
		title.setForeground(Color.DARK_GRAY);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(3, 3, 3, 3);
		titlePanel.add(title, gbc);

		statementTableModel = new StatementTableModel();
		statementTable = new JTable(statementTableModel);
		this.scroller = new JScrollPane(this.statementTable);
		statementTable.setFillsViewportHeight(true);

		statementTable.setDefaultRenderer(Object.class, //
				new StatementTableCellRenderer());

		add(titlePanel, BorderLayout.NORTH);
		add(this.scroller, BorderLayout.CENTER);

		TableColumnModel statColumnModel = statementTable.getColumnModel();

		int swidths[] = { 30, 70, 80, 80, 80, 80, 80, 40 };
		for (int i = 0; i < swidths.length; i++) {
			statColumnModel.getColumn(i).setMinWidth(swidths[i]);
			statColumnModel.getColumn(i).setMaxWidth(swidths[i]);
		}

		ListSelectionModel statementSelectionModel = statementTable.getSelectionModel();
		statementSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		statementSelectionModel.addListSelectionListener( //
				new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent e) {
						if (e.getValueIsAdjusting())
							return;

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
		statementTable.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				Rectangle lastRow = statementTable.getCellRect( //
						statementTable.getRowCount() - 1, 0, true);
				statementTable.scrollRectToVisible(lastRow);

				// int firstvrow = getFirstVisibleStatementRow();
				// System.out.println("fvr=" + firstvrow);
			}
		});
	}

	private void addContextMenu() {
		final JPopupMenu statPopupMenu = new JPopupMenu();
		JMenuItem chooseStatItem = new JMenuItem("Choose Statement");

		chooseStatItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int idx = statementTable.getSelectionModel().getMinSelectionIndex();
				Object a = statementTableModel.getValueAt(idx, 0);

				JOptionPane.showMessageDialog(MainFrame.frame, //
						"You chose statement " + a.toString());
			}
		});

		statPopupMenu.add(chooseStatItem);
		statementTable.setComponentPopupMenu(statPopupMenu);

		statPopupMenu.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						int rowAtPoint = statementTable.rowAtPoint( //
								SwingUtilities.convertPoint(statPopupMenu, new Point(0, 0), statementTable));

						if (rowAtPoint > -1) {
							statementTable.setRowSelectionInterval(rowAtPoint, rowAtPoint);
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

	int getFirstVisibleStatementRow() {
		JViewport viewport = this.scroller.getViewport();
		Point p = viewport.getViewPosition();
		int rowIndex = statementTable.rowAtPoint(p);

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
		Statement s = getSelectedStatement();

		for (StatementSelectionListener l : this.stmtSelListeners) {
			l.statementSelected(s);
		}
	}
}

@SuppressWarnings("serial")
class StatementTableCellRenderer extends DefaultTableCellRenderer {
	private static Font regularFont = new Font("Helvetica", Font.BOLD, 12);
	private static Color regularColor = Color.BLACK;
	private static Color regularBackground = new Color(250, 250, 220);

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

		boolean cleared = stmt.isBalanced;
		boolean future = !cleared && (stmt.cashBalance == null);

		if (cleared) {
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

		switch (col) {
		case 2:
		case 3:
		case 5:
		case 6:
		case 7:
			setHorizontalAlignment(JLabel.RIGHT);
			break;
		}

		return c;
	}
}