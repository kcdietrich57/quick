package qif.data;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import qif.data.SecurityPosition.PositionInfo;
import qif.importer.AccountDetailsFixer;
import qif.ui.MainWindow;

class Foo {
	public enum InvCategory {
		Contribution, Match, Grant, Dividend,
	}
}

/** Represents an account */
public class Account {
	/** Account list ordered by first/last txn dates (no nulls) */
	private static final List<Account> accounts = new ArrayList<Account>();

	/** Account list indexed by acctid (size > numAccounts, nulls) */
	private static final List<Account> accountsByID = new ArrayList<Account>();

	/** Tracks current context as we are loading */
	public static Account currAccountBeingLoaded = null;

	public static Account makeAccount( //
			String name, AccountType type, String desc, QDate closeDate, //
			int statFreq, int statDayOfMonth) {
		Account acct = Account.findAccount(name);

		if (acct == null) {
			type = AccountDetailsFixer.fixType(name, type);
			acct = new Account(name, type, desc, closeDate, statFreq, statDayOfMonth);
			Account.addAccount(acct);
		} else {
			AccountDetailsFixer.updateAccount(acct, //
					closeDate, statFreq, statDayOfMonth);
		}

		return acct;
	}

	public static int getNextAccountID() {
		return (accountsByID.isEmpty()) ? 1 : accountsByID.size();
	}

	public static int getNumAccounts() {
		return accounts.size();
	}

	public static List<Account> getAccounts() {
		return Collections.unmodifiableList(accounts);
	}

	public static Account getAccountByID(int acctid) {
		return accountsByID.get(acctid);
	}

	/** Compare two accounts by date of first and last transaction, then name */
	private static int compareAccountsByTxnDateAndName(Account a1, Account a2) {
		if (a1 == null) {
			return (a2 == null) ? 0 : 1;
		} else if (a2 == null) {
			return -1;
		}

		int ct1 = a1.transactions.size();
		int ct2 = a2.transactions.size();

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

		BigDecimal cv1 = a1.getValueForDate(MainWindow.instance.asOfDate).abs();
		BigDecimal cv2 = a2.getValueForDate(MainWindow.instance.asOfDate).abs();

		diff = cv2.subtract(cv1).signum();
		return (diff != 0) //
				? diff //
				: a1.name.compareTo(a2.name);
	}

	/** Get Account list sorted on isOpen|type|name */
	public static List<Account> getSortedAccounts() {
		List<Account> accts = new ArrayList<Account>();

		for (Account acct : accounts) {
			if (acct.isOpenAsOf(MainWindow.instance.asOfDate)) {
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

	public static void addAccount(Account acct) {
		if (acct.acctid == 0) {
			// acct.acctid = getNextAccountID();
			Common.reportError("Account '" + acct.name + "' has zero acctid");
		}

		while (accountsByID.size() <= acct.acctid) {
			accountsByID.add(null);
		}

		accountsByID.set(acct.acctid, acct);
		accounts.add(acct);

		Account.currAccountBeingLoaded = acct;

		Collections.sort(accounts, (a1, a2) -> {
			return compareAccountsByTxnDateAndName(a1, a2);
		});
	}

	public static Account findAccount(String name) {
		name = name.toLowerCase();

		for (Account acct : accounts) {
			if (acct.name.equalsIgnoreCase(name)) {
				return acct;
			}
		}

		for (Account acct : accounts) {
			if (acct.name.toLowerCase().startsWith(name)) {
				return acct;
			}
		}

		return null;
	}

	public final int acctid;

	public final String name;
	public final AccountType type;
	public final AccountCategory acctCategory;
	public final String description;
	public QDate closeDate;
	public int statementFrequency;
	public int statementDayOfMonth;

	public BigDecimal balance;
	public BigDecimal clearedBalance;

	public List<GenericTxn> transactions;
	public List<Statement> statements;
	public SecurityPortfolio securities;

	public File statementFile;

	public Account(String name, AccountType type, String desc, QDate closeDate, //
			int statFreq, int statDayOfMonth) {
		this.acctid = getNextAccountID();

		this.name = name;
		this.type = type;
		this.acctCategory = AccountCategory.forAccountType(type);
		this.description = (desc != null) ? desc : "";
		this.closeDate = closeDate;
		this.statementFrequency = (statFreq > 0) ? statFreq : 30;
		this.statementDayOfMonth = (statDayOfMonth > 0) ? statDayOfMonth : 30;

		this.balance = this.clearedBalance = BigDecimal.ZERO;

		this.transactions = new ArrayList<GenericTxn>();
		this.statements = new ArrayList<Statement>();
		this.securities = new SecurityPortfolio();

		this.statementFile = null;
	}

	public Account(String name, AccountType type) {
		this(name, type, "", null, -1, -1);
	}

	public String getDisplayName(int length) {
		String nn = this.name;
		if (nn.length() > 36) {
			nn = nn.substring(0, 33) + "...";
		}

		return nn;
	}

	public boolean isLiability() {
		return this.type.isLiability();
	}

	public boolean isAsset() {
		return this.type.isAsset();
	}

	public boolean isInvestmentAccount() {
		return this.type.isInvestment();
	}

	public boolean isCashAccount() {
		return this.type.isCash();
	}

	public boolean isNonInvestmentAccount() {
		return this.type.isNonInvestment();
	}

	/** Return the date this account opened (i.e. the first transaction date) */
	public QDate getOpenDate() {
		return (this.transactions != null) //
				? this.transactions.get(0).getDate() //
				: QDate.today();
	}

	/** Was the account opened on or before a date */
	public boolean isOpenAsOf(QDate d) {
		QDate openDate = getOpenDate();

		return (openDate != null) && (openDate.compareTo(d) <= 0);
	}

	/** Was the account closed on or before date */
	private boolean isClosedAsOf(QDate d) {
		return (this.closeDate != null) && (this.closeDate.compareTo(d) <= 0);
	}

	/** Was the account open on a given date */
	public boolean isOpenOn(QDate d) {
		if (d == null) {
			d = QDate.today();
		}

		return isOpenAsOf(d) && !isClosedAsOf(d);
	}

	public void addTransaction(GenericTxn txn) {
		int idx = getTransactionIndexForDate(txn);

		this.transactions.add(idx, txn);
	}

	/** Find the index to insert a new transaction in a list sorted by date */
	private int getTransactionIndexForDate(QDate date) {
		GenericTxn t = new NonInvestmentTxn(1);
		t.setDate(date);

		return getTransactionIndexForDate(t);
	}

	/** Find the index to insert a new transaction in a list sorted by date */
	private int getTransactionIndexForDate(GenericTxn tx) {
		Comparator<GenericTxn> c = new Comparator<GenericTxn>() {
			public int compare(GenericTxn o1, GenericTxn o2) {
				return o1.getDate().subtract(o2.getDate());
			}
		};

		int idx = Collections.binarySearch(this.transactions, tx, c);

		if (idx < 0) {
			idx = -idx - 1;
		}

		return idx;
	}

	/** Get the last statement for the account (closed or not) */
	public Statement getLastStatement() {
		return (this.statements.isEmpty()) //
				? null //
				: this.statements.get(this.statements.size() - 1);
	}

	/** Get the date of the last balanced statement for this account */
	public QDate getLastBalancedStatementDate() {
		for (int ii = this.statements.size() - 1; ii >= 0; --ii) {
			Statement stmt = this.statements.get(ii);

			if (stmt.isBalanced) {
				return stmt.date;
			}
		}

		return null;
	}

	/** Get the date of the first non-balanced statement for this account */
	public Statement getFirstUnbalancedStatement() {
		for (int ii = 0; ii < this.statements.size(); ++ii) {
			Statement stmt = this.statements.get(ii);

			if (!stmt.isBalanced) {
				return stmt;
			}
		}

		return null;
	}

	public boolean isStatementDue() {
		return getDaysUntilStatementIsDue() <= 0;
	}

	public int getDaysUntilStatementIsDue() {
		QDate laststmt = getLastBalancedStatementDate();
		if ((laststmt == null) || (this.statementFrequency <= 0)) {
			return -1;
		}

		QDate duedate = laststmt.addDays(this.statementFrequency);
		return duedate.subtract(QDate.today());
	}

	private Statement getFirstStatementAfter(QDate date) {
		for (int ii = this.statements.size() - 1; ii >= 0; --ii) {
			Statement stmt = this.statements.get(ii);

			if (date.compareTo(stmt.date) > 0) {
				return stmt;
			}
		}

		return null;
	}

	/** Get the statement with a specific closing date */
	public Statement getStatement(QDate date) {
		return getStatement(date, null);
	}

	/** Get the statement with a specific closing date and balance */
	public Statement getStatement(QDate date, BigDecimal balance) {
		if (date == null) {
			return null;
		}

		for (final Statement s : this.statements) {
			if (s.date.compareTo(date) > 0) {
				break;
			}

			if (s.date.compareTo(date) == 0) {
				if ((balance == null) //
						|| (s.closingBalance.compareTo(balance) == 0)) {
					return s;
				}
			}
		}

		Common.reportError("Can't find statement: " //
				+ this.name + " " //
				+ date.toString() + " " //
				+ Common.formatAmount(balance));
		return null;
	}

	/**
	 * Return the date of the next expected statement after the last balanced stmt
	 */
	public QDate getNextStatementDate() {
		QDate laststat = getLastBalancedStatementDate();

		if (laststat == null) {
			QDate today = QDate.today();
			return new QDate(today.getYear(), //
					today.getMonth(), //
					(this.statementDayOfMonth > 0) ? this.statementDayOfMonth : 31);
		}

		QDate nextstmt = laststat.addDays( //
				(this.statementFrequency > 0) //
						? this.statementFrequency //
						: 30);

		if (this.statementDayOfMonth > 0) {
			nextstmt = nextstmt.getDateNearestTo(this.statementDayOfMonth);
		}

		return nextstmt;
	}

	/**
	 * Return the next non-balanced statement to reconcile.<br>
	 * If none exists, we create one closing on the next expected date.
	 */
	public Statement getNextStatementToReconcile() {
		Statement stat = getFirstUnbalancedStatement();
		if (stat == null) {
			QDate laststmtdate = getLastBalancedStatementDate();
			Statement laststmt = getStatement(laststmtdate);
			stat = new Statement(this.acctid);

			stat.date = getNextStatementDate();
			stat.prevStatement = laststmt;
		}

		// Fill statement with transactions up to the closing date
		if (stat.transactions.isEmpty()) {
			stat.addTransactions(getUnclearedTransactions(), true);
		}

		return stat;
	}

	/**
	 * Return a statement with all uncleared transactions that do not belong to a
	 * statement already. (Null if there are none)
	 */
	public Statement getUnclearedStatement() {
		// Make sure statement(s) needing reconcile have their transactions
		getNextStatementToReconcile();

		Statement stat = null;

		if (MainWindow.instance.asOfDate.compareTo(QDate.today()) < 0) {
			stat = getFirstStatementAfter(MainWindow.instance.asOfDate);
			if (stat == null) {
				stat = new Statement(this.acctid);
			}
		} else {
			List<GenericTxn> txns = getUnclearedTransactions();

			if (txns.isEmpty()) {
				return null;
			}

			stat = new Statement(this.acctid);
			stat.date = QDate.today();
			stat.addTransactions(txns);
		}

		return stat;
	}

	/**
	 * Construct a statement for any uncleared transactions not already in a
	 * statement, up to the current date.
	 */
	public Statement createUnclearedStatement(Statement laststmt) {
		List<GenericTxn> txns = new ArrayList<GenericTxn>();

		if (laststmt != null) {
			int laststmtidx = this.statements.indexOf(laststmt);

			addTransactionsToAsOfDate(txns, laststmt.unclearedTransactions);

			if (laststmtidx < this.statements.size() - 1) {
				Statement nextstmt = this.statements.get(laststmtidx + 1);

				addTransactionsToAsOfDate(txns, nextstmt.transactions);
				addTransactionsToAsOfDate(txns, nextstmt.unclearedTransactions);
			}
		} else {
			addTransactionsToAsOfDate(txns, this.transactions);
		}

		Statement stmt = new Statement(this.acctid);
		Common.sortTransactionsByDate(txns);
		stmt.addTransactions(txns);
		stmt.date = MainWindow.instance.asOfDate;

		return stmt;
	}

	/** Add txns from one list to another if date <= current date */
	private void addTransactionsToAsOfDate(List<GenericTxn> txns, List<GenericTxn> srctxns) {
		for (GenericTxn txn : srctxns) {
			if ((txn.getDate().compareTo(MainWindow.instance.asOfDate) <= 0) //
					&& !txns.contains(txn)) {
				txns.add(txn);
			}
		}
	}

	/** Get the date of the first transaction */
	public QDate getFirstTransactionDate() {
		return (this.transactions.isEmpty()) ? null : this.transactions.get(0).getDate();
	}

	/** Get the date of the last non-cleared transaction */
	public QDate getFirstUnclearedTransactionDate() {
		int txidx = getFirstUnclearedTransactionIndex();
		return (txidx < 0) ? null : this.transactions.get(txidx).getDate();
	}

	public GenericTxn getFirstUnclearedTransaction() {
		int txidx = getFirstUnclearedTransactionIndex();
		return (txidx < 0) ? null : this.transactions.get(txidx);
	}

	/** Get the index of the first uncleared transaction */
	private int getFirstUnclearedTransactionIndex() {
		for (int ii = 0; ii < this.transactions.size(); ++ii) {
			GenericTxn t = this.transactions.get(ii);

			if ((t != null) && (t.stmtdate == null)) {
				return ii;
			}
		}

		return -1;
	}

	/** Return count of all unreconciled transactions */
	public int getUnclearedTransactionCount() {
		int count = 0;

		for (GenericTxn t : this.transactions) {
			if ((t != null) && (t.stmtdate == null)) {
				++count;
			}
		}

		return count;
	}

	/** Return a new list of uncleared txns that don't belong to any statement */
	private List<GenericTxn> getUnclearedTransactions() {
		List<GenericTxn> txns = new ArrayList<GenericTxn>();

		// Gather transactions not belonging to a statement
		for (int txidx = getFirstUnclearedTransactionIndex(); //
				(txidx >= 0) && (txidx < this.transactions.size()); //
				++txidx) {
			GenericTxn t = this.transactions.get(txidx);

			if ((t != null) && (t.stmtdate == null)) {
				txns.add(t);
			}
		}

		// Add transactions that belong to non-balanced statements
		for (int statidx = this.statements.size() - 1; statidx >= 0; --statidx) {
			Statement stmt = this.statements.get(statidx);

			if (stmt.isBalanced) {
				break;
			}

			txns.removeAll(stmt.transactions);
		}

		return txns;
	}

	/** Return the account value as of today */
	public BigDecimal getCurrentValue() {
		return getValueForDate(QDate.today());
	}

	/** Return the account value as of a specified date */
	public BigDecimal getValueForDate(QDate d) {
		BigDecimal cashBal = getCashValueForDate(d);
		BigDecimal secBal = this.securities.getPortfolioValueForDate(d);

		BigDecimal acctValue = cashBal.add(secBal);

		acctValue = acctValue.setScale(2, RoundingMode.HALF_UP);

		return acctValue;
	}

	/** Get account cash value for a specified date */
	private BigDecimal getCashValueForDate(QDate d) {
		BigDecimal cashBal = null;

		int idx = GenericTxn.getLastTransactionIndexOnOrBeforeDate(this.transactions, d);

		while ((idx >= 0) && (cashBal == null)) {
			GenericTxn tx = this.transactions.get(idx--);

			cashBal = tx.runningTotal;
		}

		return (cashBal != null) ? cashBal : BigDecimal.ZERO;
	}

	/** Get the value of securities for this account on a given date */
	public BigDecimal getSecuritiesValueForDate(QDate d) {
		BigDecimal portValue = BigDecimal.ZERO;

		for (final SecurityPosition pos : this.securities.positions) {
			BigDecimal posamt = pos.getValueForDate(d);

			portValue = portValue.add(posamt);
		}

		return portValue;
	}

	/** Get the value of a single security for this account on a specified date */
	public PositionInfo getSecurityValueForDate(Security sec, QDate d) {
		SecurityPosition pos = this.securities.getPosition(sec.secid);

		return (pos == null) //
				? new PositionInfo(sec, d) //
				: new PositionInfo(sec, d, //
						pos.getSharesForDate(d), //
						sec.getPriceForDate(d).getPrice(), //
						pos.getValueForDate(d));
	}

	/**
	 * Find existing transaction(s) that match a transaction being loaded.<br>
	 * Date is close, amount matches (or the amount of a split).
	 */
	public List<GenericTxn> findMatchingTransactions(SimpleTxn tx, QDate date) {
		List<GenericTxn> txns = new ArrayList<GenericTxn>();

		BigDecimal amt = tx.getAmount().abs();

		int idx = getTransactionIndexForDate(date);
		for (; idx > 0; --idx) {
			if (date.subtract(this.transactions.get(idx - 1).getDate()) > 5) {
				break;
			}
		}

		for (; idx < this.transactions.size(); ++idx) {
			GenericTxn t = this.transactions.get(idx);
			int diff = t.getDate().subtract(date);
			if (diff > 5) {
				break;
			}
			if (-diff > 5) {
				continue;
			}

			boolean match = false;

			if (t.getAmount().abs().equals(amt)) {
				match = true;
			} else if (t.hasSplits()) {
				for (SimpleTxn st : t.getSplits()) {
					if (st instanceof MultiSplitTxn) {
						for (SimpleTxn mst : ((MultiSplitTxn) st).subsplits) {
							if (mst.getAmount().abs().equals(amt)) {
								match = true;
							}
						}
					} else if (st.getAmount().abs().equals(amt)) {
						match = true;
					}
				}
			}

			if (match) {
				txns.add(t);
			}
		}

		txns.sort(new Comparator<GenericTxn>() {
			public int compare(GenericTxn o1, GenericTxn o2) {
				int diff1 = Math.abs(o1.getDate().subtract(date));
				int diff2 = Math.abs(o2.getDate().subtract(date));

				return diff1 - diff2;
			}
		});

		return txns;
	}

	/**
	 * Gather transactions that might belong to a statement.<br>
	 * i.e., They do not yet belong to a statement and their date is on or before
	 * the statement closing date.
	 */
	public List<GenericTxn> gatherTransactionsForStatement(Statement s) {
		List<GenericTxn> txns = new ArrayList<GenericTxn>();

		int idx1 = getFirstUnclearedTransactionIndex();
		if (idx1 < 0) {
			return txns;
		}

		for (int ii = idx1; ii < this.transactions.size(); ++ii) {
			GenericTxn t = this.transactions.get(ii);

			if (t.getDate().compareTo(s.date) > 0) {
				break;
			}

			if (!t.isCleared()) {
				txns.add(t);
			}
		}

		Common.sortTransactionsByDate(txns);

		return txns;
	}

	/**
	 * Return security positions in this account that are open on a specified date.
	 */
	public Map<Security, PositionInfo> getOpenPositionsForDate(QDate d) {
		return this.securities.getOpenPositionsForDate(d);
	}

	public String toString() {
		String s = "Account" + this.acctid + ": " + this.name //
				+ " type=" + this.type //
				+ " clbal=" + this.clearedBalance //
				+ " bal=" + this.balance //
				+ " desc=" + this.description //
				+ " #tx= " + this.transactions.size() //
				+ "\n";

		s += this.securities.toString();

		return s;
	}
}
