package qif.data;

import java.util.Date;

class Price {
	public String symbol;
	public String price;
	public Date date;

	public Price() {
		this.symbol = "";
		this.price = "";
		this.date = null;
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

		final String priceval = s.substring(1, idx);
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

		final Price price = new Price();

		price.symbol = sym;
		price.price = priceval;
		price.date = date;

		// Ex: "FEQIX",48 3/4," 2/16' 0"
		qfr.nextPriceLine(qline);

		switch (qline.type) {
		case EndOfSection:
			return price;

		default:
			Common.reportError("syntax error for price");
			return null;
		}
	}

	public String toString() {
		final String s = "Price: " + this.symbol //
				+ " price=" + this.price //
				+ " date=" + this.date //
				+ "\n";

		return s;
	}
}