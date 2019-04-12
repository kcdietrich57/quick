package moneymgr.ui;

import java.awt.Font;
import java.math.BigDecimal;

import javax.swing.JTextArea;

import moneymgr.model.Account;
import moneymgr.model.SecurityPosition;
import moneymgr.util.Common;
import moneymgr.util.QDate;

@SuppressWarnings("serial")
public class AccountSecuritiesPanel //
		extends JTextArea //
		implements AccountSelectionListener {

	public AccountSecuritiesPanel() {
		setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
	}

	public void accountSelected(Account acct, boolean update) {
		if (acct == null) {
			setText("");
			return;
		}

		StringBuffer txt = new StringBuffer();

		txt.append("\n");

		if (acct.isInvestmentAccount()) {
			QDate curdate = MainWindow.instance.asOfDate();

			int idx = 1;

			BigDecimal pvalue = acct.securities.getPortfolioValueForDate(curdate);
			BigDecimal cashvalue = acct.getValueForDate(curdate).subtract(pvalue);

			if (!Common.isEffectivelyZero(cashvalue)) {
				txt.append(String.format("%2d: %40s %s\n", //
						idx, //
						Common.formatString("Cash", 40), //
						Common.formatAmount(cashvalue)));
				++idx;
			}

			for (SecurityPosition pos : acct.securities.positions) {
				if (!Common.isEffectivelyZero(pos.getValueForDate(curdate))) {
					txt.append(String.format("%2d: %-40s %10s %10s\n", //
							idx, //
							Common.formatString(pos.security.getName(), -40), //
							Common.formatAmount3(pos.getSharesForDate(curdate)), //
							Common.formatAmount(pos.getValueForDate(curdate)) //
					));
					++idx;
				}
			}

			txt.append(String.format("    Total: %40s %s\n", //
					"", Common.formatAmount(pvalue)));
		} else {
			txt.append("Not an investment account");
		}

		setText(txt.toString());
	}
}