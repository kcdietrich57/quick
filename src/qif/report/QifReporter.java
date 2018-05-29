package qif.report;

import java.util.Date;

import qif.data.GenericTxn;
import qif.data.QifDom;

public class QifReporter {
	public static void reportActivity(Date d1, Date d2) {
		// TODO Auto-generated method stub
	}

	public static void showStatistics() {
		int nullt = 0;
		int reconciled = 0;
		int unreconciled = 0;

		for (GenericTxn t : QifDom.dom.getAllTransactions()) {
			if (t == null) {
				++nullt;
			} else if (t.stmtdate != null) {
				++reconciled;
			} else {
				++unreconciled;
			}
		}

		int total = (reconciled + unreconciled);
		final double pct = reconciled * 100.0 / total;

		System.out.println(String.format("%d of %d txns reconciled (%5.2f) nullTX: %d", //
				reconciled, total, pct, nullt));
	}
}
