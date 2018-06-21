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

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import qif.data.Account;
import qif.data.GenericTxn;
import qif.data.QDate;
import qif.data.Statement;
import qif.ui.model.TransactionTableModel;

/** This panel displays transactions in an account or statement */
@SuppressWarnings("serial")
public class TransactionPanel //
		extends JPanel //
		implements AccountSelectionListener, StatementSelectionListener {

	// TODO make this private
	TransactionTableModel transactionTableModel;
	private JTable transactionTable;

	public TransactionPanel(boolean highlighting) {
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

		this.transactionTableModel = new TransactionTableModel();
		this.transactionTable = new JTable(transactionTableModel);
		JScrollPane scrollPane = new JScrollPane(this.transactionTable);

		add(scrollPane, BorderLayout.CENTER);

		transactionTable.setFillsViewportHeight(true);

		transactionTable.setDefaultRenderer(Object.class, //
				new TransactionTableCellRenderer(highlighting));

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

		// Scroll to display the last (most recent) transaction
		transactionTable.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				Rectangle lastRow = transactionTable.getCellRect( //
						transactionTable.getRowCount() - 1, 0, true);
				transactionTable.scrollRectToVisible(lastRow);
			}
		});
	}

	public void accountSelected(Account account, boolean update) {
		this.transactionTableModel.accountSelected(account, update);
	}

	public void statementSelected(Statement statement) {
		this.transactionTableModel.statementSelected(statement);
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
	private static Color presentBackground = new Color(240, 240, 240);
	private static Color futureBackground = Color.WHITE;

	private boolean highlighting;

	public TransactionTableCellRenderer(boolean highlighting) {
		this.highlighting = highlighting;
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int col) {
		Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

		TransactionTableModel model = (TransactionTableModel) table.getModel();
		GenericTxn tx = model.getTransactionAt(row);

		boolean cleared = (tx != null && tx.isCleared());
		boolean future = (tx != null && tx.getDate().compareTo(QDate.today()) > 0);

		if (!this.highlighting) {
			c.setFont(regularFont);
			c.setForeground(defaultColor);
			c.setBackground(defaultBackground);
		} else if (cleared) {
			c.setFont(boldFont);
			c.setForeground(presentColor);
			c.setBackground(presentBackground);
		} else if (future) {
			c.setFont(italicFont);
			c.setForeground(futureColor);
			c.setBackground(futureBackground);
		} else {
			c.setFont(regularFont);
			c.setForeground(presentColor);
			c.setBackground(presentBackground);
		}

		if (col == 3 || col == 6) {
			setHorizontalAlignment(JLabel.RIGHT);
		} else {
			setHorizontalAlignment(JLabel.LEFT);
		}
		return c;
	}
}