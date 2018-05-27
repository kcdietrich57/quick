package qif.ui;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import qif.data.Account;
import qif.ui.model.AccountTableModel;

public class AccountListPanel extends JScrollPane {
	private static final long serialVersionUID = 1L;

	private AccountTableModel accountTableModel;
	private JTable accountTable;

	private boolean showOpenAccounts = true;

	private List<AccountSelectionListener> acctSelListeners;

	public AccountListPanel(boolean showOpenAccounts) {
		super(new JTable(new AccountTableModel()));

		acctSelListeners = new ArrayList<AccountSelectionListener>();

		this.showOpenAccounts = showOpenAccounts;

		accountTable = (JTable) getViewport().getView();
		accountTable.setFillsViewportHeight(true);
		accountTableModel = (AccountTableModel) accountTable.getModel();
		accountTableModel.load(this.showOpenAccounts);

		TableColumnModel acctColumnModel = accountTable.getColumnModel();

		int awidths[] = { 160, 20, 60 };
		for (int i = 0; i < awidths.length; i++) {
			acctColumnModel.getColumn(i).setPreferredWidth(awidths[i]);
		}

		acctColumnModel.getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 1L;

			{
				setHorizontalAlignment(JLabel.RIGHT);
			}

			public Component getTableCellRendererComponent(//
					JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
				Component c = super.getTableCellRendererComponent(//
						table, value, isSelected, hasFocus, row, col);
				return c;
			}
		});

		ListSelectionModel accountSelectionModel = accountTable.getSelectionModel();
		accountSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		accountSelectionModel.addListSelectionListener( //
				new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent e) {
						selectAccountHandler(e);
					}
				});

		createContextMenu();
	}

	public void addAccountSelectionListener(AccountSelectionListener listener) {
		this.acctSelListeners.add(listener);
	}

	public void showOpenAccounts(boolean yesno) {
		if (this.showOpenAccounts != yesno) {
			this.showOpenAccounts = yesno;

			accountTableModel.load(this.showOpenAccounts);
		}
	}

	protected void selectAccountHandler(ListSelectionEvent e) {
		if (e.getValueIsAdjusting()) {
			return;
		}

		int selectedRow = -1;

		try {
			String strSource = e.getSource().toString();
			int start = strSource.indexOf("{") + 1;
			int stop = strSource.length() - 1;

			selectedRow = Integer.parseInt(strSource.substring(start, stop));
		} catch (Exception e2) {

		}

		Account acct = accountTableModel.getAccountAt(selectedRow);

		for (AccountSelectionListener l : this.acctSelListeners) {
			l.accountSelected(acct);
		}
	}

	private void createContextMenu() {
		final JPopupMenu acctPopupMenu = new JPopupMenu();
		JMenuItem chooseAcctItem = new JMenuItem("Choose Account");

		chooseAcctItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int idx = accountTable.getSelectionModel().getMinSelectionIndex();
				Account a = accountTableModel.getAccountAt(idx);

				String aname = (a != null) ? a.getName() : null;

				// JOptionPane.showMessageDialog(MainFrame.frame, //
				// "DELETE for account " + a.toString());
				// JDialog dlg = new JDialog(MainFrame.frame, "Account Properties", false);
				// dlg.setVisible(true);

				ChooseAccountDialog dialog = new ChooseAccountDialog(new JFrame(), //
						"Account Information", //
						"This is about account " + aname);

				dialog.setSize(300, 150);
				dialog.setVisible(true);
			}
		});

		acctPopupMenu.add(chooseAcctItem);
		accountTable.setComponentPopupMenu(acctPopupMenu);

		acctPopupMenu.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						int rowAtPoint = accountTable.rowAtPoint( //
								SwingUtilities.convertPoint(acctPopupMenu, new Point(0, 0), accountTable));

						if (rowAtPoint > -1) {
							accountTable.setRowSelectionInterval(rowAtPoint, rowAtPoint);
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
	}
}
