package qif.data;

import java.math.BigDecimal;

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
