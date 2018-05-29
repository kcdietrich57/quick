package qif.data;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Document Object Model for finances from Quicken */
public class QifDom {
	// The one and only financial model
	public static QifDom dom = null;

	// Various types of information we track in this model
	private final List<Category> categories;
	private final Map<String, Security> securities;
	/**
	 * This list of accounts may not contain nulls, size == numAccounts, index is
	 * unrelated to acctid
	 */
	private final List<Account> accounts;
	/**
	 * This list of accounts is indexed by acctid, size > numAccounts, and may
	 * contain nulls
	 */
	private final List<Account> accountsByID;
	private final List<GenericTxn> allTransactionsByID;

	public SecurityPortfolio portfolio;

	/** Pay attention to version of the loaded QIF file format */
	public int loadedStatementsVersion = -1;

	// Info about reconciled statements goes in this file
	private File stmtLogFile;

	// As we are loading data, we track the account context we are within here
	public Account currAccount = null;

	public QifDom(File qifDir) {
		QifDom.dom = this;

		this.categories = new ArrayList<Category>();
		this.accountsByID = new ArrayList<Account>();
		this.accounts = new ArrayList<Account>();
		this.securities = new HashMap<String, Security>();

		this.allTransactionsByID = new ArrayList<GenericTxn>();
		this.stmtLogFile = new File(qifDir, "statementLog.dat");

		this.portfolio = new SecurityPortfolio();
	}

	public int getNumAccounts() {
		return this.accounts.size();
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

	public Account getAccountByID(int acctid) {
		return this.accountsByID.get(acctid);
	}

	public File getStatementLogFile() {
		return this.stmtLogFile;
	}

	// Sort on isOpen|type|name
	public List<Account> getSortedAccounts() {
		List<Account> ret = new ArrayList<>(this.accounts);

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
		while (this.allTransactionsByID.size() < txn.txid + 1) {
			this.allTransactionsByID.add(null);
		}

		this.allTransactionsByID.set(txn.txid, txn);
	}

	public List<GenericTxn> getAllTransactions() {
		return Collections.unmodifiableList(this.allTransactionsByID);
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

		for (Account acct : this.accounts) {
			if (acct.getName().equalsIgnoreCase(name)) {
				return acct;
			}
		}

		for (Account acct : this.accounts) {
			if (acct.getName().toLowerCase().startsWith(name)) {
				return acct;
			}
		}

		return null;
	}

	public Category findCategory(String name) {
		for (Category cat : this.categories) {
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

		for (final Account a : this.accounts) {
			final BigDecimal amt = a.getValueForDate(d);

			b.netWorth = b.netWorth.add(amt);

			if (a.isAsset()) {
				b.assets = b.assets.add(amt);
			} else if (a.isLiability()) {
				b.liabilities = b.liabilities.add(amt);
			}
		}

		return b;
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

	// Process unreconciled statements for each account, matching statements
	// with transactions and logging the results.
	public void reconcileStatements() {
		PrintWriter pw = null;

		try {
			pw = new PrintWriter(new FileWriter(this.stmtLogFile, true));

			for (Account a : this.accounts) {
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
}
