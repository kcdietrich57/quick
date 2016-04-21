package app;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import qif.data.Common;
import qif.data.Price;
import qif.data.QifDomReader;
import qif.data.Security;

public class LoadSecurityHistory {

	public static void main(String[] args) {
		final String filenames[] = { //
				"/Users/greg/quotes/ifmx", // 0
				"fmagx", // 1
				"frsgx", // 2
				"kdhax", // 3
				"mittx", // 4
				"/mpvlx", // 5
				"pcapx", // 6
				"peugx", // 7
				"uncmx", // 8
				"/Users/greg/quotes/zz" //
		};
		final File d = new File(filenames[0]);
		final File[] files = d.listFiles();

		final List<Security> securities = new ArrayList<Security>();

		for (final File f : files) {
			try {
				final Security sec = new Security(f.getName());
				securities.add(sec);
				QifDomReader.loadQuoteFile(sec, f);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}

		final List<List<Price>> mergedPrices = new ArrayList<List<Price>>();
		Date fd = getFirstDate(securities);
		final List<Price> extraPricesForDate = new ArrayList<Price>();

		while (fd != null) {
			final List<Price> pricesForDate = new ArrayList<Price>();
			pricesForDate.add(null);

			for (int ii = 0; ii < securities.size(); ++ii) {
				final List<Price> list = securities.get(ii).prices;

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

			fd = getFirstDate(securities);
		}

		System.out.println("NumPrices = " + mergedPrices.size());
	}

	static Date getFirstDate(List<Security> securities) {
		Date ret = null;

		for (final Security sec : securities) {
			final List<Price> list = sec.prices;
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
}
