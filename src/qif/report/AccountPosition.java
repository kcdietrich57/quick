package qif.report;

import java.math.BigDecimal;

import qif.data.Account;
import qif.data.SecurityPortfolio;

public class AccountPosition {
	public Account acct;
	public BigDecimal cashBefore;
	public BigDecimal cashAfter;
	public SecurityPortfolio portBefore;
	public SecurityPortfolio portAfter;

	public AccountPosition(Account a) {
		this.acct = a;
	}
}
