package moneymgr.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import moneymgr.model.Account;
import moneymgr.ui.model.AccountTableModel;
import moneymgr.util.QDate;

/**
 * This panel displays accounts, and drives the content of AccountPanel<br>
 * AccountTable
 */
@SuppressWarnings("serial")
public class AccountNavigationListPanel extends JScrollPane {

	private boolean includeClosedAccounts;
	private boolean includeZeroBalanceAccounts;

	private JTable accountTable;
	private AccountTableModel accountTableModel;

	private List<AccountSelectionListener> acctSelListeners;

	public AccountNavigationListPanel( //
			boolean showOpenAccounts, //
			boolean showZeroBalanceAccounts) {
		super(new JTable(new AccountTableModel()));

		this.includeClosedAccounts = showOpenAccounts;
		this.includeZeroBalanceAccounts = showZeroBalanceAccounts;

		this.acctSelListeners = new ArrayList<>();

		this.accountTable = (JTable) getViewport().getView();
		this.accountTable.setFillsViewportHeight(true);

		this.accountTable.setDefaultRenderer(Object.class, //
				new AccountTableCellRenderer());

		this.accountTableModel = (AccountTableModel) this.accountTable.getModel();
		this.accountTableModel.reload(this.includeClosedAccounts, this.includeZeroBalanceAccounts);

		TableColumnModel acctColumnModel = this.accountTable.getColumnModel();

		int awidths[] = { 160, 20, 60 };
		for (int i = 0; i < awidths.length; i++) {
			acctColumnModel.getColumn(i).setPreferredWidth(awidths[i]);
		}

		ListSelectionModel accountSelectionModel = this.accountTable.getSelectionModel();
		accountSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		accountSelectionModel.addListSelectionListener( //
				new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent e) {
						if (e.getValueIsAdjusting()) {
							return;
						}

						accountSelected();
					}
				});

		createContextMenu();
	}

	public void refreshAccountList() {
		Account acct = this.getSelectedAccount();
		this.accountTableModel.reload();

		if (acct != null) {
			int idx = this.accountTableModel.getAccountIndex(acct);
			if (idx >= 0) {
				this.accountTable.addRowSelectionInterval(idx, idx);
			}
		}
	}

	public void addAccountSelectionListener(AccountSelectionListener listener) {
		this.acctSelListeners.add(listener);
	}

	public void setIncludeClosedAccounts(boolean yesno) {
		if (this.includeClosedAccounts == yesno) {
			return;
		}

		this.includeClosedAccounts = yesno;

		this.accountTableModel.reload( //
				this.includeClosedAccounts, this.includeZeroBalanceAccounts);
	}

	public void setIncludeZeroBalanceAccounts(boolean yesno) {
		if (this.includeZeroBalanceAccounts == yesno) {
			return;
		}

		this.includeZeroBalanceAccounts = yesno;

		this.accountTableModel.reload( //
				this.includeClosedAccounts, this.includeZeroBalanceAccounts);
	}

	protected void accountSelected() {
		Account acct = getSelectedAccount();

		for (AccountSelectionListener l : this.acctSelListeners) {
			l.accountSelected(acct, false);
		}
	}

	private Account getSelectedAccount() {
		Account acct = null;

		if (this.accountTable.getSelectedRowCount() > 0) {
			int[] rows = this.accountTable.getSelectedRows();
			acct = this.accountTableModel.getAccountAt(rows[0]);
		}

		return acct;
	}

	private void createContextMenu() {
		JPopupMenu acctPopupMenu = new JPopupMenu();
		JMenuItem chooseAcctItem = new JMenuItem("Choose Account");

		chooseAcctItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Account a = getSelectedAccount();

				ChooseAccountDialog dialog = new ChooseAccountDialog(new JFrame(), //
						"Account Information", //
						"This is about account " + a.name);

				dialog.setSize(300, 150);
				dialog.setVisible(true);
			}
		});

		acctPopupMenu.add(chooseAcctItem);
		this.accountTable.setComponentPopupMenu(acctPopupMenu);

		acctPopupMenu.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						int rowAtPoint = AccountNavigationListPanel.this.accountTable.rowAtPoint( //
								SwingUtilities.convertPoint(acctPopupMenu, new Point(0, 0),
										AccountNavigationListPanel.this.accountTable));

						if (rowAtPoint > -1) {
							AccountNavigationListPanel.this.accountTable.setRowSelectionInterval(rowAtPoint,
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

class AccountTableCellRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = 1L;

	private static Font normalFont = new Font("Helvetica", Font.PLAIN, 12);
	private static Color normalColor = Color.BLACK;
	private static Color normalBackground = Color.WHITE;

	private static Font dueFont = new Font("Helvetica", Font.BOLD, 12);
	private static Color dueColor = Color.BLACK;

	private static Font overdueFont = new Font("Helvetica", Font.BOLD, 12);
	private static Color overdueColor = Color.BLUE;

	private static Font closedFont = new Font("Helvetica", Font.ITALIC, 12);
	private static Color closedColor = Color.GRAY;

	private static Color selectedBackground = UIConstants.LIGHT_BLUE;

	public Component getTableCellRendererComponent(//
			JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
		Component c = super.getTableCellRendererComponent(//
				table, value, isSelected, hasFocus, row, col);

		AccountTableModel model = (AccountTableModel) table.getModel();
		Account acct = model.getAccountAt(row);

		Color fgColor = normalColor;
		Color bgColor = normalBackground;

		switch (acct.type) {
		case Asset:
			bgColor = new Color(255, 255, 192);
			break;
		case Bank:
		case Cash:
			bgColor = normalBackground;
			break;
		case CCard:
			bgColor = new Color(255, 192, 192);
			break;
		case Invest:
		case InvMutual:
		case InvPort:
			bgColor = new Color(192, 192, 255);
			break;
		case Inv401k:
			bgColor = new Color(128, 255, 128);
			break;
		case Liability:
			bgColor = new Color(255, 192, 255);
			break;
		}

		if (!acct.isOpenOn(QDate.today())) {
			c.setFont(closedFont);
			fgColor = closedColor;
		} else if (acct.isStatementOverdue(5)) {
			c.setFont(overdueFont);
			fgColor = overdueColor;
		} else if (acct.isStatementDue()) {
			c.setFont(dueFont);
			fgColor = dueColor;
		} else {
			c.setFont(normalFont);
		}

		if (col == 2) {
			setHorizontalAlignment(SwingConstants.RIGHT);
		}

		c.setForeground(fgColor);
		c.setBackground((isSelected) ? selectedBackground : bgColor);

		return c;
	}
}