package moneymgr.io;

import moneymgr.model.Account;
import moneymgr.model.SecurityPortfolio;
import moneymgr.model.SecurityPosition;

/** Post-process portfolios after loading data */
public class PortfolioProcessor {
	/** Update global portfolio and each account portfolio */
	public static void fixPortfolios() {
		fixPortfolio(SecurityPortfolio.portfolio);

		for (Account a : Account.getAccounts()) {
			if (a.isInvestmentAccount()) {
				fixPortfolio(a.securities);
			}
		}
	}

	/** Update positions in a portfolio */
	private static void fixPortfolio(SecurityPortfolio port) {
		for (SecurityPosition pos : port.positions) {
			pos.updateShareBalances();
		}
	}
}