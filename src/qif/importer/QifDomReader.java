package qif.importer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

import qif.data.Account;
import qif.data.AccountType;
import qif.data.Category;
import qif.data.Common;
import qif.data.GenericTxn;
import qif.data.InvestmentTxn;
import qif.data.MultiSplitTxn;
import qif.data.NonInvestmentTxn;
import qif.data.QDate;
import qif.data.QPrice;
import qif.data.QifDom;
import qif.data.Security;
import qif.data.Security.SplitInfo;
import qif.data.SecurityPortfolio;
import qif.data.SecurityPosition;
import qif.data.SimpleTxn;
import qif.data.Statement;
import qif.data.StockOption;
import qif.data.TxAction;
import qif.importer.QFileReader.SectionType;
import qif.persistence.Reconciler;

public class QifDomReader {
	private File curFile = null;
	private QFileReader filerdr = null;
	private File qifDir;

	public static void loadDom(String[] qifFiles) {
		QifDom.qifDir = new File(qifFiles[0]).getParentFile();

		final QifDomReader rdr = new QifDomReader(QifDom.qifDir);

		// Process all the QIF files
		for (final String fn : qifFiles) {
			rdr.load(fn, true);
		}

		// Additional processing once the data is loaded (quotes, stmts, etc)
		rdr.postLoad();
	}

	public QifDomReader(File qifDir) {
		this.qifDir = qifDir;
	}

	public QFileReader getFileReader() {
		return this.filerdr;
	}

	public void load(String fileName, boolean doCleanup) {
		if (!new File(fileName).exists()) {
			if (new File("c:" + fileName).exists()) {
				fileName = "c:" + fileName;
			} else {
				Common.reportError("Input file '" + fileName + "' does not exist");
				return;
			}
		}

		init(fileName);

		processFile();

		if (doCleanup) {
			new Cleaner(this).cleanUpTransactions();
		}
	}

	private void postLoad() {
		final File d = new File(this.qifDir, "quotes");
		new SecurityProcessor(this).loadSecurityPriceHistory(d);

		new OptionsProcessor().processStockOptions();
		new SecurityProcessor(this).processSecurities();
		new OptionsProcessor().processOptions();
		new PortfolioProcessor(this).fixPortfolios();

		// TODO need to match up lots with transactions
		for (Account acct : Account.accounts) {
			if (!acct.isInvestmentAccount() || acct.securities.isEmpty()) {
				continue;
			}

		}

		final File dd = new File(this.qifDir, "statements");
		new StatementProcessor(this).processStatementFiles(dd);

		// Process saved statement reconciliation information
		Reconciler.processStatementLog();

		// Update statement reconciliation file if format has changed
		if (QifDom.loadedStatementsVersion != StatementDetails.CURRENT_VERSION) {
			Reconciler.rewriteStatementLogFile();
		}
	}

	private void init(String filename) {
		File f = new File(filename);
		if (!f.exists()) {
			Common.reportError("File '" + filename + "' does not exist");
		}

		this.curFile = f;
		this.filerdr = new QFileReader(f);
	}

	private void processFile() {
		this.filerdr.reset();

		for (SectionType sectype = this.filerdr.findFirstSection(); //
				sectype != SectionType.EndOfFile; //
				sectype = this.filerdr.nextSection()) {
			switch (sectype) {
			case Tag:
			case Category:
				new CategoryProcessor(this).loadCategories();
				break;

			case Account:
				new AccountProcessor(this).loadAccounts();
				break;

			case Asset:
			case Liability:
			case Cash:
			case CreditCard:
			case Bank:
				new TransactionProcessor(this).loadNonInvestmentTransactions();
				break;

			case Investment:
				new TransactionProcessor(this).loadInvestmentTransactions();
				break;

			case Statements:
				new StatementProcessor(this).loadStatements(this.curFile);
				break;

			case Security:
				new SecurityProcessor(this).loadSecurities();
				break;

			case Prices:
				new SecurityProcessor(this).loadPrices();
				break;

			case QClass:
				Common.reportError("TODO not implemented");
				break;

			case MemorizedTransaction:
				Common.reportError("TODO not implemented");
				break;

			default:
				break;
			}
		}
	}
}

class QQuoteLoader {
	// private QifDomReader qrdr;

	public QQuoteLoader(QifDomReader qrdr) {
		// this.qrdr = qrdr;
	}

	public void loadQuoteFile(Security sec, File f) {
		if (!f.getName().endsWith(".csv")) {
			return;
		}

		final List<QPrice> prices = sec.prices;
		final List<SplitInfo> splits = sec.splits;

		assert prices.isEmpty() && splits.isEmpty();
		prices.clear();
		splits.clear();

		FileReader fr;
		String line;
		LineNumberReader rdr;

		boolean isSplitAdjusted = false;
		boolean isWeekly = false;
		boolean dateprice = false;
		boolean chlvd = false;
		boolean dohlcv = false;

		QDate splitDate = null;

		try {
			fr = new FileReader(f);

			rdr = new LineNumberReader(fr);

			line = rdr.readLine();

			while (line != null) {
				boolean isHeader = false;

				if (line.startsWith("split adjusted")) {
					isSplitAdjusted = true;
					isHeader = true;
				} else if (line.startsWith("weekly")) {
					isWeekly = true;
					isHeader = true;
				} else if (line.startsWith("date")) {
					chlvd = false;
					dohlcv = false;
					dateprice = true;
					isHeader = true;
				} else if (line.startsWith("price")) {
					chlvd = false;
					dohlcv = false;
					dateprice = false;
					isHeader = true;
				} else if (line.startsWith("chlvd")) {
					chlvd = true;
					dohlcv = false;
					dateprice = false;
					isHeader = true;
				} else if (line.startsWith("dohlcv")) {
					chlvd = false;
					dohlcv = true;
					dateprice = false;
					isHeader = true;
				} else if (line.startsWith("split")) {
					final String[] ss = line.split(" ");
					int ssx = 1;

					final String newshrStr = ss[ssx++];
					final String oldshrStr = ss[ssx++];
					final String dateStr = ss[ssx++];

					final BigDecimal splitAdjust = new BigDecimal(newshrStr).divide(new BigDecimal(oldshrStr));
					splitDate = Common.parseQDate(dateStr);

					final SplitInfo si = new SplitInfo();
					si.splitDate = splitDate;
					si.splitRatio = splitAdjust;

					splits.add(si);
					Collections.sort(splits, (o1, o2) -> o1.splitDate.compareTo(o2.splitDate));

					isHeader = true;
				}

				if (isHeader) {
					line = rdr.readLine();
					continue;
				}

				final String[] ss = line.split(",");
				int ssx = 0;

				String pricestr;
				String datestr;

				if (chlvd) {
					pricestr = ss[ssx++];
					++ssx;
					++ssx;
					++ssx;
					datestr = ss[ssx++];
				} else if (dohlcv) {
					datestr = ss[ssx++];
					++ssx;
					++ssx;
					++ssx;
					pricestr = ss[ssx++];
				} else if (dateprice) {
					datestr = ss[ssx++];
					pricestr = ss[ssx++];
				} else {
					pricestr = ss[ssx++];
					datestr = ss[ssx++];
				}

				QDate date = Common.parseQDate(datestr);
				BigDecimal price = null;
				try {
					price = new BigDecimal(pricestr);
				} catch (final Exception e) {
					e.printStackTrace();
					Common.reportError("Invalid price in quote file");
				}

				final QPrice p = new QPrice();

				if (isWeekly) {
					// I am suspicious of this - go to middle of the week?
					date = date.addDays(4);
				}

				p.date = date;

				final BigDecimal splitRatio = sec.getSplitRatioForDate(date);

				if (isSplitAdjusted) {
					p.setSplitAdjustedPrice(price, price.multiply(splitRatio));
				} else {
					p.setSplitAdjustedPrice(price.divide(splitRatio), price);
				}

				prices.add(p);

				line = rdr.readLine();
			}

			rdr.close();
		} catch (final Exception e) {
			e.printStackTrace();
			return;
		}

		Collections.sort(prices, (o1, o2) -> o1.date.compareTo(o2.date));
	}
}

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
				new QQuoteLoader(qrdr).loadQuoteFile(sec, f);
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

			Security sec = Security.findSecurityBySymbol(price.symbol);
			if (sec != null) {
				sec.addPrice(price, true);
			}
		}
	}
}

class OptionsProcessor {
	public void processStockOptions() {
		LineNumberReader rdr = null;

		try {
			File optfile = new File(QifDom.qifDir, "options.txt");
			assert optfile.isFile() && optfile.canRead();

			rdr = new LineNumberReader(new FileReader(optfile));

			String line = rdr.readLine();
			while (line != null) {
				line = line.trim();
				if (line.isEmpty() || line.charAt(0) == '#') {
					line = rdr.readLine();
					continue;
				}

				StringTokenizer toker = new StringTokenizer(line, " ");

				String datestr = toker.nextToken();
				QDate date = Common.parseQDate(datestr);
				String op = toker.nextToken();
				String name = toker.nextToken();

				if (op.equals("GRANT")) {
					// 05/23/91 GRANT 2656 ASCL ISI_Options 500 6.00 Y 4 10y
					String secname = toker.nextToken();
					Security sec = Security.findSecurity(secname);
					String acctname = toker.nextToken().replaceAll("_", " ");
					Account acct = Account.findAccount(acctname);
					BigDecimal shares = new BigDecimal(toker.nextToken());
					BigDecimal price = new BigDecimal(toker.nextToken());
					String vestPeriod = toker.nextToken();
					int vestPeriodMonths = (vestPeriod.charAt(0) == 'Y') ? 12 : 3;
					int vestCount = Integer.parseInt(toker.nextToken());

					StockOption opt = StockOption.grant(name, date, //
							acct.acctid, sec.secid, //
							shares, price, vestPeriodMonths, vestCount, 0);
					Common.reportInfo("Granted: " + opt.toString());
				} else if (op.equals("VEST")) {
					// 05/23/92 VEST 2656 1
					int vestNumber = Integer.parseInt(toker.nextToken());

					StockOption opt = StockOption.vest(name, date, vestNumber);
					Common.reportInfo("Vested: " + opt.toString());

				} else if (op.equals("SPLIT")) {
					// 09/16/92 SPLIT 2656 2 1 [1000/3.00]
					int newShares = Integer.parseInt(toker.nextToken());
					int oldShares = Integer.parseInt(toker.nextToken());

					StockOption opt = StockOption.split(name, date, newShares, oldShares);
					Common.reportInfo("Split: " + opt.toString());
				} else if (op.equals("EXPIRE")) {
					// 05/23/01 EXPIRE 2656
					StockOption opt = StockOption.expire(name, date);
					if (opt != null) {
						Common.reportInfo("Expire: " + opt.toString());
					}
				} else if (op.equals("CANCEL")) {
					// 05/23/01 CANCEL 2656
					StockOption opt = StockOption.cancel(name, date);
					if (opt != null) {
						Common.reportInfo("Cancel: " + opt.toString());
					}
				} else if (op.equals("EXERCISE")) {
					// 09/19/95 EXERCISE 2656 2000 32.75
					BigDecimal shares = new BigDecimal(toker.nextToken());
					// BigDecimal price = new BigDecimal(toker.nextToken());

					StockOption opt = StockOption.exercise(name, date, shares);
					Common.reportInfo("Exercise: " + opt.toString());
				}

				line = rdr.readLine();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (rdr != null) {
				try {
					rdr.close();
				} catch (IOException e) {
				}
			}
		}

		List<StockOption> openOptions = StockOption.getOpenOptions();
		Common.reportInfo("\nOpen Stock Options:\n" + openOptions.toString());
	}

	public void processOptions() {
		processOptions(// SecurityPortfolio.portfolio,
				GenericTxn.getAllTransactions());

		for (Account a : Account.getAccounts()) {
			if (a.isInvestmentAccount()) {
				processOptions(// a.securities,
						a.transactions);
			}
		}
	}

	private void processOptions(// SecurityPortfolio port,
			List<GenericTxn> txns) {
		for (final GenericTxn gtxn : txns) {
			if (!(gtxn instanceof InvestmentTxn) //
					|| (((InvestmentTxn) gtxn).security == null)) {
				continue;
			}

			InvestmentTxn txn = (InvestmentTxn) gtxn;

			// SecurityPosition pos = port.getPosition(txn.security);
			// pos.transactions.add(txn);

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
				// pos.shares = pos.shares.add(txn.getShares());
				break;

			case GRANT:
				StockOption.processGrant(txn);
				break;

			case VEST:
				StockOption.processVest(txn);
				break;

			case EXERCISE:
			case EXERCISEX:
				StockOption.processExercise(txn);
				break;

			case EXPIRE:
				StockOption.processExpire(txn);
				break;

			case STOCKSPLIT:
				// StockOption.processSplit(txn);
				//
				// pos.shares = pos.shares.multiply(txn.getShares());
				// pos.shares = pos.shares.divide(BigDecimal.TEN);
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
}

class Cleaner {
	private QifDomReader qrdr;

	// Housekeeping info while processing related transfer transactions.
	// The imported data does not connect transfers, so we need to do it.
	// These keep track of successful and unsuccessful attempts to connect
	// transfers.
	private static final List<SimpleTxn> matchingTxns = new ArrayList<SimpleTxn>();
	private static int totalXfers = 0;
	private static int failedXfers = 0;

	public Cleaner(QifDomReader qrdr) {
		this.qrdr = qrdr;
	}

	public void cleanUpTransactions() {
		// TODO transactions should already be sorted?
		sortAccountTransactionsByDate();
		cleanUpSplits();
		calculateRunningTotals();
		connectTransfers();
		connectSecurityTransfers();
		new LotProcessor().setupSecurityLots();
	}

	private void sortAccountTransactionsByDate() {
		for (Account a : Account.getAccounts()) {
			Common.sortTransactionsByDate(a.transactions);
		}
	}

	private void cleanUpSplits() {
		for (Account a : Account.getAccounts()) {
			for (GenericTxn txn : a.transactions) {
				massageSplits(txn);
			}
		}
	}

	// This handles the case where we have multiple splits that involve
	// transferring from another account. The other account may have a single
	// entry that corresponds to more than one split in the other account.
	// N.B. Alternatively, we could merge the splits into one.
	private void massageSplits(GenericTxn txn) {
		if (!(txn instanceof NonInvestmentTxn)) {
			return;
		}

		final NonInvestmentTxn nitxn = (NonInvestmentTxn) txn;

		for (int ii = 0; ii < nitxn.split.size(); ++ii) {
			final SimpleTxn stxn = nitxn.split.get(ii);
			if (stxn.getCatid() >= 0) {
				continue;
			}

			MultiSplitTxn mtxn = null;

			for (int jj = ii + 1; jj < nitxn.split.size(); ++jj) {
				final SimpleTxn stxn2 = nitxn.split.get(jj);

				if (stxn.getCatid() == stxn2.getCatid()) {
					if (mtxn == null) {
						mtxn = new MultiSplitTxn(txn.acctid);
						nitxn.split.set(ii, mtxn);

						mtxn.setAmount(stxn.getAmount());
						mtxn.setCatid(stxn.getCatid());
						mtxn.subsplits.add(stxn);
					}

					mtxn.setAmount(mtxn.getAmount().add(stxn2.getAmount()));
					mtxn.subsplits.add(stxn2);

					nitxn.split.remove(jj);
					--jj;
				}
			}
		}
	}

	private void calculateRunningTotals() {
		for (Account a : Account.getAccounts()) {
			a.clearedBalance = a.balance = BigDecimal.ZERO;

			for (GenericTxn t : a.transactions) {
				BigDecimal amt = t.getCashAmount();

				if (!amt.equals(BigDecimal.ZERO)) {
					a.balance = a.balance.add(amt);
					t.runningTotal = a.balance;

					if (t.isCleared()) {
						a.clearedBalance = a.clearedBalance.add(amt);
					}
				}
			}
		}
	}

	private void connectTransfers() {
		for (Account a : Account.getAccounts()) {
			for (GenericTxn txn : a.transactions) {
				connectTransfers(txn);
			}
		}
	}

	private void connectTransfers(GenericTxn txn) {
		if ((txn instanceof NonInvestmentTxn) && !((NonInvestmentTxn) txn).split.isEmpty()) {
			for (final SimpleTxn stxn : ((NonInvestmentTxn) txn).split) {
				connectTransfers(stxn, txn.getDate());
			}
		} else if ((txn.getCatid() < 0) && //
		// opening balance shows up as xfer to same acct
				(-txn.getCatid() != txn.acctid)) {
			connectTransfers(txn, txn.getDate());
		}
	}

	private void connectTransfers(SimpleTxn txn, QDate date) {
		// Opening balance appears as a transfer to the same acct
		if ((txn.getCatid() >= 0) || (txn.getCatid() == -txn.getXferAcctid())) {
			return;
		}

		final Account a = Account.getAccountByID(-txn.getCatid());

		findMatchesForTransfer(a, txn, date, true);

		++totalXfers;

		if (matchingTxns.isEmpty()) {
			findMatchesForTransfer(a, txn, date, false); // SellX openingBal void
		}

		if (matchingTxns.isEmpty()) {
			++failedXfers;

			Common.reportInfo("match not found for xfer: " + txn);
			Common.reportInfo("  " + failedXfers + " of " + totalXfers + " failed");

			return;
		}

		SimpleTxn xtxn = null;
		if (matchingTxns.size() == 1) {
			xtxn = matchingTxns.get(0);
		} else {
			Common.reportWarning("Multiple matching transactions - using the first one.");
			xtxn = matchingTxns.get(0);
		}

		txn.setXtxn(xtxn);
		xtxn.setXtxn(txn);
	}

	private void findMatchesForTransfer(Account acct, SimpleTxn txn, QDate date, boolean strict) {
		matchingTxns.clear();

		final int idx = GenericTxn.getLastTransactionIndexOnOrBeforeDate(acct.transactions, date);
		if (idx < 0) {
			return;
		}

		boolean datematch = false;

		for (int inc = 0; datematch || (inc < 10); ++inc) {
			datematch = false;

			if (idx + inc < acct.transactions.size()) {
				final GenericTxn gtxn = acct.transactions.get(idx + inc);
				datematch = date.equals(gtxn.getDate());

				final SimpleTxn match = checkMatchForTransfer(txn, gtxn, strict);
				if (match != null) {
					matchingTxns.add(match);
				}
			}

			if (inc > 0 && idx >= inc) {
				final GenericTxn gtxn = acct.transactions.get(idx - inc);
				datematch = datematch || date.equals(gtxn.getDate());

				final SimpleTxn match = checkMatchForTransfer(txn, gtxn, strict);
				if (match != null) {
					matchingTxns.add(match);
				}
			}
		}
	}

	private SimpleTxn checkMatchForTransfer(SimpleTxn txn, GenericTxn gtxn, boolean strict) {
		assert -txn.getCatid() == gtxn.acctid;

		if (!gtxn.hasSplits()) {
			if ((gtxn.getXferAcctid() == txn.acctid) //
					&& gtxn.amountIsEqual(txn, strict)) {
				return gtxn;
			}
		} else {
			for (final SimpleTxn splitxn : gtxn.getSplits()) {
				if ((splitxn.getXferAcctid() == txn.acctid) //
						&& splitxn.amountIsEqual(txn, strict)) {
					return splitxn;
				}
			}
		}

		return null;
	}

	private void connectSecurityTransfers() {
		List<InvestmentTxn> xins = new ArrayList<InvestmentTxn>();
		List<InvestmentTxn> xouts = new ArrayList<InvestmentTxn>();

		for (GenericTxn txn : GenericTxn.getAllTransactions()) {
			if (txn instanceof InvestmentTxn) {
				if ((txn.getAction() == TxAction.SHRS_IN)) {
					xins.add((InvestmentTxn) txn);
				} else if (txn.getAction() == TxAction.SHRS_OUT) {
					xouts.add((InvestmentTxn) txn);
				}
			}
		}

		connectSecurityTransfers(xins, xouts);
	}

	private void connectSecurityTransfers(List<InvestmentTxn> xins, List<InvestmentTxn> xouts) {
		Comparator<InvestmentTxn> cpr = (o1, o2) -> {
			int diff;

			diff = o1.getDate().compareTo(o2.getDate());
			if (diff != 0) {
				return diff;
			}

			diff = o1.security.getName().compareTo(o2.security.getName());
			if (diff != 0) {
				return diff;
			}

			diff = o1.getShares().compareTo(o2.getShares());
			if (diff != 0) {
				return diff;
			}

			return o2.getAction().ordinal() - o1.getAction().ordinal();
		};

		List<InvestmentTxn> txns = new ArrayList<InvestmentTxn>(xins);
		txns.addAll(xouts);
		Collections.sort(txns, cpr);

		Collections.sort(xins, cpr);
		Collections.sort(xouts, cpr);

		List<InvestmentTxn> ins = new ArrayList<InvestmentTxn>();
		List<InvestmentTxn> outs = new ArrayList<InvestmentTxn>();
		List<InvestmentTxn> unmatched = new ArrayList<InvestmentTxn>();

		BigDecimal inshrs;
		BigDecimal outshrs;

		while (!xins.isEmpty()) {
			ins.clear();
			outs.clear();

			InvestmentTxn t = xins.get(0);

			inshrs = gatherTransactionsForSecurityTransfer(ins, xins, null, t.security, t.getDate());
			outshrs = gatherTransactionsForSecurityTransfer(outs, xouts, unmatched, t.security, t.getDate());

			if (outs.isEmpty()) {
				unmatched.addAll(ins);
			} else {
				BigDecimal inshrs2 = inshrs.setScale(3, RoundingMode.HALF_UP);
				BigDecimal outshrs2 = outshrs.setScale(3, RoundingMode.HALF_UP);

				if (inshrs2.abs().compareTo(outshrs2.abs()) != 0) {
					Common.reportError("Mismatched security transfer");
				}

				for (InvestmentTxn inTx : ins) {
					inTx.xferTxns = new ArrayList<InvestmentTxn>(outs);
				}
				for (InvestmentTxn outTx : outs) {
					outTx.xferTxns = new ArrayList<InvestmentTxn>(ins);
				}
			}

			if (QifDom.verbose && !outs.isEmpty()) {
				String s = String.format(//
						"%-20s : %10s %8s %8s INSH=%10s (%3d txns) OUTSH=%10s (%d txns)", //
						t.getAccount().getName(), //
						t.getAction().toString(), //
						t.security.symbol, //
						t.getDate().toString(), //
						Common.formatAmount3(inshrs), ins.size(), //
						Common.formatAmount3(outshrs), outs.size());
				Common.reportInfo(s);
			}
		}

		if (QifDom.verbose && !unmatched.isEmpty()) {
			for (final InvestmentTxn t : unmatched) {
				String pad = (t.getAction() == TxAction.SHRS_IN) //
						? "" //
						: "           ";

				String s = String.format("%-20s : %10s %8s %8s SHR=%s%10s", //
						t.getAccount().getName(), //
						t.getAction().toString(), //
						t.security.symbol, //
						t.getDate().toString(), //
						pad, //
						Common.formatAmount3(t.getShares()));
				Common.reportInfo(s);
			}
		}
	}

	private BigDecimal gatherTransactionsForSecurityTransfer( //
			List<InvestmentTxn> rettxns, // OUT txs for xfer
			List<InvestmentTxn> srctxns, // IN all remaining txs
			List<InvestmentTxn> unmatched, // OUT earlier txs that don't match
			Security s, // The security being transferred
			QDate d) { // The date of the transfer
		// Return number of shares collected

		BigDecimal numshrs = BigDecimal.ZERO;

		if (srctxns.isEmpty()) {
			return numshrs;
		}

		InvestmentTxn t = srctxns.get(0);

		// Skip earlier Txs, gathering in unmatched
		while (t.getDate().compareTo(d) < 0 || //
				(t.getDate().equals(d) //
						&& (t.security.getName().compareTo(s.getName()) < 0))) {
			unmatched.add(srctxns.remove(0));
			if (srctxns.isEmpty()) {
				break;
			}

			t = srctxns.get(0);
		}

		// Processing matching txs
		while (!srctxns.isEmpty()) {
			t = srctxns.get(0);

			if ((t.security != s) || //
					(t.getDate().compareTo(d) != 0)) {
				break;
			}

			rettxns.add(t);
			numshrs = numshrs.add(t.getShares());

			srctxns.remove(0);
		}

		return numshrs;
	}
}

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

class StatementProcessor {
	private QifDomReader qrdr;

	public StatementProcessor(QifDomReader qrdr) {
		this.qrdr = qrdr;
	}

	public void loadStatements(File file) {
		for (;;) {
			String s = this.qrdr.getFileReader().peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			List<Statement> stmts = loadStatementsSection(this.qrdr.getFileReader());
			for (Statement stmt : stmts) {
				Account.currAccount.statements.add(stmt);
				Account.currAccount.statementFile = file;
			}
		}
	}

	private List<Statement> loadStatementsSection(QFileReader qfr) {
		final QFileReader.QLine qline = new QFileReader.QLine();
		final List<Statement> stmts = new ArrayList<Statement>();

		Statement currstmt = null;

		for (;;) {
			qfr.nextStatementsLine(qline);

			switch (qline.type) {
			case EndOfSection:
				return stmts;

			case StmtsAccount: {
				final String aname = qline.value;
				final Account a = Account.findAccount(aname);
				if (a == null) {
					Common.reportError("Can't find account: " + aname);
				}

				Account.currAccount = a;
				currstmt = null;
				break;
			}

			case StmtsMonthly: {
				final String[] ss = qline.value.split(" ");
				int ssx = 0;

				final String datestr = ss[ssx++];
				final int slash1 = datestr.indexOf('/');
				final int slash2 = (slash1 < 0) ? -1 : datestr.indexOf('/', slash1 + 1);
				int day = 0; // last day of month
				int month = 1;
				int year = 0;
				if (slash2 > 0) {
					month = Integer.parseInt(datestr.substring(0, slash1));
					day = Integer.parseInt(datestr.substring(slash1 + 1, slash2));
					year = Integer.parseInt(datestr.substring(slash2 + 1));
				} else if (slash1 >= 0) {
					month = Integer.parseInt(datestr.substring(0, slash1));
					year = Integer.parseInt(datestr.substring(slash1 + 1));
				} else {
					year = Integer.parseInt(datestr);
				}

				while (ssx < ss.length) {
					if (month > 12) {
						Common.reportError( //
								"Statements month wrapped to next year:\n" //
										+ qline.value);
					}

					String balStr = ss[ssx++];
					if (balStr.equals("x")) {
						balStr = "0.00";
					}

					final BigDecimal bal = new BigDecimal(balStr);
					final QDate d = (day == 0) //
							? QDate.getDateForEndOfMonth(year, month) //
							: new QDate(year, month, day);

					final Statement prevstmt = (stmts.isEmpty() ? null : stmts.get(stmts.size() - 1));

					currstmt = new Statement(Account.currAccount.acctid);
					currstmt.date = d;
					currstmt.closingBalance = currstmt.cashBalance = bal;
					if ((prevstmt != null) && (prevstmt.acctid == currstmt.acctid)) {
						currstmt.prevStatement = prevstmt;
					}

					stmts.add(currstmt);

					++month;
				}

				break;
			}

			case StmtsCash:
				currstmt.cashBalance = Common.parseDecimal(qline.value);
				break;

			case StmtsSecurity: {
				final String[] ss = qline.value.split(";");
				int ssx = 0;

				// S<SYM>;[<order>;]QTY;VALUE;PRICE
				final String secStr = ss[ssx++];

				String ordStr = ss[ssx];
				if ("qpv".indexOf(ordStr.charAt(0)) < 0) {
					ordStr = "qvp";
				} else {
					++ssx;
				}

				final int qidx = ordStr.indexOf('q');
				final int vidx = ordStr.indexOf('v');
				final int pidx = ordStr.indexOf('p');

				final String qtyStr = ((qidx >= 0) && (qidx + ssx < ss.length)) ? ss[qidx + ssx] : "x";
				final String valStr = ((vidx >= 0) && (vidx + ssx < ss.length)) ? ss[vidx + ssx] : "x";
				final String priceStr = ((pidx >= 0) && (pidx + ssx < ss.length)) ? ss[pidx + ssx] : "x";

				final Security sec = Security.findSecurity(secStr);
				if (sec == null) {
					Common.reportError("Unknown security: " + secStr);
				}

				final SecurityPortfolio h = currstmt.holdings;
				final SecurityPosition p = new SecurityPosition(sec);

				p.value = (valStr.equals("x")) ? null : new BigDecimal(valStr);
				p.endingShares = (qtyStr.equals("x")) ? null : new BigDecimal(qtyStr);
				BigDecimal price = (priceStr.equals("x")) ? null : new BigDecimal(priceStr);
				final BigDecimal price4date = sec.getPriceForDate(currstmt.date).getPrice();

				// We care primarily about the number of shares. If that is not
				// present, the other two must be set for us to calculate the
				// number of shares. If the price is not present, we can use the
				// price on the day of the statement.
				// If we know two of the values, we can calculate the third.
				if (p.endingShares == null) {
					if (p.value != null) {
						if (price == null) {
							price = price4date;
						}

						p.endingShares = p.value.divide(price, RoundingMode.HALF_UP);
					}
				} else if (p.value == null) {
					if (p.endingShares != null) {
						if (price == null) {
							price = price4date;
						}

						p.value = price.multiply(p.endingShares);
					}
				} else if (price == null) {
					price = price4date;
				}

				if (p.endingShares == null) {
					Common.reportError("Missing security info in stmt");
				}

				h.positions.add(p);
				break;
			}

			default:
				Common.reportError("syntax error");
			}
		}
	}

	public void processStatementFiles(File stmtDirectory) {
		if (!stmtDirectory.isDirectory()) {
			return;
		}

		File stmtFiles[] = stmtDirectory.listFiles();

		for (final File f : stmtFiles) {
			if (!f.getName().endsWith(".qif")) {
				continue;
			}

			try {
				qrdr.load(f.getAbsolutePath(), false);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}

		buildStatementChains();
	}

	private void buildStatementChains() {
		for (Account a : Account.getAccounts()) {
			Statement last = null;

			for (Statement s : a.statements) {
				assert (last == null) || (last.date.compareTo(s.date) < 0);
				s.prevStatement = last;
				last = s;
			}
		}
	}
}

class AccountProcessor {
	private int nextAccountID = Account.getNextAccountID();
	private QifDomReader qrdr;

	public AccountProcessor(QifDomReader qrdr) {
		this.qrdr = qrdr;
	}

	public void loadAccounts() {
		for (;;) {
			String s = this.qrdr.getFileReader().peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			Account acct = loadAccount();
			if (acct == null) {
				break;
			}

			Account existing = Account.findAccount(acct.getName());

			if (existing != null) {
				updateAccount(existing, acct);
				Account.setCurrAccount(existing);
			} else {
				acct.acctid = this.nextAccountID++;
				Account.addAccount(acct);
			}
		}
	}

	private Account loadAccount() {
		final QFileReader.QLine qline = new QFileReader.QLine();

		Account acct = new Account();

		for (;;) {
			this.qrdr.getFileReader().nextAccountLine(qline);

			switch (qline.type) {
			case EndOfSection:
				QKludge.fixAccount(acct);

				return acct;

			case AcctType:
				if (acct.type == null) {
					acct.type = AccountType.parseAccountType(qline.value);
				}
				break;
			case AcctCreditLimit:
				// acct.creditLimit = Common.getDecimal(qline.value);
				break;
			case AcctDescription:
				acct.description = qline.value;
				break;
			case AcctName:
				acct.setName(qline.value);
				break;
			case AcctStmtDate:
				// acct.stmtDate = Common.GetDate(qline.value);
				break;
			case AcctStmtBal:
				// acct.stmtBalance = Common.getDecimal(qline.value);
				break;
			case AcctCloseDate:
				acct.closeDate = Common.parseQDate(qline.value);
				break;
			case AcctStmtFrequency:
				acct.statementFrequency = Integer.parseInt(qline.value);
				break;
			case AcctStmtDay:
				acct.statementDayOfMonth = Integer.parseInt(qline.value);
				break;

			default:
				Common.reportError("syntax error");
			}
		}
	}

	private void updateAccount(Account oldacct, Account newacct) {
		if ((oldacct.type != null) && (newacct.type != null) //
				&& (oldacct.type != newacct.type)) {
			String msg = "Account type mismatch: " //
					+ oldacct.type + " vs " + newacct.type;

			if (oldacct.isInvestmentAccount() != newacct.isInvestmentAccount()) {
				// Common.reportError(msg);
			}

			if (newacct.type != AccountType.Invest) {
				Common.reportWarning(msg);
			}
		}

		if (oldacct.closeDate == null) {
			oldacct.closeDate = newacct.closeDate;
		}

		if (oldacct.type == null) {
			oldacct.type = newacct.type;
		}

		oldacct.statementFrequency = newacct.statementFrequency;
		oldacct.statementDayOfMonth = newacct.statementDayOfMonth;
	}
}

class CategoryProcessor {
	QifDomReader qrdr;

	public CategoryProcessor(QifDomReader qrdr) {
		this.qrdr = qrdr;
	}

	public void loadCategories() {
		for (;;) {
			String s = this.qrdr.getFileReader().peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			Category cat = loadCategory();
			if (cat == null) {
				break;
			}

			Category existing = Category.findCategory(cat.name);

			if (existing == null) {
				Category.addCategory(cat);
			}
		}
	}

	private Category loadCategory() {
		final QFileReader.QLine qline = new QFileReader.QLine();

		final Category cat = new Category();

		for (;;) {
			this.qrdr.getFileReader().nextCategoryLine(qline);

			switch (qline.type) {
			case EndOfSection:
				return cat;

			case CatName:
				cat.name = qline.value;
				break;
			case CatDescription:
				cat.description = qline.value;
				break;
			case CatTaxRelated:
				// cat.taxRelated = Common.parseBoolean(qline.value);
				break;
			case CatIncomeCategory:
				cat.expenseCategory = !Common.parseBoolean(qline.value);
				// cat.incomeCategory = Common.parseBoolean(qline.value);
				break;
			case CatExpenseCategory:
				cat.expenseCategory = Common.parseBoolean(qline.value);
				break;
			case CatBudgetAmount:
				// cat.budgetAmount = Common.getDecimal(qline.value);
				break;
			case CatTaxSchedule:
				break;

			default:
				Common.reportError("syntax error");
			}
		}
	}
}

class TransactionProcessor {
	private static int findCategoryID(String s) {
		if (s.startsWith("[")) {
			s = s.substring(1, s.length() - 1).trim();

			Account acct = Account.findAccount(s);

			return (short) ((acct != null) ? (-acct.acctid) : 0);
		}

		int slash = s.indexOf('/');
		if (slash >= 0) {
			// Throw away tag
			s = s.substring(slash + 1);
		}

		Category cat = Category.findCategory(s);

		return (cat != null) ? (cat.catid) : 0;
	}

	private QifDomReader qrdr;

	public TransactionProcessor(QifDomReader qrdr) {
		this.qrdr = qrdr;
	}

	public void loadInvestmentTransactions() {
		for (;;) {
			String s = this.qrdr.getFileReader().peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			InvestmentTxn txn = loadInvestmentTransaction();
			if (txn == null) {
				break;
			}

			if ("[[ignore]]".equals(txn.getMemo())) {
				continue;
			}

			if ((txn.security != null) && (txn.price != null)) {
				txn.security.addTransaction(txn);
			}

			Account.currAccount.addTransaction(txn);
		}
	}

	private InvestmentTxn loadInvestmentTransaction() {
		QFileReader.QLine qline = new QFileReader.QLine();

		InvestmentTxn txn = new InvestmentTxn(Account.currAccount.acctid);

		for (;;) {
			this.qrdr.getFileReader().nextInvLine(qline);

			switch (qline.type) {
			case EndOfSection:
				txn.repair();
				return txn;

			case InvTransactionAmt: {
				BigDecimal amt = Common.getDecimal(qline.value);

				if (txn.getAmount() != null) {
					if (!txn.getAmount().equals(amt)) {
						Common.reportError("Inconsistent amount: " + qline.value);
					}
				} else {
					txn.setAmount(amt);
				}

				break;
			}
			case InvAction:
				txn.setAction(TxAction.parseAction(qline.value));
				break;
			case InvClearedStatus:
				txn.clearedStatus = qline.value;
				break;
			case InvCommission:
				txn.commission = Common.getDecimal(qline.value);
				break;
			case InvDate:
				txn.setDate(Common.parseQDate(qline.value));
				break;
			case InvMemo:
				txn.setMemo(qline.value);
				break;
			case InvPrice:
				txn.price = Common.getDecimal(qline.value);
				break;
			case InvQuantity:
				txn.setQuantity(Common.getDecimal(qline.value));
				break;
			case InvSecurity:
				txn.security = Security.findSecurityByName(qline.value);
				if (txn.security == null) {
					txn.security = Security.findSecurityByName(qline.value);
					Common.reportWarning("Txn for acct " + txn.acctid + ". " //
							+ "No security '" + qline.value + "' was found.");
				}
				break;
			case InvFirstLine:
				//txn.textFirstLine = qline.value;
				break;
			case InvXferAmt:
				txn.amountTransferred = Common.getDecimal(qline.value);
				break;
			case InvXferAcct:
				txn.accountForTransfer = qline.value;
				txn.setCatid(findCategoryID(qline.value));
				break;

			default:
				Common.reportError("syntax error; txn: " + qline);
			}
		}
	}

	public void loadNonInvestmentTransactions() {
		for (;;) {
			final String s = this.qrdr.getFileReader().peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			final NonInvestmentTxn txn = loadNonInvestmentTransaction();
			if (txn == null) {
				break;
			}

			if ("[[ignore]]".equals(txn.getMemo())) {
				continue;
			}

			txn.verifySplit();

			Account.currAccount.addTransaction(txn);
		}
	}

	private NonInvestmentTxn loadNonInvestmentTransaction() {
		QFileReader.QLine qline = new QFileReader.QLine();

		NonInvestmentTxn txn = new NonInvestmentTxn(Account.currAccount.acctid);
		SimpleTxn cursplit = null;

		for (;;) {
			this.qrdr.getFileReader().nextTxnLine(qline);

			switch (qline.type) {
			case EndOfSection:
				return txn;

			case TxnCategory: {
				int catid = findCategoryID(qline.value);

				if (catid == 0) {
					Common.reportError("Can't find xtxn: " + qline.value);
				}

				txn.setCatid(catid);
			}
				break;

			case TxnAmount: {
				final BigDecimal amt = Common.getDecimal(qline.value);

				if (txn.getAmount() != null) {
					if (!txn.getAmount().equals(amt)) {
						Common.reportError("Inconsistent amount: " + qline.value);
					}
				} else {
					txn.setAmount(amt);
				}

				break;
			}
			case TxnMemo:
				txn.setMemo(qline.value);
				break;

			case TxnDate:
				txn.setDate(Common.parseQDate(qline.value));
				break;
			case TxnClearedStatus:
				txn.clearedStatus = qline.value;
				break;
			case TxnNumber:
				txn.chkNumber = qline.value;
				break;
			case TxnPayee:
				txn.setPayee(qline.value);
				break;
//			case TxnAddress:
//				txn.address.add(qline.value);
//				break;

			case TxnSplitCategory:
				if (cursplit == null || cursplit.getCatid() != 0) {
					cursplit = new SimpleTxn(txn.acctid);
					txn.split.add(cursplit);
				}

				if (qline.value == null || qline.value.trim().isEmpty()) {
					qline.value = "Fix Me";
				}
				cursplit.setCatid(findCategoryID(qline.value));

				if (cursplit.getCatid() == 0) {
					Common.reportError("Can't find xtxn: " + qline.value);
				}
				break;
			case TxnSplitAmount:
				if (cursplit == null || cursplit.getAmount() != null) {
					txn.split.add(cursplit);
					cursplit = new SimpleTxn(txn.acctid);
				}

				cursplit.setAmount(Common.getDecimal(qline.value));
				break;
			case TxnSplitMemo:
				if (cursplit != null) {
					cursplit.setMemo(qline.value);
				}
				break;

			default:
				Common.reportError("syntax error; txn: " + qline);
			}
		}
	}
}