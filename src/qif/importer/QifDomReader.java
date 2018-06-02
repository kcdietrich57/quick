package qif.importer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import app.QifLoader;
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
import qif.data.TxAction;
import qif.importer.QFileReader.SectionType;

public class QifDomReader {

	public static File getStatementLogFile() {
		return QifDom.dom.stmtLogFile;
	}

	// Housekeeping info while processing related transfer transactions.
	// The imported data does not connect transfers, so we need to do it.
	// These keep track of successful and unsuccessful attempts to connect
	// transfers.
	private final List<SimpleTxn> matchingTxns = new ArrayList<SimpleTxn>();
	private int totalXfers = 0;
	private int failedXfers = 0;

	private QFileReader filerdr = null;
	private File qifDir = null;

	private QifDom dom = null;
	private int nextAccountID = 1;

	public static QifDom loadDom(String[] qifFiles) {
		final File qifDir = new File(qifFiles[0]).getParentFile();
		final QifDomReader rdr = new QifDomReader(qifDir);
		final QifDom dom = new QifDom(qifDir);

		// Process all the QIF files
		for (final String fn : qifFiles) {
			rdr.load(dom, fn);
		}

		// Additional processing once the data is loaded (quotes, stmts, etc)
		rdr.postLoad();

		return dom;
	}

	public QifDomReader(File qifDir) {
		this.qifDir = qifDir;
	}

	public QifDom load(QifDom refdom, String fileName) {
		if (!new File(fileName).exists()) {
			if (new File("c:" + fileName).exists()) {
				fileName = "c:" + fileName;
			} else {
				Common.reportError("Input file '" + fileName + "' does not exist");

				return null;
			}
		}

		init(fileName, refdom);

		processFile();

		cleanUpTransactions();

		return this.dom;
	}

	public void cleanUpTransactions() {
		sortTransactions();
		cleanUpSplits();
		calculateRunningTotals();
		connectTransfers();
		connectSecurityTransfers();
		setupSecurityLots();
	}

	private void sortTransactions() {
		for (Account a : QifDom.dom.getAccounts()) {
			Common.sortTransactionsByDate(a.transactions);
		}
	}

	private void cleanUpSplits() {
		for (Account a : QifDom.dom.getAccounts()) {
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
			if (stxn.catid >= 0) {
				continue;
			}

			MultiSplitTxn mtxn = null;

			for (int jj = ii + 1; jj < nitxn.split.size(); ++jj) {
				final SimpleTxn stxn2 = nitxn.split.get(jj);

				if (stxn.catid == stxn2.catid) {
					if (mtxn == null) {
						mtxn = new MultiSplitTxn(txn.acctid);
						nitxn.split.set(ii, mtxn);

						mtxn.setAmount(stxn.getAmount());
						mtxn.catid = stxn.catid;
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
		for (Account a : QifDom.dom.getAccounts()) {
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
		for (Account a : QifDom.dom.getAccounts()) {
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
		} else if ((txn.catid < 0) && //
		// opening balance shows up as xfer to same acct
				(-txn.catid != txn.acctid)) {
			connectTransfers(txn, txn.getDate());
		}
	}

	private void connectTransfers(SimpleTxn txn, QDate date) {
		// Opening balance appears as a transfer to the same acct
		if ((txn.catid >= 0) || (txn.catid == -txn.getXferAcctid())) {
			return;
		}

		QifDom dom = QifDom.dom;

		final Account a = dom.getAccountByID(-txn.catid);

		findMatches(a, txn, date, true);

		++totalXfers;

		if (this.matchingTxns.isEmpty()) {
			findMatches(a, txn, date, false); // SellX openingBal void
		}

		if (this.matchingTxns.isEmpty()) {
			++failedXfers;
			System.out.println("match not found for xfer: " + txn);
			System.out.println("  " + failedXfers + " of " + totalXfers + " failed");
			return;
		}

		SimpleTxn xtxn = null;
		if (this.matchingTxns.size() == 1) {
			xtxn = this.matchingTxns.get(0);
		} else {
			// TODO choose one more deliberately
			xtxn = this.matchingTxns.get(0);
		}

		txn.xtxn = xtxn;
		xtxn.xtxn = txn;
	}

	private void findMatches(Account acct, SimpleTxn txn, QDate date, boolean strict) {
		this.matchingTxns.clear();

		final int idx = Common.findLastTransactionOnOrBeforeDate(acct.transactions, date);
		if (idx < 0) {
			return;
		}

		boolean datematch = false;

		for (int inc = 0; datematch || (inc < 10); ++inc) {
			datematch = false;

			if (idx + inc < acct.transactions.size()) {
				final GenericTxn gtxn = acct.transactions.get(idx + inc);
				datematch = date.equals(gtxn.getDate());

				final SimpleTxn match = checkMatch(txn, gtxn, strict);
				if (match != null) {
					this.matchingTxns.add(match);
				}
			}

			if (inc > 0 && idx >= inc) {
				final GenericTxn gtxn = acct.transactions.get(idx - inc);
				datematch = datematch || date.equals(gtxn.getDate());

				final SimpleTxn match = checkMatch(txn, gtxn, strict);
				if (match != null) {
					this.matchingTxns.add(match);
				}
			}
		}
	}

	private SimpleTxn checkMatch(SimpleTxn txn, GenericTxn gtxn, boolean strict) {
		assert -txn.catid == gtxn.acctid;

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

	public void postLoad() {
		final File d = new File(this.qifDir, "quotes");
		loadSecurityPriceHistory(d);

		processSecurities();
		fixPortfolios();

		final File dd = new File(this.qifDir, "statements");
		processStatementFiles(dd);

		// Process saved statement reconciliation information
		processStatementLog();

		// Process statements that have not yet been reconciled
		if (QifLoader.scn != null) {
			this.dom.reconcileStatements();
		}

		// Update statement reconciliation file if format has changed
		if (dom.loadedStatementsVersion != StatementDetails.CURRENT_VERSION) {
			rewriteStatementLogFile();
		}
	}

	public void processSecurities() {
		QifDom dom = QifDom.dom;

		processSecurities2(dom.portfolio, GenericTxn.getAllTransactions());

		for (Account a : dom.getAccounts()) {
			if (a.isInvestmentAccount()) {
				processSecurities2(a.securities, a.transactions);
			}
		}
	}

	private void processSecurities2(SecurityPortfolio port, List<GenericTxn> txns) {
		for (final GenericTxn gtxn : txns) {
			if (!(gtxn instanceof InvestmentTxn)) {
				continue;
			}

			final InvestmentTxn txn = (InvestmentTxn) gtxn;
			if (txn.security == null) {
				continue;
			}

			final SecurityPosition pos = port.getPosition(txn.security);
			pos.transactions.add(txn);

			switch (txn.getAction()) {
			case BUY:
			case SHRS_IN:
			case REINV_DIV:
			case REINV_LG:
			case REINV_SH:
			case GRANT:
			case EXPIRE:
			case BUYX:
			case REINV_INT:
			case VEST:
			case SHRS_OUT:
			case SELL:
			case SELLX:
			case EXERCISE:
			case EXERCISEX:
				pos.shares = pos.shares.add(txn.getShares());
				break;
			// pos.shares = pos.shares.subtract(txn.quantity);
			// break;

			case STOCKSPLIT:
				pos.shares = pos.shares.multiply(txn.getShares());
				pos.shares = pos.shares.divide(BigDecimal.TEN);
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

	public void fixPortfolios() {
		fixPortfolio(QifDom.dom.portfolio);

		for (Account a : QifDom.dom.getAccounts()) {
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

	private void connectSecurityTransfers() {
		final List<InvestmentTxn> xins = new ArrayList<InvestmentTxn>();
		final List<InvestmentTxn> xouts = new ArrayList<InvestmentTxn>();

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

	private void setupSecurityLots() {
		for (GenericTxn tx : GenericTxn.getAllTransactions()) {
			if (tx instanceof InvestmentTxn) {
//				((InvestmentTxn) tx).setupLots();
			}
		}
	}

	private void connectSecurityTransfers(List<InvestmentTxn> xins, List<InvestmentTxn> xouts) {
		final Comparator<InvestmentTxn> cpr = (o1, o2) -> {
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

		final List<InvestmentTxn> txns = new ArrayList<InvestmentTxn>(xins);
		txns.addAll(xouts);
		Collections.sort(txns, cpr);

		Collections.sort(xins, cpr);
		Collections.sort(xouts, cpr);

		final List<InvestmentTxn> ins = new ArrayList<InvestmentTxn>();
		final List<InvestmentTxn> outs = new ArrayList<InvestmentTxn>();
		final List<InvestmentTxn> unmatched = new ArrayList<InvestmentTxn>();

		BigDecimal inshrs;
		BigDecimal outshrs;

		boolean isverbose = false;

		while (!xins.isEmpty()) {
			ins.clear();
			outs.clear();

			final InvestmentTxn t = xins.get(0);
			inshrs = gatherTransactionsForSecurityTransfer(ins, xins, null, t.security, t.getDate());
			outshrs = gatherTransactionsForSecurityTransfer(outs, xouts, unmatched, t.security, t.getDate());

			if (outs.isEmpty()) {
				unmatched.addAll(ins);
			} else {
				final BigDecimal inshrs2 = inshrs.setScale(3, RoundingMode.HALF_UP);
				final BigDecimal outshrs2 = outshrs.setScale(3, RoundingMode.HALF_UP);

				if (inshrs2.abs().compareTo(outshrs2.abs()) != 0) {
					Common.reportError("Mismatched security transfer");
				}

				for (final InvestmentTxn t2 : ins) {
					t2.xferInv = outs;
				}
				for (final InvestmentTxn t2 : outs) {
					t2.xferInv = ins;
				}
			}

			if (isverbose) {
				final String s = String.format(//
						"%-20s : %5s(%2d) %s INSH=%s (%2d txns) OUTSH=%s (%2d txns)", //
						t.getAccount().getName(), t.security.symbol, t.security.secid, //
						t.getDate().toString(), //
						Common.formatAmount3(inshrs), ins.size(), //
						Common.formatAmount3(outshrs), outs.size());
				System.out.println(s);
			}
		}

		if (isverbose) {
			for (final InvestmentTxn t : unmatched) {
				final String pad = (t.getAction() == TxAction.SHRS_IN) //
						? "" //
						: "                          ";

				final String s = String.format("%-20s : %5s(%2d) %s %s SHR=%s", //
						t.getAccount().getName(), t.security.symbol, t.security.secid, //
						t.getDate().toString(), pad, //
						Common.formatAmount3(t.getShares()));
				System.out.println(s);
			}
		}
	}

	private BigDecimal gatherTransactionsForSecurityTransfer( //
			List<InvestmentTxn> rettxns, //
			List<InvestmentTxn> srctxns, //
			List<InvestmentTxn> unmatched, //
			Security s, //
			QDate d) {
		BigDecimal numshrs = BigDecimal.ZERO;

		if (srctxns.isEmpty()) {
			return numshrs;
		}

		InvestmentTxn t = srctxns.get(0);

		while (t.getDate().compareTo(d) < 0 || //
				(t.getDate().equals(d) //
						&& (t.security.getName().compareTo(s.getName()) < 0))) {
			unmatched.add(srctxns.remove(0));
			if (srctxns.isEmpty()) {
				break;
			}

			t = srctxns.get(0);
		}

		while (!srctxns.isEmpty()) {
			t = srctxns.get(0);

			if ((t.security != s) || //
					(t.getDate().compareTo(d) != 0)) {
				break;
			}

			rettxns.add(t);
			numshrs = numshrs.add(t.getShares());

			if (srctxns.isEmpty()) {
				break;
			}

			srctxns.remove(0);
		}

		return numshrs;
	}

	// Read statement log file, filling in statement details.
	private void processStatementLog() {
		QifDom dom = QifDom.dom;

		if (!dom.stmtLogFile.isFile()) {
			return;
		}

		LineNumberReader stmtLogReader = null;
		List<StatementDetails> details = new ArrayList<StatementDetails>();

		try {
			stmtLogReader = new LineNumberReader(new FileReader(dom.stmtLogFile));

			String s = stmtLogReader.readLine();
			if (s == null) {
				return;
			}

			dom.loadedStatementsVersion = Integer.parseInt(s.trim());

			s = stmtLogReader.readLine();
			while (s != null) {
				final StatementDetails d = //
						new StatementDetails(dom, s, dom.loadedStatementsVersion);

				details.add(d);

				s = stmtLogReader.readLine();
			}

			processStatementDetails(details);
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			if (stmtLogReader != null) {
				try {
					stmtLogReader.close();
				} catch (final IOException e) {
				}
			}
		}
	}

	// Recreate log file when we have changed the format from the previous
	// version
	// Save the previous file as <name>.N
	public static void rewriteStatementLogFile() {
		QifDom dom = QifDom.dom;

		final String basename = dom.stmtLogFile.getName();
		final File tmpLogFile = new File(dom.stmtLogFile.getParentFile(), basename + ".tmp");
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new FileWriter(tmpLogFile));
		} catch (final IOException e) {
			Common.reportError("Can't open tmp stmt log file: " //
					+ dom.stmtLogFile.getAbsolutePath());
			return;
		}

		pw.println("" + StatementDetails.CURRENT_VERSION);
		for (Account a : dom.getAccounts()) {
			for (Statement s : a.statements) {
				pw.println(s.formatForSave());
			}
		}

		try {
			if (pw != null) {
				pw.close();
			}
		} catch (final Exception e) {
		}

		File logFileBackup = null;

		for (int ii = 1;; ++ii) {
			logFileBackup = new File(dom.stmtLogFile.getParentFile(), basename + "." + ii);
			if (!logFileBackup.exists()) {
				break;
			}
		}

		dom.stmtLogFile.renameTo(logFileBackup);
		if (logFileBackup.exists() && tmpLogFile.exists() && !dom.stmtLogFile.exists()) {
			tmpLogFile.renameTo(dom.stmtLogFile);
		}

		assert (logFileBackup.exists() && !dom.stmtLogFile.exists());

		dom.loadedStatementsVersion = StatementDetails.CURRENT_VERSION;
	}

	/**
	 * Update statements with their reconciliation information
	 *
	 * @param details
	 *            List of reconciliation info for statements
	 */
	private void processStatementDetails(List<StatementDetails> details) {
		QifDom dom = QifDom.dom;

		for (StatementDetails d : details) {
			final Account a = dom.getAccountByID(d.acctid);

			Statement s = a.getStatement(d.date, d.closingBalance);
			if (s == null) {
				Common.reportError("Can't find statement for details: " //
						+ a.getName() //
						+ "  " + d.date.toString() //
						+ "  " + d.closingBalance);
			}

			getTransactionsFromDetails(a, s, d);

			if (!s.isBalanced) {
				getTransactionsFromDetails(a, s, d);
				Common.reportError("Can't reconcile statement from log.\n" //
						+ " a=" + a.getName() //
						+ " s=" + s.toString());
			}
		}
	}

	/**
	 * Match up loaded transactions with statement details from log.
	 *
	 * @param a
	 *            Account
	 * @return True if all transactions are found
	 */
	public void getTransactionsFromDetails(Account a, Statement s, StatementDetails d) {
		if (s.isBalanced) {
			return;
		}

		s.transactions.clear();
		s.unclearedTransactions.clear();

		final List<GenericTxn> txns = a.gatherTransactionsForStatement(s);
		final List<TxInfo> badinfo = new ArrayList<TxInfo>();

		for (final TxInfo info : d.transactions) {
			boolean found = false;

			for (int ii = 0; ii < txns.size(); ++ii) {
				final GenericTxn t = txns.get(ii);

				if (info.date.compareTo(t.getDate()) == 0) {
					if ((info.cknum == t.getCheckNumber()) //
							&& (info.cashAmount.compareTo(t.getCashAmount()) == 0)) {
						if (t.stmtdate != null) {
							Common.reportError("Reconciling transaction twice:\n" //
									+ t.toString());
						}

						s.transactions.add(t);
						txns.remove(ii);
						found = true;

						break;
					}
				}

				// TODO txinfos are not sorted by date - should they be?
				// final long infoms = info.date.getTime();
				// final long tranms = t.getDate().getTime();
				//
				// if (infoms < tranms) {
				// break;
				// }
			}

			if (!found) {
				badinfo.add(info);
			}
		}

		s.unclearedTransactions.addAll(txns);

		if (!badinfo.isEmpty()) {
			// d.transactions.removeAll(badinfo);
			Common.reportWarning( //
					"Can't find " + badinfo.size() + " reconciled transactions" //
							+ " for acct " + a.getName() + ":\n" //
							+ badinfo.toString() + "\n" + toString());
			return;
		}

		for (final GenericTxn t : s.transactions) {
			t.stmtdate = s.date;
		}

		s.isBalanced = true;
	}

	private void loadSecurityPriceHistory(File quoteDirectory) {
		if (!quoteDirectory.isDirectory()) {
			return;
		}

		final File quoteFiles[] = quoteDirectory.listFiles();

		for (final File f : quoteFiles) {
			String symbol = f.getName();
			symbol = symbol.replaceFirst(".csv", "");
			final Security sec = Security.findSecurityBySymbol(symbol);

			if (sec != null) {
				loadQuoteFile(sec, f);
			}
		}
	}

	public static void loadQuoteFile(Security sec, File f) {
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
					// TODO this is serious
					e.printStackTrace();
				}

				final QPrice p = new QPrice();

				if (isWeekly) {
					// TODO I don't get this
					date = date.addDays(4);
				}

				p.date = date;

				final BigDecimal splitRatio = sec.getSplitRatioForDate(date);

				if (isSplitAdjusted) {
					p.splitAdjustedPrice = price;
					p.price = price.multiply(splitRatio);
				} else {
					p.splitAdjustedPrice = price.divide(splitRatio);
					p.price = price;
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

	private void init(String filename, QifDom refdom) {
		final File f = new File(filename);
		if (!f.exists()) {
			Common.reportError("File '" + filename + "' does not exist");
		}

		this.filerdr = new QFileReader(f);

		if (refdom != null) {
			this.dom = refdom;
			this.nextAccountID = this.dom.getNextAccountID();
		} else {
			Common.reportError("Can't have null refdom");
		}
	}

	private void processFile() {
		this.filerdr.reset();

		for (SectionType sectype = this.filerdr.findFirstSection(); //
				sectype != SectionType.EndOfFile; //
				sectype = this.filerdr.nextSection()) {
			switch (sectype) {
			case Tag:
			case Category:
				// System.out.println("Loading categories");
				loadCategories();
				break;

			case Account:
				// System.out.println("Loading accounts");
				loadAccounts();
				break;

			case Asset:
			case Liability:
			case Cash:
			case CreditCard:
			case Bank:
				// System.out.println("Loading transactions for " +
				// this.dom.currAccount.name);
				loadNonInvestmentTransactions();
				break;

			case Investment:
				// System.out.println("Loading transactions for " +
				// this.dom.currAccount.name);
				loadInvestmentTransactions();
				break;

			case Statements:
				// System.out.println("Loading statements");
				loadStatements(this.filerdr);
				break;

			case Security:
				// if (this.dom.securities.isEmpty()) {
				// System.out.println("Loading securities");
				// }
				loadSecurities();
				break;

			case Prices:
				// System.out.println("Loading prices");
				loadPrices();
				break;

			case QClass:
				Common.reportError("TODO not implemented");
				// dom.classes.addAll(Class.read(secbody));
				break;

			case MemorizedTransaction:
				Common.reportError("TODO not implemented");
				// dom.memorizedTxns.addAll(MemorizedTxn.read(secbody));
				break;

			default:
				break;
			}
		}
	}

	private void loadCategories() {
		for (;;) {
			String s = this.filerdr.peekLine();
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

	public Category loadCategory() {
		final QFileReader.QLine qline = new QFileReader.QLine();

		final Category cat = new Category();

		for (;;) {
			this.filerdr.nextCategoryLine(qline);

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

	private void loadAccounts() {
		for (;;) {
			final String s = this.filerdr.peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			final Account acct = loadAccount();
			if (acct == null) {
				break;
			}

			Account existing = this.dom.findAccount(acct.getName());

			if (existing != null) {
				updateAccount(existing, acct);
				this.dom.currAccount = existing;
			} else {
				acct.acctid = this.nextAccountID++;
				this.dom.addAccount(acct);
			}
		}
	}

	private void updateAccount(Account oldacct, Account newacct) {
		if ((oldacct.type != null) && (newacct.type != null) //
				&& (oldacct.type != newacct.type)) {
			final String msg = "Account type mismatch: " //
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
	}

	public Account loadAccount() {
		final QFileReader.QLine qline = new QFileReader.QLine();

		final Account acct = new Account(this.dom);

		for (;;) {
			this.filerdr.nextAccountLine(qline);

			switch (qline.type) {
			case EndOfSection:
				// N.B. hack for bad quicken data
				if (acct.getName().endsWith("Checking")) {
					acct.type = AccountType.Bank;
				}

				return acct;

			case AcctType:
				if (acct.type == null) {
					acct.type = AccountType.parseAccountType(qline.value);
				}
				break;
			case AcctCreditLimit:
				acct.creditLimit = Common.getDecimal(qline.value);
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

			default:
				Common.reportError("syntax error");
			}
		}
	}

	private void loadSecurities() {
		for (;;) {
			String s = this.filerdr.peekLine();
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
				// TODO verify security
				if (!existing.names.contains(sec.getName())) {
					existing.names.add(sec.getName());
				}
			} else {
				Security.addSecurity(sec);
			}
		}
	}

	public Security loadSecurity() {
		QFileReader.QLine qline = new QFileReader.QLine();

		String symbol = null;
		String name = null;
		String type = null;
		String goal = null;

		loop: for (;;) {
			this.filerdr.nextSecurityLine(qline);

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

	private void loadInvestmentTransactions() {
		for (;;) {
			final String s = this.filerdr.peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			final InvestmentTxn txn = loadInvestmentTransaction();
			if (txn == null) {
				break;
			}

			if ("[[ignore]]".equals(txn.memo)) {
				continue;
			}

			if ((txn.security != null) && (txn.price != null)) {
				txn.security.addTransaction(txn);
			}

			this.dom.currAccount.addTransaction(txn);
		}
	}

	public InvestmentTxn loadInvestmentTransaction() {
		final QFileReader.QLine qline = new QFileReader.QLine();

		final InvestmentTxn txn = new InvestmentTxn(this.dom.currAccount.acctid);

		for (;;) {
			this.filerdr.nextInvLine(qline);

			switch (qline.type) {
			case EndOfSection:
				txn.repair();
				return txn;

			case InvTransactionAmt: {
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
				txn.memo = qline.value;
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
				txn.textFirstLine = qline.value;
				break;
			case InvXferAmt:
				txn.amountTransferred = Common.getDecimal(qline.value);
				break;
			case InvXferAcct:
				txn.accountForTransfer = qline.value;
				// TODO fixme - this is never meaningfully used
				txn.xacctid = findCategoryID(qline.value);
				break;

			default:
				Common.reportError("syntax error; txn: " + qline);
			}
		}
	}

	private void loadNonInvestmentTransactions() {
		for (;;) {
			final String s = this.filerdr.peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			final NonInvestmentTxn txn = loadNonInvestmentTransaction();
			if (txn == null) {
				break;
			}

			if ("[[ignore]]".equals(txn.memo)) {
				continue;
			}

			txn.verifySplit();

			this.dom.currAccount.addTransaction(txn);
		}
	}

	public NonInvestmentTxn loadNonInvestmentTransaction() {
		final QFileReader.QLine qline = new QFileReader.QLine();

		final NonInvestmentTxn txn = new NonInvestmentTxn(this.dom.currAccount.acctid);
		SimpleTxn cursplit = null;

		for (;;) {
			this.filerdr.nextTxnLine(qline);

			switch (qline.type) {
			case EndOfSection:
				return txn;

			case TxnCategory:
				txn.catid = findCategoryID(qline.value);

				if (txn.catid == 0) {
					Common.reportError("Can't find xtxn: " + qline.value);
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
				txn.memo = qline.value;
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
			case TxnAddress:
				txn.address.add(qline.value);
				break;

			case TxnSplitCategory:
				if (cursplit == null || cursplit.catid != 0) {
					cursplit = new SimpleTxn(txn.acctid);
					txn.split.add(cursplit);
				}

				if (qline.value == null || qline.value.trim().isEmpty()) {
					qline.value = "Fix Me";
				}
				cursplit.catid = findCategoryID(qline.value);

				if (cursplit.catid == 0) {
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
					cursplit.memo = qline.value;
				}
				break;

			default:
				Common.reportError("syntax error; txn: " + qline);
			}
		}
	}

	private static int findCategoryID(String s) {
		if (s.startsWith("[")) {
			s = s.substring(1, s.length() - 1).trim();

			Account acct = QifDom.dom.findAccount(s);

			return (short) ((acct != null) ? (-acct.acctid) : 0);
		}

		final int slash = s.indexOf('/');
		if (slash >= 0) {
			// Throw away tag
			s = s.substring(slash + 1);
		}

		Category cat = Category.findCategory(s);

		return (cat != null) ? (cat.catid) : 0;
	}

	private void loadPrices() {
		for (;;) {
			final String s = this.filerdr.peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			final QPrice price = QPrice.load(this.filerdr);
			if (price == null) {
				break;
			}

			Security sec = Security.findSecurityBySymbol(price.symbol);
			if (sec != null) {
				sec.addPrice(price, true);
			}
		}
	}

	private void processStatementFiles(File stmtDirectory) {
		if (!stmtDirectory.isDirectory()) {
			return;
		}

		final File stmtFiles[] = stmtDirectory.listFiles();

		for (final File f : stmtFiles) {
			if (!f.getName().endsWith(".qif")) {
				continue;
			}

			try {
				load(this.dom, f.getAbsolutePath());
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}

		buildStatementChains();
	}

	private void buildStatementChains() {
		for (Account a : this.dom.getAccounts()) {
			Statement last = null;

			for (Statement s : a.statements) {
				assert (last == null) || (last.date.compareTo(s.date) < 0);
				s.prevStatement = last;
				last = s;
			}
		}
	}

	private void loadStatements(QFileReader qfr) {
		for (;;) {
			final String s = qfr.peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			final List<Statement> stmts = loadStatementsSection(qfr, this.dom);
			for (final Statement stmt : stmts) {
				this.dom.currAccount.statements.add(stmt);
			}
		}
	}

	private List<Statement> loadStatementsSection(QFileReader qfr, QifDom dom) {
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
				final Account a = dom.findAccount(aname);
				if (a == null) {
					Common.reportError("Can't find account: " + aname);
				}

				dom.currAccount = a;
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
							? Common.getDateForEndOfMonth(year, month) //
							: new QDate(year, month, day);

					final Statement prevstmt = (stmts.isEmpty() ? null : stmts.get(stmts.size() - 1));

					currstmt = new Statement(dom.currAccount.acctid);
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
				p.shares = (qtyStr.equals("x")) ? null : new BigDecimal(qtyStr);
				BigDecimal price = (priceStr.equals("x")) ? null : new BigDecimal(priceStr);
				final BigDecimal price4date = sec.getPriceForDate(currstmt.date).price;

				// We care primarily about the number of shares. If that is not
				// present, the other two must be set for us to calculate the
				// number of shares. If the price is not present, we can use the
				// price on the day of the statement.
				// If we know two of the values, we can calculate the third.
				if (p.shares == null) {
					if (p.value != null) {
						if (price == null) {
							price = price4date;
						}

						p.shares = p.value.divide(price, RoundingMode.HALF_UP);
					}
				} else if (p.value == null) {
					if (p.shares != null) {
						if (price == null) {
							price = price4date;
						}

						p.value = price.multiply(p.shares);
					}
				} else if (price == null) {
					price = price4date;
				}

				if (p.shares == null) {
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
}