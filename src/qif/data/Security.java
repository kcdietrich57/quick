package qif.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Security {
	private static short nextid = 1;
	public final short id;

	public String name;
	public String symbol;
	public String type;
	public String goal;

	List<Price> prices = new ArrayList<Price>();

	public Security() {
		this.id = nextid++;

		this.name = "";
		this.symbol = "";
		this.type = "";
		this.goal = "";
	}

	public static Security load(QFileReader qfr) {
		QFileReader.QLine qline = new QFileReader.QLine();

		Security security = new Security();

		for (;;) {
			qfr.nextSecurityLine(qline);

			switch (qline.type) {
			case EndOfSection:
				return security;

			case SecName:
				security.name = qline.value;
				break;
			case SecSymbol:
				security.symbol = qline.value;
				break;
			case SecType:
				security.type = qline.value;
				break;
			case SecGoal:
				security.goal = qline.value;
				break;

			default:
				Common.reportError("syntax error");
			}
		}
	}

	public void addPrice(Price price) {
		this.prices.add(price);
	}

	public String toString() {
		String s = "Security" + this.id + ": " + this.name //
				+ " sym=" + this.symbol //
				+ " type=" + this.type //
				+ " numprices=" + this.prices.size() //
				+ "\n";

		return s;
	}

	// public static void export(PrintWriter pw, List<Account> list) {
	// if ((list == null) || (list.size() == 0)) {
	// return;
	// }
	//
	// writeln(pw, Headers.HdrAccount);
	//
	// for (Account acct : list) {
	// writeIfSet(pw, ACCT_TYPE, acct.type);
	// write(pw, ACCT_CREDITLIMIT, acct.creditLimit.toString());
	// writeIfSet(pw, ACCT_DESCRIPTION, acct.description);
	// writeIfSet(pw, Headers.ACCT_NAME, acct.name);
	// write(pw, ACCT_STMTBAL, acct.stmtBalance.toString());
	// write(pw, ACCT_STMTDATE, acct.stmtDate.toString());
	// write(pw, END);
	// }
	// }
};

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
		QFileReader.QLine qline = new QFileReader.QLine();

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

		String sym = s.substring(1, idx);
		s = s.substring(idx + 1);

		if (!s.startsWith(",")) {
			Common.reportError("syntax error for price");
		}

		idx = s.indexOf(',', 1);
		if (idx < 0) {
			Common.reportError("syntax error for price");
		}

		String priceval = s.substring(1, idx);
		s = s.substring(idx + 1);

		if (!s.startsWith("\"")) {
			Common.reportError("syntax error for price");
		}

		idx = s.indexOf('"', 1);
		if (idx < 0) {
			Common.reportError("syntax error for price");
		}

		String datestr = s.substring(1, idx);
		Date date = Common.GetDate(datestr);

		Price price = new Price();

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
		String s = "Price: " + this.symbol //
				+ " price=" + this.price //
				+ " date=" + this.date //
				+ "\n";

		return s;
	}

	// public static void export(PrintWriter pw, List<Account> list) {
	// if ((list == null) || (list.size() == 0)) {
	// return;
	// }
	//
	// writeln(pw, Headers.HdrAccount);
	//
	// for (Account acct : list) {
	// writeIfSet(pw, ACCT_TYPE, acct.type);
	// write(pw, ACCT_CREDITLIMIT, acct.creditLimit.toString());
	// writeIfSet(pw, ACCT_DESCRIPTION, acct.description);
	// writeIfSet(pw, Headers.ACCT_NAME, acct.name);
	// write(pw, ACCT_STMTBAL, acct.stmtBalance.toString());
	// write(pw, ACCT_STMTDATE, acct.stmtDate.toString());
	// write(pw, END);
	// }
	// }
};
