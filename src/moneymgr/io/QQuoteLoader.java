package moneymgr.io;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import moneymgr.model.MoneyMgrModel;
import moneymgr.model.QPrice;
import moneymgr.model.Security;
import moneymgr.model.Security.StockSplitInfo;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/**
 * Load quotes from CSV files<br>
 * <br>
 * First field specifies content:<br>
 * chlvd/dohlcv: Close High Low Volume Date Open<br>
 * date price: switch to dp format<br>
 * price date: switch to pd format<br>
 * <br>
 * split adjusted: Whether quotes account for stock splits<br>
 * weekly: Quotes are weekly (not daily)<br>
 * <br>
 * split: split ratio<br>
 * <br>
 * otherwise: quote data<br>
 */
public class QQuoteLoader {
	public final MoneyMgrModel model;

	public QQuoteLoader(MoneyMgrModel model) {
		this.model = model;
	}

	public void loadQuoteFile(Security sec, File f) {
		if (!f.getName().endsWith(".csv")) {
			return;
		}

		List<QPrice> prices = sec.prices;
		List<StockSplitInfo> splits = sec.splits;

		assert prices.isEmpty() && splits.isEmpty();

		FileReader fr;
		String line;
		LineNumberReader rdr;

		boolean isSplitAdjusted = false;
		boolean isWeekly = false;
		boolean dateprice = false;
		boolean chlvd = false;
		boolean dohlcv = false;

		QDate splitDate = null;

		try {
			fr = new FileReader(f);

			rdr = new LineNumberReader(fr);

			line = rdr.readLine();

			while (line != null) {
				boolean isHeader = true;

				if (line.startsWith("split adjusted")) {
					isSplitAdjusted = true;
				} else if (line.startsWith("weekly")) {
					isWeekly = true;
				} else if (line.startsWith("date")) {
					chlvd = false;
					dohlcv = false;
					dateprice = true;
				} else if (line.startsWith("price")) {
					chlvd = false;
					dohlcv = false;
					dateprice = false;
				} else if (line.startsWith("chlvd")) {
					chlvd = true;
					dohlcv = false;
					dateprice = false;
				} else if (line.startsWith("dohlcv")) {
					chlvd = false;
					dohlcv = true;
					dateprice = false;
				} else if (line.startsWith("split")) {
					String[] ss = line.split(" ");
					int ssx = 1;

					String newshrStr = ss[ssx++];
					String oldshrStr = ss[ssx++];
					String dateStr = ss[ssx++];

					BigDecimal splitAdjust = new BigDecimal(newshrStr).divide(new BigDecimal(oldshrStr));
					splitDate = Common.parseQDate(dateStr);

					StockSplitInfo si = new StockSplitInfo(splitDate, splitAdjust);

					splits.add(si);
					Collections.sort(splits, (o1, o2) -> o1.splitDate.compareTo(o2.splitDate));
				} else {
					isHeader = false;
				}

				if (isHeader) {
					line = rdr.readLine();
					continue;
				}

				String[] ss = line.split(",");
				int ssx = 0;

				String pricestr;
				String datestr;

				// Extract date/price depending on line format
				if (chlvd) {
					pricestr = ss[ssx++];
					++ssx;
					++ssx;
					++ssx;
					datestr = ss[ssx++];
				} else if (dohlcv) {
					datestr = ss[ssx++];
					++ssx;
					++ssx;
					++ssx;
					pricestr = ss[ssx++];
				} else if (dateprice) {
					datestr = ss[ssx++];
					pricestr = ss[ssx++];
				} else {
					pricestr = ss[ssx++];
					datestr = ss[ssx++];
				}

				QDate date = Common.parseQDate(datestr);

				BigDecimal price = null;
				try {
					price = new BigDecimal(pricestr);
				} catch (final Exception e) {
					e.printStackTrace();
					Common.reportError("Invalid price in quote file");
				}

				if (isWeekly) {
					// NB unused: Suspicious - go to middle of the week?
					date = date.addDays(4);
				}

				BigDecimal splitRatio = sec.getSplitRatioForDate(date);

				BigDecimal saprice;

				if (isSplitAdjusted) {
					// Value if held until now (i.e. accounting for future splits)
					saprice = price;
					// Price as of the date of the quote
					price = price.multiply(splitRatio);
				} else {
					// NB unused: this is not normal
					saprice = price.divide(splitRatio);
				}

				QPrice qprice = new QPrice(this.model, date, sec.secid, price, saprice);

				sec.addPrice(qprice);

				line = rdr.readLine();
			}

			rdr.close();
		} catch (final Exception e) {
			e.printStackTrace();
			return;
		}

		Collections.sort(prices, (o1, o2) -> o1.date.compareTo(o2.date));
	}
}