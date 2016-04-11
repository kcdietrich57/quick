package app;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import qif.data.Common;
import qif.data.Price;

public class LoadSecurityHistory {

	public static void main(String[] args) {
		final String filenames[] = { //
				"/Users/greg/quotes/ifmx", // 0
				"fmagx", // 1
				"frsgx", // 2
				"kdhax", // 3
				"mittx", // 4
				"/Users/greg/quotes/mpvlx", // 5
				"/Users/greg/quotes/pcapx", // 6
				"peugx", // 7
				"uncmx", // 8
				"/Users/greg/quotes/zz" //
		};
		final File d = new File(filenames[8]);
		final File[] files = d.listFiles();

		final List<List<Price>> prices = new ArrayList<List<Price>>();

		for (final File f : files) {
			try {
				final List<Price> pp = loadQuoteFile(f);
				prices.add(pp);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}

		final List<List<Price>> mergedPrices = new ArrayList<List<Price>>();
		Date fd = getFirstDate(prices);
		final List<Price> extraPricesForDate = new ArrayList<Price>();

		while (fd != null) {
			final List<Price> pricesForDate = new ArrayList<Price>();
			pricesForDate.add(null);

			for (int ii = 0; ii < prices.size(); ++ii) {
				final List<Price> list = prices.get(ii);

				if ((list == null) || list.isEmpty()) {
					pricesForDate.add(null);
					continue;
				}

				final Price p = list.get(0);
				if (p.date.compareTo(fd) == 0) {
					list.remove(0);
					pricesForDate.add(p);

					while (!list.isEmpty()) {
						final Price pp = list.get(0);
						if (pp.date.compareTo(fd) != 0) {
							break;
						}

						if (pp.price.compareTo(p.price) != 0) {
							// System.out.println( //
							// "WARNING! Price mismatch for " + //
							// Common.getDateString(fd) + " " + //
							// p.price + " <> " + pp.price);
							extraPricesForDate.add(pp);
						}

						list.remove(0);
					}
				} else {
					pricesForDate.add(null);
				}
			}

			Price pcommon = null;
			boolean allmatch = true;

			for (int ii = 1; ii < pricesForDate.size(); ++ii) {
				final Price pp = pricesForDate.get(ii);

				if (pcommon == null) {
					pcommon = pp;
				} else if ((pp != null) && //
						pcommon.price.compareTo(pp.price) != 0) {
					allmatch = false;
					break;
				}
			}
			if (allmatch) {
				pricesForDate.set(0, pcommon);
				for (int ii = 1; ii < pricesForDate.size(); ++ii) {
					pricesForDate.set(ii, null);
				}
			}

			mergedPrices.add(pricesForDate);

			String s = Common.getDateString(fd);

			for (int ii = 1; ii < pricesForDate.size(); ++ii) {
				final Price p = pricesForDate.get(ii);
				if (p == null) {
					pricesForDate.remove(ii);
					--ii;
				} else {
					for (int jj = ii + 1; jj < pricesForDate.size(); ++jj) {
						final Price pp = pricesForDate.get(jj);

						if ((pp != null) && (p.price.compareTo(pp.price) == 0)) {
							pricesForDate.remove(jj);
							--jj;
						}
					}
				}
			}
			for (int ii = 0; ii < extraPricesForDate.size(); ++ii) {
				final Price p = extraPricesForDate.get(ii);
				for (final Price pp : pricesForDate) {
					if ((pp != null) && (p.price.compareTo(pp.price) == 0)) {
						extraPricesForDate.remove(ii);
						--ii;
					}
				}
			}

			for (final Price p : pricesForDate) {
				if (p != null) {
					s += String.format("  %10.3f", p.price);
				} else {
					s += "            ";
				}
			}
			for (final Price p : extraPricesForDate) {
				s += String.format(" *%10.3f", p.price);
			}
			extraPricesForDate.clear();

			System.out.println(s);

			fd = getFirstDate(prices);
		}

		for (int ii = 0; ii < prices.size() - 1; ++ii) {
			final List<Price> pp1 = prices.get(ii);
			final List<Price> pp2 = prices.get(ii + 1);
			final Date sdate = pp2.get(0).date;

			int jj = 0;
			while ((jj < pp1.size()) && //
					((pp1.get(jj) == null) || //
							(sdate.compareTo(pp1.get(jj).date) > 0))) {
				++jj;
				pp2.add(0, null);
			}
		}
	}

	static Date getFirstDate(List<List<Price>> prices) {
		Date ret = null;

		for (final List<Price> list : prices) {
			if (list.isEmpty()) {
				continue;
			}

			final Date d = list.get(0).date;

			if ((ret == null) || (ret.compareTo(list.get(0).date) > 0)) {
				ret = d;
			}
		}

		return ret;
	}

	public static List<Price> loadQuoteFile(File f) throws Exception {
		final List<Price> prices = new ArrayList<Price>();

		if (!f.getName().endsWith(".csv")) {
			return prices;
		}

		System.out.println("Reading file: " + f.getPath());

		final FileReader fr = new FileReader(f);
		final LineNumberReader rdr = new LineNumberReader(fr);

		boolean dateprice = false;
		boolean chlvd = false;
		boolean dohlcv = false;

		String line = rdr.readLine();

		while (line != null) {
			if (line.startsWith("date")) {
				chlvd = false;
				dohlcv = false;
				dateprice = true;
				line = rdr.readLine();
				continue;
			}

			if (line.startsWith("price")) {
				chlvd = false;
				dohlcv = false;
				dateprice = false;
				line = rdr.readLine();
				continue;
			}

			if (line.startsWith("chlvd")) {
				chlvd = true;
				dohlcv = false;
				dateprice = false;
				line = rdr.readLine();
				continue;
			}

			if (line.startsWith("dohlcv")) {
				chlvd = false;
				dohlcv = true;
				dateprice = false;
				line = rdr.readLine();
				continue;
			}

			final StringTokenizer toker = new StringTokenizer(line, ",");

			String pricestr;
			String datestr;

			if (chlvd) {
				pricestr = toker.nextToken();
				toker.nextToken();
				toker.nextToken();
				toker.nextToken();
				datestr = toker.nextToken();
			} else if (dohlcv) {
				datestr = toker.nextToken();
				toker.nextToken();
				toker.nextToken();
				toker.nextToken();
				pricestr = toker.nextToken();
			} else if (dateprice) {
				datestr = toker.nextToken();
				pricestr = toker.nextToken();
			} else {
				pricestr = toker.nextToken();
				datestr = toker.nextToken();
			}

			final Date date = Common.parseDate(datestr);
			final BigDecimal price = new BigDecimal(pricestr);

			final Price p = new Price();
			p.date = date;
			p.price = price;
			prices.add(p);

			// System.out.println(Common.getDateString(date) + " : " + price);
			line = rdr.readLine();
		}

		System.out.println();

		rdr.close();

		Collections.sort(prices, (o1, o2) -> o1.date.compareTo(o2.date));

		return prices;
	}
}
