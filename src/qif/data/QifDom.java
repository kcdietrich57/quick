
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

import qif.data.Account.AccountType;
import qif.data.SimpleTxn.Action;
import qif.data.Statement.StatementDetails;

//--------------------------------------------------------------------
//TO DO
//--------------------------------------------------------------------
// 2/9/16 Use Acct ID in transactions
// 2/9 Use Category ID in transactions
// 2/9 Create Split transactions with IDs
// 2/9 Verify split consistency
// 2/9 Sort transactions by date
// 2/11 Connect xfer transactions
// 2/11 Security, prices
// 2/12 Account type
// 2/12 Account summary
// 2/13 Non-Investment Statements
// 2/16 Balance Non-Investment Statements
// 2/20 Relaxed loading, fix load bugs, better errors, load old files
// 2/21 Add to git
// 2/22 Cloning Category/Security/Account info in new file (prep for merge)
// 2/28 Running cash balance
// 3/5 Non-investment register - running balance
// 3/27 Investment cash balance
// 3/27 Investment transactions - share balance
// 3/28 Verify all transaction types - Dividends, Splits
// 3/29 ShrsIn/ShrsOut (xfer)
// 4/2 Share balance in account positions
// 4/2 Security positions (all accounts)
// 4/3 Security position by account and security for any date
// 4/20 Security price history
// 4/20 Point-in time positions (net worth)
// 4/20 Handle investment amounts and transfers
// 4/23 Consolidate existing data files
// 5/7 Statements - store separately
// 5/7 Manage security price history separately
// 5/7 Portfolio market value
// 5/7 round account value to nearest cent
// 5/16 Review/reconcile statements
//
// Persist info in extended QIF files
// Synchronize data with updated qif file
//Specify expected statements per account
//Prompt for info for missing statements; persist information
//
// Merge/compare files (load mulitple qif data files)
//
// REPORTS
// Account as of date - incl last statement
// Net worth vs time
// Account value vs time
// Categories vs time
// Cash flow
// Investment income
//
// INVESTMENTS
// Stock Splits
// Dump portfolio for each month (positions)
// Associate security sales with purchases (lots)
// ShrsIn/ShrsOut - add/remove
// Track cost basis/gain/loss
// ESPP grants
// Extra ESPP tax info
// Options - Grant, Vest, Exercise, Expire
// Include vested options in portfolio
// Optionally include non-vested options in portfolio (separately, perhaps)
// Exclude expired options
// Investment statement with additional info for securities
//
// ASSETS
// LOANS
//
//Code review/cleanup - ids for more fields?
//
// Encryption, security
// GUI Register
// Graphs
//
// Look up transaction by id?
//--------------------------------------------------------------------

// Document Object Model for a QIF file.
public class QifDom {
	private static short nextdomid = 1;
	private static List<QifDom> doms = new ArrayList<QifDom>();
	private static boolean isverbose = false;

	private static int totalXfers = 0;
	private static int failedXfers = 0;

	public static QifDom getDomById(int domid) {
		return ((domid >= 0) && (doms.size() > domid)) ? doms.get(domid) : null;
	}

	public static boolean verbose() {
		return isverbose;
	}

	public final short domid;
	private final List<Category> categories;
	private final Map<String, Security> securities;

	private final List<Account> accounts;
	private final List<Account> accounts_bytime;

	SecurityPortfolio portfolio;
	int loadedStatementsVersion = -1;

	public Account currAccount = null;

	private final List<SimpleTxn> matchingTxns = new ArrayList<SimpleTxn>();

	public QifDom() {
		this.domid = nextdomid++;

		while (doms.size() < this.domid) {
			doms.add(null);
		}

		doms.add(this);

		this.categories = new ArrayList<Category>();
		this.accounts = new ArrayList<Account>();
		this.accounts_bytime = new ArrayList<Account>();
		this.securities = new HashMap<String, Security>();

		this.portfolio = new SecurityPortfolio();

		// this.classes = new ArrayList<Class>();
		// this.memorizedTxns = new ArrayList<MemorizedTxn>();
	}

	public QifDom(QifDom other) {
		this();

		for (final Category c : other.categories) {
			if (c != null) {
				addCategory(new Category(c));
			}
		}

		for (final Security s : other.securities.values()) {
			if (s != null) {
				addSecurity(new Security(s));
			}
		}

		for (final Account a : other.accounts) {
			if (a != null) {
				addAccount(new Account(a, this));
			}
		}
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

	public Account getAccount(int acctid) {
		return this.accounts.get(acctid);
	}

	public Account getAccountByTime(int acctid) {
		return this.accounts_bytime.get(acctid);
	}

	public Category getCategory(int catid) {
		return this.categories.get(catid);
	}

	public Date getFirstTransactionDate() {
		Date retdate = null;

		for (final Account a : this.accounts) {
			if (a == null) {
				continue;
			}

			final Date d = a.getFirstTransactionDate();
			if ((retdate == null) || d.compareTo(retdate) < 0) {
				retdate = d;
			}
		}

		return retdate;
	}

	public Date getLastTransactionDate() {
		Date retdate = null;

		for (final Account a : this.accounts) {
			if (a == null) {
				continue;
			}

			final Date d = a.getLastTransactionDate();
			if ((retdate == null) || d.compareTo(retdate) > 0) {
				retdate = d;
			}
		}

		return retdate;
	}

	public List<GenericTxn> getAllTransactions() {
		final List<GenericTxn> txns = new ArrayList<GenericTxn>();

		for (final Account a : this.accounts) {
			if (a == null) {
				continue;
			}
			txns.addAll(a.transactions);

			Common.sortTransactionsByDate(txns);
		}

		return txns;
	}

	public void addAccount(Account acct) {
		while (this.accounts.size() <= acct.acctid) {
			this.accounts.add(null);
		}

		this.accounts.set(acct.acctid, acct);
		this.accounts_bytime.add(acct);

		this.currAccount = acct;

		Collections.sort(this.accounts_bytime, (a1, a2) -> {
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

			return (a1.name.compareTo(a2.name));
		});
	}

	public void updateAccount(Account oldacct, Account newacct) {
		if ((oldacct.type != null) && (newacct.type != null) //
				&& (oldacct.type != newacct.type)) {
			final String msg = "Account type mismatch: " //
					+ oldacct.type + " vs " + newacct.type;

			if (oldacct.isInvestmentAccount() != newacct.isInvestmentAccount()) {
				Common.reportError(msg);
			}

			if (newacct.type != AccountType.Invest) {
				Common.reportWarning(msg);
			}
		}

		if (oldacct.type == null) {
			oldacct.type = newacct.type;
		}

		this.currAccount = oldacct;
	}

	public Account getAccount(short acctid) {
		return this.accounts.get(acctid);
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

		for (final Account acct : this.accounts) {
			if (acct != null && acct.name.equalsIgnoreCase(name)) {
				return acct;
			}
		}

		for (final Account acct : this.accounts) {
			if (acct != null && acct.name.toLowerCase().startsWith(name)) {
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

	public void reportStatusForDate(Date d, boolean itemizeAccounts) {
		System.out.println();
		System.out.println("Global status for date: " + Common.getDateString(d));
		System.out.println("----------------------------------");
		System.out.println(String.format("  %-36s : %10s", "Account", "Balance"));

		BigDecimal netWorth = BigDecimal.ZERO;

		final AccountType atypes[] = { //
				AccountType.Bank, AccountType.Cash, AccountType.Asset, //
				AccountType.CCard, AccountType.Liability, //
				AccountType.Invest, AccountType.InvPort, //
				AccountType.InvMutual, AccountType.Inv401k, //
		};
		final AccountType sections[] = { //
				AccountType.Bank, //
				AccountType.CCard, //
				AccountType.Invest, //
				AccountType.InvMutual //
		};
		final String sectionName[] = { //
				"Bank Accounts", "Credit Accounts", //
				"Investment Accounts", "Retirement Accounts" //
		};

		int snum = 0;
		BigDecimal subtotal = BigDecimal.ZERO;

		for (final AccountType at : atypes) {
			if ((snum < sections.length) && (at == sections[snum])) {
				System.out.println(String.format("Section Total: %15.2f", subtotal));
				subtotal = BigDecimal.ZERO;
				System.out.println("======== " + sectionName[snum] + " accounts ========");
				++snum;
			}

			for (final Account a : this.accounts) {
				if ((a != null) && (a.type == at)) {
					final BigDecimal amt = a.reportStatusForDate(d);

					netWorth = netWorth.add(amt);
					subtotal = subtotal.add(amt);
				}
			}
		}

		System.out.println(String.format("Section Total: %15.2f", subtotal));

		System.out.println();
		System.out.println(String.format("Balance: %15.2f", netWorth));
	}

	public String toString() {
		String s = "";

		s += "ID: " + this.domid;
		s += "Categories: " + this.categories;
		s += "Accounts: " + this.accounts_bytime;
		s += "Securities: " + this.securities;

		return s;
	}

	public int getNextAccountID() {
		return (this.accounts.isEmpty()) ? 1 : this.accounts.size();
	}

	public int getNextCategoryID() {
		return (this.categories.isEmpty()) ? 1 : this.categories.size();
	}

	// Read statement log file, filling in statement details.
	public void processStatementLog(File logFile) {
		if (!logFile.isFile()) {
			return;
		}

		LineNumberReader stmtLogReader = null;
		final List<StatementDetails> details = new ArrayList<StatementDetails>();

		try {
			stmtLogReader = new LineNumberReader(new FileReader(logFile));

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
	public void rewriteStatementLogFile(File logFile) {
		final String basename = logFile.getName();
		final File tmpLogFile = new File(logFile.getParentFile(), basename + ".tmp");
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new FileWriter(tmpLogFile));
		} catch (final IOException e) {
			Common.reportError("Can't open tmp stmt log file: " + logFile.getAbsolutePath());
			return;
		}

		pw.println("" + Statement.StatementDetails.CURRENT_VERSION);
		for (int acctid = 1; acctid <= getNumAccounts(); ++acctid) {
			final Account a = getAccount(acctid);

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
			logFileBackup = new File(logFile.getParentFile(), basename + "." + ii);
			if (!logFileBackup.exists()) {
				break;
			}
		}

		logFile.renameTo(logFileBackup);
		if (logFileBackup.exists() && tmpLogFile.exists() && !logFile.exists()) {
			tmpLogFile.renameTo(logFile);
		}

		assert (logFileBackup.exists() && !logFile.exists());

		this.loadedStatementsVersion = Statement.StatementDetails.CURRENT_VERSION;
	}

	// Process unreconciled statements for each account, matching statements
	// with transactions and logging the results.
	public void reconcileStatements(File stmtlogFile) {
		PrintWriter pw = null;

		try {
			pw = new PrintWriter(new FileWriter(stmtlogFile, true));

			for (int acctid = 1; acctid <= getNumAccounts(); ++acctid) {
				final Account a = getAccount(acctid);
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
	 * Find a statement in this dom that corresponds to a statement in another
	 * dom. Used when copying/cloning a dom.
	 *
	 * @param stmt
	 *            The statement in another dom
	 * @return The same statement in this dom
	 */
	public Statement findStatement(Statement stmt) {
		final Account a = getAccount(stmt.acctid);
		if (a == null) {
			Common.reportError("Can't find statement");
		}

		return a.getStatement(stmt.date, stmt.closingBalance);
	}

	public void fixPortfolios() {
		fixPortfolio(this.portfolio);

		for (int acctid = 1; acctid <= getNumAccounts(); ++acctid) {
			final Account a = getAccount(acctid);

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
			if (t.getAction() == Action.STOCKSPLIT) {
				shrbal = shrbal.multiply(t.getShares());
				shrbal = shrbal.divide(BigDecimal.TEN);
			} else if (t.getShares() != null) {
				shrbal = shrbal.add(t.getShares());
			}

			p.shrBalance.add(shrbal);
		}
	}

	public void processSecurities() {
		processSecurities2(this.portfolio, getAllTransactions());

		for (int acctid = 1; acctid <= getNumAccounts(); ++acctid) {
			final Account a = getAccount(acctid);
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
		for (int acctid = 1; acctid <= getNumAccounts(); ++acctid) {
			final Account a = getAccount(acctid);
			if (a == null) {
				continue;
			}

			Common.sortTransactionsByDate(a.transactions);
		}
	}

	private void cleanUpSplits() {
		for (int acctid = 1; acctid <= getNumAccounts(); ++acctid) {
			final Account a = getAccount(acctid);
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
						mtxn = new MultiSplitTxn(this.domid, txn.acctid);
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
			final Account a = getAccountByTime(idx);

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
		for (int acctid = 1; acctid <= getNumAccounts(); ++acctid) {
			final Account a = getAccount(acctid);
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

		final Account a = getAccount(-txn.catid);

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

		for (int acctid = 1; acctid <= getNumAccounts(); ++acctid) {
			final Account a = getAccount(acctid);
			if (!a.isInvestmentAccount()) {
				continue;
			}

			for (final GenericTxn txn : a.transactions) {
				if (!(txn instanceof InvestmentTxn)) {
					continue;
				}

				if ((txn.getAction() == Action.SHRS_IN)) {
					xins.add((InvestmentTxn) txn);
				} else if (txn.getAction() == Action.SHRS_OUT) {
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

			if (QifDom.verbose()) {
				final String s = String.format(//
						"%-20s : %5s(%2d) %s INSH=%10.3f (%2d txns) OUTSH=%10.3f (%2d txns)", //
						t.getAccount().name, t.security.symbol, t.security.secid, //
						Common.getDateString(t.getDate()), //
						inshrs, ins.size(), outshrs, outs.size());
				System.out.println(s);
			}
		}

		if (QifDom.verbose()) {
			for (final InvestmentTxn t : unmatched) {
				final String pad = (t.getAction() == Action.SHRS_IN) //
						? "" //
						: "                          ";

				final String s = String.format("%-20s : %5s(%2d) %s %s SHR=%10.3f", //
						t.getAccount().name, t.security.symbol, t.security.secid, //
						Common.getDateString(t.getDate()), pad, t.getShares());
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
			final Account a = getAccount(d.acctid);

			final Statement s = a.getStatement(d.date, d.closingBalance);
			if (s == null) {
				Common.reportError("Can't find statement for details: " //
						+ a.name //
						+ "  " + Common.getDateString(d.date) //
						+ "  " + d.closingBalance);
			}

			s.getTransactionsFromDetails(a, d);

			if (!s.isBalanced) {
				Common.reportError("Can't reconcile statement from log.");
			}
		}
	}
}
