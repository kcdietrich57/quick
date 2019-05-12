package moneymgr.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import app.QifDom;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/** Class for tracking security option grants of various kinds */
public class StockOption {
	// Life cycle of an option
	// ==============================
	// ---GRANT Name Date Qty Price VestPeriod NumVests lifeMonths
	// This defines the parameters (size, vest method, price, lifetime)
	// ---VEST Name Date VestNum
	// Control of part or all of the options is given to the grantee
	// ---SPLIT Name Date NewShr OldShr [newQty/newPrice]
	// Splits divide or combine shares
	// ---EXERCISE DName ate Qty Price
	// Convert options to shares belonging to the grantee
	// ---CANCEL Name Date
	// The grantor withdraws the option from grant
	// ---EXPIRE Name Date
	// Non-exercised option automatically expires at the end of its life
	// ==============================

	public static final List<StockOption> options = new ArrayList<>();

	/**
	 * ESPP Purchase of discounted ESPP shares.
	 *
	 * @param date     Date the grant is given
	 * @param acctid   Account tracking the grant
	 * @param secid    Security being granted
	 * @param shares   Number of shares granted
	 * @param buyPrice The share price paid
	 * @param cost     The total price paid
	 * @param mktPrice The market price on the purchase date
	 * @param value    The value of the shares on the purchase date
	 *
	 * @return A new option object
	 */
	public static StockOption esppPurchase( //
			QDate date, //
			int acctid, //
			int secid, //
			BigDecimal shares, //
			BigDecimal buyPrice, //
			BigDecimal cost, //
			BigDecimal mktPrice, //
			BigDecimal value) {
		StockOption opt = new StockOption(date, acctid, secid, //
				shares, buyPrice, cost, mktPrice, value);

		options.add(opt);

		return opt;

	}

	/**
	 * GRANT Grantor gives an option to the grantee.
	 *
	 * @param name           Meaningful name given to identify the grant
	 * @param date           Date the grant is given
	 * @param acctid         Account tracking the grant
	 * @param secid          Security being granted
	 * @param shares         Number of shares granted
	 * @param price          The share price the grantee pays on exercise
	 * @param vestPeriod     How long until options are fully granted
	 * @param vestCount      How many increments for vesting
	 * @param lifetimeMonths How long from the grant date until expiration
	 *
	 * @return A new option object
	 */
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

	/**
	 * VEST An option becomes (partially) eligible for exercise by the grantee.
	 *
	 * @param name       Option name
	 * @param date       Date of vesting
	 * @param vestNumber Number of the vest event
	 *
	 * @return A new option object for the vested option shares
	 */
	public static StockOption vest(String name, QDate date, int vestNumber) {
		StockOption src = getOpenOption(name);
		StockOption dst = new StockOption(src, date, vestNumber);

		assert getOpenOption(name) == null;

		options.add(dst);

		return dst;
	}

	/**
	 * SPLIT Shares are split or combined and the price adjusted accordingly.
	 *
	 * @param name   Option name
	 * @param date   Date of stock split
	 * @param newshr Number of new shares for
	 * @param oldshr This number of old shares
	 *
	 * @return New option object representing the resulting option shares
	 */
	public static StockOption split(String name, QDate date, int newshr, int oldshr) {
		StockOption src = getOpenOption(name);
		StockOption dst = new StockOption(src, date, newshr, oldshr);

		assert getOpenOption(name) == null;

		options.add(dst);

		return dst;
	}

	/**
	 * EXPIRE Options expire without being exercised
	 *
	 * @param name Option name
	 * @param date Date of expiration
	 *
	 *             TODO why do we need an option object for non-options here?
	 * @return New option object representing the new option state
	 */
	public static StockOption expire(String name, QDate date) {
		StockOption src = getOpenOption(name);

		if (src == null) {
			Common.reportWarning("Stock option expired with no option object");
			return null;
		}

		StockOption dst = new StockOption(src, date);

		options.add(dst);

		assert getOpenOption(name) == null;

		return dst;
	}

	/**
	 * CANCEL Options withdrawn by grantee without being exercised
	 *
	 * @param name Option name
	 * @param date Date of cancellation
	 *
	 *             TODO why do we need an option object for non-options here?
	 * @return New option object representing the new option state
	 */
	public static StockOption cancel(String name, QDate date) {
		// TODO No apparent need to distinguish this from expiring
		return expire(name, date);
	}

	/**
	 * EXERCISE Options converted into shares by grantee
	 *
	 * @param name   Option name
	 * @param date   Date of exercise
	 * @param shares Number of shares exercised
	 *
	 * @return New option object representing the exercised shares
	 */
	public static StockOption exercise(String name, QDate date, BigDecimal shares) {
		StockOption src = getOpenOption(name);
		if (src == null) {
			Common.reportError("Can't find option for exercise");
		}

		StockOption dst = new StockOption(src, date, shares);

		// if ((shares.compareTo(opt.getAvailableShares(false)) < 0) //
		// || (opt.vestCurrent < opt.vestCount)) {
		// }

		StockOption.options.add(dst);

		return dst;
	}

	/** Match up an ESPP transaction with the option object */
	public static void processEspp(InvestmentTxn txn) {
		for (StockOption opt : options) {
			if ((opt.srcOption == null) //
					&& opt.date.equals(txn.getDate()) //
					&& (opt.secid == txn.security.secid) //
					&& (opt.acctid == txn.acctid)) {
				if (// QifDom.verbose &&
				!opt.grantShares.equals(txn.getShares())) {
					// TODO grant transactions in quicken have 0 shares
					Common.reportWarning(String.format( //
							"ESPP shares (%s) don't match tx (%s)", //
							Common.formatAmount3(opt.grantShares).trim(), //
							Common.formatAmount3(txn.getShares()).trim()));
				}
				if (opt.transaction == null) {
					opt.transaction = txn;
					txn.option = opt;
					return;
				}
			}
		}

		if (QifDom.verbose) {
			Common.reportWarning("Can't find match for ESPP transaction " + txn.security.getSymbol());
		}
	}

	/** Match up a grant transaction with the option object */
	public static void processGrant(InvestmentTxn txn) {
		for (StockOption opt : options) {
// TODO match option name with transaction memo
// TODO fix other actions similarly
			if ((opt.srcOption == null) //
					&& opt.date.equals(txn.getDate()) //
					&& (opt.secid == txn.security.secid) //
					&& (opt.acctid == txn.acctid)) {
				if (QifDom.verbose && !opt.grantShares.equals(txn.getShares())) {
					// TODO grant transactions in quicken have 0 shares
					Common.reportWarning(String.format( //
							"Option Grant shares (%s) don't match tx (%s)", //
							Common.formatAmount3(opt.grantShares).trim(), //
							Common.formatAmount3(txn.getShares()).trim()));
				}

				if (opt.transaction == null) {
					opt.transaction = txn;
					txn.option = opt;
					return;
				}
			}
		}

		if (QifDom.verbose) {
			Common.reportWarning("Can't find match for option grant transaction " + txn.security.getSymbol());
		}
	}

	/** Match up a vest transaction with the option object */
	public static void processVest(InvestmentTxn txn) {
		for (StockOption opt : options) {
			if ((opt.srcOption != null) //
					&& (opt.secid == txn.security.secid) //
					&& opt.date.equals(txn.getDate()) //
					&& (opt.vestCurrent != opt.srcOption.vestCurrent)) {
				if (opt.transaction == null) {
					opt.transaction = txn;
					txn.option = opt;
					return;
				}
			}
		}

		if (QifDom.verbose) {
			Common.reportWarning("Can't find match for option vest transaction " //
					+ txn.toString());
		}
	}

	/** Match up a split transaction with the option object */
	public static void processSplit(InvestmentTxn txn) {
		boolean found = false;

		for (StockOption opt : options) {
			if ((opt.srcOption != null) //
					&& opt.date.equals(txn.getDate()) //
					&& (opt.vestCurrent == opt.srcOption.vestCurrent) //
					&& (opt.grantShares.compareTo(opt.srcOption.grantShares) != 0) //
					&& (opt.strikePrice.compareTo(opt.srcOption.strikePrice) != 0)) {
				if (opt.transaction == null) {
					opt.transaction = txn;
					txn.option = opt;
					found = true;
				}
			}
		}

		// TODO why would we always expect to find an option?
		if (!found && QifDom.verbose) {
			Common.reportWarning("Can't find match for option split transaction " //
					+ txn.toString());
		}
	}

	/** Match up an exercise transaction with the option object */
	public static void processExercise(InvestmentTxn txn) {
		for (StockOption opt : options) {
			if ((opt.srcOption != null) //
					&& (opt.secid == txn.security.secid) //
					&& opt.date.equals(txn.getDate()) //
					&& (opt.vestCurrent == opt.srcOption.vestCurrent) //
					&& (opt.getAvailableShares(true).compareTo( //
							opt.srcOption.getAvailableShares(true)) < 0)) {
				if (opt.transaction == null) {
					opt.transaction = txn;
					txn.option = opt;
					return;
				}
			}
		}

		if (QifDom.verbose) {
			Common.reportWarning("Can't find match for option exercise transaction " //
					+ txn.toString());
		}
	}

	/** Match up an expire transaction with the option object */
	public static void processExpire(InvestmentTxn txn) {
		for (StockOption opt : options) {
			if ((opt.secid == txn.security.secid) //
					&& opt.date.equals(txn.getDate()) //
					&& (opt.srcOption != null) //
					&& (opt.vestCurrent == opt.srcOption.vestCurrent) //
					&& (opt.getAvailableShares(false).signum() == 0)) {
				if (opt.transaction == null) {
					opt.transaction = txn;
					txn.option = opt;
					return;
				}
			}
		}

		if (QifDom.verbose) {
			Common.reportWarning("Can't find match for option expire transaction " //
					+ txn.toString());
		}
	}

	/** Return an open option with a given name */
	public static StockOption getOpenOption(String name) {
		for (StockOption opt : StockOption.options) {
			if ((opt.cancelDate == null) && (opt.name.equals(name))) {
				return opt;
			}
		}

		return null;
	}

	/** Return a list of all open options */
	public static List<StockOption> getOpenOptions() {
		List<StockOption> openOptions = new ArrayList<>();

		for (StockOption opt : options) {
			if (opt.cancelDate == null) {
				openOptions.add(opt);
			}
		}

		return openOptions;
	}

	/** Return open options for an account on a given date */
	public static List<StockOption> getOpenOptions(Account acct, QDate date) {
		List<StockOption> retOptions = new ArrayList<>();

		for (StockOption opt : options) {
			if ((opt.acctid == acct.acctid) //
					&& (opt.getAvailableShares(true).signum() != 0) //
					&& opt.isLiveOn(date)) {
				retOptions.add(opt);
			}
		}

		return retOptions;
	}

	/** The option this is derived from */
	public final StockOption srcOption;

	public final String name;
	public final QDate date;

	public final int acctid;
	public final int secid;
	public final BigDecimal grantShares;

	public final BigDecimal strikePrice;
	public final BigDecimal marketPrice;
	public final BigDecimal cost;
	public final BigDecimal marketValueAtPurchase;

	// Vesting info
	public final int lifetimeMonths;
	public final int vestFrequencyMonths;
	public final int vestCount;
	public final int vestCurrent;

	/** Remaining shares after partial exercise(s) */
	public final BigDecimal sharesRemaining;

	public InvestmentTxn transaction;

	public QDate cancelDate;

	/**
	 * Create a new option for an ESPP purchase
	 * 
	 * @param date     Purchase date
	 * @param acctid   Account holding shares
	 * @param secid    Security purchased
	 * @param shares   Number of shares purchased
	 * @param buyPrice Cost per share paid for security
	 * @param cost     Total cost for shares purchased
	 * @param mktPrice Market share price on purchase date
	 * @param mktValue Market value of shares on purchase date
	 */
	public StockOption( //
			QDate date, //
			int acctid, //
			int secid, //
			BigDecimal shares, //
			BigDecimal buyPrice, //
			BigDecimal cost, //
			BigDecimal mktPrice, //
			BigDecimal mktValue) {
		this.srcOption = null;
		this.cancelDate = null;

		this.name = "espp";
		this.date = date;

		this.acctid = acctid;
		this.secid = secid;
		this.grantShares = shares;
		this.strikePrice = buyPrice;
		this.cost = cost;
		this.marketPrice = mktPrice;
		this.marketValueAtPurchase = mktValue;

		this.lifetimeMonths = -1;
		this.sharesRemaining = shares;
		this.vestCount = 0;
		this.vestFrequencyMonths = 0;
		this.vestCurrent = 0;

	}

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
		this.marketPrice = null;
		this.cost = null;
		this.marketValueAtPurchase = null;

		this.lifetimeMonths = lifetimeMonths;
		this.vestFrequencyMonths = vestPeriod;
		this.vestCount = vestCount;
		this.vestCurrent = 0;

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
		this.marketPrice = null;
		this.cost = null;
		this.marketValueAtPurchase = null;

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
		this.marketPrice = null;
		this.cost = null;
		this.marketValueAtPurchase = null;

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
		this.marketPrice = null;
		this.cost = null;
		this.marketValueAtPurchase = null;

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

	/** Return whether this option is still in play on a given date */
	public boolean isLiveOn(QDate date) {
		return (this.date.compareTo(date) < 0) //
				&& ((this.cancelDate == null) || (this.cancelDate.compareTo(date) > 0));

	}

	public BigDecimal getDiscount() {
		if (this.marketValueAtPurchase == null || this.cost == null) {
			return BigDecimal.ZERO;
		}
		return this.marketValueAtPurchase.subtract(this.cost);
	}

	public BigDecimal getBasisPrice(QDate date) {
		// TODO apply rules for holding periods
		return this.strikePrice;
	}

	/** Return the option value on a given date (zero if underwater) */
	public BigDecimal getValueForDate(QDate date) {
		if (!isLiveOn(date)) {
			return BigDecimal.ZERO;
		}

		BigDecimal shares = getAvailableShares(true);
		QPrice qprice = Security.getSecurity(this.secid).getPriceForDate(date);
		BigDecimal netPrice = qprice.getPrice().subtract(this.strikePrice);

		return (netPrice.signum() > 0) ? shares.multiply(netPrice) : BigDecimal.ZERO;
	}

	/** Get the number of shares currently available */
	public BigDecimal getAvailableShares() {
		return getAvailableShares(null, false);
	}

	/** Get the number of shares currently available on a given date */
	public BigDecimal getAvailableShares(QDate date) {
		return getAvailableShares(date, false);
	}

	/**
	 * Get the number of shares currently available (optionally include canceled
	 * options)
	 */
	public BigDecimal getAvailableShares(boolean ignoreCancel) {
		return getAvailableShares(null, ignoreCancel);
	}

	/**
	 * Get the number of shares available on a date (optionally include canceled
	 * options)
	 */
	private BigDecimal getAvailableShares(QDate date, boolean ignoreCancel) {
		StockOption opt = this;
		while ((date != null) && (opt.srcOption != null) && //
				date.compareTo(srcOption.date) < 0) {
			opt = opt.srcOption;
		}

		boolean canceledOnDate = (opt.cancelDate != null) //
				&& ((date == null) || (opt.cancelDate.compareTo(date) <= 0));

		if ((canceledOnDate && !ignoreCancel) //
				|| (opt.getVestNum(date) <= 0)) {
			return BigDecimal.ZERO;
		}

		BigDecimal vestRatio = new BigDecimal(this.getVestNum(date)) //
				.divide(new BigDecimal(this.vestCount));

		BigDecimal totalUnvestedShares = this.grantShares.subtract( //
				this.grantShares.multiply(vestRatio));

		return this.sharesRemaining.subtract(totalUnvestedShares);
	}

	public QDate getGrantDate() {
		StockOption opt = this;
		while (opt.srcOption != null) {
			opt = opt.srcOption;
		}

		return opt.date;
	}

	public int getVestNum(QDate date) {
		if (date == null) {
			return this.vestCurrent;
		}

		QDate gd = getGrantDate();

		int days = date.subtract(gd);

		int months = 0;
		int vestnum = 0;
		while (days >= 365) {
			months += 12;
			days -= 365;
			++vestnum; // TODO assuming yearly vesting
		}

		return Math.min(vestnum, this.vestCount);
	}

	public String toString() {
		String s = "Option(";

		s += this.name;
		s += ", " + this.date.longString;
		s += ", " + Account.getAccountByID(this.acctid).name;
		s += ", " + Security.getSecurity(this.secid).getSymbol();
		s += ", " + this.grantShares;
		s += ":" + Common.formatAmount3(this.strikePrice).trim();
		s += ", disc=" + Common.formatAmount3(getDiscount()).trim();

		Object o;
		o = this.marketPrice;
		// o = this.cost;
		// o = this.marketValueAtPurchase;

		if (this.vestCount > 0) {
			s += ", " + String.format("VESTED %d/%d ", this.vestCurrent, this.vestCount);
			s += ", (" + Common.formatAmount0(getAvailableShares(true)).trim();
			s += ", " + Common.formatAmount0(this.sharesRemaining).trim() + ")";
			o = this.lifetimeMonths;
		}

		s += ")";

		return s;
	}

	public String formatInfo(QDate date) {
		StringBuffer ret = new StringBuffer(this.toString());

		Security sec = Security.getSecurity(this.secid);

		ret.append("\n");

		BigDecimal price = sec.getPriceForDate(date).getPrice();

		BigDecimal val = this.sharesRemaining.multiply(price);
		BigDecimal cost = this.sharesRemaining.multiply(this.strikePrice);
		BigDecimal basis = this.sharesRemaining.multiply(getBasisPrice(date));
		BigDecimal discount = val.subtract(cost);
		BigDecimal gain = val.subtract(basis);

		BigDecimal availshares = getAvailableShares(date);
		BigDecimal vval = availshares.multiply(price);
		BigDecimal vcost = availshares.multiply(this.strikePrice);
		BigDecimal vbasis = availshares.multiply(getBasisPrice(date));
		BigDecimal vdiscount = vval.subtract(vcost);
		BigDecimal vgain = vval.subtract(vbasis);

		ret.append("\n");
		ret.append(String.format("As of: %s  strike: %s  price: %s vest %d/%d\n", //
				Common.formatDate(date), //
				Common.formatAmount(this.strikePrice), //
				Common.formatAmount(price), //
				getVestNum(date), //
				this.vestCount));
		ret.append(String.format("Value    : %12s  %12s\n", //
				Common.formatAmount(val), Common.formatAmount(vval)));
		ret.append(String.format("Cost     : %12s  %12s\n", //
				Common.formatAmount(cost), Common.formatAmount(vcost)));
		ret.append(String.format("Basis    : %12s  %12s\n", //
				Common.formatAmount(basis), Common.formatAmount(vbasis)));
		ret.append(String.format("Discount : %12s  %12s\n", //
				Common.formatAmount(discount), Common.formatAmount(vdiscount)));
		ret.append(String.format("Gain/loss: %12s  %12s\n", //
				Common.formatAmount(gain), Common.formatAmount(vgain)));

//		ret.append(String.format("\nVested value: %12s", Common.formatAmount(val)));
//		ret.append(String.format("  cost: %12s", Common.formatAmount(cost)));
//		ret.append(String.format("  basis: %12s", Common.formatAmount(basis)));
//		ret.append(String.format("  discount: %12s", Common.formatAmount(discount)));
//		ret.append(String.format("  gain: %12s", Common.formatAmount(basis)));

		return ret.toString();
	}
}