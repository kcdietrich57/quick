package qif.data;

import java.math.BigDecimal;

import qif.importer.QFileReader;

/** Price for a security on a given date */
public class QPrice implements Comparable<QPrice> {
	public final QDate date;
	public final int secid;

	// TODO meaning of price vs split-adjusted price?
	private final BigDecimal price;
	private final BigDecimal splitAdjustedPrice;

	public QPrice(QDate date, int secid, BigDecimal price, BigDecimal splitAdjPrice) {
		this.date = date;
		this.secid = secid;
		this.price = price;
		this.splitAdjustedPrice = splitAdjPrice;
		
		if (splitAdjPrice != null) {
			//System.out.println();
		}
	}

	public BigDecimal getPrice() {
		return this.price;
	}

	public BigDecimal getSplitAdjustedPrice() {
		return (this.splitAdjustedPrice != null) //
				? this.splitAdjustedPrice //
				: this.price;
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

		Security sec = Security.findSecurity(sym);
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

		// figure out splitAdjustedPrice (or ignore quicken price history?)
		QPrice p = new QPrice(date, sec.secid, price, null);

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
		return this.getSplitAdjustedPrice().compareTo(o.getSplitAdjustedPrice());
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
}
