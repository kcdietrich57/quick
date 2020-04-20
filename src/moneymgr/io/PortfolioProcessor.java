package moneymgr.io;

import moneymgr.model.Account;
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.SecurityPortfolio;
import moneymgr.model.SecurityPosition;
import moneymgr.model.Statement;

/** Post-process portfolios after loading data */
public class PortfolioProcessor {
	/** Update global portfolio and each account portfolio */
	public static void fixPortfolios() {
		fixPortfolio(SecurityPortfolio.portfolio);

		for (Account a : MoneyMgrModel.currModel.getAccounts()) {
			if (a.isInvestmentAccount()) {
				fixPortfolio(a.securities);

				for (Statement stat : a.statements) {
					fixPortfolio(stat.holdings);
				}
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