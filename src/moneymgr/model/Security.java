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

		public String toString() {
			return String.format("Split(%s,%s)", //
					this.splitDate.toString(), this.splitRatio.toString());
		}

		public boolean matches(StockSplitInfo other) {
			return this.splitDate.equals(other.splitDate) //
					&& Common.isEffectivelyEqual(this.splitRatio, other.splitRatio);
		}
	}

	public final MoneyMgrModel model;
	public final int secid;

	/** Names the security is known by (first is default) */
	public final List<String> names;

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

	public Security(int secid, String symbol, String name, String type, String goal) {
		this.model = MoneyMgrModel.currModel;
		this.secid = (secid > 0) ? secid : this.model.nextSecurityId();
		this.symbol = (symbol != null) ? symbol : name;
		this.type = type;
		this.goal = (goal != null) ? goal : "";

		this.names = new ArrayList<>();
		this.names.add(name);
	}

	/** Constructor - quicken-style info */
	public Security(String symbol, String name, String type, String goal) {
		this(0, symbol, name, type, goal);
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

		for (Lot lot : lots) {
			addLot(lot);
		}
	}

	public void addLot(Lot lot) {
		int idx;

		for (idx = this.lots.size(); idx > 0; --idx) {
			Lot other = this.lots.get(idx - 1);
			int diff = lot.createDate.compareTo(other.createDate);

			if (diff == 0) {
				if (lot.lotid > other.lotid) {
					break;
				}
			} else if (diff > 0) {
				break;
			}
		}

		this.lots.add(idx, lot);
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

	public InvestmentTxn getLastTransaction() {
		return this.transactions.isEmpty() //
				? null
				: this.transactions.get(this.transactions.size() - 1);
	}

	/** Add a new transaction involving this security */
	public void addTransaction(InvestmentTxn txn) {
		InvestmentTxn lastt = getLastTransaction();
		if ((lastt != null) && (txn.getDate().compareTo(lastt.getDate()) >= 0)) {
			this.transactions.add(txn);
			return;
		}

		InvestmentTxn search_tx = new InvestmentTxn(0);
		search_tx.setDate(txn.getDate());
		int idx = Collections.binarySearch(this.transactions, search_tx);
		if (idx < 0) {
			idx = -idx - 1;
		}
		this.transactions.add(idx, txn);

		if ((txn.getPrice() != null) && //
				(txn.getPrice().compareTo(BigDecimal.ZERO) != 0)) {

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
			BigDecimal oldp = p.getPrice(); // p.getSplitAdjustedPrice();
			BigDecimal newp = newPrice.getPrice(); // newPrice.getSplitAdjustedPrice();

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
		return new QPrice(date, this.secid, BigDecimal.ZERO);
	}

	/**
	 * Find position in the price history nearest/prior to a given date.<br>
	 * Return exact match if possible.<br>
	 * Else return closest date before if possible.<br>
	 * Return first date if argument is before the start of the history.
	 */
	private int getPriceIndexForDate(QDate date) {
		int idx = Collections.binarySearch(this.prices, //
				new QPrice(date, 1, BigDecimal.ZERO), //
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

	public int hashCode() {
		return this.symbol.hashCode();
	}

	public boolean equals(Object obj) {
		return (obj instanceof Security) //
				&& this.symbol.equals(((Security) obj).symbol);
	}

	public String matches(Security other) {
		if (!this.symbol.equals(other.symbol) //
				|| !Common.safeEquals(this.goal, other.goal) //
				|| !this.type.equals(other.type) //
				|| !this.getName().equals(other.getName())) {
			return "info1";
		}

		for (String name : this.names) {
			if (!other.names.contains(name)) {
				return "name";
			}
		}

		if (this.splits.size() != other.splits.size()) {
			return "numsplits";
		}

		if (this.prices.size() != other.prices.size()) {
			return "numprices";
		}

		if (this.lots.size() != other.lots.size()) {
			return "numlots";
		}

		if (this.transactions.size() != other.transactions.size()) {
			return "numtxn";
		}

		for (int idx = 0; idx < this.transactions.size(); ++idx) {
			InvestmentTxn tx = this.transactions.get(idx);
			InvestmentTxn otx = other.transactions.get(idx);

			String res = tx.matches(otx);
			if (res != null) {
				return "txn:" + res;
			}
		}

		for (int idx = 0; idx < this.splits.size(); ++idx) {
			StockSplitInfo ssi = this.splits.get(idx);
			StockSplitInfo ossi = other.splits.get(idx);

			if (!ssi.matches(ossi)) {
				return "stocksplit";
			}
		}

		for (int idx = 0; idx < this.prices.size(); ++idx) {
			QPrice price = this.prices.get(idx);
			QPrice oprice = other.prices.get(idx);

			if (!price.matches(oprice)) {
				return "price";
			}
		}

		for (int idx = 0; idx < this.lots.size(); ++idx) {
			Lot lot = this.lots.get(idx);
			Lot olot = other.lots.get(idx);

			String res = lot.matches(olot);
			if (res != null) {
				return res;
			}
		}

		if (!this.transactions.isEmpty()) {
			InvestmentTxn tx1 = this.transactions.get(0);
			QDate lastDate = getLastTransaction().getDate();

			for (QDate date = tx1.getDate(); //
					date.compareTo(lastDate) <= 0; //
					date = date.addDays(1)) {

				QPrice p1 = getPriceForDate(date);
				QPrice p2 = other.getPriceForDate(date);

				if (!p1.equals(p2)) {
					return String.format("security %s %s price(%s vs %s)", //
							getName(), date.toString(), p1.toString(), p2.toString());
//					Common.formatAmount(getPriceForDate(date).getPrice()), //
//					Common.formatAmount(other.getPriceForDate(date).getPrice()));
				}

				BigDecimal r1 = getSplitRatioForDate(date);
				BigDecimal r2 = other.getSplitRatioForDate(date);

				if (!Common.isEffectivelyEqual(r1, r2)) {
					return String.format("security %s %s splitratio(%s vs %s)", //
							getName(), date.toString(), //
							Common.formatAmount(r1).trim(), //
							Common.formatAmount(r2).trim());
				}
			}
		}

		return null;
	}
}
