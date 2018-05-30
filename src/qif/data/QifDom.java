package qif.data;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/** Document Object Model for finances from Quicken */
public class QifDom {
	// The one and only financial model
	public static QifDom dom = null;

	// TODO move to Account class
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

	// TODO move to Transaction class
	private final List<GenericTxn> allTransactionsByID;

	// TODO move to Portfolio class
	public SecurityPortfolio portfolio;

	// TODO move to Statement class
	/** Pay attention to version of the loaded QIF file format */
	public int loadedStatementsVersion = -1;

	// Info about reconciled statements goes in this file
	private File stmtLogFile;

	// As we are loading data, we track the account context we are within here
	public Account currAccount = null;

	public QifDom(File qifDir) {
		QifDom.dom = this;

		this.accountsByID = new ArrayList<Account>();
		this.accounts = new ArrayList<Account>();

		this.allTransactionsByID = new ArrayList<GenericTxn>();
		this.stmtLogFile = new File(qifDir, "statementLog.dat");

		this.portfolio = new SecurityPortfolio();
	}

	public int getNumAccounts() {
		return this.accounts.size();
	}

	public List<Account> getAccounts() {
		return Collections.unmodifiableList(this.accounts);
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
		if (acct.acctid == 0) {
			acct.acctid = getNextAccountID();
		}

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

	public int getNextAccountID() {
		return (this.accountsByID.isEmpty()) ? 1 : this.accountsByID.size();
	}

	// Process unreconciled statements for each account, matching statements
	// with transactions and logging the results.
	public void reconcileStatements() {
		PrintWriter pw = null;

		try {
			pw = new PrintWriter(new FileWriter(this.stmtLogFile, true));

			for (Account a : this.accounts) {
				Reconciler.reconcileStatements(a, pw);
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
