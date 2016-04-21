package qif.data;

import java.math.BigDecimal;
import java.util.Date;

public class Price {
	public static final Price ZERO = new Price(new BigDecimal(0));

	// TODO get rid of symbol here
	public String symbol;

	// Price at the specified date
	public BigDecimal price;
	// Current price adjusted for splits
	public BigDecimal splitAdjustedPrice;
	public Date date;

	public Price() {
		this.symbol = "";
		this.price = null;
		this.splitAdjustedPrice = null;
		this.date = null;
	}

	public Price(BigDecimal val) {
		this();

		this.price = val;
	}

	public Price(BigDecimal val, Date date) {
		this(val);

		this.date = date;
	}

	public static Price load(QFileReader qfr) {
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
		final Date date = Common.parseDate(datestr);

		final Price p = new Price();

		p.symbol = sym;
		p.price = price;
		// TODO figure this out if possible (or just ignore quicken's price
		// history?Ïß)
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

	public String toString() {
		final String s = "Price: " + this.symbol //
				+ " date=" + this.date //
				+ " price=" + this.price //
				+ " splitAdjusted=" + this.splitAdjustedPrice //
				+ "\n";

		return s;
	}
}