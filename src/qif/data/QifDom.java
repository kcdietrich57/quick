package qif.data;

import java.io.File;
import java.math.BigDecimal;

/** Document Object Model for finances from Quicken */
public class QifDom {
	// The one and only financial model
	public static QifDom dom = null;
	public static File qifDir;

	public boolean verbose = false;

	/** Pay attention to version of the loaded QIF file format */
	public int loadedStatementsVersion = -1;

	public QifDom(File qifDir) {
		QifDom.dom = this;
		QifDom.qifDir = qifDir;
	}

	public static class Balances {
		public BigDecimal netWorth = BigDecimal.ZERO;
		public BigDecimal assets = BigDecimal.ZERO;
		public BigDecimal liabilities = BigDecimal.ZERO;
	}

	public Balances getNetWorthForDate(QDate d) {
		final Balances b = new Balances();

		if (d == null) {
			d = QDate.today();
		}

		for (Account a : Account.accounts) {
			final BigDecimal amt = a.getValueForDate(d);

			b.netWorth = b.netWorth.add(amt);

			if (a.isAsset()) {
				b.assets = b.assets.add(amt);
			} else if (a.isLiability()) {
				b.liabilities = b.liabilities.add(amt);
			}
		}

		return b;
	}
}
