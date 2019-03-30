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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;

import moneymgr.model.GenericTxn;
import moneymgr.model.SecurityPortfolio;
import moneymgr.model.Statement;
import moneymgr.ui.model.ReconcileTransactionTableModel;

/**
 * This panel shows transactions for a statement being reconciled and lets the
 * user include/exclude the transactions<br>
 * Title<br>
 * Transactions
 */
@SuppressWarnings("serial")
public class AccountInfoReconcileTransactionsPanel extends JPanel //
		implements StatementSelectionListener {

	private JPanel titlePanel;
	private JLabel title;
	private JTable reconcileTransactionTable;
	private ReconcileTransactionTableModel reconcileTransactionTableModel;

	private List<TransactionSelectionListener> txnSelListeners;

	public AccountInfoReconcileTransactionsPanel() {
		super(new BorderLayout());

		setLayout(new BorderLayout());

		this.titlePanel = new JPanel(new GridBagLayout());
		this.titlePanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.DARK_GRAY));

		this.title = new JLabel("Transactions");
		this.title.setFont(new Font("Helvetica", Font.BOLD, 14));
		this.title.setForeground(Color.DARK_GRAY);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(3, 3, 3, 3);
		this.titlePanel.add(this.title, gbc);

		add(this.titlePanel, BorderLayout.NORTH);

		this.reconcileTransactionTableModel = new ReconcileTransactionTableModel();
		this.reconcileTransactionTable = new JTable(this.reconcileTransactionTableModel);
		JScrollPane transactionScrollPane = new JScrollPane(this.reconcileTransactionTable);

		add(transactionScrollPane, BorderLayout.CENTER);

		this.reconcileTransactionTable.setFillsViewportHeight(true);

		this.reconcileTransactionTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		this.reconcileTransactionTableModel.setColumnWidths(this.reconcileTransactionTable.getColumnModel());
		this.reconcileTransactionTableModel.addColumnWidthListeners(this.reconcileTransactionTable);

		this.reconcileTransactionTable.setDefaultRenderer(Object.class, //
				new ReconcileTransactionTableCellRenderer());

		this.txnSelListeners = new ArrayList<>();

		// Scroll to display the last (most recent) transaction
		this.reconcileTransactionTable.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				Rectangle lastRow = AccountInfoReconcileTransactionsPanel.this.reconcileTransactionTable.getCellRect( //
						AccountInfoReconcileTransactionsPanel.this.reconcileTransactionTable.getRowCount() - 1, 0,
						true);
				AccountInfoReconcileTransactionsPanel.this.reconcileTransactionTable.scrollRectToVisible(lastRow);
			}
		});

		this.reconcileTransactionTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				int row = AccountInfoReconcileTransactionsPanel.this.reconcileTransactionTable
						.rowAtPoint(evt.getPoint());

				if (row >= 0) {
					clickTransactionHandler(row);
				}
			}
		});
	}

	public void unclearAll() {
		this.reconcileTransactionTableModel.unclearAll();
	}

	public void clearAll() {
		this.reconcileTransactionTableModel.clearAll();
	}

	public void finishStatement() {
		this.reconcileTransactionTableModel.finishStatement();
	}

	public Statement createNextStatementToReconcile() {
		return this.reconcileTransactionTableModel.createNextStatementToReconcile();
	}

	public BigDecimal getDebits() {
		return this.reconcileTransactionTableModel.getDebits();
	}

	public BigDecimal getClearedCashBalance() {
		return this.reconcileTransactionTableModel.getClearedCashBalance();
	}

	public SecurityPortfolio getPortfolioDelta() {
		return this.reconcileTransactionTableModel.getPortfolioDelta();
	}

	public BigDecimal getCredits() {
		return this.reconcileTransactionTableModel.getCredits();
	}

	public void addTransactionSelectionListener(TransactionSelectionListener listener) {
		this.txnSelListeners.add(listener);
	}

	protected void clickTransactionHandler(int row) {
		GenericTxn txn = this.reconcileTransactionTableModel.getTransactionAt(row);

		this.reconcileTransactionTableModel.toggleTransactionCleared(txn);
		this.reconcileTransactionTableModel.fireTableRowsUpdated( //
				0, this.reconcileTransactionTableModel.getRowCount() - 1);

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

	public void statementSelected(Statement statement) {
		this.reconcileTransactionTableModel.statementSelected(statement);
	}

	public void updateQifProperties() {
		this.reconcileTransactionTableModel.updateQifColumnProperties();
	}
}

class ReconcileTransactionTableCellRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = 1L;

	private static Font boldFont = new Font("Helvetica", Font.BOLD, 12);
	private static Font regularFont = new Font("Helvetica", Font.PLAIN, 12);

	private static Color unclearedColor = Color.BLACK;
	private static Color clearedColor = Color.BLUE;

	private static Color unclearedBackground = Color.WHITE;
	private static Color clearedBackground = UIConstants.LIGHT_BLUE;

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int col) {
		Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

		ReconcileTransactionTableModel model = (ReconcileTransactionTableModel) table.getModel();

		GenericTxn tx = model.getTransactionAt(row);

		boolean cleared = model.isCleared(tx);

		if (cleared) {
			c.setFont(boldFont);
			c.setForeground(clearedColor);
			c.setBackground(clearedBackground);
		} else {
			c.setFont(regularFont);
			c.setForeground(unclearedColor);
			c.setBackground(unclearedBackground);
		}

		if ((col == 3) || (col == 6)) {
			setHorizontalAlignment(SwingConstants.RIGHT);
		} else {
			setHorizontalAlignment(SwingConstants.LEFT);
		}
		return c;
	}
}