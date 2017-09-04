package qif.data;

public enum TxAction {
	OTHER, CASH, XIN, XOUT, WITHDRAWX, //
	CONTRIBX, INT_INC, MISC_INCX, //
	BUY, BUYX, SELL, SELLX, //
	GRANT, VEST, EXERCISE, EXERCISEX, EXPIRE, //
	SHRS_IN, SHRS_OUT, //
	DIV, REINV_DIV, REINV_LG, REINV_SH, //
	REINV_INT, //
	STOCKSPLIT, REMINDER;

	public static TxAction parseAction(String s) {
		if ("StkSplit".equals(s)) {
			return TxAction.STOCKSPLIT;
		}
		if ("Cash".equals(s)) {
			return TxAction.CASH;
		}
		if ("XIn".equals(s)) {
			return TxAction.XIN;
		}
		if ("XOut".equals(s)) {
			return TxAction.XOUT;
		}
		if ("Buy".equals(s)) {
			return TxAction.BUY;
		}
		if ("BuyX".equals(s)) {
			return TxAction.BUYX;
		}
		if ("Sell".equals(s)) {
			return TxAction.SELL;
		}
		if ("SellX".equals(s)) {
			return TxAction.SELLX;
		}
		if ("ShrsIn".equals(s)) {
			return TxAction.SHRS_IN;
		}
		if ("ShrsOut".equals(s)) {
			return TxAction.SHRS_OUT;
		}
		if ("Grant".equals(s)) {
			return TxAction.GRANT;
		}
		if ("Vest".equals(s)) {
			return TxAction.VEST;
		}
		if ("Exercise".equals(s)) {
			return TxAction.EXERCISE;
		}
		if ("ExercisX".equals(s)) {
			return TxAction.EXERCISEX;
		}
		if ("Expire".equals(s)) {
			return TxAction.EXPIRE;
		}
		if ("WithdrwX".equals(s)) {
			return TxAction.WITHDRAWX;
		}
		if ("IntInc".equals(s)) {
			return TxAction.INT_INC;
		}
		if ("MiscIncX".equals(s)) {
			return TxAction.MISC_INCX;
		}
		if ("Div".equals(s)) {
			return TxAction.DIV;
		}
		if ("ReinvDiv".equals(s)) {
			return TxAction.REINV_DIV;
		}
		if ("ReinvLg".equals(s)) {
			return TxAction.REINV_LG;
		}
		if ("ReinvSh".equals(s)) {
			return TxAction.REINV_SH;
		}
		if ("ReinvInt".equals(s)) {
			return TxAction.REINV_INT;
		}
		if ("ContribX".equals(s)) {
			return TxAction.CONTRIBX;
		}
		if ("Reminder".equals(s)) {
			return TxAction.REMINDER;
		}

		return TxAction.OTHER;
	}
}