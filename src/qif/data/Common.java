
package qif.data;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Common {
	public static void reportWarning(String s) {
		System.out.println("**** Warning!" + s);
	}

	public static void reportError(String s) {
		throw new RuntimeException(s);
	}

	public static boolean GetBoolean(String value) {
		return (value.length() > 0) && Boolean.parseBoolean(value);
	}

	public static String GetRealDateString(String qifDateString) {
		int i = qifDateString.indexOf("'");
		if (i != -1) {
			qifDateString = qifDateString.substring(0, i) + "/" + qifDateString.substring(i + 1);
		}

		return qifDateString.replace(" ", "0");
	}

	public static BigDecimal getDecimal(String value) {
		StringBuilder sb = new StringBuilder(value);
		for (;;) {
			int comma = sb.indexOf(",");
			if (comma < 0) {
				break;
			}

			sb.deleteCharAt(comma);
		}

		return new BigDecimal(sb.toString());
	}

	public static Date GetDate(String value) {
		try {
			DateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
			String s = GetRealDateString(value);

			Date d = dateFormat.parse(s);
			return d;
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static String getDateString(Date date) {
		DateFormat dfmt = new SimpleDateFormat("MM/dd/yyyy");
		return dfmt.format(date);
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
};
