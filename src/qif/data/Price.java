package qif.data;

import java.math.BigDecimal;
import java.util.Date;

public class Price {
	public String symbol;
	public BigDecimal price;
	public Date date;

	public Price() {
		this.symbol = "";
		this.price = null;
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

		final String pricestr = s.substring(1, idx);
		final BigDecimal price = parsePrice(pricestr);

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

	private static BigDecimal parsePrice(String pricestr) {
		if (pricestr.length() == 0) {
			return BigDecimal.ZERO;
		}

		String fracstr = null;
		int slash = pricestr.indexOf('/');
		if (slash > 0) {
			int space = pricestr.indexOf(' ');

			fracstr = (space > 0) ? pricestr.substring(space) : pricestr;
			pricestr = (space > 0) ? pricestr.substring(0, space) : "0";
		}

		BigDecimal price = new BigDecimal(pricestr);
		if (fracstr != null) {
			BigDecimal frac = parseFraction(fracstr);
			price = frac.add(price);
		}

		return price;
	}

	private static BigDecimal parseFraction(String fracstr) {
		if ((fracstr.length() != 4) || //
				(fracstr.charAt(0) != ' ') || //
				(fracstr.charAt(2) != '/')) {
			return BigDecimal.ZERO;
		}

		int numerator = " 1 3 5 7".indexOf(fracstr.charAt(1));
		if ((numerator < 1) || ((numerator & 1) == 0)) {
			return BigDecimal.ZERO;
		}

		int denominator = " 248".indexOf(fracstr.charAt(3));
		if (denominator < 1) {
			return BigDecimal.ZERO;
		}
		denominator = 1 << denominator;

		return new BigDecimal(numerator).divide(new BigDecimal(denominator));
	}

	public String toString() {
		final String s = "Price: " + this.symbol //
				+ " price=" + this.price //
				+ " date=" + this.date //
				+ "\n";

		return s;
	}
}