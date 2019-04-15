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

		QDate curdate = MainWindow.instance.asOfDate();

		BigDecimal acctvalue = acct.getValueForDate(curdate);
		BigDecimal secvalue = (acct.isInvestmentAccount()) //
				? acct.securities.getPortfolioValueForDate(curdate) //
				: BigDecimal.ZERO;
		BigDecimal cashvalue = acctvalue.subtract(secvalue);

		int idx = 1;

		if (acct.isInvestmentAccount()) {
			boolean hasSecurity = false;

			for (SecurityPosition pos : acct.securities.positions) {
				if (!Common.isEffectivelyZero(pos.getValueForDate(curdate))) {
					txt.append(String.format("%2d: %-40s %12s %12s\n", //
							idx, //
							Common.formatString(pos.security.getName(), -40), //
							Common.formatAmount3(pos.getSharesForDate(curdate)), //
							Common.formatAmount(pos.getValueForDate(curdate)) //
					));

					hasSecurity = true;
					++idx;
				}
			}

			if (hasSecurity) {
				txt.append(String.format("    %53s  %11s\n", //
						Common.repeatChar('-', 53), //
						Common.repeatChar('-', 11)));

				txt.append(String.format("    %50s    %12s\n", //
						Common.formatString("Securities Total", 50), //
						Common.formatAmount(secvalue)));

				txt.append("\n");

				txt.append(String.format("    %53s  %11s\n", //
						Common.repeatChar('-', 53), //
						Common.repeatChar('-', 11)));
			}
		}
//		else {
//			txt.append("Not an investment account");
//		}

		txt.append(String.format("%2d: %50s    %12s\n", //
				idx, //
				Common.formatString("Cash", -50), //
				Common.formatAmount(cashvalue)));
		++idx;

		txt.append("\n");

		txt.append(String.format("    %53s  %11s\n", //
				Common.repeatChar('=', 53), //
				Common.repeatChar('=', 11)));

		txt.append(String.format("    %50s    %12s\n", //
				Common.formatString("Account Total", 50), //
				Common.formatAmount(acctvalue)));

		setText(txt.toString());
	}
}