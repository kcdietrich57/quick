package qif.data;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import qif.ui.MainWindow;

public class Account {
	/**
	 * This list of accounts may not contain nulls, size == numAccounts, index is
	 * unrelated to acctid
	 */
	public static final List<Account> accounts = new ArrayList<Account>();

	/**
	 * This list of accounts is indexed by acctid, size > numAccounts, and may
	 * contain nulls
	 */
	private static final List<Account> accountsByID = new ArrayList<Account>();

	// As we are loading data, we track the account context we are within here
	public static Account currAccount = null;

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

	// Sort on isOpen|type|name
	public static List<Account> getSortedAccounts() {
		List<Account> accts = new ArrayList<Account>();

		for (Account acct : accounts) {
			if (acct == null) {
				continue;
			}

			if (acct.isOpenOn(MainWindow.instance.asOfDate) //
					|| (acct.getOpenDate().compareTo(MainWindow.instance.asOfDate) <= 0)) {
				accts.add(acct);
			}
		}

		Collections.sort(accts, new Comparator<Account>() {
			public int compare(Account a1, Account a2) {
				if (a1.type != a2.type) {
					return a1.type.compareTo(a2.type);
				}

				return a1.getName().compareTo(a2.getName());
			}
		});

		return accts;
	}

	public static void addAccount(Account acct) {
		if (acct.acctid == 0) {
			acct.acctid = getNextAccountID();
		}

		while (accountsByID.size() <= acct.acctid) {
			accountsByID.add(null);
		}

		accountsByID.set(acct.acctid, acct);
		accounts.add(acct);

		currAccount = acct;

		Collections.sort(accounts, (a1, a2) -> {
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

	public static Account findAccount(String name) {
		name = name.toLowerCase();

		for (Account acct : accounts) {
			if (acct.getName().equalsIgnoreCase(name)) {
				return acct;
			}
		}

		for (Account acct : accounts) {
			if (acct.getName().toLowerCase().startsWith(name)) {
				return acct;
			}
		}

		return null;
	}

	public int acctid;

	private String name;
	public AccountType type;
	public String description;
	public QDate closeDate;
	public BigDecimal balance;
	public BigDecimal clearedBalance;
	public int statementFrequency = 30;
	public int statementDayOfMonth = 30;

	public List<GenericTxn> transactions;
	public List<Statement> statements;
	public SecurityPortfolio securities;

	public File statementFile;

	public Account() {
		this.acctid = 0;

		this.name = "";
		this.type = null;
		this.description = "";
		this.balance = this.clearedBalance = BigDecimal.ZERO;

		this.transactions = new ArrayList<GenericTxn>();
		this.statements = new ArrayList<Statement>();
		this.securities = new SecurityPortfolio();

		this.statementFile = null;
	}

	public QDate getOpenDate() {
		return (this.transactions != null) ? this.transactions.get(0).getDate() : QDate.today();
	}

	private boolean isOpenAsOf(QDate d) {
		QDate openDate = getOpenDate();

		return (openDate != null) && (openDate.compareTo(d) <= 0);
	}

	private boolean isClosedAsOf(QDate d) {
		return (this.closeDate != null) && (this.closeDate.compareTo(d) <= 0);
	}

	public boolean isOpenOn(QDate d) {
		if (d == null) {
			d = QDate.today();
		}

		return isOpenAsOf(d) && !isClosedAsOf(d);
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public QDate getLastStatementDate() {
		return (this.statements.isEmpty()) //
				? null //
				: this.statements.get(this.statements.size() - 1).date;
	}

	public Statement getFirstUnbalancedStatement() {
		if (this.statements.isEmpty()) {
			return null;
		}

		for (int ii = 0; ii < this.statements.size(); ++ii) {
			Statement stmt = this.statements.get(ii);

			if (!stmt.isBalanced) {
				return stmt;
			}
		}

		return null;
	}

	public boolean isStatementDue() {
		return getStatementDaysTillDue() <= 0;
	}

	public int getStatementDaysTillDue() {
		QDate laststmt = getLastBalancedStatementDate();
		if ((laststmt == null) || (this.statementFrequency <= 0)) {
			return -1;
		}

		QDate duedate = laststmt.addDays(this.statementFrequency);
		return duedate.subtract(QDate.today());
	}

	public QDate getLastBalancedStatementDate() {
		if (this.statements.isEmpty()) {
			return null;
		}

		for (int ii = this.statements.size() - 1; ii >= 0; --ii) {
			Statement stmt = this.statements.get(ii);

			if (stmt.isBalanced) {
				return stmt.date;
			}
		}

		return null;
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

	public Statement getLastStatement() {
		return (this.statements.isEmpty()) //
				? null //
				: this.statements.get(this.statements.size() - 1);
	}

	public Statement getStatement(QDate date) {
		return getStatement(date, null);
	}

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

	public Statement getNextStatementToReconcile() {
		Statement stat = getFirstUnbalancedStatement();
		if (stat == null) {
			QDate laststmtdate = getLastBalancedStatementDate();
			Statement laststmt = getStatement(laststmtdate);
			stat = new Statement(this.acctid);

			stat.date = getNextStatementDate();
			stat.prevStatement = laststmt;
		}

		if (stat.transactions.isEmpty()) {
			// NB. Only transactions up to the statement date
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

	public Statement getUnclearedStatement(Statement laststmt) {
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

	private void addTransactionsToAsOfDate(List<GenericTxn> txns, List<GenericTxn> srctxns) {
		for (GenericTxn txn : srctxns) {
			if ((txn.getDate().compareTo(MainWindow.instance.asOfDate) <= 0) //
					&& !txns.contains(txn)) {
				txns.add(txn);
			}
		}
	}

	public QDate getFirstTransactionDate() {
		return (this.transactions.isEmpty()) ? null : this.transactions.get(0).getDate();
	}

	public QDate getLastTransactionDate() {
		return (this.transactions.isEmpty()) ? null : this.transactions.get(this.transactions.size() - 1).getDate();
	}

	public QDate getFirstUnclearedTransactionDate() {
		GenericTxn t = getFirstUnclearedTransaction();

		return (t == null) ? null : t.getDate();
	}

	public int getFirstUnclearedTransactionIndex() {
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

		for (final GenericTxn t : this.transactions) {
			if ((t != null) && (t.stmtdate == null)) {
				++count;
			}
		}

		return count;
	}

	/**
	 * Return a new collection of uncleared transactions that don't belong to any
	 * statement
	 */
	private List<GenericTxn> getUnclearedTransactions() {
		List<GenericTxn> txns = new ArrayList<GenericTxn>();

		for (int txidx = getFirstUnclearedTransactionIndex(); //
				(txidx >= 0) && (txidx < this.transactions.size()); //
				++txidx) {
			GenericTxn t = this.transactions.get(txidx);

			if ((t != null) && (t.stmtdate == null)) {
				txns.add(t);
			}
		}

		for (int statidx = this.statements.size() - 1; statidx >= 0; --statidx) {
			Statement stmt = this.statements.get(statidx);

			if (stmt.isBalanced) {
				break;
			}

			txns.removeAll(stmt.transactions);
		}

		return txns;
	}

	public GenericTxn getFirstUnclearedTransaction() {
		for (GenericTxn t : this.transactions) {
			if ((t != null) && (t.stmtdate == null)) {
				return t;
			}
		}

		return null;
	}

	public boolean isInvestmentAccount() {
		switch (this.type) {
		case Bank:
		case Cash:
		case CCard:
		case Asset:
		case Liability:
			return false;

		case Inv401k:
		case InvMutual:
		case InvPort:
		case Invest:
			return true;

		default:
			Common.reportError("unknown acct type: " + this.type);
			return false;
		}
	}

	public boolean isLiability() {
		return !isAsset();
	}

	public boolean isAsset() {
		switch (this.type) {
		case Bank:
		case Cash:
		case Asset:
		case InvMutual:
		case InvPort:
		case Invest:
		case Inv401k:
			return true;

		case CCard:
		case Liability:
			return false;
		}

		return false;
	}

	public boolean isCashAccount() {
		switch (this.type) {
		case Bank:
		case Cash:
			return true;

		case CCard:
		case Asset:
		case Liability:
		case Inv401k:
		case InvMutual:
		case InvPort:
		case Invest:
			return false;

		default:
			Common.reportError("unknown acct type: " + this.type);
			return false;
		}
	}

	public boolean isNonInvestmentAccount() {
		switch (this.type) {
		case Bank:
		case CCard:
		case Cash:
		case Asset:
		case Liability:
			return true;

		case Inv401k:
		case InvMutual:
		case InvPort:
		case Invest:
			return false;

		default:
			Common.reportError("unknown acct type: " + this.type);
			return false;
		}
	}

	public void addTransaction(GenericTxn txn) {
		this.transactions.add(txn);
	}

	private int findFirstNonClearedTransaction() {
		for (int ii = 0; ii < this.transactions.size(); ++ii) {
			final GenericTxn t = this.transactions.get(ii);

			if (!t.isCleared()) {
				return ii;
			}
		}

		return -1;
	}

	public String getDisplayName(int length) {
		String nn = this.name;
		if (nn.length() > 36) {
			nn = nn.substring(0, 33) + "...";
		}

		return nn;
	}

	public BigDecimal getCurrentValue() {
		return getValueForDate(QDate.today());
	}

	public BigDecimal getFinalValue() {
		QDate d = (this.transactions.isEmpty()) //
				? QDate.today() //
				: this.transactions.get(this.transactions.size() - 1).getDate();
		return getValueForDate(d);
	}

	private BigDecimal getCashValueForDate(QDate d) {
		BigDecimal cashBal = null;

		int idx = Common.findLastTransactionOnOrBeforeDate(this.transactions, d);
		while ((cashBal == null) && (idx >= 0)) {
			final GenericTxn tx = this.transactions.get(idx);
			--idx;

			cashBal = tx.runningTotal;
		}

		return (cashBal != null) ? cashBal : BigDecimal.ZERO;
	}

	public BigDecimal getValueForDate(QDate d) {
		final BigDecimal cashBal = getCashValueForDate(d);
		final BigDecimal secBal = this.securities.getPortfolioValueForDate(d);

		BigDecimal acctValue = cashBal.add(secBal);

		acctValue = acctValue.setScale(2, RoundingMode.HALF_UP);

		return acctValue;
	}

	public BigDecimal getSecuritiesValueForDate(QDate d) {
		BigDecimal portValue = BigDecimal.ZERO;

		for (final SecurityPosition pos : this.securities.positions) {
			BigDecimal posamt = pos.getValueForDate(d);

			portValue = portValue.add(posamt);
		}

		return portValue;
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

	public List<SimpleTxn> findMatchingTransactions(SimpleTxn tx, QDate date) {
		List<SimpleTxn> txns = new ArrayList<SimpleTxn>();

		BigDecimal amt = tx.getAmount().abs();

		for (GenericTxn t : this.transactions) {
			if (Math.abs(t.getDate().subtract(date)) > 3) {
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

		return txns;
	}

	public List<GenericTxn> gatherTransactionsForStatement(Statement s) {
		final List<GenericTxn> txns = new ArrayList<GenericTxn>();

		final int idx1 = findFirstNonClearedTransaction();
		if (idx1 < 0) {
			return txns;
		}

		for (int ii = idx1; ii < this.transactions.size(); ++ii) {
			final GenericTxn t = this.transactions.get(ii);

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

	class AccountPosition {
		Account acct;
		BigDecimal cashBefore;
		BigDecimal cashAfter;
		SecurityPortfolio portBefore;
		SecurityPortfolio portAfter;

		AccountPosition(Account a) {
			this.acct = a;
		}
	};

	public AccountPosition getPosition(Date d1, Date d2) {
		AccountPosition apos = new AccountPosition(this);

		// BigDecimal v1 = getCashValueForDate(d1);

		return apos;
	}

	// FIXME account status for period unused, unfinished
	public void getPositionsForDate(Date d) {
		this.securities.getPositionsForDate(d);
	}
}
