package qif.data;

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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import qif.data.Statement.StatementDetails;

/** Document Object Model for finances from Quicken */
public class QifDom {
	// The one and only financial model
	public static QifDom dom = null;

	// The imported data does not connect transfers, so we need to do it.
	// These keep track of successful and unsuccessful attempts to connect transfers
	private static int totalXfers = 0;
	private static int failedXfers = 0;

	// Various types of information we track in this model
	private final List<Category> categories;
	private final Map<String, Security> securities;
	private final List<Account> accounts;
	private final List<Account> accountsByID;
	private final List<GenericTxn> allTransactions;

	public SecurityPortfolio portfolio;
	public int loadedStatementsVersion = -1;

	private File stmtLogFile;

	// As we are loading data, we track the account context we are within here
	public Account currAccount = null;

	private final List<SimpleTxn> matchingTxns = new ArrayList<SimpleTxn>();

	public QifDom(File qifDir) {
		QifDom.dom = this;

		this.categories = new ArrayList<Category>();
		this.accountsByID = new ArrayList<Account>();
		this.accounts = new ArrayList<Account>();
		this.securities = new HashMap<String, Security>();

		this.allTransactions = new ArrayList<GenericTxn>();
		this.stmtLogFile = new File(qifDir, "statementLog.dat");

		this.portfolio = new SecurityPortfolio();

		// this.classes = new ArrayList<Class>();
		// this.memorizedTxns = new ArrayList<MemorizedTxn>();
	}

	public int getNumAccounts() {
		return this.accounts.size() - 1;
	}

	public int getNumCategories() {
		return this.categories.size() - 1;
	}

	public int getNumSecurities() {
		return this.securities.size() - 1;
	}

	public List<Account> getAccounts() {
		return Collections.unmodifiableList(this.accounts);
	}

	public Account getAccount(int idx) {
		return this.accounts.get(idx);
	}

	/**
	 * Return the descriptor for a specific account
	 * 
	 * DO NOT use this to iterate over accounts; rather use getAccount(idx)
	 */
	public Account getAccountByID(int acctid) {
		return this.accountsByID.get(acctid);
	}

	public List<Account> getSortedAccounts() {
		List<Account> ret = new ArrayList<>(this.accountsByID);

		for (int ii = ret.size() - 1; ii >= 0; --ii) {
			if (ret.get(ii) == null) {
				ret.remove(ii);
			}
		}

		Collections.sort(ret, new Comparator<Account>() {
			public int compare(Account a1, Account a2) {
				if (a1.isOpenOn(null) != a2.isOpenOn(null)) {
					return a1.isOpenOn(null) ? 1 : -1;
				} else if (a1.type != a2.type) {
					return a1.type.compareTo(a2.type);
				}

				return a1.getName().compareTo(a2.getName());
			}
		});

		return ret;
	}

	public Category getCategory(int catid) {
		return this.categories.get(catid);
	}

	public void addTransaction(GenericTxn txn) {
		while (this.allTransactions.size() < txn.txid + 1) {
			this.allTransactions.add(null);
		}

		this.allTransactions.set(txn.txid, txn);
	}

	public Date getFirstTransactionDate() {
		Date retdate = null;

		for (final Account a : this.accountsByID) {
			if (a == null) {
				continue;
			}

			final Date d = a.getFirstTransactionDate();
			if ((d != null) && ((retdate == null) || d.compareTo(retdate) < 0)) {
				retdate = d;
			}
		}

		return retdate;
	}

	public Date getLastTransactionDate() {
		Date retdate = null;

		for (final Account a : this.accountsByID) {
			if (a == null) {
				continue;
			}

			final Date d = a.getLastTransactionDate();
			if ((d != null) && ((retdate == null) || d.compareTo(retdate) > 0)) {
				retdate = d;
			}
		}

		return retdate;
	}

	public List<GenericTxn> getAllTransactions() {
		final List<GenericTxn> txns = new ArrayList<GenericTxn>();

		for (final Account a : this.accountsByID) {
			if (a == null) {
				continue;
			}
			txns.addAll(a.transactions);

			Common.sortTransactionsByDate(txns);
		}

		return txns;
	}

	public void addAccount(Account acct) {
		while (this.accountsByID.size() <= acct.acctid) {
			this.accountsByID.add(null);
		}

		this.accountsByID.set(acct.acctid, acct);
		this.accounts.add(acct);

		this.currAccount = acct;

		Collections.sort(this.accounts, (a1, a2) -> {
			if (a1 == null) {
				return (a2 == null) ? 0 : 1;
			} else if (a2 == null) {
				return -1;
			}

			// Order by firsttran, lasttran
			final int ct1 = a1.transactions.size();
			final int ct2 = a2.transactions.size();

			if (ct1 == 0) {
				return (ct2 == 0) ? 0 : -1;
			} else if (ct2 == 0) {
				return 1;
			}

			final GenericTxn firsttxn1 = a1.transactions.get(0);
			final GenericTxn lasttxn1 = a1.transactions.get(ct1 - 1);
			final GenericTxn firsttxn2 = a2.transactions.get(0);
			final GenericTxn lasttxn2 = a2.transactions.get(ct2 - 1);

			int diff = firsttxn1.getDate().compareTo(firsttxn2.getDate());
			if (diff != 0) {
				return diff;
			}

			diff = lasttxn1.getDate().compareTo(lasttxn2.getDate());
			if (diff != 0) {
				return diff;
			}

			return (a1.getName().compareTo(a2.getName()));
		});
	}

	public void updateAccount(Account oldacct, Account newacct) {
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

		this.currAccount = oldacct;
	}

	public void addCategory(Category cat) {
		final Category existing = findCategory(cat.name);

		if (existing != null) {
			Common.reportError("Adding duplicate category");
		}

		while (this.categories.size() <= cat.catid) {
			this.categories.add(null);
		}

		this.categories.set(cat.catid, cat);
	}

	public void addSecurity(Security sec) {
		final Security existingName = findSecurityByName(sec.getName());
		if (existingName != null) {
			Common.reportWarning("Adding duplicate security");
		}

		if (sec.symbol == null) {
			// TODO this should not happen
			sec.symbol = sec.getName();
		}

		final Security existingSymbol = this.securities.get(sec.symbol);
		if (existingSymbol != null) {
			Common.reportWarning("Adding duplicate security");
		}

		this.securities.put(sec.symbol, sec);
	}

	public int findCategoryID(String s) {
		if (s.startsWith("[")) {
			s = s.substring(1, s.length() - 1).trim();

			final Account acct = findAccount(s);

			return (short) ((acct != null) ? (-acct.acctid) : 0);
		}

		final int slash = s.indexOf('/');
		if (slash >= 0) {
			// Throw away tag
			s = s.substring(slash + 1);
		}

		final Category cat = findCategory(s);

		return (cat != null) ? (cat.catid) : 0;
	}

	public Security findSecurity(String nameOrSymbol) {
		final Security s = findSecurityBySymbol(nameOrSymbol);

		return (s != null) ? s : findSecurityByName(nameOrSymbol);
	}

	public Security findSecurityByName(String name) {
		for (final Security sec : this.securities.values()) {
			if (sec != null && sec.names.contains(name)) {
				return sec;
			}
		}

		return null;
	}

	public Security findSecurityBySymbol(String sym) {
		return this.securities.get(sym);
	}

	public Account findAccount(String name) {
		name = name.toLowerCase();

		for (final Account acct : this.accountsByID) {
			if (acct != null && acct.getName().equalsIgnoreCase(name)) {
				return acct;
			}
		}

		for (final Account acct : this.accountsByID) {
			if (acct != null && acct.getName().toLowerCase().startsWith(name)) {
				return acct;
			}
		}

		return null;
	}

	public Category findCategory(String name) {
		for (final Category cat : this.categories) {
			if ((cat != null) && cat.name.equals(name)) {
				return cat;
			}
		}

		return null;
	}

	public static class Balances {
		public BigDecimal netWorth = BigDecimal.ZERO;
		public BigDecimal assets = BigDecimal.ZERO;
		public BigDecimal liabilities = BigDecimal.ZERO;
	}

	public Balances getNetWorthForDate(Date d) {
		final Balances b = new Balances();

		if (d == null) {
			d = new Date();
		}

		for (final Account a : this.accountsByID) {
			if (a != null) {
				final BigDecimal amt = a.getValueForDate(d);

				b.netWorth = b.netWorth.add(amt);

				if (a.isAsset()) {
					b.assets = b.assets.add(amt);
				} else if (a.isLiability()) {
					b.liabilities = b.liabilities.add(amt);
				}
			}
		}

		return b;
	}

	public void showStatistics() {
		int total = this.allTransactions.size() - 1;

		int nullt = 0;
		int reconciled = 0;
		int unreconciled = 0;
		for (int ii = 0; ii <= total; ++ii) {
			final GenericTxn t = this.allTransactions.get(ii);

			if (t == null) {
				++nullt;
			} else if (t.stmtdate != null) {
				++reconciled;
			} else {
				++unreconciled;
			}
		}

		total = (reconciled + unreconciled);
		final double pct = reconciled * 100.0 / total;
		System.out.println(String.format("%d of %d txns reconciled (%5.2f) nullTX: %d", //
				reconciled, total, pct, nullt));
	}

	public String toString() {
		String s = "";

		s += "Categories: " + this.categories;
		s += "Accounts: " + this.accounts;
		s += "Securities: " + this.securities;

		return s;
	}

	public int getNextAccountID() {
		return (this.accountsByID.isEmpty()) ? 1 : this.accountsByID.size();
	}

	public int getNextCategoryID() {
		return (this.categories.isEmpty()) ? 1 : this.categories.size();
	}

	public void buildStatementChains() {
		for (final Account a : this.accountsByID) {
			if (a == null) {
				continue;
			}

			Statement last = null;
			for (final Statement s : a.statements) {
				assert (last == null) || (last.date.compareTo(s.date) < 0);
				s.prevStatement = last;
				last = s;
			}
		}
	}

	// Read statement log file, filling in statement details.
	public void processStatementLog() {
		if (!this.stmtLogFile.isFile()) {
			return;
		}

		LineNumberReader stmtLogReader = null;
		final List<StatementDetails> details = new ArrayList<StatementDetails>();

		try {
			stmtLogReader = new LineNumberReader(new FileReader(this.stmtLogFile));

			String s = stmtLogReader.readLine();
			if (s == null) {
				return;
			}

			this.loadedStatementsVersion = Integer.parseInt(s.trim());

			s = stmtLogReader.readLine();
			while (s != null) {
				final Statement.StatementDetails d = //
						new StatementDetails(this, s, this.loadedStatementsVersion);

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
	public void rewriteStatementLogFile() {
		final String basename = this.stmtLogFile.getName();
		final File tmpLogFile = new File(this.stmtLogFile.getParentFile(), basename + ".tmp");
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new FileWriter(tmpLogFile));
		} catch (final IOException e) {
			Common.reportError("Can't open tmp stmt log file: " //
					+ this.stmtLogFile.getAbsolutePath());
			return;
		}

		pw.println("" + Statement.StatementDetails.CURRENT_VERSION);
		for (int anum = 0; anum <= getNumAccounts(); ++anum) {
			final Account a = getAccount(anum);

			for (final Statement s : a.statements) {
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
			logFileBackup = new File(this.stmtLogFile.getParentFile(), basename + "." + ii);
			if (!logFileBackup.exists()) {
				break;
			}
		}

		this.stmtLogFile.renameTo(logFileBackup);
		if (logFileBackup.exists() && tmpLogFile.exists() && !this.stmtLogFile.exists()) {
			tmpLogFile.renameTo(this.stmtLogFile);
		}

		assert (logFileBackup.exists() && !this.stmtLogFile.exists());

		this.loadedStatementsVersion = Statement.StatementDetails.CURRENT_VERSION;
	}

	// Process unreconciled statements for each account, matching statements
	// with transactions and logging the results.
	public void reconcileStatements() {
		PrintWriter pw = null;

		try {
			pw = new PrintWriter(new FileWriter(this.stmtLogFile, true));

			for (int anum = 0; anum <= getNumAccounts(); ++anum) {
				final Account a = getAccount(anum);
				a.reconcileStatements(pw);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			if (pw != null) {
				pw.close();
			}
		}
	}

	/**
	 * Find a statement in this dom that corresponds to a statement in another dom.
	 * Used when copying/cloning a dom.
	 *
	 * @param stmt
	 *            The statement in another dom
	 * @return The same statement in this dom
	 */
	public Statement findStatement(Statement stmt) {
		final Account a = getAccountByID(stmt.acctid);
		if (a == null) {
			Common.reportError("Can't find statement");
		}

		return a.getStatement(stmt.date, stmt.closingBalance);
	}

	public void fixPortfolios() {
		fixPortfolio(this.portfolio);

		for (int anum = 0; anum <= getNumAccounts(); ++anum) {
			final Account a = getAccount(anum);

			if (a != null && a.isInvestmentAccount()) {
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

	public void processSecurities() {
		processSecurities2(this.portfolio, getAllTransactions());

		for (int anum = 0; anum <= getNumAccounts(); ++anum) {
			final Account a = getAccount(anum);
			if ((a == null) || !a.isInvestmentAccount()) {
				continue;
			}

			processSecurities2(a.securities, a.transactions);
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

	public void cleanUpTransactions() {
		sortTransactions();
		cleanUpSplits();
		calculateRunningTotals();
		connectTransfers();
		connectSecurityTransfers();
	}

	private void sortTransactions() {
		for (int anum = 0; anum <= getNumAccounts(); ++anum) {
			final Account a = getAccount(anum);
			if (a == null) {
				continue;
			}

			Common.sortTransactionsByDate(a.transactions);
		}
	}

	private void cleanUpSplits() {
		for (int anum = 0; anum <= getNumAccounts(); ++anum) {
			final Account a = getAccount(anum);
			if (a == null) {
				continue;
			}

			for (final GenericTxn txn : a.transactions) {
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
		for (int idx = 0; idx < getNumAccounts(); ++idx) {
			final Account a = getAccount(idx);

			a.clearedBalance = a.balance = BigDecimal.ZERO;

			for (final GenericTxn t : a.transactions) {
				final BigDecimal amt = t.getCashAmount();

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
		for (int anum = 0; anum <= getNumAccounts(); ++anum) {
			final Account a = getAccount(anum);
			if (a == null) {
				continue;
			}

			for (final GenericTxn txn : a.transactions) {
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

	private void connectTransfers(SimpleTxn txn, Date date) {
		// Opening balance appears as a transfer to the same acct
		if ((txn.catid >= 0) || (txn.catid == -txn.getXferAcctid())) {
			return;
		}

		final Account a = getAccountByID(-txn.catid);

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

	private void findMatches(Account acct, SimpleTxn txn, Date date, boolean strict) {
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

	private void connectSecurityTransfers() {
		final List<InvestmentTxn> xins = new ArrayList<InvestmentTxn>();
		final List<InvestmentTxn> xouts = new ArrayList<InvestmentTxn>();

		for (int anum = 0; anum <= getNumAccounts(); ++anum) {
			final Account a = getAccount(anum);
			if (a == null || !a.isInvestmentAccount()) {
				continue;
			}

			for (final GenericTxn txn : a.transactions) {
				if (!(txn instanceof InvestmentTxn)) {
					continue;
				}

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
						Common.formatDate(t.getDate()), //
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
						Common.formatDate(t.getDate()), pad, //
						Common.formatAmount3(t.getShares()));
				System.out.println(s);
			}
		}
	}

	private BigDecimal gatherTransactionsForSecurityTransfer( //
			List<InvestmentTxn> rettxns, //
			List<InvestmentTxn> srctxns, //
			List<InvestmentTxn> unmatched, //
			Security s, Date d) {
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

	/**
	 * Update statements with their reconciliation information
	 *
	 * @param details
	 *            List of reconciliation info for statements
	 */
	private void processStatementDetails(List<StatementDetails> details) {
		for (final StatementDetails d : details) {
			final Account a = getAccountByID(d.acctid);

			final Statement s = a.getStatement(d.date, d.closingBalance);
			if (s == null) {
				Common.reportError("Can't find statement for details: " //
						+ a.getName() //
						+ "  " + Common.formatDate(d.date) //
						+ "  " + d.closingBalance);
			}

			s.getTransactionsFromDetails(a, d);

			if (!s.isBalanced) {
				s.getTransactionsFromDetails(a, d);
				Common.reportError("Can't reconcile statement from log.\n" //
						+ " a=" + a.getName() //
						+ " s=" + s.toString());
			}
		}
	}
}
