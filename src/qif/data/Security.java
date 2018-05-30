package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Security {
	private static final Map<String, Security> securities = new HashMap<String, Security>();

	private static int nextSecurityID = 1;

	public static void addSecurity(Security sec) {
		Security existingName = findSecurityByName(sec.getName());
		if (existingName != null) {
			Common.reportWarning("Adding duplicate security name");
		}

		if (sec.symbol == null) {
			sec.symbol = sec.getName();
		}

		Security existingSymbol = (sec.symbol != null) ? securities.get(sec.symbol) : null;
		if (existingSymbol != null) {
			Common.reportWarning("Adding duplicate security symbol");
		}

		if (sec.secid == 0) {
			sec.secid = nextSecurityID++;
		}

		securities.put(sec.symbol, sec);
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
	public List<String> names;
	public String symbol;
	public String type;
	public String goal;

	public final List<InvestmentTxn> transactions = new ArrayList<InvestmentTxn>();
	public final List<QPrice> prices = new ArrayList<QPrice>();

	public static class SplitInfo {
		public Date splitDate;
		public BigDecimal splitRatio;
	}

	public List<SplitInfo> splits = new ArrayList<SplitInfo>();

	public Security(String symbol) {
		this.secid = 0;
		this.symbol = symbol;

		this.names = new ArrayList<String>();
		this.type = "";
		this.goal = "";
	}

	public Object getSymbol() {
		return (this.symbol != null) ? this.symbol : getName();
	}

	public String getName() {
		if (this.names.isEmpty()) {
			return "";
		} else {
			return this.names.get(0);
		}
	}

	public void addTransaction(InvestmentTxn txn) {
		this.transactions.add(txn);

		if ((txn.price != null) && //
				(txn.price.compareTo(BigDecimal.ZERO) != 0)) {

			// The price for a transaction doesn't replace the price in the
			// history. It is intra-day, and in the case of ESPP/options,
			// may be discounted.
			// TODO add prices from transactions after loading price history,
			// if at all.
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
					// TODO warn if different?
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

	private void sortPrices() {
		Collections.sort(this.prices, (o1, o2) -> o1.date.compareTo(o2.date));
	}

	public QPrice getPriceForDate(Date d) {
		final int idx = getPriceIndexForDate(d);

		if (idx < 0) {
			return QPrice.ZERO;
		}

		final QPrice p = this.prices.get(idx);
		return p;
	}

	private int getPriceIndexForDate(Date d) {
		if (this.prices.isEmpty()) {
			return -1;
		}

		int loidx = 0;
		int hiidx = this.prices.size() - 1;
		final Date loval = this.prices.get(loidx).date;
		final Date hival = this.prices.get(hiidx).date;
		if (loval.compareTo(d) >= 0) {
			return loidx;
		}
		if (hival.compareTo(d) <= 0) {
			return hiidx;
		}

		int idx = loidx;
		while (loidx < hiidx) {
			final int mididx = (loidx + hiidx) / 2;
			if (mididx <= loidx || mididx >= hiidx) {
				idx = mididx;
				break;
			}
			final Date val = this.prices.get(mididx).date;

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

	public BigDecimal getSplitRatioForDate(Date d) {
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
