package qif.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
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
import qif.data.QDate;
import qif.data.Statement;
import qif.ui.model.AccountTableModel;

/** This panel displays accounts, and drives the content of AccountPanel */
@SuppressWarnings("serial")
public class AccountListPanel extends JScrollPane {

	private AccountTableModel accountTableModel;
	private JTable accountTable;

	private boolean showOpenAccounts = true;

	private List<AccountSelectionListener> acctSelListeners;

	public AccountListPanel(boolean showOpenAccounts) {
		super(new JTable(new AccountTableModel()));

		this.showOpenAccounts = showOpenAccounts;

		acctSelListeners = new ArrayList<AccountSelectionListener>();

		accountTable = (JTable) getViewport().getView();
		accountTable.setFillsViewportHeight(true);

		accountTable.setDefaultRenderer(Object.class, //
				new AccountTableCellRenderer());

		accountTableModel = (AccountTableModel) accountTable.getModel();
		accountTableModel.reload(this.showOpenAccounts);

		TableColumnModel acctColumnModel = accountTable.getColumnModel();

		int awidths[] = { 160, 20, 60 };
		for (int i = 0; i < awidths.length; i++) {
			acctColumnModel.getColumn(i).setPreferredWidth(awidths[i]);
		}

		ListSelectionModel accountSelectionModel = accountTable.getSelectionModel();
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
		// int selrow = this.accountTable.getSelectedRow();
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

	public void showOpenAccounts(boolean yesno) {
		if (this.showOpenAccounts != yesno) {
			this.showOpenAccounts = yesno;

			accountTableModel.reload(this.showOpenAccounts);
		}
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
			acct = accountTableModel.getAccountAt(rows[0]);
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
	private static Color dueColor = Color.BLUE;
	private static Color dueBackground = Color.WHITE;

	private static Font closedFont = new Font("Helvetica", Font.ITALIC, 12);
	private static Color closedColor = Color.GRAY;

	public Component getTableCellRendererComponent(//
			JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
		Component c = super.getTableCellRendererComponent(//
				table, value, isSelected, hasFocus, row, col);

		AccountTableModel model = (AccountTableModel) table.getModel();
		Account acct = model.getAccountAt(row);

		boolean statementDue = acct.isStatementDue();

		Color co = normalColor;
		switch (acct.type) {
		case Asset:
			co = Color.GREEN;
			break;
		case Bank:
		case Cash:
			co = Color.RED;
			break;
		case CCard:
			co = Color.ORANGE;
			break;
		case Invest:
		case InvMutual:
		case InvPort:
			co = Color.BLUE;
			break;
		case Inv401k:
			co = Color.DARK_GRAY;
			break;
		case Liability:
			co = Color.MAGENTA;
			break;
		}

		c.setForeground(co);

		if (!acct.isOpenOn(QDate.today())) {
			c.setFont(closedFont);
			// c.setForeground(closedColor);
		} else if (statementDue) {
			Statement stat = acct.getFirstUnbalancedStatement();

			c.setFont(dueFont);

			if ((stat != null) && (stat.cashBalance.signum() != 0)) {
				// c.setForeground(dueColor);
				c.setBackground(dueBackground);
			} else {
				// c.setForeground(normalColor);
				c.setBackground(normalBackground);
			}
		} else {
			c.setFont(normalFont);
			// c.setForeground(normalColor);
			c.setBackground(normalBackground);
		}

		if (col == 2) {
			setHorizontalAlignment(JLabel.RIGHT);
		}

		if (isSelected) {
			setBackground(UICommon.LIGHT_BLUE);
		} else {
			setBackground(Color.WHITE);
		}

		return c;
	}
}