
package qif.data;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import qif.data.Account.AccountType;
import qif.data.SimpleTxn.Action;

public class Common {
	public static void reportWarning(String s) {
		System.out.println("**** Warning!" + s);
	}

	public static void reportError(String s) {
		throw new RuntimeException(s);
	}

	public static boolean parseBoolean(String value) {
		return (value.length() > 0) && Boolean.parseBoolean(value);
	}

	public static String convertQIFDateString(String qifDateString) {
		final int i = qifDateString.indexOf("'");
		if (i != -1) {
			qifDateString = qifDateString.substring(0, i) + "/" + qifDateString.substring(i + 1);
		}

		return qifDateString.replace(" ", "0");
	}

	public static BigDecimal getDecimal(String value) {
		final StringBuilder sb = new StringBuilder(value);
		for (;;) {
			final int comma = sb.indexOf(",");
			if (comma < 0) {
				break;
			}

			sb.deleteCharAt(comma);
		}

		return new BigDecimal(sb.toString());
	}

	public static Date parseDate(String value) {
		try {
			final DateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
			final String s = convertQIFDateString(value);

			final Date d = dateFormat.parse(s);
			return d;
		} catch (final ParseException e) {
			// e.printStackTrace();
		}

		try {
			final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			final String s = convertQIFDateString(value);

			final Date d = dateFormat.parse(s);
			return d;
		} catch (final ParseException e) {
			// e.printStackTrace();
		}

		try {
			final DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yy");
			final String s = convertQIFDateString(value);

			final Date d = dateFormat.parse(s);
			return d;
		} catch (final ParseException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static String getDateString(Date date) {
		final DateFormat dfmt = new SimpleDateFormat("MM/dd/yyyy");
		return dfmt.format(date);
	}

	public static BigDecimal parsePrice(String pricestr) {
		if (pricestr.length() == 0) {
			return BigDecimal.ZERO;
		}

		String fracstr = null;
		final int slash = pricestr.indexOf('/');
		if (slash > 0) {
			final int space = pricestr.indexOf(' ');

			fracstr = (space > 0) ? pricestr.substring(space) : pricestr;
			pricestr = (space > 0) ? pricestr.substring(0, space) : "0";
		}

		BigDecimal price = new BigDecimal(pricestr);
		if (fracstr != null) {
			final BigDecimal frac = parseFraction(fracstr);
			price = frac.add(price);
		}

		return price;
	}

	public static BigDecimal parseFraction(String fracstr) {
		if ((fracstr.length() != 4) || //
				(fracstr.charAt(0) != ' ') || //
				(fracstr.charAt(2) != '/')) {
			return BigDecimal.ZERO;
		}

		final int numerator = " 1 3 5 7".indexOf(fracstr.charAt(1));
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

	public static AccountType parseAccountType(String s) {
		switch (s.charAt(0)) {
		case 'B':
			if (s.equals("Bank")) {
				return AccountType.Bank;
			}
			break;
		case 'C':
			if (s.equals("CCard")) {
				return AccountType.CCard;
			}
			if (s.equals("Cash")) {
				return AccountType.Cash;
			}
			break;
		case 'I':
			if (s.equals("Invst")) {
				return AccountType.Invest;
			}
			break;
		case 'M':
			if (s.equals("Mutual")) {
				return AccountType.InvMutual;
			}
			break;
		case 'O':
			if (s.equals("Oth A")) {
				return AccountType.Asset;
			}
			if (s.equals("Oth L")) {
				return AccountType.Liability;
			}
			break;
		case 'P':
			if (s.equals("Port")) {
				return AccountType.InvPort;
			}
			break;
		case '4':
			if (s.equals("401(k)/403(b)")) {
				return AccountType.Inv401k;
			}
			break;
		}

		Common.reportError("Unknown account type: " + s);
		return AccountType.Bank;
	}

	public static Action parseAction(String s) {
		if ("StkSplit".equals(s)) {
			return Action.STOCKSPLIT;
		}
		if ("Cash".equals(s)) {
			return Action.CASH;
		}
		if ("XIn".equals(s)) {
			return Action.XIN;
		}
		if ("XOut".equals(s)) {
			return Action.XOUT;
		}
		if ("Buy".equals(s)) {
			return Action.BUY;
		}
		if ("BuyX".equals(s)) {
			return Action.BUYX;
		}
		if ("Sell".equals(s)) {
			return Action.SELL;
		}
		if ("SellX".equals(s)) {
			return Action.SELLX;
		}
		if ("ShrsIn".equals(s)) {
			return Action.SHRS_IN;
		}
		if ("ShrsOut".equals(s)) {
			return Action.SHRS_OUT;
		}
		if ("Grant".equals(s)) {
			return Action.GRANT;
		}
		if ("Vest".equals(s)) {
			return Action.VEST;
		}
		if ("ExercisX".equals(s)) {
			return Action.EXERCISEX;
		}
		if ("Expire".equals(s)) {
			return Action.EXPIRE;
		}
		if ("WithdrwX".equals(s)) {
			return Action.WITHDRAWX;
		}
		if ("IntInc".equals(s)) {
			return Action.INT_INC;
		}
		if ("MiscIncX".equals(s)) {
			return Action.MISC_INCX;
		}
		if ("Div".equals(s)) {
			return Action.DIV;
		}
		if ("ReinvDiv".equals(s)) {
			return Action.REINV_DIV;
		}
		if ("ReinvLg".equals(s)) {
			return Action.REINV_LG;
		}
		if ("ReinvSh".equals(s)) {
			return Action.REINV_SH;
		}
		if ("ReinvInt".equals(s)) {
			return Action.REINV_INT;
		}
		if ("ContribX".equals(s)) {
			return Action.CONTRIBX;
		}
		if ("Reminder".equals(s)) {
			return Action.REMINDER;
		}

		return Action.OTHER;
	}

	public static void writeIfSet(PrintWriter pw, String tag, String value) {
		pw.println("" + tag + value);
	}

	public static void writeln(PrintWriter pw, String tag) {
		pw.println("" + tag);
	}

	public static void write(PrintWriter pw, char key) {
		pw.println("" + key);
	}

	public static void write(PrintWriter pw, String s) {
		pw.println(s);
	}

	public static void writeIfSet(PrintWriter pw, char key, String value) {
		if (value != null && value.length() > 0) {
			write(pw, key, value);
		}
	}

	public static void write(PrintWriter pw, char key, String value) {
		pw.println("" + key + value);
	}

	// Return the index of the first transaction on or after a given date
	// in a list of transactions sorted by date.
	public static int findFirstTransactionOnOrAfterDate( //
			List<? extends GenericTxn> txns, Date d) {
		if (txns.isEmpty()) {
			return -1;
		}

		int idx = findTransactionForDate(txns, d);

		while ((idx > 0) //
				&& (txns.get(idx - 1).getDate().equals(d))) {
			--idx;
		}

		return idx;
	}

	// Return the index of the last transaction on or before a given date
	// in a list of transactions sorted by date.
	// Returns -1 if no such transaction exists.
	public static int findLastTransactionOnOrBeforeDate( //
			List<? extends GenericTxn> txns, Date d) {
		if (txns.isEmpty()) {
			return -1;
		}

		int idx = findTransactionForDate(txns, d);

		while ((idx < txns.size() - 1) //
				&& (txns.get(idx + 1).getDate().equals(d))) {
			++idx;
		}

		if (txns.get(idx).getDate().compareTo(d) > 0) {
			return -1;
		}

		return idx;
	}

	// Return the index of a transaction on or before a given date
	// in a list of transactions sorted by date.
	private static int findTransactionForDate( //
			List<? extends GenericTxn> txns, Date d) {
		int loidx = 0;
		int hiidx = txns.size() - 1;
		final Date loval = txns.get(loidx).getDate();
		final Date hival = txns.get(hiidx).getDate();
		if (loval.compareTo(d) >= 0) {
			return loidx;
		}
		if (hival.compareTo(d) <= 0) {
			return hiidx;
		}

		while (loidx < hiidx) {
			final int idx = (loidx + hiidx) / 2;
			if (idx <= loidx || idx >= hiidx) {
				return idx;
			}
			final Date val = txns.get(idx).getDate();

			if (val.compareTo(d) < 0) {
				loidx = idx;
			} else if (val.compareTo(d) > 0) {
				hiidx = idx;
			} else {
				return idx;
			}
		}

		return loidx;
	}

	private static int MONTH_DAYS[] = { //
			31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 //
	};

	public static Date getDate(int year, int month, int day) {
		final String datestr = "" + month + "/" + day + "/" + year;

		return parseDate(datestr);
	}

	public static Date getDateForEndOfMonth(int year, int month) {
		final String datestr = "" + month + "/" + MONTH_DAYS[month - 1] + "/" + year;

		return parseDate(datestr);
	}
};
