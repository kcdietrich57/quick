package qif.data;

import java.math.BigDecimal;

import qif.importer.QFileReader;

public class QPrice implements Comparable<QPrice> {
	public static final QPrice ZERO = new QPrice(new BigDecimal(0));

	public QDate date;
	public String symbol;
	private BigDecimal price;
	private BigDecimal splitAdjustedPrice;

	public QPrice() {
		this.price = null;
		this.splitAdjustedPrice = null;
		this.date = null;
	}

	public QPrice(QDate date, String symbol, BigDecimal price, BigDecimal splitAdjPrice) {
		this.date = date;
		this.symbol = symbol;
		this.price = price;
		this.splitAdjustedPrice = splitAdjPrice;
	}

	private QPrice(BigDecimal val) {
		this();

		this.price = val;
	}

	public BigDecimal getPrice() {
		return this.price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public BigDecimal getSplitAdjustedPrice() {
		return (this.splitAdjustedPrice != null) ? this.splitAdjustedPrice : this.price;
	}

	public void setSplitAdjustedPrice(BigDecimal saPrice, BigDecimal price) {
		this.price = price;
		this.splitAdjustedPrice = saPrice;
	}

	/**
	 * Read/parse QIF security price (symbol/price/date)
	 * 
	 * @param qfr
	 *            File reader
	 * @return Price object
	 */
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

		final QPrice p = new QPrice();

		p.symbol = sym;
		p.price = price;
		// FIXME figure out splitAdjustedPrice (or ignore quicken price history?)
		p.splitAdjustedPrice = null;
		p.date = date;

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

		return Common.isEffectivelyEqual(this.splitAdjustedPrice, ((QPrice) o).splitAdjustedPrice);
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
