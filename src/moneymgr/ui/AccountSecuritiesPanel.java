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

	private void addInfoForDate(StringBuffer txt, Account acct, QDate date) {
		BigDecimal acctvalue = acct.getValueForDate(date);
		BigDecimal secvalue = (acct.isInvestmentAccount()) //
				? acct.securities.getPortfolioValueForDate(date) //
				: BigDecimal.ZERO;
		BigDecimal cashvalue = acctvalue.subtract(secvalue);

		int idx = 1;

		if (acct.isInvestmentAccount()) {
			boolean hasSecurity = false;

			for (SecurityPosition pos : acct.securities.getPositions()) {
				if (!Common.isEffectivelyZero(pos.getValueForDate(date))) {
					txt.append(String.format("%2d: %-40s %12s %12s\n", //
							idx, //
							Common.formatString(pos.security.getName(), -40), //
							Common.formatAmount3(pos.getSharesForDate(date)), //
							Common.formatAmount(pos.getValueForDate(date)) //
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
	}

	public void accountSelected(Account acct, boolean update) {
		if (acct == null) {
			setText("");
			return;
		}

		StringBuffer txt = new StringBuffer();

		txt.append("\n");

		QDate curdate = MainWindow.instance.asOfDate();

		addInfoForDate(txt, acct, curdate);

		if (curdate != QDate.today()) {
			txt.append("\n======= TODAY =======\n");

			addInfoForDate(txt, acct, QDate.today());
		}

		setText(txt.toString());
	}
}