package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class StockOption {
	// GRANT Name Date Qty Price VestPeriod NumVests lifeMonths
	// VEST Name Date VestNum
	// SPLIT Name Date NewShr OldShr [newQty/newPrice]
	// EXERCISE DName ate Qty Price
	// CANCEL Name Date
	// EXPIRE Name Date

	public static List<StockOption> options = new ArrayList<StockOption>();

	// GRANT Name Date Qty Price VestPeriod NumVests lifeMonths
	public static StockOption grant( //
			String name, //
			QDate date, //
			int acctid, //
			int secid, //
			BigDecimal shares, //
			BigDecimal price, //
			int vestPeriod, //
			int vestCount, //
			int lifetimeMonths) {
		StockOption opt = new StockOption(name, date, acctid, secid, //
				shares, price, vestPeriod, vestCount, lifetimeMonths);

		options.add(opt);

		return opt;
	}

	// VEST Name Date VestNum
	public static StockOption vest(String name, QDate date, int vestNumber) {
		StockOption src = getOpenOption(name);
		StockOption dst = new StockOption(src, date, vestNumber);

		assert getOpenOption(name) == null;

		options.add(dst);

		return dst;
	}

	// SPLIT Name Date NewShr OldShr [newQty/newPrice]
	public static StockOption split(String name, QDate date, int newshr, int oldshr) {
		StockOption src = getOpenOption(name);
		StockOption dst = new StockOption(src, date, newshr, oldshr);

		assert getOpenOption(name) == null;

		options.add(dst);

		return dst;
	}

	// EXPIRE Name Date
	public static StockOption expire(String name, QDate date) {
		StockOption src = getOpenOption(name);
		StockOption dst = null;

		if (src != null) {
			dst = new StockOption(src, date);
			options.add(dst);

			assert getOpenOption(name) == null;
		}

		return dst;
	}

	// CANCEL Name Date
	public static StockOption cancel(String name, QDate date) {
		// TODO Currently no need to distinguish this from expiring
		return expire(name, date);
	}

	// EXERCISE Name Date Qty Price
	public static StockOption exercise(String name, QDate date, BigDecimal shares) {
		StockOption opt = getOpenOption(name);
		if (opt == null) {
			Common.reportError("Can't find option for exercise");
		}

		StockOption dst = new StockOption(opt, date, shares);

		// if ((shares.compareTo(opt.getAvailableShares(false)) < 0) //
		// || (opt.vestCurrent < opt.vestCount)) {
		// }

		StockOption.options.add(dst);

		return dst;
	}

	public static void processGrant(InvestmentTxn txn) {
		for (StockOption opt : options) {
			if ((opt.srcOption == null) //
					&& opt.date.equals(txn.getDate()) //
					&& (opt.secid == txn.security.secid) //
			// && opt.grantShares.equals(txn.getShares())
			) {
				opt.transaction = txn;
				return;
			}
		}

		Common.reportWarning("Can't find match for option grant transaction " + txn.security.getSymbol());
	}

	public static void processVest(InvestmentTxn txn) {
		for (StockOption opt : options) {
			if ((opt.srcOption != null) //
					&& (opt.secid == txn.security.secid) //
					&& opt.date.equals(txn.getDate()) //
					&& (opt.vestCurrent != opt.srcOption.vestCurrent)) {
				opt.transaction = txn;
				return;
			}
		}

		Common.reportWarning("Can't find match for option vest transaction " //
				+ txn.toString());
	}

	public static void processSplit(InvestmentTxn txn) {
		boolean found = false;

		for (StockOption opt : options) {
			if ((opt.srcOption != null) //
					&& opt.date.equals(txn.getDate()) //
					&& (opt.vestCurrent == opt.srcOption.vestCurrent) //
					&& (opt.grantShares.compareTo(opt.srcOption.grantShares) != 0) //
					&& (opt.strikePrice.compareTo(opt.srcOption.strikePrice) != 0)) {
				opt.transaction = txn;
				found = true;
			}
		}

		if (!found) {
			Common.reportWarning("Can't find match for option split transaction " //
					+ txn.toString());
		}
	}

	public static void processExercise(InvestmentTxn txn) {
		for (StockOption opt : options) {
			if ((opt.srcOption != null) //
					&& (opt.secid == txn.security.secid) //
					&& opt.date.equals(txn.getDate()) //
					&& (opt.vestCurrent == opt.srcOption.vestCurrent) //
					&& (opt.getAvailableShares(true).compareTo( //
							opt.srcOption.getAvailableShares(true)) < 0)) {
				opt.transaction = txn;
				return;
			}
		}

		Common.reportWarning("Can't find match for option exercise transaction " //
				+ txn.toString());
	}

	public static void processExpire(InvestmentTxn txn) {
		for (StockOption opt : options) {
			if ((opt.secid == txn.security.secid) //
					&& opt.date.equals(txn.getDate()) //
					&& (opt.srcOption != null) //
					&& (opt.vestCurrent == opt.srcOption.vestCurrent) //
					&& (opt.getAvailableShares(false).signum() == 0)) {
				opt.transaction = txn;
				return;

			}
		}

		Common.reportWarning("Can't find match for option expire transaction " //
				+ txn.toString());
	}

	public static StockOption getOpenOption(String name) {
		for (StockOption opt : StockOption.options) {
			if ((opt.cancelDate == null) && (opt.name.equals(name))) {
				return opt;
			}
		}

		return null;
	}

	public static List<StockOption> getOpenOptions() {
		List<StockOption> openOptions = new ArrayList<StockOption>();

		for (StockOption opt : StockOption.options) {
			if (opt.cancelDate == null) {
				openOptions.add(opt);
			}
		}

		return openOptions;
	}

	public static List<StockOption> getOpenOptions(Account acct, QDate date) {
		List<StockOption> retOptions = new ArrayList<StockOption>();

		for (StockOption opt : options) {
			if ((opt.acctid == acct.acctid) //
					&& (opt.getAvailableShares(true).signum() != 0) //
					&& opt.isLiveOn(date)) {
				retOptions.add(opt);
			}
		}

		return retOptions;
	}

	public final StockOption srcOption;

	public final String name;
	public final QDate date;

	public final int acctid;
	public final int secid;
	public final BigDecimal strikePrice;
	public final BigDecimal grantShares;
	public final int lifetimeMonths;

	public final int vestFrequencyMonths;
	public final int vestCount;
	public final int vestCurrent;

	/** Remaining shares after partial exercise */
	public final BigDecimal sharesRemaining;

	// TODO connect to creating transaction
	public InvestmentTxn transaction;

	public QDate cancelDate;

	/** Create a new option (GRANT) */
	public StockOption( //
			String name, //
			QDate date, //
			int acctid, //
			int secid, //
			BigDecimal shares, //
			BigDecimal price, //
			int vestPeriod, //
			int vestCount, //
			int lifetimeMonths) {
		this.srcOption = null;
		this.cancelDate = null;

		this.name = name;
		this.date = date;

		this.acctid = acctid;
		this.secid = secid;
		this.grantShares = shares;
		this.strikePrice = price;

		this.vestFrequencyMonths = vestPeriod;
		this.vestCount = vestCount;
		this.vestCurrent = 0;
		this.lifetimeMonths = lifetimeMonths;

		this.sharesRemaining = this.grantShares;
	}

	/** Change the vesting of an option (VEST) */
	public StockOption( //
			StockOption src, //
			QDate date, //
			int vestNumber) {
		this.srcOption = src;
		src.cancelDate = date;

		this.name = src.name;
		this.date = date;

		this.acctid = src.acctid;
		this.secid = src.secid;
		this.grantShares = src.grantShares;
		this.strikePrice = src.strikePrice;

		this.vestFrequencyMonths = src.vestFrequencyMonths;
		this.vestCount = src.vestCount;
		this.vestCurrent = vestNumber;
		this.lifetimeMonths = src.lifetimeMonths;

		this.sharesRemaining = src.sharesRemaining;
	}

	/** Process a stocksplit (SPLIT) */
	public StockOption( //
			StockOption src, //
			QDate date, //
			int newshr, //
			int oldshr) {
		this.srcOption = src;
		src.cancelDate = date;

		this.name = src.name;
		this.date = date;

		this.acctid = src.acctid;
		this.secid = src.secid;

		BigDecimal ratio = new BigDecimal(newshr).divide(new BigDecimal(oldshr));

		this.grantShares = src.grantShares.multiply(ratio);
		this.strikePrice = src.strikePrice.divide(ratio);

		this.vestFrequencyMonths = src.vestFrequencyMonths;
		this.vestCount = src.vestCount;
		this.vestCurrent = src.vestCurrent;
		this.lifetimeMonths = src.lifetimeMonths;

		this.sharesRemaining = src.sharesRemaining.multiply(ratio);
	}

	/** Exercise/cancel part of the option (EXERCISE/CANCEL/EXPIRE) */
	public StockOption( //
			StockOption src, //
			QDate date, //
			BigDecimal shares) {
		this.srcOption = src;
		src.cancelDate = date;

		this.name = src.name;
		this.date = date;

		this.acctid = src.acctid;
		this.secid = src.secid;
		this.grantShares = src.grantShares;
		this.strikePrice = src.strikePrice;

		this.vestFrequencyMonths = src.vestFrequencyMonths;
		this.vestCount = src.vestCount;
		this.vestCurrent = src.vestCurrent;
		this.lifetimeMonths = src.lifetimeMonths;

		this.sharesRemaining = src.sharesRemaining.subtract(shares);
	}

	/** Cancel an option (CANCEL/EXPIRE) */
	public StockOption( //
			StockOption src, //
			QDate date) {
		this(src, date, BigDecimal.ZERO);

		this.cancelDate = date;
	}

	public boolean isLiveOn(QDate date) {
		return (this.date.compareTo(date) < 0) //
				&& ((this.cancelDate == null) || (this.cancelDate.compareTo(date) > 0));

	}

	public BigDecimal getValueForDate(QDate date) {
		if (!isLiveOn(date)) {
			return BigDecimal.ZERO;
		}

		BigDecimal shares = getAvailableShares(true);
		QPrice qprice = Security.getSecurity(this.secid).getPriceForDate(date);
		BigDecimal netPrice = qprice.price.subtract(this.strikePrice);

		return (netPrice.signum() > 0) ? shares.multiply(netPrice) : BigDecimal.ZERO;
	}

	public BigDecimal getAvailableShares() {
		return getAvailableShares(false);
	}

	public BigDecimal getAvailableShares(boolean ignoreCancel) {
		if (((this.cancelDate != null) && !ignoreCancel) //
				|| (this.vestCurrent <= 0)) {
			return BigDecimal.ZERO;
		}

		BigDecimal vestRatio = new BigDecimal(this.vestCurrent) //
				.divide(new BigDecimal(this.vestCount));

		BigDecimal totalUnvestedShares = this.grantShares.subtract( //
				this.grantShares.multiply(vestRatio));

		return this.sharesRemaining.subtract(totalUnvestedShares);
	}

	public String toString() {
		String s = "Option(";

		s += this.name + ", ";
		s += this.date.longString + ", ";
		s += Security.getSecurity(this.secid).getSymbol() + ", ";
		s += String.format("vest %d/%d, ", this.vestCurrent, this.vestCount);
		s += "remain=" + Common.formatAmount3(this.sharesRemaining).trim() + ", ";
		s += "avail=" + Common.formatAmount3(getAvailableShares(true)).trim() + ", ";
		s += "price=" + Common.formatAmount3(this.strikePrice).trim() + ", ";

		s += ")";

		return s;
	}
}