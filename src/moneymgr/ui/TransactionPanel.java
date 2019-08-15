package moneymgr.ui;

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
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;

import moneymgr.model.Account;
import moneymgr.model.GenericTxn;
import moneymgr.model.Statement;
import moneymgr.ui.model.TransactionTableModel;

/**
 * This panel displays transactions in an account or statement<br>
 * Title<br>
 * Transactions<br>
 * TransactionDetails
 */
@SuppressWarnings("serial")
public class TransactionPanel //
		extends JPanel //
		implements AccountSelectionListener, StatementSelectionListener {

	private TransactionDetailsPanel transactionDetailsPanel;
	private JTable transactionTable;
	private TransactionTableModel transactionTableModel;

	private List<TransactionSelectionListener> txnSelListeners;

	public TransactionPanel(boolean highlighting) {
		setLayout(new BorderLayout());

		JPanel transactionTitlePanel = createTitlePanel();
		JComponent transactionsTable = createTransactionsTable(highlighting);
		JComponent transactionDetails = createTransactionDetails();

		add(transactionTitlePanel, BorderLayout.NORTH);
		add(transactionsTable, BorderLayout.CENTER);
		add(transactionDetails, BorderLayout.SOUTH);

		this.txnSelListeners = new ArrayList<>();

		this.transactionTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				int row = TransactionPanel.this.transactionTable.getSelectedRow();

				if (row >= 0) {
					transactionSelected(row);
				}
			}
		});

		// Scroll to display the last (most recent) transaction
		this.transactionTable.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				super.componentResized(e);

				Rectangle lastRow = TransactionPanel.this.transactionTable.getCellRect( //
						TransactionPanel.this.transactionTable.getRowCount() - 1, 0, true);
				TransactionPanel.this.transactionTable.scrollRectToVisible(lastRow);
			}

			// TODO componentShown() does not seem to fire
			public void componentShown(ComponentEvent e) {
				super.componentShown(e);

				TransactionPanel.this.transactionTableModel
						.setColumnWidths(TransactionPanel.this.transactionTable.getColumnModel());
			}
		});
	}

	private JPanel createTitlePanel() {
		JPanel transactionTitlePanel = new JPanel(new GridBagLayout());
		transactionTitlePanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.DARK_GRAY));

		JLabel title = new JLabel("Transactions");
		title.setFont(new Font("Helvetica", Font.BOLD, 14));
		title.setForeground(Color.DARK_GRAY);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(3, 3, 3, 3);
		transactionTitlePanel.add(title, gbc);

		return transactionTitlePanel;
	}

	private JComponent createTransactionsTable(boolean highlighting) {
		this.transactionTableModel = new TransactionTableModel();
		this.transactionTable = new JTable(this.transactionTableModel);

		this.transactionTable.setFillsViewportHeight(true);

		this.transactionTable.setDefaultRenderer(Object.class, //
				new TransactionTableCellRenderer(highlighting));

		this.transactionTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		this.transactionTableModel.setColumnWidths(this.transactionTable.getColumnModel());
		this.transactionTableModel.addColumnWidthListeners(this.transactionTable);

		this.transactionTable.setRowSelectionAllowed(true);

		return new JScrollPane(this.transactionTable);
	}

	private JComponent createTransactionDetails() {
		this.transactionDetailsPanel = new TransactionDetailsPanel(10, 90);

		return new JScrollPane(this.transactionDetailsPanel);
	}

	private void transactionSelected(int row) {
		GenericTxn txn = this.transactionTableModel.getTransactionAt(row);

		// TODO implement transactions property pane
		this.transactionDetailsPanel.setText(txn.formatValue());

		for (TransactionSelectionListener l : this.txnSelListeners) {
			l.transactionSelected(txn);
		}
	}

	public void addTransactionSelectionListener(TransactionSelectionListener listener) {
		this.txnSelListeners.add(listener);
	}

	public void accountSelected(Account account, boolean update) {
		this.transactionTableModel.accountSelected(account, update);
	}

	public void statementSelected(Statement statement) {
		this.transactionTableModel.statementSelected(statement);
	}

	public void updateQifProperties() {
		this.transactionTableModel.updateQifColumnProperties();
	}
}

class TransactionTableCellRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = 1L;

	private static Font boldFont = new Font("Helvetica", Font.BOLD, 12);
	private static Font regularFont = new Font("Helvetica", Font.PLAIN, 12);
	private static Font italicFont = new Font("Helvetica", Font.ITALIC, 12);

	private static Color defaultColor = Color.BLACK;
	private static Color presentColor = Color.BLACK;
	private static Color futureColor = Color.GRAY;

	private static Color defaultBackground = Color.WHITE;
	private static Color presentBackground = UIConstants.LIGHT_GRAY;
	private static Color futureBackground = Color.WHITE;

	private boolean highlighting;

	public TransactionTableCellRenderer(boolean highlighting) {
		this.highlighting = true; // highlighting;
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int col) {
		Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

		TransactionTableModel model = (TransactionTableModel) table.getModel();
		GenericTxn tx = model.getTransactionAt(row);

		boolean cleared = ((tx != null) && tx.isCleared());
		boolean future = ((tx != null) //
				&& (tx.getDate().compareTo(MainWindow.instance.currentDate()) > 0));

		if (!this.highlighting) {
			c.setFont(regularFont);
			c.setForeground(defaultColor);
			c.setBackground(defaultBackground);
		} else if (future) {
			c.setFont(italicFont);
			c.setForeground(futureColor);
			c.setBackground(futureBackground);
		} else if (cleared) {
			c.setFont(boldFont);
			c.setForeground(presentColor);
			c.setBackground(presentBackground);
		} else {
			c.setFont(regularFont);
			c.setForeground(presentColor);
			c.setBackground(presentBackground);
		}

		// TODO link formatting to column type, not position. they can be moved.
		if ((col == 3) || (col == 6) || (col == 7)) {
			setHorizontalAlignment(SwingConstants.RIGHT);
		} else {
			setHorizontalAlignment(SwingConstants.LEFT);
		}
		return c;
	}
}