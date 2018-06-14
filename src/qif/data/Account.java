package qif.data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

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
		List<Account> ret = new ArrayList<>(accounts);

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
	public BigDecimal creditLimit;
	public BigDecimal balance;
	public BigDecimal clearedBalance;

	public List<GenericTxn> transactions;
	public List<Statement> statements;
	public SecurityPortfolio securities;

	public Account() {
		this.acctid = 0;

		this.name = "";
		this.type = null;
		this.description = "";
		this.creditLimit = null;
		this.balance = this.clearedBalance = BigDecimal.ZERO;

		this.transactions = new ArrayList<GenericTxn>();
		this.statements = new ArrayList<Statement>();
		this.securities = new SecurityPortfolio();
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
				+ this.name //
				+ date.toString() + " " //
				+ Common.formatAmount(balance));
		return null;
	}

	public Statement getUnclearedStatement() {
		Statement stat = new Statement(this.acctid);

		stat.date = QDate.today();
		stat.addTransactions(getUnclearedTransactions());

		return stat;
	}

	public int getUnclearedTransactionCount() {
		int count = 0;

		for (final GenericTxn t : this.transactions) {
			if ((t != null) && (t.stmtdate == null)) {
				++count;
			}
		}

		return count;
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

	public List<GenericTxn> getUnclearedTransactions() {
		List<GenericTxn> txns = new ArrayList<GenericTxn>();

		for (int txidx = getFirstUnclearedTransactionIndex(); //
				txidx < this.transactions.size(); //
				++txidx) {
			GenericTxn t = this.transactions.get(txidx);

			if ((t != null) && (t.stmtdate == null)) {
				txns.add(t);
			}
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
			BigDecimal posamt = pos.getSecurityPositionValueForDate(d);

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
				+ " limit=" + this.creditLimit //
				+ " #tx= " + this.transactions.size() //
				+ "\n";

		s += this.securities.toString();

		return s;
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

	// FIXME unused, unfinished
	public void getPositionsForDate(Date d) {
		this.securities.getPositionsForDate(d);
	}
}
