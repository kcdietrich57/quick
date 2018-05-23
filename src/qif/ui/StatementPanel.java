package qif.ui;

import java.awt.BorderLayout;
import java.awt.Color;
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
import java.util.Date;

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
import javax.swing.table.TableColumnModel;

import qif.data.Account;
import qif.data.Statement;
import qif.ui.model.StatementTableModel;

public class StatementPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	public StatementTableModel statementTableModel;
	private JTable statementTable;
	private JScrollPane scroller;

	public StatementDetailsPanel statementDetails;

	public StatementPanel() {
		setLayout(new BorderLayout());

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

		add(titlePanel, BorderLayout.NORTH);

		statementTableModel = new StatementTableModel();
		statementTable = new JTable(statementTableModel);
		this.scroller = new JScrollPane(this.statementTable);
		statementTable.setFillsViewportHeight(true);

		add(this.scroller, BorderLayout.CENTER);

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

				// int firstvrow = getFirstVisibleStatementRow();
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
		JViewport viewport = this.scroller.getViewport();
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

		Account a = statementTableModel.getAccount();
		Statement s = (a != null) ? a.getStatement(d) : null;

		statementDetails.setStatement(s);
	}
}