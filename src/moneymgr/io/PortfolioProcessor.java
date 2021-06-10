package moneymgr.io;

import moneymgr.model.Account;
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.SecurityPortfolio;
import moneymgr.model.SecurityPosition;
import moneymgr.model.Statement;

/** Post-process portfolios after loading data */
public class PortfolioProcessor {
	public final MoneyMgrModel model;

	public PortfolioProcessor(MoneyMgrModel model) {
		this.model = model;
	}

	/** Update global portfolio and each account portfolio */
	public void fixPortfolios() {
		fixPortfolio(this.model.portfolio);

		for (Account a : this.model.getAccounts()) {
			if (a.isInvestmentAccount()) {
				fixPortfolio(a.securities);

				for (Statement stat : a.getStatements()) {
					fixPortfolio(stat.holdings);
				}
			}
		}
	}

	/** Update positions in a portfolio */
	private void fixPortfolio(SecurityPortfolio port) {
		for (SecurityPosition pos : port.getPositions()) {
			pos.updateShareBalances();
		}
	}
}