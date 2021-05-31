package moneymgr.model;

import java.util.HashMap;
import java.util.Map;

/** A transaction's type (what it did) */
public enum TxAction {
	OTHER(""), CASH("Cash"), XIN("XIn", true), XOUT("XOut", true), //
	WITHDRAWX("WithdrwX", true), CONTRIBX("ContribX"), //
	INT_INC("IntInc"), MISC_INCX("MiscIncX", true), //
	BUY("Buy"), BUYX("BuyX", true), SELL("Sell"), SELLX("SellX", true), //
	GRANT("Grant"), VEST("Vest"), EXERCISE("Exercise"), //
	EXERCISEX("ExercisX", true), EXPIRE("Expire"), //
	SHRS_IN("ShrsIn", true), SHRS_OUT("ShrsOut", true), //
	DIV("Div"), REINV_DIV("ReinvDiv"), REINV_LG("ReinvLg"), REINV_SH("ReinvSh"), //
	REINV_INT("ReinvInt"), //
	STOCKSPLIT("StkSplit"), REMINDER("Reminder");

	private static Map<String, TxAction> actionMap = new HashMap<>();

	private static void addAction(TxAction action) {
		actionMap.put(action.key, action);
	}

	static {
		addAction(STOCKSPLIT);
		addAction(CASH);
		addAction(XIN);
		addAction(XOUT);
		addAction(BUY);
		addAction(BUYX);
		addAction(SELL);
		addAction(SELLX);
		addAction(SHRS_IN);
		addAction(SHRS_OUT);
		addAction(GRANT);
		addAction(VEST);
		addAction(EXERCISE);
		addAction(EXERCISEX);
		addAction(EXPIRE);
		addAction(WITHDRAWX);
		addAction(INT_INC);
		addAction(MISC_INCX);
		addAction(DIV);
		addAction(REINV_DIV);
		addAction(REINV_LG);
		addAction(REINV_SH);
		addAction(REINV_INT);
		addAction(CONTRIBX);
		addAction(REMINDER);
	}

	/** This is set for both cash and security transfers */
	public final boolean isTransfer;
	public final String key;

	public static TxAction parseAction(String s) {
		TxAction act = actionMap.get(s);
		return (act != null) ? act : TxAction.OTHER;
	}

	private TxAction(String key, boolean isxfer) {
		this.key = key;
		this.isTransfer = isxfer;
	}

	private TxAction(String key) {
		this(key, false);
	}

	/** Define equivalent actions when comparing win/mac */
	public boolean isEquivalentTo(TxAction other) {
		if (this.equals(other)) {
			return true;
		}

		switch (this) {
		case CASH:
			return other == WITHDRAWX || other == XIN || other == XOUT;

		case XIN:
			return other == CONTRIBX || other == CASH;

		case XOUT:
			return other == WITHDRAWX || other == CASH;

		case CONTRIBX:
			return other == XIN || other == CASH;

		case WITHDRAWX:
			return other == XOUT || other == CASH;

		default:
			return false;
		}
	}
}