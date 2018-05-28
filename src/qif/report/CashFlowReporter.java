package qif.report;

import java.math.BigDecimal;
import java.util.Date;

import qif.data.Account;
import qif.data.QifDom;

public class CashFlowReporter {

	public static void reportCashFlow(Date d1, Date d2) {
		QifDom dom = QifDom.dom;

		AccountPosition[] info = new AccountPosition[dom.getNumAccounts()];

		for (int anum = 0; anum <= dom.getNumAccounts(); ++anum) {
			Account a = dom.getAccount(anum);

			if (a != null) {
				info[anum - 1].acct = a;

				BigDecimal v1 = a.getCashValueForDate(d1);
			}
		}
	}

}