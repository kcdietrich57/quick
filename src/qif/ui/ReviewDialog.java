package qif.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;

import qif.data.Account;
import qif.data.Common;
import qif.data.GenericTxn;
import qif.data.QifDom;
import qif.data.Statement;

public class ReviewDialog extends JFrame {
	private static ReviewDialog dlg = null;

	public static void review(Statement stmt) {
		if (dlg == null) {
			dlg = new ReviewDialog(stmt);
		} else {
			dlg.setStatement(stmt);
		}

		dlg.setVisible(true);
	}

	public List<GenericTxn> cleared = new ArrayList<GenericTxn>();
	public List<GenericTxn> uncleared = new ArrayList<GenericTxn>();

	List<GenericTxn> txns = new ArrayList<GenericTxn>();

	private JList<String> txlist;
	private JButton okbut;
	private JTextArea status;

	private Statement stmt;

	public ReviewDialog(Statement stmt) {
		init(stmt);
	}

	private void init(final Statement stmt) {
		setLocation(100, 100);
		setSize(500, 600);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		this.txns = new ArrayList<GenericTxn>();

		this.txlist = new JList<String>();
		this.txlist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		final JScrollPane txscroll = new JScrollPane(this.txlist);

		getContentPane().add(txscroll, BorderLayout.CENTER);

		this.status = new JTextArea(4, 2);

		getContentPane().add(this.status, BorderLayout.NORTH);

		this.okbut = new JButton("Ok");
		final JButton canbut = new JButton("Cancel");
		final JPanel butpanel = new JPanel(new GridLayout(1, 2));
		butpanel.add(this.okbut);
		butpanel.add(canbut);

		getContentPane().add(butpanel, BorderLayout.SOUTH);

		addListeners();

		setStatement(stmt);
	}

	public void setStatement(Statement stmt) {
		this.stmt = stmt;

		this.txns.clear();

		this.txns.addAll(stmt.transactions);
		this.txns.addAll(stmt.unclearedTransactions);
		Common.sortTransactionsByDate(this.txns);

		final String[] items = new String[this.txns.size()];
		for (int ii = 0; ii < items.length; ++ii) {
			items[ii] = this.txns.get(ii).toStringShort(true);
		}

		this.txlist.setListData(items);

		final int[] indices = new int[stmt.transactions.size()];
		int idx = 0;
		for (final GenericTxn t : stmt.transactions) {
			indices[idx++] = this.txns.indexOf(t);
		}

		this.txlist.setSelectedIndices(indices);

		final int[] sel = this.txlist.getSelectedIndices();
		final String statusMsg = buildStatusString(stmt, sel);
		this.status.setText(statusMsg);
	}

	private void addListeners() {
		this.txlist.setSelectionModel(new DefaultListSelectionModel() {
			public void setSelectionInterval(int index0, int index1) {
				if (index0 == index1) {
					if (isSelectedIndex(index0)) {
						removeSelectionInterval(index0, index0);
					} else {
						super.addSelectionInterval(index0, index1);
					}

					return;
				}

				super.setSelectionInterval(index0, index1);
			}

			public void addSelectionInterval(int index0, int index1) {
				if (index0 == index1) {
					if (isSelectedIndex(index0)) {
						removeSelectionInterval(index0, index0);
						return;
					}

					super.addSelectionInterval(index0, index1);
				}
			}
		});

		this.txlist.addListSelectionListener(e -> {
			final int[] sel1 = this.txlist.getSelectedIndices();
			final String statusMsg1 = buildStatusString(this.stmt, sel1);

			// this.stmt.clearTransactions(this.cleared, this.uncleared);

			this.status.setText(statusMsg1);
		});

		this.okbut.addActionListener(e -> {
			this.stmt.clearTransactions(this.cleared, this.uncleared);
		});
	}

	private String buildStatusString(Statement stmt, int[] sel) {
		this.cleared.clear();
		this.uncleared.clear();

		this.uncleared.addAll(this.txns);

		String statusMsg = "No Statement!";

		if (this.stmt != null) {
			BigDecimal clearedBalance = stmt.getOpeningCashBalance();
			for (final int idx : sel) {

				final GenericTxn t = this.txns.get(idx);
				clearedBalance = clearedBalance.add(t.getCashAmount());

				this.uncleared.remove(t);
				this.cleared.add(t);
			}

			final Account a = QifDom.getDomById(stmt.domid).getAccount(stmt.acctid);
			statusMsg = a.name + ": " + Common.formatDate(stmt.date) + "\n";
			statusMsg += String.format( //
					"Opening Balance:\t%10.2f\nClosing Balance:\t%10.2f\nCleared Balance:\t%10.2f", //
					stmt.getOpeningCashBalance(), stmt.cashBalance, clearedBalance);

			final BigDecimal diff = clearedBalance.subtract(stmt.cashBalance);

			statusMsg += String.format(" %10.2f", diff);
			if (Common.isEffectivelyEqual(diff, BigDecimal.ZERO)) {
				statusMsg += " OK";
			}
		}

		return statusMsg;
	}
}