package moneymgr.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import moneymgr.io.AccountDetailsFixer;
import moneymgr.model.compare.CompareModels;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/** Class comprising the complete MoneyManager data model */
public class MoneyMgrModel {
	public static final String WIN_QIF_MODEL_NAME = "Windows QIF";
	public static final String WIN_JSON_MODEL_NAME = "Windows JSON";
	public static final String MAC_CSV_MODEL_NAME = "Mac CSV";

	/** Maintain multiple models so we can switch between them and compare */
	private static final Map<String, MoneyMgrModel> models = new HashMap<String, MoneyMgrModel>();

	public static MoneyMgrModel currModel;

	/** Dummy transaction for binary search */
	private static GenericTxn SEARCH_TX;

	// -------------------------------------

	public static void deleteModel(String name) {
		if (MoneyMgrModel.currModel == MoneyMgrModel.getModel(name)) {
			MoneyMgrModel.currModel = null;
		}

		MoneyMgrModel.models.remove(name);
	}

	public static MoneyMgrModel getModel(String name) {
		return MoneyMgrModel.models.get(name);
	}

	/** Change the current model */
	public static MoneyMgrModel changeModel(String name) {
		MoneyMgrModel.currModel = MoneyMgrModel.models.get(name);

		if (MoneyMgrModel.currModel == null) {
			// Common.reportError(String.format("Model '%s' does not exist", name));
			MoneyMgrModel.currModel = new MoneyMgrModel(name);
		}

		return MoneyMgrModel.currModel;
	}

	public static void compareModels(String m1, String m2) {
		CompareModels.compareModels(models.get(m1), models.get(m2));
	}

	// -------------------------------------

	public final String name;

	private int nextTxid = 1;

	private final List<Category> categories = new ArrayList<>();

	/** The global history of all securities */
	public SecurityPortfolio portfolio = new SecurityPortfolio(this, null);

	/** Securities indexed by ID */
	private final List<Security> securitiesByID = new ArrayList<>();

	/** Map symbol to security */
	private final Map<String, Security> securities = new HashMap<>();

	/** Account list ordered by first/last txn dates (no gaps/nulls) */
	private final List<Account> accounts = new ArrayList<>();

	/** Account list indexed by acctid (size > numAccounts, may have gaps/nulls) */
	private final List<Account> accountsByID = new ArrayList<>();

	/** Tracks current context as we are loading */
	public Account currAccountBeingLoaded = null;

	/**
	 * Indicator for which transaction index to return from get by date.
	 *
	 * @value INSERT The index at which to insert a new transaction
	 * @value FIRST The first transaction on the date (-1 if not found)
	 * @value LAST The last transaction on the date (-1 if not found)
	 */
	private enum TxIndexType {
		INSERT, FIRST, LAST
	}

	/** All transactions indexed by ID. May contain gaps/null values */
	private final List<SimpleTxn> allTransactionsByID = new ArrayList<>();

	/** All transactions sorted by date. Will not contain null values */
	private final List<GenericTxn> allTransactionsByDate = new ArrayList<>();

	private List<Lot> lots = new ArrayList<Lot>();

	private final List<StockOption> stockOptions = new ArrayList<>();

	private QDate _currentDate;
	private QDate _asOfDate;

	private final AccountDetailsFixer accountDetailsFixer;

	// -------------------------------------

	public MoneyMgrModel(String name) {
		int nn = 0;
		String thename = name;
		while (MoneyMgrModel.models.containsKey(thename)) {
			thename = String.format("%s_%d", name, nn++);
		}

		this.name = thename;
		this.accountDetailsFixer = new AccountDetailsFixer(this);

		MoneyMgrModel.models.put(thename, this);
	}

	public QDate getCurrentDate() {
		return this._currentDate;
	}

	public void setCurrentDate(QDate date) {
		this._currentDate = date;
	}

	public QDate getAsOfDate() {
		return this._asOfDate;
	}

	public void setAsOfDate(QDate date) {
		this._asOfDate = date;
	}

	public List<Category> getCategories() {
		return Collections.unmodifiableList(this.categories);
	}

	public int nextCategoryID() {
		return (categories.isEmpty()) ? 1 : categories.size();
	}

	public void addCategory(Category cat) {
		assert (findCategory(cat.name) == null);

		while (categories.size() <= cat.catid) {
			categories.add(null);
		}

		categories.set(cat.catid, cat);
	}

	public Category getCategory(int catid) {
		return ((catid > 0) && (catid < categories.size())) //
				? categories.get(catid) //
				: null;
	}

	public Category findCategory(String name) {
		if (!name.isEmpty()) {
			for (Category cat : categories) {
				if ((cat != null) && cat.name.equals(name)) {
					return cat;
				}
			}
		}

		return null;
	}

	public int parseCategory(String s) {
		if (s.startsWith("[") || s.startsWith("Transfer:[")) {
			s = s.substring(s.indexOf("[") + 1, s.length() - 1).trim();

			Account acct = findAccount(s);

			return (short) ((acct != null) ? (-acct.acctid) : 0);
		}

		int slash = s.indexOf('/');
		if (slash >= 0) {
			// Throw away tag
			s = s.substring(slash + 1);
		}

		Category cat = findCategory(s);

		return (cat != null) ? (cat.catid) : 0;
	}

	// -------------------------------------

	public int nextSecurityId() {
		return securities.size() + 1;
	}

	public Collection<Security> getSecurities() {
		return Collections.unmodifiableCollection(securities.values());
	}

	public List<Security> getSecuritiesById() {
		return Collections.unmodifiableList(securitiesByID);
	}

	/**
	 * Introduce a new security - checks for already existing security first.<br>
	 * It is an error if the name or symbol is already used.
	 */
	public void addSecurity(Security sec) {
		Security existingByName = findSecurityByName(sec.getName());
		if (existingByName != null) {
			Common.reportError("Adding duplicate security name '" + sec.getName() + "'");
		}

		Security existingBySymbol = (sec.symbol != null) ? securities.get(sec.symbol) : null;
		if (existingBySymbol != null) {
			Common.reportError("Adding duplicate security symbol '" + sec.symbol + "'");
		}

// TODO	seems like a bad test
		// if (sec.secid != (securities.size() + 1)) {
//			Common.reportError("Bad security id '" + sec.secid + "'" //
//					+ " should be " + (securities.size() + 1));
//		}

		while (securitiesByID.size() <= sec.secid) {
			securitiesByID.add(null);
		}

		securitiesByID.set(sec.secid, sec);
		securities.put(sec.symbol.toUpperCase(), sec);
	}

	public Security getSecurity(int secid) {
		return (secid >= 0 && this.securitiesByID.size() > secid) //
				? securitiesByID.get(secid) //
				: null;
	}

	/** Look up a security whose name or symbol matches an input string. */
	public Security findSecurity(String nameOrSymbol) {
		final Security s = findSecurityBySymbol(nameOrSymbol);

		return (s != null) ? s : findSecurityByName(nameOrSymbol);
	}

	/**
	 * Look up a security whose name matches an input string.<br>
	 * Quicken windows QIF export uses security name, not symbol.
	 */
	public Security findSecurityByName(String name) {
		for (Security sec : securities.values()) {
			if ((sec != null) && sec.names.contains(name)) {
				return sec;
			}
		}

		return null;
	}

	/** Look up a security whose symbol matches an input string. */
	public Security findSecurityBySymbol(String sym) {
		return securities.get(sym.toUpperCase());
	}

	// -------------------------------------

	public Account makeAccount( //
			String name, AccountType type, String desc, QDate closeDate, //
			int statFreq, int statDayOfMonth) {
		Account acct = findAccount(name);

		if (acct == null) {
			type = this.accountDetailsFixer.fixType(name, type);
			acct = new Account(name, type, desc, closeDate, statFreq, statDayOfMonth);
			addAccount(acct);
		} else {
			this.accountDetailsFixer.updateAccount(acct, //
					closeDate, statFreq, statDayOfMonth);
		}

		return acct;
	}

	public int nextAccountID() {
		return (accountsByID.isEmpty()) ? 1 : accountsByID.size();
	}

	public int getNumAccounts() {
		return accounts.size();
	}

	/** Return list of accounts ordered by date - no nulls */
	public List<Account> getAccounts() {
		return Collections.unmodifiableList(accounts);
	}

	/** Return list of accounts indexed by id - contains nulls */
	public List<Account> getAccountsById() {
		return Collections.unmodifiableList(accountsByID);
	}

	public Account getAccountByID(int acctid) {
		return (acctid > 0 && acctid < this.accountsByID.size()) //
				? this.accountsByID.get(acctid) //
				: null;
	}

	/** Get Account list sorted on isOpen|type|name */
	public List<Account> getSortedAccounts(boolean showToday) {
		List<Account> accts = new ArrayList<>();
		QDate thedate = (showToday) ? QDate.today() : getAsOfDate();

		for (Account acct : getAccounts()) {
			if (acct.isOpenAsOf(thedate)) {
				accts.add(acct);
			}
		}

		Collections.sort(accts, new Comparator<Account>() {
			public int compare(Account a1, Account a2) {
				return compareAccountsByTypeAndName(a1, a2);
			}
		});

		return accts;
	}

	/** Look up an account by name */
	public Account findAccount(String name) {
		name = name.toLowerCase();

		for (Account acct : getAccounts()) {
			if (acct.name.equalsIgnoreCase(name)) {
				return acct;
			}
		}

		for (Account acct : getAccounts()) {
			if (acct.name.toLowerCase().startsWith(name)) {
				Common.reportWarning(String.format( //
						"Returning inexact account '%s' for '%s'", //
						acct.name, name));
				return acct;
			}
		}

		return null;
	}

	/** Add an account, maintaining proper ordering in the list(s) */
	public void addAccount(Account acct) {
		if (acct.acctid == 0) {
			Common.reportError("Account '" + acct.name + "' has zero acctid");
		}

		while (this.accountsByID.size() <= acct.acctid) {
			this.accountsByID.add(null);
		}

		this.accountsByID.set(acct.acctid, acct);
		this.accounts.add(acct);

		this.currAccountBeingLoaded = acct;

		Collections.sort(this.accounts, (a1, a2) -> {
			return compareAccountsByTxnDateAndName(a1, a2);
		});
	}

	/** Compare two accounts by date of first and last transaction, then name */
	private static int compareAccountsByTxnDateAndName(Account a1, Account a2) {
		if (a1 == null) {
			return (a2 == null) ? 0 : 1;
		} else if (a2 == null) {
			return -1;
		}

		int ct1 = a1.getNumTransactions();
		int ct2 = a2.getNumTransactions();

		if (ct1 == 0) {
			return (ct2 == 0) ? 0 : -1;
		} else if (ct2 == 0) {
			return 1;
		}

		List<GenericTxn> txns1 = a1.getTransactions();
		List<GenericTxn> txns2 = a2.getTransactions();

		final GenericTxn firsttxn1 = txns1.get(0);
		final GenericTxn lasttxn1 = txns1.get(ct1 - 1);
		final GenericTxn firsttxn2 = txns2.get(0);
		final GenericTxn lasttxn2 = txns2.get(ct2 - 1);

		int diff = firsttxn1.getDate().compareTo(firsttxn2.getDate());
		if (diff != 0) {
			return diff;
		}

		diff = lasttxn1.getDate().compareTo(lasttxn2.getDate());
		if (diff != 0) {
			return diff;
		}

		return a1.name.compareTo(a2.name);
	}

	/** Compare two accounts by date of first and last transaction, then name */
	private static int compareAccountsByTypeAndName(Account a1, Account a2) {
		int diff;

		if (a1.type != a2.type) {
			AccountCategory cat1 = AccountCategory.forAccountType(a1.type);
			AccountCategory cat2 = AccountCategory.forAccountType(a2.type);

			diff = cat1.getAccountListOrder() - cat2.getAccountListOrder();
			if (diff != 0) {
				return diff;
			}

			return a1.type.compareTo(a2.type);
		}

		QDate aod = a1.model.getAsOfDate();

		BigDecimal cv1 = a1.getValueForDate(aod).abs();
		BigDecimal cv2 = a2.getValueForDate(aod).abs();

		diff = cv2.subtract(cv1).signum();
		return (diff != 0) //
				? diff //
				: a1.name.compareTo(a2.name);
	}

	/**
	 * TODO This duplicates TransactionCleaner findMatchesForTransfer()<br>
	 * Find existing transaction(s) that match a transaction being loaded.<br>
	 * Date is close, amount matches (or the amount of a split).
	 */
	public List<SimpleTxn> findMatchingTransactions(Account acct, SimpleTxn tx) {
		List<SimpleTxn> ret = acct.findMatchingTransactions(tx, false);

		if (ret.size() > 1) {
			List<SimpleTxn> newret = new ArrayList<>(ret);
			for (Iterator<SimpleTxn> iter = newret.iterator(); iter.hasNext();) {
				SimpleTxn st = iter.next();
				if (!st.getDate().equals(tx.getDate())) {
					iter.remove();
				} else if (st.getCatid() != tx.getCatid()) {
					iter.remove();
				}
			}

			if (!newret.isEmpty() && newret.size() < ret.size()) {
//				if (newret.size() > 1 //
//						&& !Common.isEffectivelyZero(tx.getAmount())) {
//					System.out.println("xyzzy");
//				}
				ret = newret;
			}
		}

		if (ret.size() > 1) {
			int parentmemomatch = 0;
			int memomatch = 0;
			int checkmatch = 0;
			SimpleTxn parentmemotx = null;
			SimpleTxn memotx = null;
			SimpleTxn checktx = null;

			for (SimpleTxn stx : ret) {
				SimpleTxn mtxn = getMatchTx(tx, stx);

				if (mtxn.getMemo().equals(tx.getMemo())) {
					++memomatch;
					memotx = stx;
				}
				if ((mtxn instanceof SplitTxn) && (tx instanceof SplitTxn)) {
					// System.out.println("xyzzy " + tx.toString());
					if (tx.toString().contains("7/11/89")) {
						// System.out.println("xyzzy");
					}
					GenericTxn mtxn_parent = ((SplitTxn) mtxn).getContainingTxn();
					GenericTxn tx_parent = ((SplitTxn) tx).getContainingTxn();
					if (!tx.getMemo().isEmpty()) {
						if (tx.getMemo().equals(mtxn.getMemo()) //
								|| tx.getMemo().equals(mtxn_parent.getMemo())) {
							++parentmemomatch;
							parentmemotx = stx;
						}
					}
					if (!tx_parent.getMemo().isEmpty()) {
						if (tx_parent.getMemo().equals(mtxn.getMemo()) //
								|| tx_parent.getMemo().equals(mtxn_parent.getMemo())) {
							++parentmemomatch;
							parentmemotx = stx;
						}
					}
				}
				if (mtxn.getCheckNumber() == tx.getCheckNumber()) {
					++checkmatch;
					checktx = stx;
				}
			}

			if (checkmatch == 1) {
				ret.clear();
				ret.add(checktx);
			} else if (parentmemomatch == 1) {
				ret.clear();
				ret.add(parentmemotx);
			} else if (memomatch == 1) {
				ret.clear();
				ret.add(memotx);
			}
		}

		if (ret.size() > 1) {
//			System.out.println(tx.toString());
//			System.out.println("xyzzy");
		}
		return ret;
	}

	public SimpleTxn getMatchTx(SimpleTxn tx, SimpleTxn matchtxn) {
		if (Common.isEffectivelyEqual(tx.getAmount().abs(), matchtxn.getAmount().abs())) {
			return matchtxn;
		}

		SimpleTxn xtxn = matchtxn.getCashTransferTxn();
		if (xtxn != null) {
			matchtxn = xtxn;
		}

		if (matchtxn.hasSplits()) {
			for (SimpleTxn stx : matchtxn.getSplits()) {
				if (Common.isEffectivelyEqual(tx.getAmount().abs(), stx.getAmount().abs())) {
					return stx;
				}
			}
		}

		Common.reportError("Can't find match");
		return null;
	}

	// -------------------------------------

	public int createTxid() {
		return this.nextTxid++;
	}

	/** Compare two transactions by date, ascending */
	private static final Comparator<GenericTxn> compareByDate = new Comparator<GenericTxn>() {
		public int compare(GenericTxn o1, GenericTxn o2) {
			return o1.getDate().subtract(o2.getDate());
		}
	};

	public static GenericTxn SEARCH() {
		if (SEARCH_TX == null) {
			SEARCH_TX = new NonInvestmentTxn(0);
		}

		return SEARCH_TX;
	}

	/** Return transaction list indexed by ID */
	public List<SimpleTxn> getAllTransactions() {
		return Collections.unmodifiableList(allTransactionsByID);
	}

	/** Return transaction list ordered by ascending date */
	public List<GenericTxn> getTransactionsByDate() {
		return Collections.unmodifiableList(allTransactionsByDate);
	}

	public GenericTxn getTransaction(int txid) {
		SimpleTxn txn = getSimpleTransaction(txid);

		return (txn instanceof GenericTxn) ? (GenericTxn) txn : null;
	}

	public SimpleTxn getSimpleTransaction(int txid) {
		if ((txid > 0) && (txid < this.allTransactionsByID.size())) {
			return this.allTransactionsByID.get(txid);
		}

		return null;
	}

	/** Add a new transaction to the appropriate collection(s) */
	public void addTransaction(SimpleTxn txn) {
		int txid = txn.getTxid();

		if (txid <= 0 || txn.getAccountID() <= 0) {
			return;
		}

		while (this.allTransactionsByID.size() <= txid) {
			this.allTransactionsByID.add(null);
		}

		if (this.allTransactionsByID.get(txid) != null) {
			Common.reportWarning(String.format("Replacing transaction %d", txid));
			Common.reportInfo(this.allTransactionsByID.get(txid).toString());
			Common.reportInfo(txn.toString());
		}

		this.allTransactionsByID.set(txid, txn);

		if ((txn instanceof GenericTxn) && (txn.getDate() != null)) {
			addTransactionDate((GenericTxn) txn);
		}
	}

	/** Insert a transaction into the date-sorted list */
	private void addTransactionDate(GenericTxn txn) {
		int idx = getTransactionInsertIndexByDate(allTransactionsByDate, txn);

		allTransactionsByDate.add(idx, txn);
	}

	/** Fix up information about a transaction whose date has changed */
	public void changeTransactionDate(GenericTxn txn, QDate olddate) {
		if (txn.getAccountID() != 0) {
			// TODOif (olddate != null) {
			this.allTransactionsByDate.remove(txn);
			// }

			if (txn.getDate() != null) {
				addTransactionDate(txn);
			}
		}
	}

	/** Return the date of the earliest transaction */
	public QDate getFirstTransactionDate() {
		return (allTransactionsByDate.isEmpty()) //
				? null //
				: allTransactionsByDate.get(0).getDate();
	}

	/** Return the date of the last transaction */
	public QDate getLastTransactionDate() {
		return (allTransactionsByDate == null || allTransactionsByDate.isEmpty()) //
				? null //
				: allTransactionsByDate.get(allTransactionsByDate.size() - 1).getDate();
	}

	/**
	 * Return investment transactions for a date range
	 *
	 * @param start Earliest date to include
	 * @param end   Latest date to include
	 */
	public List<GenericTxn> getInvestmentTransactions(QDate start, QDate end) {
		List<GenericTxn> txns = new ArrayList<>();

		for (GenericTxn txn : getTransactions(start, end)) {
			if (txn instanceof InvestmentTxn) {
				txns.add(txn);
			}
		}

		return txns;
	}

	/**
	 * Return all transactions for a date range
	 *
	 * @param start Earliest date to include
	 * @param end   Latest date to include
	 */
	private List<GenericTxn> getTransactions(QDate start, QDate end) {
		// TODO Why not firstOnOrAfter(start) to lastOnOrBefore(end)?????
		int idx1 = getFirstTransactionIndexByDate(allTransactionsByDate, start);
		int idx2 = getLastTransactionIndexByDate(allTransactionsByDate, end);

		if (idx1 < 0) {
			// Calculate closest index for no match (see Collections.binarySearch)
			// This will be the first transaction after the start date (or end
			// of the list)
			idx1 = -idx1 - 1;
		}

		if (idx2 < 0) {
			// Calculate closest index for no match (see Collections.binarySearch)
			// This will be the first tx after the target date
			idx2 = -idx2 - 1;

			// Move idx back to previous transaction if possible
			// (If it is already zero, there are no transactions to return)
			if (idx2 > 0) {
				--idx2;
			}
		} else {
			// Advance index to the last matching transaction
			while ((idx2 >= 0) //
					&& ((idx2 + 1) < allTransactionsByDate.size()) //
					&& allTransactionsByDate.get(idx2 + 1).getDate().equals(end)) {
				++idx2;
			}
		}

		return allTransactionsByDate.subList(idx1, idx2);
	}

	/** Return the largest current transaction ID */
	public int getLastTransactionId() {
		return this.allTransactionsByID.size() + 1;
	}

	/**
	 * Return the index of the last transaction on or prior to a given date.<br>
	 * Return -1 if there is no such transaction.
	 */
	public int getLastTransactionIndexOnOrBeforeDate( //
			List<? extends GenericTxn> txns, QDate d) {
		if (txns.isEmpty()) {
			return -1;
		}

		int idx = getLastTransactionIndexByDate(txns, d);
		if (idx >= 0) {
			return idx;
		}

		idx = getTransactionInsertIndexByDate(txns, SEARCH());

		return (idx > 0) ? idx - 1 : -1;
	}

	/** Return the index of the first transaction on a date. (<0 if none) */
	private static int getFirstTransactionIndexByDate( //
			List<? extends GenericTxn> txns, QDate date) {
		SEARCH().setDate(date);

		return getTransactionIndexByDate(txns, SEARCH(), TxIndexType.FIRST);
	}

	/** Return the index of the last transaction on a date. (<0 if none) */
	private static int getLastTransactionIndexByDate( //
			List<? extends GenericTxn> txns, QDate date) {
		SEARCH().setDate(date);

		return getTransactionIndexByDate(txns, SEARCH(), TxIndexType.LAST);
	}

	/** Return the index for inserting a transaction into a list sorted by date */
	public int getTransactionInsertIndexByDate( //
			List<? extends GenericTxn> txns, GenericTxn txn) {
		return getTransactionIndexByDate(txns, txn, TxIndexType.INSERT);
	}

	/**
	 * Return the index for inserting a transaction into a list sorted by date
	 *
	 * @param txn
	 * @param before If true, the insertion point before the first txn on the date
	 *               otherwise, the point after all txns on the date.
	 * @return Index for insert (cannot be negative)
	 */
	private int getTransactionInsertIndexByDate( //
			List<? extends GenericTxn> txns, QDate date) {
		SEARCH().setDate(date);

		return getTransactionIndexByDate(txns, SEARCH(), TxIndexType.INSERT);
	}

	/**
	 * Return the index of a transaction in a list sorted by date
	 *
	 * @param txn
	 * @param TxIndexType If FIRST/LAST, the index of the first/last txn on that
	 *                    date (<0 if no such txn exists);<br>
	 *                    If INSERT the index after all txns on or before the date.
	 * @return Index; -(insert index + 1) if FIRST/LAST and no match exists
	 */
	private static int getTransactionIndexByDate(List<? extends GenericTxn> txns, GenericTxn txn, TxIndexType which) {
		if (txns.isEmpty()) {
			return (which == TxIndexType.INSERT) ? 0 : -1;
		}

		int sz = txns.size();

		int idx = Collections.binarySearch(txns, txn, compareByDate);

		// TODO If idx is >= 0, we found a match. If so, the date should match
		// exactly so I don't think this will do anything
		while ((idx >= 0) && (idx < sz)) {
			QDate dt = txns.get(idx).getDate();
			int diff = dt.subtract(txn.getDate());
			if (diff == 0) {
				break;
			}

			Common.reportError("getTransactionIndexByDate: strange date comparison");
		}

		if (idx < 0) {
			// There is no match - Return an insertion point that maintains order.
			int insertIdx = -idx - 1;

			if (which == TxIndexType.INSERT) {
				// Return the insertion point indicated by the search result.
				return insertIdx;
			}

			if (insertIdx >= txns.size()) {
				// make sure insertIdx is a valid transaction index
				insertIdx = txns.size() - 1;
			}

			// TODO It seems this is guaranteed since we did not find a match
			if (!txns.get(insertIdx).getDate().equals(txn.getDate())) {
				return idx;
			}

			// TODO Therefore this should be unreachable
			idx = insertIdx;
		}

		// We have a match for the date. Traverse to the requested position in
		// the list.
		switch (which) {
		case FIRST:
			// Go to the index of the first matching transaction
			while ((idx > 0) //
					&& txns.get(idx - 1).getDate().equals(txn.getDate())) {
				--idx;
			}
			break;

		case LAST:
			// Go to the index of the last matching transaction
			while (((idx + 1) < sz) //
					&& txns.get(idx + 1).getDate().equals(txn.getDate())) {
				++idx;
			}
			break;

		case INSERT:
			// Go to the index after the last matching transaction
			while ((idx < sz) //
					&& txns.get(idx).getDate().equals(txn.getDate())) {
				++idx;
			}
			break;
		}

		return idx;
	}

	// -------------------------------------

	public int nextLotId() {
		return (this.lots.isEmpty()) ? 1 : this.lots.size();
	}

	public void addLot(Lot lot) {
		while (this.lots.size() <= lot.lotid) {
			this.lots.add(null);
		}

		this.lots.set(lot.lotid, lot);
	}

	public final List<Lot> getLots() {
		return Collections.unmodifiableList(this.lots);
	}

	public final Lot getLot(int lotid) {
		if ((lotid < 0) || (lotid >= this.lots.size())) {
			return null;
		}

		return this.lots.get(lotid);
	}

	// -------------------------------------

	public int nextStockOptionId() {
		return (this.stockOptions.isEmpty()) ? 1 : this.stockOptions.size();
	}

	public void addStockOption(StockOption opt) {
		while (this.stockOptions.size() <= opt.optid) {
			this.stockOptions.add(null);
		}

		this.stockOptions.set(opt.optid, opt);
	}

	public StockOption getStockOption(int optid) {
		return ((optid > 0) && (optid < this.stockOptions.size())) //
				? this.stockOptions.get(optid) //
				: null;
	}

	public List<StockOption> getStockOptions() {
		return Collections.unmodifiableList(this.stockOptions);
	}

	public String toString() {
		return String.format("Model[%s]", this.name);
	}
}