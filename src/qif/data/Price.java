package qif.data;

import java.math.BigDecimal;
import java.util.Date;

public class Price {
	public static final Price ZERO = new Price(new BigDecimal(0));

	// Price at the specified date
	public BigDecimal price;
	// Current price adjusted for splits
	public BigDecimal splitAdjustedPrice;
	public Date date;

	public Price() {
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
		final Date date = Common.parseDate(datestr);

		final QPrice p = new QPrice();

		p.symbol = sym;
		p.price.price = price;
		// TODO figure out splitAdjustedPrice (or ignore quicken price history?)
		p.price.splitAdjustedPrice = null;
		p.price.date = date;

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
		String s = String.format("Price: %s  %10.2f", //
				Common.formatDate(this.date), //
				this.price);

		if (this.splitAdjustedPrice != null) {
			s += String.format("  splitAdj(%10.2f)\n", this.splitAdjustedPrice);
		}

		return s + "\n";
	}
}

class QPrice {
	public String symbol;
	public Price price;

	public QPrice() {
		this.price = new Price();
		this.symbol = "";
	}

	public String toString() {
		final String s = "Price: " + this.symbol //
				+ " date=" + this.price.date //
				+ " price=" + this.price.price //
				+ " splitAdjusted=" + this.price.splitAdjustedPrice //
				+ "\n";

		return s;
	}
}