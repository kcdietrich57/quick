package qif.data;

import java.util.Calendar;
import java.util.Date;

public class QDate implements Comparable<QDate> {
	public static QDate today() {
		return new QDate(new Date());
	}

	private final int datevalue;
	private final String datestring;
	public final String shortString;
	public final String longString;
	public final String monthYearString;

	public QDate(long time) {
		this(new Date(time));
	}

	private static final int MONTH_DAYS[] = { //
			31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 //
	};

	public static QDate getDateForEndOfMonth(int year, int month) {
		return (month == 2) //
				? new QDate(year, 3, 1).addDays(-1) //
				: new QDate(year, month, MONTH_DAYS[(month + 11) % 12]);
	}

	public QDate getDateNearestTo(int day) {
		return getDateNearestTo(day, 0);
	}

	public QDate getDateNearestTo(int day, int recurse) {
		int diff = getDay() - day;

		if (diff == 0) {
			return this;
		}

		int adjust = 0;

		if (diff > 15) {
			adjust = 30 - diff;
			diff = -1;
		} else if (diff > -15) {
			adjust = -diff;
		} else {
			adjust = -(30 + diff);
			diff = 1;
		}

		// FIXME could have off by one errors here?
		if (adjust == 0) {
			adjust = (diff < 0) ? 1 : -1;
			return this;
		}

		if (recurse > 5) {
			// System.out.println("recursing: " + this.longString + " : " + day);
			return this;
		}

		QDate d2 = addDays(adjust).getDateNearestTo(day, recurse + 1);
		return d2;
	}

	public QDate(int y, int m, int d) {
		y = adjustYear(y);

		if (m == 2 && d > 28) {
			QDate qd = new QDate(y, m, 28).addDays(d - 28);

			y = qd.getYear();
			m = qd.getMonth();
			d = qd.getDay();
		}

		this.datevalue = y * 10000 + m * 100 + d;

		this.datestring = String.format("%d/%d/%02d", m, d, y % 100);
		this.longString = String.format("%02d/%02d/%04d", m, d, y);
		this.shortString = String.format("%02d/%02d", m, d);
		this.monthYearString = String.format("%02d/%04d", m, y);
	}

	public QDate(Date dt) {
		Calendar cal = Calendar.getInstance();
		cal.setTime((dt != null) ? dt : new Date());

		int y = cal.get(Calendar.YEAR);
		int m = cal.get(Calendar.MONTH) + 1;
		int d = cal.get(Calendar.DAY_OF_MONTH);

		this.datevalue = y * 10000 + m * 100 + d;

		this.datestring = String.format("%d/%d/%02d", m, d, y % 100);
		this.longString = String.format("%02d/%02d/%04d", m, d, y);
		this.shortString = String.format("%02d/%02d", m, d);
		this.monthYearString = String.format("%02d/%04d", m, y);
	}

	public int adjustYear(int y) {
		if (y < 30) {
			return 2000 + y;
		}

		if (y < 100) {
			return 1900 + y;
		}

		return y;
	}

	public int getYear() {
		return this.datevalue / 10000;
	}

	public int getMonth() {
		return (this.datevalue / 100) % 100;
	}

	public int getDay() {
		return this.datevalue % 100;
	}

	public QDate addDays(int days) {
		Calendar cal = Calendar.getInstance();
		Date d = toDate();
		cal.setTime(d);
		cal.add(Calendar.DAY_OF_MONTH, days);
		d = cal.getTime();

		return new QDate(d);
	}

	public boolean equals(Object obj) {
		return (obj instanceof QDate) ? this.datevalue == ((QDate) obj).datevalue : false;
	}

	public int compareTo(QDate o) {
		return this.datevalue - o.datevalue;
	}

	public int subtract(QDate o) {
		int diff = this.datevalue - o.datevalue;
		if (Math.abs(diff) <= 31) {
			return diff;
		}

		Calendar c1 = Calendar.getInstance();
		c1.setTime(toDate());
		Calendar c2 = Calendar.getInstance();
		c2.setTime(o.toDate());

		long msdiff = c1.getTimeInMillis() - c2.getTimeInMillis();

		return Common.msToDays(msdiff);
	}

	public Date toDate() {
		Calendar cal = Calendar.getInstance();
		cal.set(this.datevalue / 10000, //
				(this.datevalue / 100) % 100 - 1, //
				this.datevalue % 100, //
				0, 0, 1);

		return cal.getTime();
	}

	public String toString() {
		return this.datestring;
	}

	public static void main(String[] args) {
		QDate estimate = new QDate(2016, 3, 4);

		QDate exact = estimate.getDateNearestTo(29);
		System.out.println(exact.toString());
	}
}