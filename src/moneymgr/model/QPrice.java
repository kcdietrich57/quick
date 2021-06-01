package moneymgr.model;

import java.math.BigDecimal;

import moneymgr.io.qif.QFileReader;
import moneymgr.model.Security.StockSplitInfo;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/** Price for a security on a given date */
public class QPrice implements Comparable<QPrice> {
	public final MoneyMgrModel model;
	public final int secid;

	public final QDate date;

	/** price is the price on the date of the quote */
	private final BigDecimal price;
	/** splitAdjustedPrice is the price adjusted for later splits */
	private BigDecimal splitAdjustedPrice;

	public QPrice(QDate date, int secid, BigDecimal price, BigDecimal splitAdjPrice) {
		this.model = MoneyMgrModel.currModel;
		
		this.secid = secid;
		this.date = date;
		this.price = price;
		this.splitAdjustedPrice = splitAdjPrice;
	}

	public QPrice(QDate date, int secid, BigDecimal price) {
		this(date, secid, price, null);
	}

	public BigDecimal getPrice() {
		return this.price;
	}

	public BigDecimal getSplitAdjustedPrice() {
		if (this.splitAdjustedPrice == null) {
			this.splitAdjustedPrice = this.price;

			for (StockSplitInfo split : this.model.getSecurity(this.secid).splits) {
				if (split.splitDate.compareTo(this.date) > 0) {
					this.splitAdjustedPrice = this.splitAdjustedPrice.divide(split.splitRatio);
				}
			}
		}

		return this.splitAdjustedPrice;
	}

	/** Read/parse QIF security price (symbol/price/date) */
	public static QPrice load(QFileReader qfr) {
		final QFileReader.QLine qline = new QFileReader.QLine();

		// Ex: "FEQIX",48 3/4," 2/16' 0"
		String s = qfr.readLine();
		if (s == null) {
			return null;
		}

		if (!s.startsWith("\"")) {
			Common.reportError("syntax error for price");
		}

		int idx = s.indexOf('"', 1);
		if (idx < 0) {
			Common.reportError("syntax error for price");
		}

		final String sym = s.substring(1, idx);
		s = s.substring(idx + 1);

		if (!s.startsWith(",")) {
			Common.reportError("syntax error for price");
		}

		idx = s.indexOf(',', 1);
		if (idx < 0) {
			Common.reportError("syntax error for price");
		}

		Security sec = MoneyMgrModel.currModel.findSecurity(sym);
		final String pricestr = s.substring(1, idx);
		final BigDecimal price = Common.parsePrice(pricestr);

		s = s.substring(idx + 1);

		if (!s.startsWith("\"")) {
			Common.reportError("syntax error for price");
		}

		idx = s.indexOf('"', 1);
		if (idx < 0) {
			Common.reportError("syntax error for price");
		}

		final String datestr = s.substring(1, idx);
		final QDate date = Common.parseQDate(datestr);

		// TODO figure out splitAdjustedPrice (or ignore quicken price history?)
		QPrice p = new QPrice(date, sec.secid, price);

		// Ex: "FEQIX",48 3/4," 2/16' 0"
		qfr.nextPriceLine(qline);

		switch (qline.type) {
		case EndOfSection:
			return p;

		default:
			Common.reportError("syntax error for price");
			return null;
		}
	}

	public boolean equals(Object o) {
		if (!(o instanceof QPrice)) {
			return false;
		}

		return Common.isEffectivelyEqual( //
				this.splitAdjustedPrice, ((QPrice) o).splitAdjustedPrice);
	}

	public int compareTo(QPrice o) {
		// return this.getSplitAdjustedPrice().compareTo(o.getSplitAdjustedPrice());
		return this.getPrice().compareTo(o.getPrice());
	}

	public String toString() {
		String s = String.format("Price: %s  %s", //
				this.date.toString(), //
				Common.formatAmount(this.price));

		if (this.splitAdjustedPrice != null) {
			s += String.format("  splitAdj(%s)", //
					Common.formatAmount(this.splitAdjustedPrice));
		}

		return s;
	}

	public boolean matches(QPrice other) {
		return this.secid == other.secid //
				&& this.date.equals(other.date) //
				// && Common.isEffectivelyEqual(this.getSplitAdjustedPrice(),
				// other.getSplitAdjustedPrice()) //
				&& Common.isEffectivelyEqual(this.price, other.price);
	}
}
