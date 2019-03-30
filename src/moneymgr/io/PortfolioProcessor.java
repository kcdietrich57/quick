package moneymgr.io;

import java.math.BigDecimal;

import moneymgr.model.Account;
import moneymgr.model.InvestmentTxn;
import moneymgr.model.SecurityPortfolio;
import moneymgr.model.SecurityPosition;
import moneymgr.model.TxAction;

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
			fixPosition(pos);
		}
	}

	/** Update running share totals in a position */
	private static void fixPosition(SecurityPosition p) {
		BigDecimal shrbal = BigDecimal.ZERO;
		p.shrBalance.clear();

		for (InvestmentTxn t : p.transactions) {
			if (t.getAction() == TxAction.STOCKSPLIT) {
				shrbal = shrbal.multiply(t.getSplitRatio());
			} else if (t.getShares() != null) {
				shrbal = shrbal.add(t.getShares());
			}

			p.shrBalance.add(shrbal);
		}
	}
}