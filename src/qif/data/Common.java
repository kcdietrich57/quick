package qif.data;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class Common {
	public static final BigDecimal CLOSE_ENOUGH_TO_ZERO = new BigDecimal(0.005);

	public static void reportWarning(String s) {
		System.out.println("**** Warning!" + s);
	}

	public static void reportError(String s) {
		throw new RuntimeException(s);
	}

	/**
	 * Values sometimes use thousands separators ",". Delete them.
	 * 
	 * @param value
	 * @return Massaged value
	 */
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

	public static BigDecimal parseDecimal(String s) {
		try {
			return new BigDecimal(s);
		} catch (final Exception e) {
			e.printStackTrace();
			reportError("Bad decimal string: " + s);
		}

		return null;
	}

	public static boolean isEffectivelyZero(BigDecimal n) {
		return (CLOSE_ENOUGH_TO_ZERO.compareTo(n.abs()) > 0);
	}

	public static boolean isEffectivelyEqual(BigDecimal d1, BigDecimal d2) {
		final BigDecimal diff = d1.subtract(d2).abs();
		return (CLOSE_ENOUGH_TO_ZERO.compareTo(diff) > 0);
	}

	public static boolean parseBoolean(String value) {
		return (value.length() > 0) && Boolean.parseBoolean(value);
	}

	/**
	 * QIF dates sometimes use "'" for separator. Change to "/".
	 * 
	 * @param qifDateString
	 * @return Massaged date string
	 */
	public static String convertQIFDateString(String qifDateString) {
		final int i = qifDateString.indexOf("'");
		if (i != -1) {
			qifDateString = qifDateString.substring(0, i) + "/" + qifDateString.substring(i + 1);
		}

		return qifDateString.replace(" ", "0");
	}

	public static QDate parseQDate(String value) {
		Date d = parseDate(value);

		return new QDate(d);
	}

	/**
	 * Parse a date in various formats
	 */
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

	public static Date getDate(int year, int month, int day) {
		final String datestr = "" + month + "/" + day + "/" + year;

		return parseDate(datestr);
	}

	public static String stringValue(Object o) {
		if (o == null) {
			return "";
		}

		if (!(o instanceof String)) {
			return o.toString();
		}

		return (String) o;
	}

	public static String formatAmount(BigDecimal amt) {
		if (amt == null) {
			return "null";
		}

		return String.format("%10.2f", amt);
	}

	public static String formatAmount0(BigDecimal amt) {
		if (amt == null) {
			return "null";
		}

		return String.format("%,10.0f", amt);
	}

	public static String formatAmount3(BigDecimal amt) {
		if (amt == null) {
			return "null";
		}

		return String.format("%10.3f", amt);
	}

	public static String formatDate(Date date) {
		if (date == null) {
			return "null";
		}

		final DateFormat dfmt = new SimpleDateFormat("MM/dd/yy");
		return dfmt.format(date);
	}

	public static String formatDateLong(Date date) {
		if (date == null) {
			return "null";
		}

		final DateFormat dfmt = new SimpleDateFormat("MM/dd/yyyy");
		return dfmt.format(date);
	}

	public static String formatDateShort(Date date) {
		final DateFormat dfmt = new SimpleDateFormat("MM/dd");
		return dfmt.format(date);
	}

	public static String formatDateMonthYear(Date date) {
		final DateFormat dfmt = new SimpleDateFormat("MM/yyyy");
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

	public static void writeIfSet(PrintWriter pw, String tag, String value) {
		pw.println("" + tag + value);
	}

	public static void writeln(PrintWriter pw, String tag) {
		pw.println(tag);
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

	public static BigDecimal sumAmounts(List<GenericTxn> txns) {
		BigDecimal totaltx = BigDecimal.ZERO;
		for (final GenericTxn t : txns) {
			totaltx = totaltx.add(t.getAmount());
		}

		return totaltx;
	}

	public static BigDecimal sumCashAmounts(List<GenericTxn> txns) {
		BigDecimal totaltx = BigDecimal.ZERO;
		for (final GenericTxn t : txns) {
			totaltx = totaltx.add(t.getCashAmount());
		}

		return totaltx;
	}

	public static String getCheckNumString(GenericTxn t) {
		return (t instanceof NonInvestmentTxn) //
				? ((NonInvestmentTxn) t).chkNumber //
				: "";
	}

	public static void sortTransactionsByDate(List<GenericTxn> txns) {
		final Comparator<GenericTxn> cmptor = (t1, t2) -> {
			final int diff = t1.getDate().compareTo(t2.getDate());

			if (diff != 0) {
				return diff;
			}

			return t1.txid - t2.txid;
		};

		Collections.sort(txns, cmptor);
	}

	// Return the index of the first transaction on or after a given date
	// in a list of transactions sorted by date.
	public static int findFirstTransactionOnOrAfterDate( //
			List<? extends GenericTxn> txns, QDate d) {
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
			List<? extends GenericTxn> txns, QDate d) {
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
			List<? extends GenericTxn> txns, QDate d) {
		int loidx = 0;
		int hiidx = txns.size() - 1;
		final QDate loval = txns.get(loidx).getDate();
		final QDate hival = txns.get(hiidx).getDate();
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
			final QDate val = txns.get(idx).getDate();

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

	public static void listTransactions(List<GenericTxn> txns, int max) {
		System.out.println("Transaction list");

		for (int ii = Math.max(0, txns.size() - max); ii < txns.size(); ++ii) {
			final GenericTxn t = txns.get(ii);

			String cknum = "";
			if (t instanceof NonInvestmentTxn) {
				cknum = ((NonInvestmentTxn) t).chkNumber;
			}

			System.out.println(String.format("%s  %5s %s  %s", //
					t.getDate().toString(), //
					cknum, //
					Common.formatAmount(t.getAmount()), //
					Common.formatAmount(t.runningTotal)));
		}
	}

	private static final int SUBSET_LIMIT = 10;

	public static void findSubsetTotaling(List<GenericTxn> txns, List<GenericTxn> subset, BigDecimal diff) {
		// First try removing one transaction, then two, ...
		// Return a list of the fewest transactions totaling the desired amount
		// Limit how far back we go.
		final int lowlimit = Math.max(0, txns.size() - 50);

		for (int nn = 1; (nn <= txns.size()) && (nn < SUBSET_LIMIT); ++nn) {
			findSubsetTotaling(txns, subset, diff, nn, lowlimit, txns.size());

			if (!subset.isEmpty()) {
				return;
			}
		}
	}

	// Try combinations of nn transactions, indexes between min and max-1.
	// Return the first that adds up to tot.
	private static void findSubsetTotaling( //
			List<GenericTxn> txns, List<GenericTxn> subset, //
			BigDecimal tot, int nn, int min, int max) {

		if (nn > (max - min)) {
			return;
		}

		// Remove one transaction, starting with the most recent
		for (int ii = max - 1; ii >= min; --ii) {
			final GenericTxn t = txns.get(ii);
			final BigDecimal newtot = tot.subtract(t.getCashAmount());

			if ((nn == 1) && (newtot.signum() == 0)) {
				// We are looking for one transaction and found it
				subset.add(t);
				return;
			}

			if ((nn > 1) && (nn <= ii)) {
				// We need n-1 more transactions - we have already considered
				// combinations with transactions after index ii, so start
				// before that, looking for n-1 transactions adding up to the
				// adjusted total.
				findSubsetTotaling(txns, subset, newtot, nn - 1, min, ii);

				if (!subset.isEmpty()) {
					subset.add(t);
					return;
				}
			}
		}
	}
}
