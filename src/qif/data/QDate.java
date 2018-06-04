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

	public QDate(int y, int m, int d) {
		y = adjustYear(y);

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

	public int compareTo(QDate o) {
		return this.datevalue - o.datevalue;
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
}