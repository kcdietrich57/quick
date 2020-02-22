package moneymgr.model;

import java.util.HashMap;
import java.util.Map;

/** A transaction's type (what it did) */
public enum TxAction {
	OTHER, CASH, XIN, XOUT, WITHDRAWX, //
	CONTRIBX, INT_INC, MISC_INCX, //
	BUY, BUYX, SELL, SELLX, //
	GRANT, VEST, EXERCISE, EXERCISEX, EXPIRE, //
	SHRS_IN, SHRS_OUT, //
	DIV, REINV_DIV, REINV_LG, REINV_SH, //
	REINV_INT, //
	STOCKSPLIT, REMINDER;

	private static Map<String, TxAction> actionMap = new HashMap<>();

	static {
		actionMap.put("StkSplit", TxAction.STOCKSPLIT);
		actionMap.put("Cash", TxAction.CASH);
		actionMap.put("XIn", TxAction.XIN);
		actionMap.put("XOut", TxAction.XOUT);
		actionMap.put("Buy", TxAction.BUY);
		actionMap.put("BuyX", TxAction.BUYX);
		actionMap.put("Sell", TxAction.SELL);
		actionMap.put("SellX", TxAction.SELLX);
		actionMap.put("ShrsIn", TxAction.SHRS_IN);
		actionMap.put("ShrsOut", TxAction.SHRS_OUT);
		actionMap.put("Grant", TxAction.GRANT);
		actionMap.put("Vest", TxAction.VEST);
		actionMap.put("Exercise", TxAction.EXERCISE);
		actionMap.put("ExercisX", TxAction.EXERCISEX);
		actionMap.put("Expire", TxAction.EXPIRE);
		actionMap.put("WithdrwX", TxAction.WITHDRAWX);
		actionMap.put("IntInc", TxAction.INT_INC);
		actionMap.put("MiscIncX", TxAction.MISC_INCX);
		actionMap.put("Div", TxAction.DIV);
		actionMap.put("ReinvDiv", TxAction.REINV_DIV);
		actionMap.put("ReinvLg", TxAction.REINV_LG);
		actionMap.put("ReinvSh", TxAction.REINV_SH);
		actionMap.put("ReinvInt", TxAction.REINV_INT);
		actionMap.put("ContribX", TxAction.CONTRIBX);
		actionMap.put("Reminder", TxAction.REMINDER);
	}

	public static TxAction parseAction(String s) {
		TxAction act = actionMap.get(s);
		return (act != null) ? act : TxAction.OTHER;
	}
}