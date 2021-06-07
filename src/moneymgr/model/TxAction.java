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

	private static void addAction(TxAction action, String[] names) {
		actionMap.put(action.key, action);
		for (String name : names) {
			actionMap.put(name, action);
		}
	}

	private static void addAction(TxAction action) {
		addAction(action, new String[0]);
	}

	// Miscellaneous Expense,
	static {
		addAction(STOCKSPLIT, new String[] { "Stock Split" });
		addAction(CASH, new String[] { "Payment/Deposit" });
		addAction(XIN);
		addAction(XOUT);
		addAction(BUY, new String[] { "Buy", "Buy Bonds" });
		addAction(BUYX);
		addAction(SELL, new String[] { "Sell", "Sell Bonds" });
		addAction(SELLX);
		addAction(SHRS_IN, new String[] { "Add Shares" });
		addAction(SHRS_OUT, new String[] { "Remove Shares" });
		addAction(GRANT);
		addAction(VEST);
		addAction(EXERCISE);
		addAction(EXERCISEX);
		addAction(EXPIRE);
		addAction(WITHDRAWX);
		addAction(INT_INC, new String[] { "Interest Income" });
		addAction(MISC_INCX, new String[] { "Miscellaneous Income" });
		addAction(DIV, new String[] { "Dividend Income" });
		addAction(REINV_DIV, new String[] { "Reinvest Dividend" });
		addAction(REINV_LG, new String[] { "Reinvest Long-term Capital Gain" });
		addAction(REINV_SH, new String[] { "Reinvest Short-term Capital Gain" });
		addAction(REINV_INT, new String[] { "Reinvest Interest" });
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
			return other == OTHER || other == WITHDRAWX || other == XIN || other == XOUT;

		case XIN:
			return other == CONTRIBX || other == CASH;

		case XOUT:
			return other == WITHDRAWX || other == CASH;

		case CONTRIBX:
			return other == XIN || other == CASH;

		case WITHDRAWX:
			return other == XOUT || other == CASH;

		case OTHER:
			return other == CASH;

		default:
			return false;
		}
	}
}