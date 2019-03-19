package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Security {
	/** Map symbol to security */
	private static final Map<String, Security> securities = new HashMap<String, Security>();

	/** Securities indexed by ID */
	private static final List<Security> securitiesByID = new ArrayList<Security>();

	public static Collection<Security> getSecurities() {
		return Collections.unmodifiableCollection(securities.values());
	}

	public static void addSecurity(Security sec) {
		Security existingByName = findSecurityByName(sec.getName());
		if (existingByName != null) {
			Common.reportError("Adding duplicate security name '" + sec.getName() + "'");
		}

		Security existingBySymbol = (sec.symbol != null) ? securities.get(sec.symbol) : null;
		if (existingBySymbol != null) {
			Common.reportError("Adding duplicate security symbol '" + sec.symbol + "'");
		}

		if (sec.secid != securities.size() + 1) {
			Common.reportError("Bad security id '" + sec.secid + "'" //
					+ " should be " + (securities.size() + 1));
		}

		while (securitiesByID.size() <= sec.secid) {
			securitiesByID.add(null);
		}

		securitiesByID.set(sec.secid, sec);
		securities.put(sec.symbol, sec);
	}

	public static Security getSecurity(int secid) {
		return securitiesByID.get(secid);
	}

	public static Security findSecurity(String nameOrSymbol) {
		final Security s = findSecurityBySymbol(nameOrSymbol);

		return (s != null) ? s : findSecurityByName(nameOrSymbol);
	}

	public static Security findSecurityByName(String name) {
		for (Security sec : securities.values()) {
			if (sec != null && sec.names.contains(name)) {
				return sec;
			}
		}

		return null;
	}

	public static Security findSecurityBySymbol(String sym) {
		return securities.get(sym);
	}

	/** Information about a split involving this security */
	public static class SplitInfo {
		/** Date of the split */
		public QDate splitDate;

		/** Multiplier applied to shares for the split (newsh = oldsh * ratio) */
		public BigDecimal splitRatio;
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

	/** Lots for all holdings/transactions for this security */
	private final List<Lot> lots = new ArrayList<Lot>();

	/** Transactions involving this security */
	public final List<InvestmentTxn> transactions = new ArrayList<InvestmentTxn>();

	/** Price history for this security */
	public final List<QPrice> prices = new ArrayList<QPrice>();

	/** Information about splits for this security */
	public List<SplitInfo> splits = new ArrayList<SplitInfo>();

	public Security(String symbol, String name, String type, String goal) {
		this.secid = securities.size() + 1;

		this.symbol = (symbol != null) ? symbol : name;

		this.names = new ArrayList<String>();
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

	public List<Lot> getLots() {
		return Collections.unmodifiableList(this.lots);
	}

	/** Set/replace the set of lots for transactions on this security */
	public void setLots(List<Lot> lots) {
		this.lots.clear();
		this.lots.addAll(lots);
	}

	public void addTransaction(InvestmentTxn txn) {
		// TODO should the list be sorted?
		this.transactions.add(txn);

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
	public boolean addPrice(QPrice newPrice) {
		if (newPrice == null) {
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

			if (!oldp.equals(newp)) {
				if (QifDom.verbose) {
					Common.reportWarning(String.format( //
							"Security price (%s) was replaced (%s)", //
							Common.formatAmount3(oldp).trim(), //
							Common.formatAmount3(newp).trim()));
				}

				this.prices.set(idx, newPrice);
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
	 * Return closest date before if possible.<br>
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

		for (SplitInfo si : this.splits) {
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
