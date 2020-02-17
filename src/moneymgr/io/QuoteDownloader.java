package moneymgr.io;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import org.json.JSONObject;

import app.QifDom;
import moneymgr.model.QPrice;
import moneymgr.model.Security;
import moneymgr.util.Common;

/**
 * Download security history from online service.<br>
 * Use information cached in files instead if available.
 */
public class QuoteDownloader {
	private static final String queryPattern = "%s/query?function=%s&symbol=%s%s&apikey=%s";

	private static final String DOMAIN = "https://www.alphavantage.co";
	private static final String FULL_HISTORY = "&outputsize=full";
	private static final String FUNC_DAILY = "TIME_SERIES_DAILY_ADJUSTED";
	private static final String NAME_DAILY = "Time Series (Daily)";
	private static final String API_KEY = "O7JYIMJXJOWQB9BY";

	static List<String> securitiesNotDownlaoded = new ArrayList<String>();

	public static List<QPrice> loadPriceHistory(String symbol) {
		JSONObject quotes = loadQuoteHistory(symbol, FUNC_DAILY, true);

		return extractQuoteHistory(symbol, quotes);
	}

	public static List<QPrice> loadQuotes(String symbol) {
		JSONObject quotes = loadQuoteHistory(symbol, FUNC_DAILY, false);

		return extractQuoteHistory(symbol, quotes);
	}

	/** Process JSON results to get quote data */
	private static List<QPrice> extractQuoteHistory(String symbol, JSONObject quotes) {
		if (quotes == null) {
			return null;
		}

		JSONObject quoteObj = null;

		try {
			quoteObj = quotes.getJSONObject(NAME_DAILY);
		} catch (Exception e) {
			if (QifDom.verbose) {
				Common.reportWarning("Couldn't get quotes for " + symbol);
			}

			securitiesNotDownlaoded.add(symbol);

			return null;
		}

		List<String> dates = new ArrayList<>(quoteObj.keySet());
		Collections.sort(dates);

		List<QPrice> prices = new ArrayList<QPrice>();
		String closePriceKey = null;
		String splitPriceKey = null;

		for (String date : dates) {
			JSONObject quote = quoteObj.getJSONObject(date);

			if (closePriceKey == null) {
				for (Iterator<String> kiter = quote.keys(); kiter.hasNext();) {
					String k = kiter.next();
					if (k.endsWith(". close")) {
						closePriceKey = k;
					} else if (k.endsWith(". adjusted close")) {
						splitPriceKey = k;
					}
				}
			}

			BigDecimal closingPrice = quote.getBigDecimal(closePriceKey);
			BigDecimal splitPrice = quote.getBigDecimal(splitPriceKey);

			prices.add(new QPrice(Common.parseQDate(date), //
					Security.findSecurity(symbol).secid, //
					closingPrice, splitPrice));
		}

		return prices;
	}

	/**
	 * Load quotes for a security
	 * 
	 * @param symbol   Ticker symbol
	 * @param function Load function (e.g. "TIME_SERIES_DAILY_ADJUSTED")
	 * @param full     True - full history, False - recent prices
	 * @return JSON results
	 */
	private static JSONObject loadQuoteHistory(String symbol, String function, boolean full) {
		StringBuilder sb = new StringBuilder();
		String json = "No quotes";
		File outdir = new File(QifDom.qifDir, "quotes");
		File outfile = new File(outdir, symbol + ".quote");

		// Read the file if present
		if (outfile.isFile() && outfile.canRead()) {
			try {
				LineNumberReader rdr = new LineNumberReader(new FileReader(outfile));
				for (;;) {
					String line = rdr.readLine();
					if (line == null) {
						rdr.close();
						break;
					}

					sb.append(line);
					sb.append('\n');
				}

				json = sb.toString();
			} catch (Exception e) {
				Common.reportError("Error opening/reading file " + outfile.toString());
			}
		} else {
			// Download quotes
			String charset = "UTF-8";

			URLConnection connection = null;
			InputStream response = null;

			String urlString = String.format(queryPattern, //
					DOMAIN, function, symbol, (full) ? FULL_HISTORY : "", API_KEY);

			try {
				connection = new URL(urlString).openConnection();
				connection.setRequestProperty("Accept-Charset", charset);
				response = connection.getInputStream();

				try (Scanner scanner = new Scanner(response)) {
					String line = scanner.useDelimiter("\\A").next();

					sb.append(line);
				}

				json = sb.toString();
			} catch (Exception e) {
				// e.printStackTrace();
			} finally {
				try {
					if (response != null) {
						response.close();
					}
				} catch (Exception e) {
				}
			}

			try {
				PrintWriter writer = new PrintWriter(outfile);
				writer.print(json);
				writer.close();
			} catch (Exception e) {

			}
		}

		try {
			if (!json.startsWith("No quotes")) {
				return new JSONObject(json);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static void main(String[] args) {
		List<QPrice> prices = loadQuotes("TSLA");
		System.out.println(prices.toString());
	}
}
