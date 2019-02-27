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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableCellRenderer;

import qif.data.Account;
import qif.data.Common;
import qif.data.GenericTxn;
import qif.data.InvestmentTxn;
import qif.data.Lot;
import qif.data.NonInvestmentTxn;
import qif.data.QDate;
import qif.data.Security;
import qif.data.SimpleTxn;
import qif.data.Statement;
import qif.data.TxAction;
import qif.ui.model.TransactionTableModel;

/** This panel displays transactions in an account or statement */
@SuppressWarnings("serial")
public class TransactionPanel //
		extends JPanel //
		implements AccountSelectionListener, StatementSelectionListener {

	public JTextArea textArea;
	private TransactionTableModel transactionTableModel;
	private JTable transactionTable;
	private List<TransactionSelectionListener> txnSelListeners;

	public TransactionPanel(boolean highlighting) {
		setLayout(new BorderLayout());

		this.txnSelListeners = new ArrayList<TransactionSelectionListener>();

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

		this.textArea = new JTextArea(10, 90);
		this.textArea.setText("Transaction info goes here");
		add(this.textArea, BorderLayout.SOUTH);

		this.transactionTableModel = new TransactionTableModel();
		this.transactionTable = new JTable(transactionTableModel);
		JScrollPane scrollPane = new JScrollPane(this.transactionTable);

		add(scrollPane, BorderLayout.CENTER);

		this.transactionTable.setFillsViewportHeight(true);

		this.transactionTable.setDefaultRenderer(Object.class, //
				new TransactionTableCellRenderer(highlighting));

		this.transactionTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		transactionTableModel.setColumnWidths(this.transactionTable.getColumnModel());
		transactionTableModel.addColumnWidthListeners(this.transactionTable);

		this.transactionTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				int row = transactionTable.rowAtPoint(evt.getPoint());

				if (row >= 0) {
					clickTransactionHandler(row);
				}
			}
		});

		// Scroll to display the last (most recent) transaction
		transactionTable.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				super.componentResized(e);

				Rectangle lastRow = transactionTable.getCellRect( //
						transactionTable.getRowCount() - 1, 0, true);
				transactionTable.scrollRectToVisible(lastRow);
			}

			// TODO componentShown() does not seem to fire
			public void componentShown(ComponentEvent e) {
				super.componentShown(e);

				transactionTableModel.setColumnWidths(transactionTable.getColumnModel());
			}
		});
	}

	private void clickTransactionHandler(int row) {
		GenericTxn txn = this.transactionTableModel.getTransactionAt(row);

		QDate date = txn.getDate();
		String payee = txn.getPayee();
		boolean clr = txn.isCleared();
		QDate stmtdate = txn.stmtdate;

		TxAction action = txn.getAction();
		BigDecimal amt = txn.getAmount();
		BigDecimal cashamt = txn.getCashAmount();
		String cat = txn.getCategory();
		String memo = txn.getMemo();

		int xacctid = txn.getXferAcctid();
		BigDecimal xamt = txn.getXferAmount();
		SimpleTxn xtxn = txn.getXtxn();

		List<SimpleTxn> splits = txn.getSplits(); // NOSPLITS

		this.textArea.setText(txn.formatValue());

		if (true) {
			return;
		}
		
		if (txn instanceof NonInvestmentTxn) {
			NonInvestmentTxn nit = (NonInvestmentTxn) txn;

			String cknumStr = nit.chkNumber;
			// int cknum = nit.getCheckNumber();
			// List<String> addr = nit.address;

			try {
				// System.out.println("Non-Investment Transaction:");
				if (!amt.equals(cashamt)) {
					System.out.println("Warning: amt/cash not equal");
				}

				System.out.println(String.format("%-10s |%c| %-6s | %-30s | %12s", //
						date.longString, //
						((clr) ? 'C' : ' '), //
						((cknumStr != null) //
								? cknumStr //
								: action.toString()), //
						payee, //
						Common.formatAmount(amt)));

				System.out.println(String.format("  %-20s | %s", //
						cat, //
						memo));

				if (nit.hasSplits()) {
					int ii = 1;
					for (SimpleTxn split : splits) {
						BigDecimal samt = split.getAmount();
						if (Common.isEffectivelyZero(samt)) {
							continue;
						}

						System.out.println(String.format("    %2d: %15s  %20s  %20s", //
								ii++, //
								Common.formatAmount(samt), //
								split.getCategory(), //
								split.getMemo()));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (txn instanceof InvestmentTxn) {
			InvestmentTxn it = (InvestmentTxn) txn;

			try {
				String xacctName = it.accountForTransfer;
				BigDecimal xferAmt = it.amountTransferred;
				List<InvestmentTxn> xtxns = it.xferTxns;
				BigDecimal commission = it.commission;
				List<Lot> srclots = it.srcLots;
				List<Lot> dstlots = it.dstLots;
				Security sec = it.security;
				BigDecimal price = it.price;
				BigDecimal shares = it.getShares();

				System.out.println();
				System.out.println("-----------------------");
				System.out.println(String.format("%-10s |%c| %-6s | %-30s | %12s", //
						date.longString, //
						((clr) ? 'C' : ' '), //
						action.toString(), //
						payee, //
						Common.formatAmount(amt)));

				System.out.println(String.format("  %-20s | %s", //
						cat, //
						memo));

				if (it.hasSplits()) {
					int ii = 1;
					for (SimpleTxn split : it.getSplits()) {
						BigDecimal samt = split.getAmount();
						if (Common.isEffectivelyZero(samt)) {
							continue;
						}

						System.out.println(String.format("    %2d: %15s  %20s  %20s", //
								ii++, //
								Common.formatAmount(samt), //
								split.getCategory(), //
								split.getMemo()));
					}
				}

				System.out.println();
				if (sec != null) {
					System.out.println("  Sec/shr/price: " + sec.getName() //
							+ " | " + Common.formatAmount3(shares) //
							+ " | " + Common.formatAmount3(price));
				}
				System.out.println("  Commission: " + commission //
						+ "  Xamt: " + Common.formatAmount(xferAmt) //
						+ "  Xacct: " + xacctName);
				if (!xtxns.isEmpty()) {
					System.out.println("  Xtxns: ");

					for (InvestmentTxn ixtxn : xtxns) {
						System.out.println("    " + ixtxn);
					}
				}

				if ((srclots != null) && !srclots.isEmpty()) {
					System.out.println("  Src lots: ");

					for (Lot lot : srclots) {
						System.out.println("    " + lot);
					}
				}

				if ((dstlots != null) && !dstlots.isEmpty()) {
					System.out.println("  Dst lots: ");

					for (Lot lot : dstlots) {
						System.out.println("    " + lot);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

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
	private static Color presentBackground = UICommon.LIGHT_GRAY;
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

		boolean cleared = (tx != null && tx.isCleared());
		boolean future = ((tx != null) //
				&& (tx.getDate().compareTo(MainWindow.instance.asOfDate) > 0));

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

		if (col == 3 || col == 6) {
			setHorizontalAlignment(JLabel.RIGHT);
		} else {
			setHorizontalAlignment(JLabel.LEFT);
		}
		return c;
	}
}