package moneymgr.util;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import app.QifDom;
import moneymgr.model.Account;
import moneymgr.model.Category;
import moneymgr.model.GenericTxn;
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.QPrice;

/** Useful utility functions */
public class Common {
	/** Threshold for deeming BigDecimal values to be effectively equal */
	private static final BigDecimal CLOSE_ENOUGH_TO_ZERO = new BigDecimal(0.005);

	/** Log info message */
	public static void reportInfo(String s) {
		System.out.println("**** Info: " + s);
	}

	/** Log info message */
	public static void debugInfo(String s) {
		if (QifDom.verbose) {
			System.out.println("**** Info: " + s);
		}
	}

	/** Log warning message */
	public static void reportWarning(String s) {
		System.out.println("**** Warning! " + s);
	}

	/** Log error message */
	public static void reportError(String s) {
		System.out.println("**** ERROR! " + s);
		throw new RuntimeException(s);
	}

	/** One day, in milliseconds */
	private static final long MS_PER_DAY = (long) (24 * 60 * 60 * 1000);

	/** Convert ms to whole days */
	public static int msToDays(long ms) {
		int sign = (ms < 0) ? -1 : 1;
		return sign * (int) ((Math.abs(ms) + MS_PER_DAY / 2) / MS_PER_DAY);
	}

	/** TODO move this to tx: Sort transactions list by date and id */
	public static void sortTransactionsByDate(List<GenericTxn> txns) {
		final Comparator<GenericTxn> cmptor = (t1, t2) -> {
			final int diff = t1.getDate().compareTo(t2.getDate());

			if (diff != 0) {
				return diff;
			}

			return t1.getTxid() - t2.getTxid();
		};

		Collections.sort(txns, cmptor);
	}

	public static int parseCategory(String s) {
		if (s.startsWith("[") || s.startsWith("Transfer:[")) {
			s = s.substring(s.indexOf("[") + 1, s.length() - 1).trim();

			Account acct = MoneyMgrModel.currModel.findAccount(s);

			return (short) ((acct != null) ? (-acct.acctid) : 0);
		}

		int slash = s.indexOf('/');
		if (slash >= 0) {
			// Throw away tag
			s = s.substring(slash + 1);
		}

		Category cat = MoneyMgrModel.currModel.findCategory(s);

		return (cat != null) ? (cat.catid) : 0;
	}

	/** Parse a decimal value string (possibly with separators) */
	public static BigDecimal getDecimal(String value) {
		return parseDecimal(value.trim().replace(",", ""));
	}

	/** Parse a decimal value string */
	public static BigDecimal parseDecimal(String s) {
		try {
			return new BigDecimal(s);
		} catch (final Exception e) {
			reportError("Bad decimal string: '" + s + "'");
		}

		return null;
	}

	/** Is a value zero (or very close) */
	public static boolean isEffectivelyZero(BigDecimal n) {
		return (n.signum() == 0) || (CLOSE_ENOUGH_TO_ZERO.compareTo(n.abs()) > 0);
	}

	/** Is a value null or zero (or very close) */
	public static boolean isEffectivelyZeroOrNull(BigDecimal n) {
		return (n == null) || isEffectivelyZero(n);
	}

	/** Are two values equal (or very close) */
	public static boolean isEffectivelyEqual(BigDecimal d1, BigDecimal d2) {
		if (d1 == null) {
			d1 = BigDecimal.ZERO;
		}
		if (d2 == null) {
			d2 = BigDecimal.ZERO;
		}

		if (d1 == null) {
			return (d2 == null);
		} else if (d2 == null) {
			return false;
		}

		final BigDecimal diff = d1.subtract(d2).abs();
		return (diff.signum() == 0) ||(CLOSE_ENOUGH_TO_ZERO.compareTo(diff) > 0);
	}

	/** Parse an input boolean string (using Java rules) */
	public static boolean parseBoolean(String value) {
		return (value.length() > 0) && Boolean.parseBoolean(value);
	}

	/**
	 * Convert a QIF date string to standard format.<br>
	 * Sometimes use "'" for separator. Change to "/".<br>
	 * Sometimes have spaces with single-digit day/month numbers.
	 */
	public static String convertQIFDateString(String qifDateString) {
		qifDateString = qifDateString.replaceFirst("'", "/");

		return qifDateString.replace(" ", "0");
	}

	/** Parse a date in various formats. Return QDate object */
	public static QDate parseQDate(String value) {
		return new QDate(parseDate(value));
	}

	/** Parse a date in various formats. Return Java Date object */
	public static Date parseDate(String value) {
		try {
			// Format found in QIF transactions
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
			// e.printStackTrace();
		}

		return null;
	}

	/** Construct a Java Date from y/m/d values */
	public static Date getDate(int year, int month, int day) {
		final String datestr = "" + month + "/" + day + "/" + year;

		return parseDate(datestr);
	}

	/**
	 * Pad/truncate a string, possibly null, to maximum length<br>
	 * If maxlen is zero, return entire string or "" if null<br>
	 * If maxlen is negative, left justify the result.
	 */
	public static String formatString(String s, int maxlen) {
		if (s == null) {
			s = "";
		}
		if (maxlen == 0) {
			return s;
		}

		boolean leftJustify = false;

		if (maxlen < 0) {
			leftJustify = true;
			maxlen = -maxlen;
		}

		if (s.length() > maxlen) {
			return s.substring(0, maxlen);
		}

		String pattern = "%" + ((leftJustify) ? "-" : "") + maxlen + "s";
		return String.format(pattern, s);
	}

	public static String repeatChar(char c, int length) {
		StringBuffer sb = new StringBuffer();

		while (length-- > 0) {
			sb.append(c);
		}

		return sb.toString();
	}

	/** Safely get the string representation of an object, possibly null */
	public static String stringValue(Object o) {
		if (o == null) {
			return "";
		}

		if (!(o instanceof String)) {
			return o.toString();
		}

		return (String) o;
	}

	/** Format a decimal value to two places */
	public static String formatAmount(BigDecimal amt) {
		return (amt != null) ? String.format("%10.2f", amt) : "0.0";
	}

	/** Format a decimal value as integer */
	public static String formatAmount0(BigDecimal amt) {
		return (amt != null) ? String.format("%,10.0f", amt) : "0.0";
	}

	/** Format a decimal value to three places */
	public static String formatAmount3(BigDecimal amt) {
		return (amt != null) ? String.format("%10.3f", amt) : "0.0";
	}

	/** Format a decimal value to two places */
	public static String formatAmount(QPrice amt) {
		return (amt != null) ? formatAmount(amt.getPrice()) : "0.0";
	}

	/** Format a decimal value as integer */
	public static String formatAmount0(QPrice amt) {
		return (amt != null) ? formatAmount0(amt.getPrice()) : "0.0";
	}

	/** Format a decimal value to three places */
	public static String formatAmount3(QPrice amt) {
		return (amt != null) ? formatAmount3(amt.getPrice()) : "0.0";
	}

	/** Format a date mm/dd/yy */
	public static String formatDate(QDate date) {
		return formatDate(date, "MM/dd/yy");
	}

//	/** Format a date mm/dd/yyyy */
//	private static String formatDateLong(QDate date) {
//		return formatDate(date, "MM/dd/yyyy");
//	}
//
//	/** Format a date mm/dd */
//	private static String formatDateShort(QDate date) {
//		return formatDate(date, "MM/dd");
//	}
//
//	/** Format a date mm/yyyy */
//	private static String formatDateMonthYear(QDate date) {
//		return formatDate(date, "MM/yyyy");
//	}

	/** Format a date (possibly null) with a given SimpleDateFormat format */
	private static String formatDate(QDate date, String format) {
		if (date == null) {
			return "null";
		}

		return new SimpleDateFormat(format).format(date.toDate());
	}

	/** Parse a security price (decimal or fraction) */
	public static BigDecimal parsePrice(String pricestr) {
		if (pricestr.length() == 0) {
			return BigDecimal.ZERO;
		}

		pricestr = pricestr.replaceAll(",", "");

		// Separate decimal and fraction part
		String fracstr = null;
		int slash = pricestr.indexOf('/');
		if (slash > 0) {
			final int space = pricestr.indexOf(' ');

			fracstr = (space > 0) ? pricestr.substring(space) : pricestr;
			pricestr = (space > 0) ? pricestr.substring(0, space) : "0";
		}

		// Parse decimal part
		BigDecimal price = new BigDecimal(pricestr);

		// Add fraction
		if (fracstr != null) {
			final BigDecimal frac = parseFraction(fracstr);
			price = frac.add(price);
		}

		return price;
	}

	/** Parse fractional part of price */
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

	public static BigDecimal sumCashAmounts(List<GenericTxn> txns) {
		BigDecimal totaltx = BigDecimal.ZERO;
		for (GenericTxn t : txns) {
			totaltx = totaltx.add(t.getCashAmount());
		}

		return totaltx;
	}

	public static String safeToString(Object o) {
		return (o != null) ? o.toString() : "null";
	}

	public static boolean safeEquals(String s1, String s2) {
		if (s1 == null) {
			s1 = "";
		}
		if (s2 == null) {
			s2 = "";
		}

		return s1.equals(s2);
	}

//	private static void writeIfSet(PrintWriter pw, String tag, String value) {
//		pw.println("" + tag + value);
//	}
//
//	private static void writeln(PrintWriter pw, String tag) {
//		pw.println(tag);
//	}
//
//	private static void write(PrintWriter pw, char key) {
//		pw.println("" + key);
//	}
//
//	private static void write(PrintWriter pw, String s) {
//		pw.println(s);
//	}
//
//	private static void writeIfSet(PrintWriter pw, char key, String value) {
//		if (value != null && value.length() > 0) {
//			write(pw, key, value);
//		}
//	}
//
//	private static BigDecimal sumAmounts(List<GenericTxn> txns) {
//		BigDecimal totaltx = BigDecimal.ZERO;
//		for (final GenericTxn t : txns) {
//			totaltx = totaltx.add(t.getAmount());
//		}
//
//		return totaltx;
//	}
//
//	private static String getCheckNumString(GenericTxn t) {
//		return (t instanceof NonInvestmentTxn) //
//				? ((NonInvestmentTxn) t).chkNumber //
//				: "";
//	}
//
//	private static void write(PrintWriter pw, char key, String value) {
//		pw.println("" + key + value);
//	}
}
