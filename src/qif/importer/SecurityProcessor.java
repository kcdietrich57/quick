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

/** Load securities and set up security and lot details afterwards. */
class SecurityProcessor {
	private final QifDomReader qrdr;

	public SecurityProcessor(QifDomReader qrdr) {
		this.qrdr = qrdr;
	}

	/** Process securities section of an input file and create security objects */
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

	/** Load an individual security from the input file */
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
				Common.reportError("Unknown security field: " + qline.value);
			}
		}

		if (name == null) {
			Common.reportError("No name or symbol for security");
		}

		if (QifDom.verbose && (symbol == null)) {
			Common.reportWarning("No ticker symbol for '" + name + "'");
		}

		return new Security(symbol, name, type, goal);
	}

	/** Process security txns (global, accounts) after loading from QIF */
	public void processSecurities() {
		// Process global porfolio info
		processSecurities2(SecurityPortfolio.portfolio, GenericTxn.getAllTransactions());

		// Process holdings for each account
		for (Account a : Account.getAccounts()) {
			if (a.isInvestmentAccount()) {
				processSecurities2(a.securities, a.transactions);
			}
		}
	}

	/**
	 * Process transactions for securities in a portfolio.<br>
	 * Add transactions to positions appropriately.<br>
	 * Add share balance to positions.<br>
	 * Process splits along the way.
	 */
	private void processSecurities2(SecurityPortfolio port, List<GenericTxn> txns) {
		for (GenericTxn gtxn : txns) {
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
				pos.endingShares = pos.endingShares.add(txn.getShares());
				break;

			case SHRS_OUT:
			case SELL:
			case SELLX:
				pos.endingShares = pos.endingShares.add(txn.getShares());
				break;

			case STOCKSPLIT:
				StockOption.processSplit(txn);

				pos.endingShares = pos.endingShares.multiply(txn.getShares());
				pos.endingShares = pos.endingShares.divide(BigDecimal.TEN);
				break;

			case GRANT:
			case VEST:
			case EXERCISE:
			case EXERCISEX:
			case EXPIRE:
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

	/** Load quotes from CSV input files in a specified directory */
	public void loadSecurityPriceHistory(File quoteDirectory) {
		if (!quoteDirectory.isDirectory()) {
			return;
		}

		File quoteFiles[] = quoteDirectory.listFiles();

		// Load saved quote data
		for (File f : quoteFiles) {
			String symbol = f.getName().replaceFirst(".csv", "");
			Security sec = Security.findSecurityBySymbol(symbol);

			if (sec != null) {
				QQuoteLoader.loadQuoteFile(sec, f);
			}
		}

		// Download quotes from service
		for (Security sec : Security.getSecurities()) {
			String symbol = sec.getSymbol();

			if (symbol != null) {
				int warningCount = 0;
				Common.reportInfo("Comparing price history for " + symbol);

				List<QPrice> prices = QuoteDownloader.loadPriceHistory(symbol);

				if (prices != null) {
					for (QPrice price : prices) {
						if (!sec.addPrice(price)) {
							++warningCount;
						}
					}

					if (QifDom.verbose && (warningCount > 0)) {
						Common.reportWarning( //
								"Security prices replaced for " + symbol + ":" //
										+ Integer.toString(warningCount));
					}
				}
			}
		}
	}

	/** Load quotes from a QIF input file */
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
				sec.addPrice(price);
			}
		}
	}
}