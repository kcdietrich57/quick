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

import qif.data.Common;
import qif.data.GenericTxn;
import qif.data.Statement;

public class ReviewDialog extends JFrame {
	public List<GenericTxn> cleared = new ArrayList<GenericTxn>();
	public List<GenericTxn> uncleared = new ArrayList<GenericTxn>();
	List<GenericTxn> txns = new ArrayList<GenericTxn>();

	public ReviewDialog(Statement stmt) {
		init(stmt);
	}

	private void init(final Statement stmt) {
		setLocation(100, 100);
		setSize(500, 600);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		this.txns = new ArrayList<GenericTxn>();
		this.txns.addAll(stmt.transactions);
		this.txns.addAll(stmt.unclearedTransactions);
		Common.sortTransactionsByDate(this.txns);

		final String[] items = new String[this.txns.size()];
		for (int ii = 0; ii < items.length; ++ii) {
			items[ii] = this.txns.get(ii).toStringShort(true);
		}

		final JList<String> txlist = new JList<String>(items);
		txlist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		txlist.setSelectionModel(new DefaultListSelectionModel() {
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

		final int[] indices = new int[stmt.transactions.size()];
		int idx = 0;
		for (final GenericTxn t : stmt.transactions) {
			indices[idx++] = this.txns.indexOf(t);
		}
		txlist.setSelectedIndices(indices);

		final JScrollPane txscroll = new JScrollPane(txlist);

		getContentPane().add(txscroll, BorderLayout.CENTER);

		final int[] sel = txlist.getSelectedIndices();
		final String statusMsg = buildStatusString(stmt, sel);

		final JTextArea status = new JTextArea(statusMsg, 3, 2);

		getContentPane().add(status, BorderLayout.NORTH);

		final JButton okbut = new JButton("Ok");
		final JButton canbut = new JButton("Cancel");
		final JPanel butpanel = new JPanel(new GridLayout(1, 2));
		butpanel.add(okbut);
		butpanel.add(canbut);

		getContentPane().add(butpanel, BorderLayout.SOUTH);

		txlist.addListSelectionListener(e -> {
			final int[] sel1 = txlist.getSelectedIndices();
			final String statusMsg1 = buildStatusString(stmt, sel1);

			status.setText(statusMsg1);
		});

		okbut.addActionListener(e -> {
			stmt.clearTransactions(this.cleared, this.uncleared);
		});
	}

	private String buildStatusString(Statement stmt, int[] sel) {
		this.cleared.clear();
		this.uncleared.clear();

		this.uncleared.addAll(this.txns);

		BigDecimal clearedBalance = stmt.getOpeningCashBalance();
		for (final int idx : sel) {
			final GenericTxn t = stmt.transactions.get(idx);
			clearedBalance = clearedBalance.add(t.getCashAmount());

			this.uncleared.remove(t);
			this.cleared.add(t);
		}

		String statusMsg = String.format( //
				"Opening Balance:\t%10.2f\nClosing Balance:\t%10.2f\nCleared Balance:\t%10.2f", //
				stmt.getOpeningCashBalance(), stmt.cashBalance, clearedBalance);

		final BigDecimal diff = clearedBalance.subtract(stmt.cashBalance);

		statusMsg += String.format(" %10.2f", diff);
		if (Common.isEffectivelyEqual(diff, BigDecimal.ZERO)) {
			statusMsg += " OK";
		}

		return statusMsg;
	}
}
