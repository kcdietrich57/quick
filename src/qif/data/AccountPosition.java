package qif.data;

import java.math.BigDecimal;

public class AccountPosition {
	Account acct;
	BigDecimal cashBefore;
	BigDecimal cashAfter;
	SecurityPortfolio portBefore;
	SecurityPortfolio portAfter;
	
	AccountPosition(Account a) {
		this.acct = a;
	}
}
