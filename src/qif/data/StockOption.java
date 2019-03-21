package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

	public static final List<StockOption> options = new ArrayList<StockOption>();

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
		StockOption dst = null;

		if (src != null) {
			dst = new StockOption(src, date);
			options.add(dst);

			assert getOpenOption(name) == null;
		}

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

	/** Match up a grant transaction with the option object */
	public static void processGrant(InvestmentTxn txn) {
		for (StockOption opt : options) {
			if ((opt.srcOption == null) //
					&& opt.date.equals(txn.getDate()) //
					&& (opt.secid == txn.security.secid)) {
				if (QifDom.verbose && !opt.grantShares.equals(txn.getShares())) {
					// TODO grant transactions in quicken have 0 shares
					Common.reportWarning(String.format( //
							"Option Grant shares (%s) don't match tx (%s)", //
							Common.formatAmount3(opt.grantShares).trim(), //
							Common.formatAmount3(txn.getShares()).trim()));
				}

				opt.transaction = txn;
				return;
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
				opt.transaction = txn;
				return;
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
				opt.transaction = txn;
				found = true;
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
				opt.transaction = txn;
				return;
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
				opt.transaction = txn;
				return;

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
		List<StockOption> openOptions = new ArrayList<StockOption>();

		for (StockOption opt : options) {
			if (opt.cancelDate == null) {
				openOptions.add(opt);
			}
		}

		return openOptions;
	}

	/** Return open options for an account on a given date */
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

	/** The option this is derived from */
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

	/** Remaining shares after partial exercise(s) */
	public final BigDecimal sharesRemaining;

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

	/** Return whether this option is still in play on a given date */
	public boolean isLiveOn(QDate date) {
		return (this.date.compareTo(date) < 0) //
				&& ((this.cancelDate == null) || (this.cancelDate.compareTo(date) > 0));

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
		return getAvailableShares(false);
	}

	/**
	 * Get the number of shares currently available (optionally include canceled
	 * options)
	 */
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