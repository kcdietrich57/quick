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
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.QPrice;
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

	// If we appear to have hit the 500 api calls per hour limit, simply
	// disable quote downloads for this session.
	private static boolean disableDownload = false;

	public List<String> securitiesNotDownlaoded = new ArrayList<String>();
	public final MoneyMgrModel model;

	public QuoteDownloader(MoneyMgrModel model) {
		this.model = model;
	}

	public List<QPrice> loadPriceHistory(String symbol) {
		JSONObject quotes = loadQuoteHistory(symbol, FUNC_DAILY, true);

		return extractQuoteHistory(symbol, quotes);
	}

	public List<QPrice> loadQuotes(String symbol) {
		JSONObject quotes = loadQuoteHistory(symbol, FUNC_DAILY, false);

		return extractQuoteHistory(symbol, quotes);
	}

	/** Process JSON results to get quote data */
	private List<QPrice> extractQuoteHistory(String symbol, JSONObject quotes) {
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

			prices.add(new QPrice(this.model, Common.parseQDate(date), //
					this.model.findSecurity(symbol).secid, //
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
	private JSONObject loadQuoteHistory(String symbol, String function, boolean full) {
		File outdir = new File(QifDom.qifDir, "quotes");
		File outfile = new File(outdir, symbol + ".quote");

		String json = "No quotes";

		long now = System.currentTimeMillis();
		long mod = outfile.lastModified();
		long ONE_DAY = ((long) 1000) * 60 * 60 * 24;

		boolean refreshQuotes = false;
		boolean oldQuotes = (now - mod) > ONE_DAY;
		boolean retryAfterThrottle = false;

		// TODO disregard securities that I don't hold any more?
		String msg = String.format("Checking quote history for '%s'", symbol);

		for (;;) {
			if (!refreshQuotes && outfile.isFile() && outfile.canRead()) {
				msg += "... loading file";
				json = loadQuoteFile(outfile);
			} else if (disableDownload) {
				Common.reportInfo(msg + "... downloads disabled");
				return null;
			} else {
				msg += "... downloading quotes";
				json = downloadQuotes(symbol, outfile, function, full);
				oldQuotes = false;
			}

			try {
				if (json.startsWith("No quotes")) {
					// We don't expect to be able to download for this security
					Common.reportInfo(msg + "... download not available");
					return null;
				}

				if (oldQuotes) {
					msg += "... old, downloading latest";
					refreshQuotes = true;
					continue;
				}

				if (!json.contains("calls per minute")) {
					break;
				}

				if (retryAfterThrottle) {
					Common.reportInfo(msg + "... Hourly limit reached, disabling downloads");

					disableDownload = true;
					return null;
				}

				msg += "... limit reached, waiting 60s";
				refreshQuotes = true;
				retryAfterThrottle = true;

				try {
					Thread.sleep(60000);
				} catch (InterruptedException e1) {
				}

			} catch (Exception e) {
				Common.reportWarning(String.format( //
						"%s...\n  Invalid quote JSON: '%s'", msg, json));
				return null;
			}
		}

		Common.reportInfo(msg + "... success");

		return new JSONObject(json);
	}

	private String downloadQuotes(String symbol, File quoteFile, //
			String function, boolean full) {
		String charset = "UTF-8";

		URLConnection connection = null;
		InputStream response = null;

		String urlString = String.format(queryPattern, //
				DOMAIN, function, symbol, (full) ? FULL_HISTORY : "", API_KEY);

		String json = "No quotes";
		StringBuilder sb = new StringBuilder();

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
			PrintWriter writer = new PrintWriter(quoteFile);
			writer.print(json);
			writer.close();
		} catch (Exception e) {

		}

		return json;
	}

	private String loadQuoteFile(File quoteFile) {
		try {
			LineNumberReader rdr = new LineNumberReader(new FileReader(quoteFile));
			StringBuilder sb = new StringBuilder();

			for (;;) {
				String line = rdr.readLine();
				if (line == null) {
					rdr.close();
					break;
				}

				sb.append(line);
				sb.append('\n');
			}

			return sb.toString();

		} catch (Exception e) {
			Common.reportError("Error opening/reading file " + quoteFile.toString());
		}

		return null;
	}

	public static void main(String[] args) {
		QuoteDownloader quoteLoader = new QuoteDownloader(null);

		List<QPrice> prices = quoteLoader.loadQuotes("TSLA");
		System.out.println(prices.toString());
	}
}
