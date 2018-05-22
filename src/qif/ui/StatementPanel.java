package qif.ui;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Date;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
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
import javax.swing.table.TableColumnModel;

import qif.data.Account;
import qif.data.Statement;
import qif.ui.model.StatementTableModel;
import qif.ui.model.TransactionTableModel;

public class StatementPanel extends JScrollPane {
	private static final long serialVersionUID = 1L;

	public StatementTableModel statementTableModel;
	public TransactionTableModel transactionTableModel;
	public JTable statementTable;

	public StatementDetailsPanel statementDetails;

	public StatementPanel() {
		super(new JTable(new StatementTableModel()));

		statementTable = (JTable) getViewport().getView();
		statementTable.setFillsViewportHeight(true);
		statementTableModel = (StatementTableModel) statementTable.getModel();

		// Account table column widths
		TableColumnModel statColumnModel = statementTable.getColumnModel();

		int swidths[] = { 70, 90, 90, 90, 90, 50 };
		for (int i = 0; i < swidths.length; i++) {
			statColumnModel.getColumn(i).setMinWidth(swidths[i]);
			statColumnModel.getColumn(i).setMaxWidth(swidths[i]);
		}

		ListSelectionModel statementSelectionModel = statementTable.getSelectionModel();
		statementSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		statementSelectionModel.addListSelectionListener( //
				new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent e) {
						// selectAccountHandler(e);
					}
				});

		setActions();
	}

	private void setActions() {
		this.statementTableModel.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				statementTableModel.detailsPanel.setStatement(null);
			}
		});

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
				// TODO Auto-generated method stub
			}

			public void popupMenuCanceled(PopupMenuEvent e) {
				// TODO Auto-generated method stub
			}
		});

		// Scroll to display the last (most recent) transaction
		statementTable.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				Rectangle lastRow = statementTable.getCellRect( //
						statementTable.getRowCount() - 1, 0, true);
				statementTable.scrollRectToVisible(lastRow);

				int firstvrow = getFirstVisibleStatementRow();
				// System.out.println("fvr=" + firstvrow);
			}
		});

		ListSelectionModel statementSelectionModel = statementTable.getSelectionModel();
		statementSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		statementSelectionModel.addListSelectionListener( //
				new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent e) {
						selectStatementHandler(e);
					}
				});
	}

	int getFirstVisibleStatementRow() {
		JViewport viewport = getViewport();
		Point p = viewport.getViewPosition();
		int rowIndex = statementTable.rowAtPoint(p);

		return rowIndex;
	}

	protected void selectStatementHandler(ListSelectionEvent e) {
		if (e.getValueIsAdjusting())
			return;

		String strSource = e.getSource().toString();
		int start = strSource.indexOf("{") + 1;
		int stop = strSource.length() - 1;
		int iSelectedIndex = -1;

		try {
			iSelectedIndex = Integer.parseInt(strSource.substring(start, stop));
		} catch (Exception e2) {

		}

		Date d = statementTableModel.getDate(iSelectedIndex);
		// String dstr = (d != null) ? Common.formatDate(d) : "";
		// transactionTableModel.setStatementDate(d);

		Account a = statementTableModel.getAccount();
		Statement s = a.getStatement(d);

		statementDetails.setStatement(s);
	}

	// public void setSelectedAccount(Account acct) {
	// this.transactionTableModel.setAccount(acct);
	// }
}