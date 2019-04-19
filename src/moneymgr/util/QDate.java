package moneymgr.util;

import java.util.Calendar;
import java.util.Date;

/** Efficient date wrapper supporting quick comparison and display */
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

	/** Days in months (with February complication of course) */
	private static final int MONTH_DAYS[] = { //
			31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 //
	};

	/** Return the correct date for the last day in a given year/month */
	public static QDate getDateForEndOfMonth(int year, int month) {
		// TODO can I just do Year/Month+1/1 minus one day?
		return (month == 2) //
				? new QDate(year, 3, 1).addDays(-1) //
				: new QDate(year, month, MONTH_DAYS[(month + 11) % 12]);
	}

	/** Construct a date y/m/d */
	public QDate(int y, int m, int d) {
		while (m > 12) {
			m -= 12;
			++y;
		}
		// TODO we could have trouble with (y,2,29), but should be avoiding that

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

	/** Construct from a Date object */
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

	public int getYear() {
		return this.datevalue / 10000;
	}

	public int getMonth() {
		return (this.datevalue / 100) % 100;
	}

	public int getDay() {
		return this.datevalue % 100;
	}

	/** Return the last day of the month for this date */
	public QDate getLastDayOfMonth() {
		return new QDate(getYear(), getMonth() + 1, 1).addDays(-1);
	}

	/** Calculate the date (given day of month) nearest to this date */
	public QDate getDateNearestTo(int day) {
		if (day == getDay()) {
			return this;
		}

		// e.g. For 2/5 and 24 (24-5 > 15) 2/24 is nearer to 3/5 than 3/24
		if (day - getDay() > 15) {
			QDate lastMonth = new QDate(getYear(), getMonth(), 1).addDays(-1);

			return lastMonth.getDateNearestTo(day);
		}

		// e.g. For 2/24 and 5 (24-5 > 15) 3/5 is nearer to 2/24 than 2/5
		if (getDay() - day > 15) {
			QDate nextMonth = getLastDayOfMonth().addDays(1);

			return nextMonth.getDateNearestTo(day);
		}

		// current/desired day are near to each other - usually the same month
		// Check for day near end of month
		if (day < MONTH_DAYS[getMonth() - 1]) {
			return new QDate(getYear(), getMonth(), day);
		}

		return getLastDayOfMonth();
	}

	/** Convert two-digit year to four digit year */
	public int adjustYear(int y) {
		if (y < 30) {
			return 2000 + y;
		}

		if (y < 100) {
			return 1900 + y;
		}

		return y;
	}

	/** Get a date a given number of days away from this date */
	public QDate addDays(int days) {
		Calendar cal = Calendar.getInstance();
		Date d = toDate();
		cal.setTime(d);
		cal.add(Calendar.DAY_OF_MONTH, days);
		d = cal.getTime();

		return new QDate(d);
	}

	/** Get a date a given number of months away from this date */
	public QDate addMonths(int months) {
		int year = getYear();
		int month = getMonth();

		while (months >= 12) {
			++year;
			months -= 12;
		}
		while (-months >= 12) {
			--year;
			months += 12;
		}

		if (months > 0) {
			if (month + months <= 12) {
				month += months;
			} else {
				++year;
				month = (month + months) - 12;
			}

			months = 0;
		}
		if (months < 0) {
			if (month > -months) {
				month += months;
			} else {
				--year;
				month = (month + months) + 12;
			}

			months = 0;
		}

		return QDate.getDateForEndOfMonth(year, month);
	}

	public boolean equals(Object obj) {
		return (obj instanceof QDate) //
				&& this.datevalue == ((QDate) obj).datevalue;
	}

	public int compareTo(QDate o) {
		return this.datevalue - o.datevalue;
	}

	/** Return the number of days separating this date from another */
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

	/** Convert this date to a Java date object */
	public Date toDate() {
		Calendar cal = Calendar.getInstance();
		cal.set(getYear(), getMonth() - 1, getDay(), 0, 0, 1);

		return cal.getTime();
	}

	public String toString() {
		return this.datestring;
	}

	/** Unit test */
	public static void main(String[] args) {
		for (int month = 1; month <= 12; ++month) {
			QDate estimate;
			QDate exact;

			estimate = new QDate(2016, month, 3);
			exact = estimate.getDateNearestTo(31);
			System.out.println(estimate.toString() + " - " + exact.toString());

			estimate = new QDate(2016, month, 27);
			exact = estimate.getDateNearestTo(1);
			System.out.println("  " + estimate.toString() + " - " + exact.toString());
		}
	}
}