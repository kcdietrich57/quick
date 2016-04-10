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

	List<Price> prices = new ArrayList<Price>();

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

	public BigDecimal getPriceForDate(Date d) {
		if (this.prices.isEmpty()) {
			return BigDecimal.ZERO;
		}

		int loidx = 0;
		int hiidx = this.prices.size() - 1;
		final Date loval = this.prices.get(loidx).date;
		final Date hival = this.prices.get(hiidx).date;
		if (loval.compareTo(d) >= 0) {
			return this.prices.get(0).price;
		}
		if (hival.compareTo(d) <= 0) {
			return this.prices.get(hiidx).price;
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

		return this.prices.get(idx).price;
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
