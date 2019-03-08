package qif.importer;

import java.math.BigDecimal;

import qif.data.Account;
import qif.data.InvestmentTxn;
import qif.data.SecurityPortfolio;
import qif.data.SecurityPosition;
import qif.data.TxAction;

class PortfolioProcessor {
	// private QifDomReader qrdr;

	public PortfolioProcessor(QifDomReader qrdr) {
		// this.qrdr = qrdr;
	}

	public void fixPortfolios() {
		fixPortfolio(SecurityPortfolio.portfolio);

		for (Account a : Account.getAccounts()) {
			if (a.isInvestmentAccount()) {
				fixPortfolio(a.securities);
			}
		}
	}

	private void fixPortfolio(SecurityPortfolio port) {
		for (final SecurityPosition pos : port.positions) {
			fixPosition(pos);
		}
	}

	private void fixPosition(SecurityPosition p) {
		BigDecimal shrbal = BigDecimal.ZERO;
		p.shrBalance.clear();

		for (final InvestmentTxn t : p.transactions) {
			if (t.getAction() == TxAction.STOCKSPLIT) {
				shrbal = shrbal.multiply(t.getSplitRatio());
			} else if (t.getShares() != null) {
				shrbal = shrbal.add(t.getShares());
			}

			p.shrBalance.add(shrbal);
		}
	}
}