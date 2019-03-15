package qif.importer;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;

import qif.data.Account;
import qif.data.Common;
import qif.data.GenericTxn;
import qif.data.InvestmentTxn;
import qif.data.QPrice;
import qif.data.QifDom;
import qif.data.Security;
import qif.data.SecurityPortfolio;
import qif.data.SecurityPosition;
import qif.data.StockOption;

class SecurityProcessor {
	private QifDomReader qrdr;

	public SecurityProcessor(QifDomReader qrdr) {
		this.qrdr = qrdr;
	}

	public void loadSecurities() {
		for (;;) {
			String s = this.qrdr.getFileReader().peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			Security sec = loadSecurity();
			if (sec == null) {
				break;
			}

			Security existing = (sec.symbol != null) //
					? Security.findSecurityBySymbol(sec.symbol) //
					: Security.findSecurityByName(sec.getName());

			if (existing != null) {
				if (!existing.names.contains(sec.getName())) {
					existing.names.add(sec.getName());
				}
			} else {
				Security.addSecurity(sec);
			}
		}
	}

	private Security loadSecurity() {
		QFileReader.QLine qline = new QFileReader.QLine();

		String symbol = null;
		String name = null;
		String type = null;
		String goal = null;

		loop: for (;;) {
			this.qrdr.getFileReader().nextSecurityLine(qline);

			switch (qline.type) {
			case EndOfSection:
				break loop;

			case SecName:
				name = qline.value;
				break;
			case SecSymbol:
				symbol = qline.value;
				break;
			case SecType:
				type = qline.value;
				break;
			case SecGoal:
				goal = qline.value;
				break;

			default:
				Common.reportError("syntax error");
			}
		}

		if (symbol == null) {
			// Common.reportWarning("Security '" + name + //
			// "' does not specify a ticker symbol.");
		}

		final Security security = new Security(symbol);
		security.names.add(name);
		security.type = type;
		security.goal = goal;

		return security;
	}

	public void processSecurities() {
		processSecurities2(SecurityPortfolio.portfolio, GenericTxn.getAllTransactions());

		for (Account a : Account.getAccounts()) {
			if (a.isInvestmentAccount()) {
				processSecurities2(a.securities, a.transactions);
			}
		}
	}

	private void processSecurities2(SecurityPortfolio port, List<GenericTxn> txns) {
		for (final GenericTxn gtxn : txns) {
			if (!(gtxn instanceof InvestmentTxn) //
					|| (((InvestmentTxn) gtxn).security == null)) {
				continue;
			}

			InvestmentTxn txn = (InvestmentTxn) gtxn;

			SecurityPosition pos = port.getPosition(txn.security);
			pos.transactions.add(txn);

			switch (txn.getAction()) {
			case BUY:
			case SHRS_IN:
			case REINV_DIV:
			case REINV_LG:
			case REINV_SH:
			case BUYX:
			case REINV_INT:
			case SHRS_OUT:
			case SELL:
			case SELLX:
				pos.endingShares = pos.endingShares.add(txn.getShares());
				break;

			case GRANT:
			case VEST:
			case EXERCISE:
			case EXERCISEX:
			case EXPIRE:
				break;

			case STOCKSPLIT:
				StockOption.processSplit(txn);

				pos.endingShares = pos.endingShares.multiply(txn.getShares());
				pos.endingShares = pos.endingShares.divide(BigDecimal.TEN);
				break;

			case CASH:
			case DIV:
			case INT_INC:
			case MISC_INCX:
				break;

			default:
				break;
			}
		}
	}

	public void loadSecurityPriceHistory(File quoteDirectory) {
		if (!quoteDirectory.isDirectory()) {
			return;
		}

		final File quoteFiles[] = quoteDirectory.listFiles();

		for (final File f : quoteFiles) {
			String symbol = f.getName();
			symbol = symbol.replaceFirst(".csv", "");
			final Security sec = Security.findSecurityBySymbol(symbol);

			if (sec != null) {
				new QQuoteLoader().loadQuoteFile(sec, f);
			}
		}

		for (Security sec : Security.getSecurities()) {
			String symbol = sec.getSymbol();

			if (symbol != null) {
				int warningCount = 0;
				Common.reportInfo("Comparing price history for " + symbol);

				List<QPrice> prices = QuoteDownloader.loadPriceHistory(symbol);

				if (prices != null) {
					for (QPrice price : prices) {
						QPrice secprice = sec.getPriceForDate(price.date);

						if (secprice == null) {
							sec.addPrice(price, false);
						} else if (price.compareTo(secprice) != 0) {
							sec.addPrice(price, true);
							++warningCount;
						}
					}

					if (QifDom.verbose && (warningCount > 0)) {
						Common.reportWarning( //
								"Security price mismatches for " + symbol + ":" //
										+ Integer.toString(warningCount));
					}
				}
			}
		}
	}

	public void loadPrices() {
		for (;;) {
			String s = this.qrdr.getFileReader().peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			QPrice price = QPrice.load(this.qrdr.getFileReader());
			if (price == null) {
				break;
			}

			Security sec = Security.getSecurity(price.secid);
			if (sec != null) {
				sec.addPrice(price, true);
			}
		}
	}
}