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

	private static int nextSecurityID = 1;

	public static Collection<Security> getSecurities() {
		return Collections.unmodifiableCollection(securities.values());
	}

	public static void addSecurity(Security sec) {
		Security existingByName = findSecurityByName(sec.getName());
		if (existingByName != null) {
			Common.reportWarning("Adding duplicate security name");
		}

		if (sec.symbol == null) {
			sec.symbol = sec.getName();
		}

		Security existingBySymbol = (sec.symbol != null) ? securities.get(sec.symbol) : null;
		if (existingBySymbol != null) {
			Common.reportWarning("Adding duplicate security symbol");
		}

		if (sec.secid == 0) {
			sec.secid = nextSecurityID++;
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

	public int secid;
	/** Names the security is known by (first is default) */
	public List<String> names;
	/** Ticker symbol of security */
	public String symbol;

	public String type;
	public String goal;

	/** Lots for all holdings/transactions for this security */
	private final List<Lot> lots = new ArrayList<Lot>();

	/** Transactions involving this security */
	public final List<InvestmentTxn> transactions = new ArrayList<InvestmentTxn>();

	/** Price history for this security */
	public final List<QPrice> prices = new ArrayList<QPrice>();

	/** Information about a split involving this security */
	public static class SplitInfo {
		public QDate splitDate;
		/** Multiplier applied to shares for the split (newsh = oldsh * ratio) */
		public BigDecimal splitRatio;
	}

	/** Information about splits for this security */
	public List<SplitInfo> splits = new ArrayList<SplitInfo>();

	public Security(String symbol) {
		this.secid = 0;
		this.symbol = symbol;

		this.names = new ArrayList<String>();
		this.type = "";
		this.goal = "";
	}

	public List<Lot> getLots() {
		return Collections.unmodifiableList(this.lots);
	}

	public void setLots(List<Lot> lots) {
		this.lots.clear();
		this.lots.addAll(lots);
	}

	public String getSymbol() {
		return (this.symbol != null) ? this.symbol : getName();
	}

	public String getName() {
		return (this.names.isEmpty()) ? "" : this.names.get(0);
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

	public void addPrice(QPrice newPrice, boolean replace) {
		if (newPrice == null) {
			return;
		}

		final int idx = getPriceIndexForDate(newPrice.date);

		if (idx >= 0) {
			final QPrice p = this.prices.get(idx);
			final int diff = newPrice.date.compareTo(p.date);

			if (diff == 0) {
				if (replace) {
					this.prices.set(idx, newPrice);
				} else {
					if (!p.getSplitAdjustedPrice().equals(newPrice.getSplitAdjustedPrice()) //
							&& QifDom.verbose) {
						Common.reportWarning("Security price mismatch");
					}
				}
			} else if (diff < 0) {
				this.prices.add(idx, newPrice);
			} else {
				this.prices.add(idx + 1, newPrice);
			}
		} else {
			this.prices.add(newPrice);
		}
	}

	// private void sortPrices() {
	// Collections.sort(this.prices, (o1, o2) -> o1.date.compareTo(o2.date));
	// }

	public QPrice getPriceForDate(QDate d) {
		int idx = getPriceIndexForDate(d);

		if (idx < 0) {
			return new QPrice(d, this.secid, BigDecimal.ZERO, null);
		}

		QPrice p = this.prices.get(idx);
		return p;
	}

	private int getPriceIndexForDate(QDate d) {
		// TODO use binary search
		int idx2 = Collections.binarySearch(this.prices, //
				new QPrice(d, 1, BigDecimal.ZERO, BigDecimal.ZERO), //
				new Comparator<QPrice>() {
					public int compare(QPrice p1, QPrice p2) {
						int diff = p1.date.compareTo(p2.date);
						return diff;
					}
				});

		if (this.prices.isEmpty()) {
			return -1;
		}

		int loidx = 0;
		int hiidx = this.prices.size() - 1;
		QDate loval = this.prices.get(loidx).date;
		QDate hival = this.prices.get(hiidx).date;

		if (loval.compareTo(d) >= 0) {
			return loidx;
		}
		if (hival.compareTo(d) <= 0) {
			return hiidx;
		}

		int idx = loidx;
		while (loidx < hiidx) {
			int mididx = (loidx + hiidx) / 2;
			if (mididx <= loidx || mididx >= hiidx) {
				idx = mididx;
				break;
			}

			QDate val = this.prices.get(mididx).date;

			if (val.compareTo(d) < 0) {
				loidx = mididx;
			} else if (val.compareTo(d) > 0) {
				hiidx = mididx;
			} else {
				idx = mididx;
				break;
			}
		}

		return idx;
	}

	public BigDecimal getSplitRatioForDate(QDate d) {
		BigDecimal ret = BigDecimal.ONE;

		for (int ii = this.splits.size() - 1; ii >= 0; --ii) {
			final SplitInfo si = this.splits.get(ii);
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
};
