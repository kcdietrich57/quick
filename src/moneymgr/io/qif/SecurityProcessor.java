package moneymgr.io.qif;

import java.io.File;
import java.util.List;

import app.QifDom;
import moneymgr.io.QQuoteLoader;
import moneymgr.io.QuoteDownloader;
import moneymgr.model.Account;
import moneymgr.model.GenericTxn;
import moneymgr.model.InvestmentTxn;
import moneymgr.model.QPrice;
import moneymgr.model.Security;
import moneymgr.model.SecurityPortfolio;
import moneymgr.model.SecurityPosition;
import moneymgr.model.SimpleTxn;
import moneymgr.model.StockOption;
import moneymgr.model.TxAction;
import moneymgr.util.Common;

/** Load securities and set up security and lot details afterwards. */
public class SecurityProcessor {
	private final QifDomReader qrdr;
	private final QuoteDownloader quoteDownloader;
	private final QQuoteLoader quoteLoader;

	public SecurityProcessor(QifDomReader qrdr) {
		this.qrdr = qrdr;
		this.quoteDownloader = new QuoteDownloader(qrdr.model);
		this.quoteLoader = new QQuoteLoader(qrdr.model);
	}

	/** Process securities section of an input file and create security objects */
	public void loadSecurities() {
		for (;;) {
			String s = this.qrdr.getFileReader().peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			Security sec = loadSecurity(this.qrdr);
			if (sec == null) {
				break;
			}

			Security existing = (sec.symbol != null) //
					? this.qrdr.model.findSecurityBySymbol(sec.symbol) //
					: this.qrdr.model.findSecurityByName(sec.getName());

			if (existing != null) {
				if (!existing.names.contains(sec.getName())) {
					existing.names.add(sec.getName());
				}
			} else {
				this.qrdr.model.addSecurity(sec);
			}
		}
	}

	/** Load an individual security from the input file */
	private Security loadSecurity(QifDomReader qrdr) {
		QFileReader.QLine qline = new QFileReader.QLine();

		String symbol = null;
		String name = null;
		String type = null;
		String goal = null;

		loop: for (;;) {
			qrdr.getFileReader().nextSecurityLine(qline);

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
		processAllSecurities2(this.qrdr.model.portfolio, //
				this.qrdr.model.getAllTransactions());

		// Process holdings for each account
		for (Account a : this.qrdr.model.getAccounts()) {
			if (a.isInvestmentAccount()) {
				processAccountSecurities2(a.securities, a.getTransactions());
			}
		}
	}

	/**
	 * TODO processSecurities2 - this can basically go away<br>
	 * Process transactions for securities in a portfolio.<br>
	 * Add transactions to positions appropriately.<br>
	 * Add share balance to positions.<br>
	 * Process splits along the way.
	 */
	private void processAllSecurities2(SecurityPortfolio port, List<SimpleTxn> txns) {
		for (SimpleTxn stxn : txns) {
			if ((stxn != null) && (stxn.getSecurity() != null)) {
				processSecurities2(port, (InvestmentTxn) stxn);
			}
		}
	}

	private void processAccountSecurities2(SecurityPortfolio port, List<GenericTxn> txns) {
		for (SimpleTxn stxn : txns) {
			if ((stxn != null) && (stxn.getSecurity() != null)) {
				processSecurities2(port, (InvestmentTxn) stxn);
			}
		}
	}

	private void processSecurities2(SecurityPortfolio port, InvestmentTxn txn) {
		if (txn.getAction() == TxAction.STOCKSPLIT) {
			// TODO processSplit() - only keep one split tx, not one per acct
			StockOption.processSplit(txn);
		}

		SecurityPosition pos = port.getPosition(txn.getSecurity());
		pos.addTransaction(txn);
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
			Security sec = this.qrdr.model.findSecurityBySymbol(symbol);

			if (sec != null) {
				this.quoteLoader.loadQuoteFile(sec, f);
			}
		}

		// Download quotes from service
		for (Security sec : this.qrdr.model.getSecurities()) {
			String symbol = sec.getSymbol();

			if (symbol != null) {
				int warningCount = 0;
				Common.debugInfo("Loading/comparing price history for " + symbol);

				List<QPrice> prices = this.quoteDownloader.loadPriceHistory(symbol);

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

			Security sec = this.qrdr.model.getSecurity(price.secid);
			if (sec != null) {
				sec.addPrice(price);
			}
		}
	}
}