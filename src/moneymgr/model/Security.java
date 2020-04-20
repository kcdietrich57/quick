package moneymgr.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.QifDom;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/** Class representing a security, its price history, activity, and so on */
public class Security {
	/** Information about a split involving this security */
	public static class StockSplitInfo {
		/** Date of the split */
		public QDate splitDate;

		/** Multiplier applied to shares for the split (newsh = oldsh * ratio) */
		public BigDecimal splitRatio;

		public StockSplitInfo(QDate date, BigDecimal ratio) {
			this.splitDate = date;
			this.splitRatio = ratio;
		}

		public StockSplitInfo(InvestmentTxn tx) {
			this(tx.getDate(), tx.getSplitRatio());
		}

		public boolean equals(Object other) {
			if (!(other instanceof StockSplitInfo)) {
				return false;
			}

			return this.splitDate.equals(((StockSplitInfo) other).splitDate) && //
					Common.isEffectivelyEqual(this.splitRatio, ((StockSplitInfo) other).splitRatio);
		}
	}

	public final int secid;

	/** Names the security is known by (first is default) */
	public List<String> names;

	/** Ticker symbol of security */
	public final String symbol;

	/** Security type (stock, mutual fund, etc) */
	public final String type;

	/** Security goal (growth, income, etc) */
	public final String goal;

	/**
	 * Lots for all holdings/transactions for this security.<br>
	 * This constitutes our entire history for this security.
	 */
	private final List<Lot> lots = new ArrayList<>();

	/** All of our transactions involving this security */
	private final List<InvestmentTxn> transactions = new ArrayList<>();

	/** TODO make private - Price history for this security sorted by date */
	public final List<QPrice> prices = new ArrayList<>();

	/** TODO make private - Information about splits for this security */
	public final List<StockSplitInfo> splits = new ArrayList<>();

	/** Constructor - quicken-style info */
	public Security(String symbol, String name, String type, String goal) {
		this.secid = MoneyMgrModel.currModel.nextSecurityId();

		this.symbol = (symbol != null) ? symbol : name;

		this.names = new ArrayList<>();
		this.names.add(name);
		this.type = type;
		this.goal = goal;
	}

	public Security(String symbol, String name) {
		this(symbol, name, "", "");
	}

	public String getSymbol() {
		return this.symbol;
	}

	public String getName() {
		return (this.names.isEmpty()) ? "" : this.names.get(0);
	}

	public List<InvestmentTxn> getTransactions() {
		return Collections.unmodifiableList(this.transactions);
	}

	public List<Lot> getLots() {
		return Collections.unmodifiableList(this.lots);
	}

	/** Set/replace the set of lots for transactions on this security */
	public void setLots(List<Lot> lots) {
		this.lots.clear();
		this.lots.addAll(lots);
	}

	public static void fixSplits() {
		for (Security sec : MoneyMgrModel.currModel.getSecurities()) {
			sec.splits.clear();
			StockSplitInfo last = null;

			for (InvestmentTxn tx : sec.getTransactions()) {
				if (tx.getAction() == TxAction.STOCKSPLIT) {
					StockSplitInfo info = new StockSplitInfo(tx);
					if (!info.equals(last)) {
						sec.splits.add(info);
						last = info;
					}
				}
			}
		}
	}

	/** Add a new transaction involving this security */
	public void addTransaction(InvestmentTxn txn) {
		InvestmentTxn search_tx = new InvestmentTxn(0);
		search_tx.setDate(txn.getDate());
		int idx = Collections.binarySearch(this.transactions, search_tx);
		if (idx < 0) {
			idx = -idx - 1;
		}
		this.transactions.add(idx, txn);

		if ((txn.price != null) && //
				(txn.price.compareTo(BigDecimal.ZERO) != 0)) {

			// The price for a transaction doesn't replace the price in the
			// history. It is intra-day, and in the case of ESPP/options,
			// may be discounted.
			// N.B. add prices from txns after loading price history, if at all.
			// addPrice(new Price(txn.price, txn.getDate()), false);
		}
	}

	/**
	 * Add a price to the history of this security.<br>
	 * If the quote is new or we already have a matching quote, we return true.<br>
	 * If a different quote for that day exists, we replace it and return false.
	 */
	public static Map<String, Integer> dupQuotes = new HashMap<String, Integer>();

	public boolean addPrice(QPrice newPrice) {
		if ((newPrice == null) //
				// Price in options txns is zero
				|| Common.isEffectivelyZero(newPrice.getPrice())) {
			return true;
		}

		if (this.prices.isEmpty()) {
			this.prices.add(newPrice);
			return true;
		}

		int idx = getPriceIndexForDate(newPrice.date);
		if ((idx < 0) || (idx >= this.prices.size())) {
			this.prices.add(newPrice);
			return true;
		}

		QPrice p = this.prices.get(idx);
		int diff = newPrice.date.compareTo(p.date);

		if (diff == 0) {
			BigDecimal oldp = p.getSplitAdjustedPrice();
			BigDecimal newp = newPrice.getSplitAdjustedPrice();

			this.prices.set(idx, newPrice);

			if (!Common.isEffectivelyEqual(oldp, newp)) {
				Integer n = dupQuotes.get(this.symbol.toUpperCase());
				if (n == null) {
					n = new Integer(0);
				}
				dupQuotes.put(this.symbol.toUpperCase(), new Integer(n + 1));

				if (QifDom.verbose) {
					Common.reportWarning(String.format( //
							"Security price (%s) was replaced (%s)", //
							Common.formatAmount3(oldp).trim(), //
							Common.formatAmount3(newp).trim()));
				}

				return false;
			}
		} else if (diff > 0) {
			this.prices.add(idx + 1, newPrice);
		} else {
			this.prices.add(idx, newPrice);
		}

		return true;
	}

	/** Get Security price on a given date */
	public QPrice getPriceForDate(QDate date) {
		int idx = getPriceIndexForDate(date);

		if (idx >= 0) {
			QPrice ret = this.prices.get(idx);

			if (ret.date.compareTo(date) <= 0) {
				return ret;
			}
		}

		// We either have no price history, or the date is before the start
		return new QPrice(date, this.secid, BigDecimal.ZERO, null);
	}

	/**
	 * Find position in the price history nearest/prior to a given date.<br>
	 * Return exact match if possible.<br>
	 * Else return closest date before if possible.<br>
	 * Return first date if argument is before the start of the history.
	 */
	private int getPriceIndexForDate(QDate date) {
		int idx = Collections.binarySearch(this.prices, //
				new QPrice(date, 1, BigDecimal.ZERO, BigDecimal.ZERO), //
				new Comparator<QPrice>() {
					public int compare(QPrice p1, QPrice p2) {
						return p1.date.compareTo(p2.date);
					}
				});

		// Adjust position when we don't have an exact match
		if (idx < 0) {
			idx = -(idx + 1); // insertion point

			// Use the date prior to the insertion point, if not at beginning
			if (idx > 0) {
				--idx;
			}

			if (idx >= this.prices.size()) {
				idx = this.prices.size() - 1;
			}
		}

		return idx;
	}

	/**
	 * Return the cumulative split multiplier for this security from the start of
	 * the history up to a specific date.
	 */
	public BigDecimal getSplitRatioForDate(QDate d) {
		BigDecimal ret = BigDecimal.ONE;

		for (StockSplitInfo si : this.splits) {
			if (si.splitDate.compareTo(d) < 0) {
				break;
			}

			ret = ret.multiply(si.splitRatio);
		}

		return ret;
	}

	public String toString() {
		String s = "Security[" + this.secid + "]: '";

		boolean first = true;
		for (final String n : this.names) {
			if (!first) {
				s += ", ";
			} else {
				first = false;
			}

			s += n;
		}

		s += "' sym=" + this.symbol //
				+ " type=" + this.type //
				+ " numprices=" + this.prices.size() //
				+ "\n";

		return s;
	}
}
