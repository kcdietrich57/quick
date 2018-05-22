package qif.ui;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.TableColumnModel;

import qif.data.Account;
import qif.data.QifDom;
import qif.ui.model.AccountTableModel;
import qif.ui.model.StatementTableModel;
import qif.ui.model.TransactionTableModel;

public class AccountPanel extends JScrollPane {
	private static final long serialVersionUID = 1L;

	private AccountTableModel accountTableModel;
	private JTable accountTable;

	public TransactionTableModel transactionTableModel;
	public StatementTableModel statementTableModel;

	private boolean showOpenAccounts = true;

	public AccountPanel(boolean showOpenAccounts) {
		super(new JTable(new AccountTableModel()));

		this.showOpenAccounts = showOpenAccounts;

		accountTable = (JTable) getViewport().getView();
		accountTable.setFillsViewportHeight(true);
		accountTableModel = (AccountTableModel) accountTable.getModel();
		accountTableModel.load(this.showOpenAccounts);

		TableColumnModel acctColumnModel = accountTable.getColumnModel();

		int awidths[] = { 170, 40, 70 };
		for (int i = 0; i < awidths.length; i++) {
			acctColumnModel.getColumn(i).setPreferredWidth(awidths[i]);
		}

		ListSelectionModel accountSelectionModel = accountTable.getSelectionModel();
		accountSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		accountSelectionModel.addListSelectionListener( //
				new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent e) {
						selectAccountHandler(e);
					}
				});

		setActions();
	}

	public void showOpenAccounts() {
		if (!this.showOpenAccounts) {
			this.showOpenAccounts = true;

			accountTableModel.load(showOpenAccounts);
		}
	}

	public void showClosedAccounts() {
		if (this.showOpenAccounts) {
			this.showOpenAccounts = false;

			accountTableModel.load(showOpenAccounts);
		}
	}

	public String getSelectedAcccountName() {
		int idx = accountTable.getSelectionModel().getMinSelectionIndex();
		Object a = accountTableModel.getValueAt(idx, 0);

		return a.toString();
	}

	private void setActions() {
		final JPopupMenu acctPopupMenu = new JPopupMenu();
		JMenuItem chooseAcctItem = new JMenuItem("Choose Account");

		chooseAcctItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String aname = getSelectedAcccountName();
				// int idx = accountTable.getSelectionModel().getMinSelectionIndex();
				// Object a = accountTableModel.accountValues.get(idx)[0];

				// JOptionPane.showMessageDialog(MainFrame.frame, //
				// "DELETE for account " + a.toString());

				// JDialog dlg = new JDialog(MainFrame.frame, "Account Properties", false);
				// dlg.setVisible(true);

				ChooseAccountDialog dialog = new ChooseAccountDialog(new JFrame(), "Account Information", //
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

	protected void selectAccountHandler(ListSelectionEvent e) {
		if (e.getValueIsAdjusting()) {
			return;
		}

		String strSource = e.getSource().toString();
		int start = strSource.indexOf("{") + 1;
		int stop = strSource.length() - 1;
		int iSelectedIndex = -1;

		try {
			iSelectedIndex = Integer.parseInt(strSource.substring(start, stop));
		} catch (Exception e2) {

		}

		String acctname = (String) accountTableModel.getValueAt(iSelectedIndex, 0);
		Account acct = (acctname != null) ? QifDom.getDomById(1).findAccount(acctname) : null;

		setSelectedAccount(acct);
	}

	public void setSelectedAccount(Account acct) {
		this.statementTableModel.setAccount(acct);
		this.transactionTableModel.setAccount(acct);
	}
}

class ChooseAccountDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	public ChooseAccountDialog(JFrame parent, String title, String message) {
		super(parent, title);

		Point p = new Point(400, 400);
		setLocation(p.x, p.y);

		JPanel messagePane = new JPanel();
		messagePane.add(new JLabel(message));
		getContentPane().add(messagePane);

		JPanel buttonPane = new JPanel();
		JButton button = new JButton("Close me");
		buttonPane.add(button);

		button.addActionListener(new MyActionListener());
		getContentPane().add(buttonPane, BorderLayout.PAGE_END);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		pack();
		setVisible(true);
	}

	public JRootPane createRootPane() {
		JRootPane rootPane = new JRootPane();
		KeyStroke stroke = KeyStroke.getKeyStroke("ESCAPE");

		Action action = new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				System.out.println("escaping..");
				setVisible(false);
				dispose();
			}
		};

		InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		inputMap.put(stroke, "ESCAPE");
		rootPane.getActionMap().put("ESCAPE", action);

		return rootPane;
	}

	class MyActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			System.out.println("disposing the window..");
			setVisible(false);
			dispose();
		}
	}
}
