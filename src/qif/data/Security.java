package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Security {
	public short id;
	public String name;
	public String symbol;
	public String type;
	public String goal;

	public List<Price> prices = new ArrayList<Price>();

	static class SplitInfo {
		Date splitDate;
		BigDecimal splitRatio;
	}

	List<SplitInfo> splits = new ArrayList<SplitInfo>();

	public Security() {
		this.id = 0;

		this.name = "";
		this.symbol = "";
		this.type = "";
		this.goal = "";
	}

	public Security(Security other) {
		this.id = other.id;
		this.name = other.name;
		this.symbol = other.symbol;
		this.type = other.type;
		this.goal = other.goal;
	}

	public void addPrice(Price price) {
		this.prices.add(price);
	}

	public Price getPriceForDate(Date d) {
		if (this.prices.isEmpty()) {
			return Price.ZERO;
		}

		int loidx = 0;
		int hiidx = this.prices.size() - 1;
		final Date loval = this.prices.get(loidx).date;
		final Date hival = this.prices.get(hiidx).date;
		if (loval.compareTo(d) >= 0) {
			return this.prices.get(0);
		}
		if (hival.compareTo(d) <= 0) {
			return this.prices.get(hiidx);
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

		return this.prices.get(idx);
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
		final String s = "Security[" + this.id + "]: " + this.name //
				+ " sym=" + this.symbol //
				+ " type=" + this.type //
				+ " numprices=" + this.prices.size() //
				+ "\n";

		return s;
	}
};
