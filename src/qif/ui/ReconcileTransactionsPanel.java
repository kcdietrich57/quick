package qif.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import qif.data.Account;
import qif.data.GenericTxn;
import qif.data.Statement;
import qif.ui.model.ReconcileTransactionTableModel;

class ReconcileTransactionsPanel //
		extends JPanel //
		implements AccountSelectionListener, StatementSelectionListener {
	private static final long serialVersionUID = 1L;

	ReconcileTransactionTableModel transactionTableModel;
	JTable transactionTable;

	private List<TransactionSelectionListener> txnSelListeners;

	public ReconcileTransactionsPanel() {
		super(new BorderLayout());

		setLayout(new BorderLayout());

		JPanel titlePanel = new JPanel(new GridBagLayout());
		titlePanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.DARK_GRAY));

		JLabel title = new JLabel("Transactions");
		title.setFont(new Font("Helvetica", Font.BOLD, 14));
		title.setForeground(Color.DARK_GRAY);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(3, 3, 3, 3);
		titlePanel.add(title, gbc);

		add(titlePanel, BorderLayout.NORTH);

		this.transactionTableModel = new ReconcileTransactionTableModel();
		this.transactionTable = new JTable(transactionTableModel);
		JScrollPane transactionScrollPane = new JScrollPane(this.transactionTable);

		add(transactionScrollPane, BorderLayout.CENTER);

		transactionTable.setFillsViewportHeight(true);

		transactionTable.setDefaultRenderer(Object.class, //
				new ReconcileTransactionTableCellRenderer());

		TableColumnModel tranColumnModel = transactionTable.getColumnModel();

		int twidths[] = { 60, 50, 100, 80, 80, 90, 90 };

		for (int i = 0; i < twidths.length; i++) {
			switch (i) {
			case 0:
			case 1:
			case 3:
			case 6:
				tranColumnModel.getColumn(i).setMinWidth(twidths[i]);
				tranColumnModel.getColumn(i).setMaxWidth(twidths[i]);
				break;

			case 2:
			case 4:
			case 5:
				tranColumnModel.getColumn(i).setMinWidth(twidths[i]);
				break;

			default:
				tranColumnModel.getColumn(i).setPreferredWidth(twidths[i]);
				break;
			}
		}

		this.txnSelListeners = new ArrayList<TransactionSelectionListener>();

		// Scroll to display the last (most recent) transaction
		transactionTable.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				Rectangle lastRow = transactionTable.getCellRect( //
						transactionTable.getRowCount() - 1, 0, true);
				transactionTable.scrollRectToVisible(lastRow);
			}
		});

		// ListSelectionModel transactionSelectionModel =
		// transactionTable.getSelectionModel();
		// transactionSelectionModel.addListSelectionListener( //
		// new ListSelectionListener() {
		// public void valueChanged(ListSelectionEvent e) {
		// selectTransactionHandler(e);
		// }
		// });

		this.transactionTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				int row = transactionTable.rowAtPoint(evt.getPoint());
				// int col = transactionTable.columnAtPoint(evt.getPoint());

				if (row >= 0) {
					clickTransactionHandler(row);
				}
			}
		});
	}

	public void addTransactionSelectionListener(TransactionSelectionListener listener) {
		this.txnSelListeners.add(listener);
	}

	protected void clickTransactionHandler(int row) {
		GenericTxn txn = transactionTableModel.getTransactionAt(row);

		transactionTableModel.toggleTransactionCleared(txn);
		transactionTableModel.fireTableRowsUpdated( //
				0, transactionTableModel.getRowCount() - 1);

		for (TransactionSelectionListener l : this.txnSelListeners) {
			l.transactionSelected(txn);
		}
	}

	protected void selectTransactionHandler(ListSelectionEvent e) {
		if (e.getValueIsAdjusting()) {
			return;
		}

		int selectedRow = -1;

		try {
			String strSource = e.getSource().toString();
			int start = strSource.indexOf("{") + 1;
			int stop = strSource.length() - 1;

			if (stop > start) {
				selectedRow = Integer.parseInt(strSource.substring(start, stop));

				clickTransactionHandler(selectedRow);
			}
		} catch (Exception e2) {
			e2.printStackTrace();
		}
	}

	/** Respond to account selection in accountlist */
	public void accountSelected(Account account) {
		// TODO this.transactionTableModel.setAccount(account);
	}

	public void statementSelected(Statement statement) {
		this.transactionTableModel.setStatement(statement);
	}
}

class ReconcileTransactionTableCellRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = 1L;

	private static Font boldFont = new Font("Helvetica", Font.BOLD, 12);
	private static Font regularFont = new Font("Helvetica", Font.PLAIN, 12);

	private static Color unclearedColor = Color.BLACK;
	private static Color clearedColor = Color.BLUE;

	private static Color unclearedBackground = Color.WHITE;
	private static Color clearedBackground = new Color(240, 240, 240);

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int col) {
		Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

		ReconcileTransactionTableModel model = (ReconcileTransactionTableModel) table.getModel();

		GenericTxn tx = model.getTransactionAt(row);

		boolean cleared = model.clearedTransactions.contains(tx);

		if (cleared) {
			c.setFont(boldFont);
			c.setForeground(clearedColor);
			c.setBackground(clearedBackground);
		} else {
			c.setFont(regularFont);
			c.setForeground(unclearedColor);
			c.setBackground(unclearedBackground);
		}

		if (col == 3 || col == 6) {
			setHorizontalAlignment(JLabel.RIGHT);
		} else {
			setHorizontalAlignment(JLabel.LEFT);
		}
		return c;
	}
}