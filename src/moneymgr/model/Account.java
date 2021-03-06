package moneymgr.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import moneymgr.model.SecurityPosition.PositionInfo;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/**
 * Represents an account<br>
 * 
 * Contains information about transactions, statements, securities<br>
 * and daily balances
 */
public class Account {
	public final MoneyMgrModel model;
	public final int acctid;

	public final String name;
	public final AccountType type;
	public final AccountCategory acctCategory;
	public final String description;

	private QDate closeDate;
	private int statementFrequency;
	private int statementDayOfMonth;

	private BigDecimal balance;
	private BigDecimal clearedBalance;

	private final List<GenericTxn> transactions;
	private final List<Statement> statements;
	public final SecurityPortfolio securities;

	public Account(MoneyMgrModel model, int acctid, //
			String name, String desc, AccountType type, int statFreq, int statDayOfMonth) {
		this.model = (model != null) ? model : MoneyMgrModel.currModel;
		this.acctid = (acctid > 0) ? acctid : this.model.nextAccountID();
		this.name = name;
		this.description = (desc != null) ? desc : "";
		this.type = type;
		this.acctCategory = AccountCategory.forAccountType(type);

		setStatementFrequency(statFreq, statDayOfMonth);

		this.balance = this.clearedBalance = BigDecimal.ZERO;

		this.transactions = new ArrayList<>();
		this.statements = new ArrayList<>();
		this.securities = new SecurityPortfolio(this, null);
	}

	public Account(String name, AccountType type, String desc, QDate closeDate, //
			int statFreq, int statDayOfMonth) {
		this(null, 0, name, desc, type, statFreq, statDayOfMonth);

		this.closeDate = closeDate;
	}

	public Account(String name, AccountType type) {
		this(name, type, "", null, -1, -1);
	}

	public int getStatementFrequency() {
		return this.statementFrequency;
	}

	public int getStatementDay() {
		return this.statementDayOfMonth;
	}

	public BigDecimal getBalance() {
		return this.balance;
	}

	public BigDecimal getClearedBalance() {
		return this.clearedBalance;
	}

	public void setBalance(BigDecimal bal) {
		this.balance = bal;
	}

	public void setClearedBalance(BigDecimal bal) {
		this.clearedBalance = bal;
	}

	public void setStatementFrequency(int freq, int dom) {
		this.statementFrequency = (freq > 0) ? freq : 30;
		this.statementDayOfMonth = (dom > 0) ? dom : 30;
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
		return (!this.transactions.isEmpty()) //
				? this.transactions.get(0).getDate() //
				: QDate.today();
	}

	public QDate getCloseDate() {
		return this.closeDate;
	}

	public void setCloseDate(QDate date) {
		if ((date == null) && (this.closeDate != null)) {
			Common.reportWarning( //
					String.format("Clearing close date for %s - was %s", //
							this.name, this.closeDate.toString()));
		}

		if ((this.closeDate != null) && !this.closeDate.equals(date)) {
			Common.reportWarning( //
					String.format("Changing close date for %s from %s to %s", //
							this.name, this.closeDate.toString(), date.toString()));
		}

		if (date != null && //
				!this.transactions.isEmpty() && //
				getOpenDate().compareTo(date) > 0) {
			Common.reportWarning( //
					String.format("Close date %s is before open date {}", //
							date, getOpenDate()));

			date = getOpenDate();
		}

		this.closeDate = date;
	}

	/** Was the account open on a given date */
	public boolean isOpenOn(QDate d) {
		if (d == null) {
			d = QDate.today();
		}

		return isOpenAsOf(d) && !isClosedAsOf(d);
	}

	/** Was the account any time between two dates */
	public boolean isOpenDuring(QDate start, QDate end) {
		if (end == null) {
			end = QDate.today();
		}

		return !(isClosedAsOf(start) || !isOpenAsOf(end));
	}

	/** Was the account opened on or before a date */
	public boolean isOpenAsOf(QDate d) {
		return getOpenDate().compareTo(d) <= 0;
	}

	/** Was the account closed on or before date */
	public boolean isClosedAsOf(QDate d) {
		return (this.closeDate != null) && (this.closeDate.compareTo(d) < 0);
	}

	public void addTransaction(GenericTxn txn) {
		if (txn.getAction() == TxAction.STOCKSPLIT) {
			// TODO I don't like having stock split transactions in accounts
			// But if we don't the share balances are incorrect
//			Common.reportWarning(String.format(//
//					"Adding stocksplit tx to %s", this.name));
//			return;
		}

		int idx = this.model.getTransactionInsertIndexByDate(//
				this.transactions, txn);

		this.transactions.add(idx, txn);
	}

	public void removeTransaction(GenericTxn txn) {
		if (this.transactions.contains(txn)) {
			if (txn.isCleared()) {
				Common.reportError("Account: Can't remove cleared transaction");
			}

			this.transactions.remove(txn);
		}
	}

	/** Find the index to insert a new transaction in a list sorted by date */
	private int getTransactionIndexForDate(QDate date) {
		MoneyMgrModel.SEARCH().setDate(date);

		return getTransactionIndexForDate(MoneyMgrModel.SEARCH());
	}

	static final Comparator<GenericTxn> c = new Comparator<GenericTxn>() {
		public int compare(GenericTxn o1, GenericTxn o2) {
			return o1.getDate().subtract(o2.getDate());
		}
	};

	/** Find the index to insert a new transaction in a list sorted by date */
	private int getTransactionIndexForDate(GenericTxn tx) {
		int idx = Collections.binarySearch(this.transactions, tx, c);

		if (idx < 0) {
			idx = -idx - 1;
		}

		return idx;
	}

	public int getNumTransactions() {
		return this.transactions.size();
	}

	public List<GenericTxn> getTransactions() {
		return Collections.unmodifiableList(this.transactions);
	}

	/** Get transactions for a period (inclusive of start/end date) */
	public List<SimpleTxn> getTransactions(QDate start, QDate end) {
		List<SimpleTxn> ret = new ArrayList<>();

		int idx = getTransactionIndexForDate(start);
		if (idx < 0) {
			return ret;
		}

		while ((idx > 0) && (idx < this.transactions.size())//
				&& (c.compare(this.transactions.get(idx - 1), //
						this.transactions.get(idx)) >= 0)) {
			--idx;
		}

		while ((idx >= 0) && (idx < this.transactions.size()) //
				&& (this.transactions.get(idx).getDate().compareTo(end) <= 0)) {
			ret.add(this.transactions.get(idx));
			++idx;
		}

		return ret;
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

			if (stmt.isBalanced()) {
				return stmt.date;
			}
		}

		return null;
	}

	/** Get the date of the first non-balanced statement for this account */
	public Statement getFirstUnbalancedStatement() {
		for (int ii = 0; ii < this.statements.size(); ++ii) {
			Statement stmt = this.statements.get(ii);

			if (!stmt.isBalanced()) {
				return stmt;
			}
		}

		return null;
	}

	public boolean isStatementDue() {
		return getDaysUntilStatementIsDue() <= 0;
	}

	public boolean isStatementOverdue(int days) {
		return getDaysUntilStatementIsDue() <= -days;
	}

	public int getDaysUntilStatementIsDue() {
		QDate laststmt = getLastBalancedStatementDate();
		if ((laststmt == null) || (this.statementFrequency <= 0)) {
			return -1;
		}

		QDate duedate;

		switch (this.statementFrequency) {
		case 30:
			duedate = laststmt.addMonths(1);
			break;
		case 90:
			duedate = laststmt.addMonths(3);
			break;
		case 360:
			duedate = laststmt.addMonths(12);
			break;
		default:
			duedate = laststmt.addDays(this.statementFrequency);
			break;
		}

		return duedate.subtract(QDate.today());
	}

	public int getNumStatements() {
		return this.statements.size();
	}

	public List<Statement> getStatements() {
		return Collections.unmodifiableList(this.statements);
	}

	public void addStatement(Statement s) {
		if (!this.statements.contains(s)) {
			this.statements.add(s);
		}
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

		for (Statement s : this.statements) {
			if (s.date.compareTo(date) > 0) {
				break;
			}

			if (s.date.compareTo(date) == 0) {
				if ((balance == null) //
						|| (s.closingBalance.compareTo(balance) == 0)) {
					return s;
				}

				Common.reportWarning(String.format( //
						"Statement date (%s) matches but balance does not (%s/%s)", //
						date.toString(), //
						balance.toString(), //
						s.closingBalance.toString()));
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
			int lastday = QDate.lastDayOfMonth(today.getYear(), today.getMonth());

			if (this.statementDayOfMonth > 0 && this.statementDayOfMonth < lastday) {
				lastday = this.statementDayOfMonth;
			}

			return new QDate(today.getYear(), today.getMonth(), lastday);
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
			stat = new Statement(this.acctid, getNextStatementDate(), getLastStatement());
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
		QDate aod = this.model.getAsOfDate();

		if (aod.compareTo(QDate.today()) < 0) {
			// TODO I don't get this - when slider is before today - what if it is
			// reconciled?
			stat = getFirstStatementAfter(aod);
			if (stat == null) {
				stat = new Statement(this.acctid, aod, getLastStatement());
			}
		} else {
			List<GenericTxn> txns = getUnclearedTransactions();

			if (txns.isEmpty()) {
				return null;
			}

			stat = new Statement(this.acctid, QDate.today(), getLastStatement());
			stat.addTransactions(txns);
		}

		return stat;
	}

	/**
	 * Construct a statement for any uncleared transactions not already in a
	 * statement, up to the current date.
	 */
	public Statement createUnclearedStatement(Statement laststmt) {
		List<GenericTxn> txns = new ArrayList<>();

		if (laststmt != null) {
			int laststmtidx = this.statements.indexOf(laststmt);

			addTransactionsToAsOfDate(txns, laststmt.unclearedTransactions);

			if (laststmtidx < (this.statements.size() - 1)) {
				Statement nextstmt = this.statements.get(laststmtidx + 1);

				addTransactionsToAsOfDate(txns, nextstmt.transactions);
				addTransactionsToAsOfDate(txns, nextstmt.unclearedTransactions);
			}
		} else {
			addTransactionsToAsOfDate(txns, this.transactions);
		}

		Statement stmt = new Statement( //
				this.acctid, this.model.getAsOfDate(), getLastStatement());
		Common.sortTransactionsByDate(txns);
		stmt.addTransactions(txns);

		return stmt;
	}

	/** Add txns from one list to another if date <= current date */
	private void addTransactionsToAsOfDate(List<GenericTxn> txns, List<GenericTxn> srctxns) {
		for (GenericTxn txn : srctxns) {
			if ((txn.getDate().compareTo(this.model.getAsOfDate()) <= 0) //
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

			if ((t != null) && !t.isCleared()) {
				return ii;
			}
		}

		return -1;
	}

	/** Return count of all unreconciled transactions */
	public int getUnclearedTransactionCount() {
		int count = 0;

		for (GenericTxn t : this.transactions) {
			if ((t != null) && !t.isCleared()) {
				++count;
			}
		}

		return count;
	}

	/** Return a new list of uncleared txns that don't belong to any statement */
	private List<GenericTxn> getUnclearedTransactions() {
		List<GenericTxn> txns = new ArrayList<>();

		// Gather transactions not belonging to a statement
		for (int txidx = getFirstUnclearedTransactionIndex(); //
				(txidx >= 0) && (txidx < this.transactions.size()); //
				++txidx) {
			GenericTxn t = this.transactions.get(txidx);

			if ((t != null) && !t.isCleared()) {
				txns.add(t);
			}
		}

		// Add transactions that belong to non-balanced statements
		for (int statidx = this.statements.size() - 1; statidx >= 0; --statidx) {
			Statement stmt = this.statements.get(statidx);

			if (stmt.isBalanced()) {
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
		BigDecimal optBal = getOptionsValueForDate(d);

		BigDecimal acctValue = cashBal.add(secBal).add(optBal);

		acctValue = acctValue.setScale(2, RoundingMode.HALF_UP);

		return acctValue;
	}

	public BigDecimal getOptionsValueForDate(QDate d) {
		BigDecimal bal = BigDecimal.ZERO;

		List<StockOption> opts = StockOption.getOpenOptions(this, d);
		for (StockOption opt : opts) {
			bal = bal.add(opt.getValueForDate(d));
		}

		return bal;
	}

	/** Get account cash value for a specified date */
	private BigDecimal getCashValueForDate(QDate d) {
		BigDecimal cashBal = null; // BigDecimal.ZERO;

		int idx = this.model.getLastTransactionIndexOnOrBeforeDate(this.transactions, d);

		while ((idx >= 0) && (cashBal == null)) {
			GenericTxn tx = this.transactions.get(idx--);

			cashBal = tx.getRunningTotal();
		}

		return (cashBal != null) ? cashBal : BigDecimal.ZERO;
	}

	/** Get the value of securities for this account on a given date */
	public BigDecimal getSecuritiesValueForDate(QDate d) {
		BigDecimal portValue = BigDecimal.ZERO;

		for (final SecurityPosition pos : this.securities.getPositions()) {
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

//	public List<SimpleTxn> findPotentialMatchingTransactions(SimpleTxn tx) {
//		List<SimpleTxn> txns = new ArrayList<>();
//		int TOLERANCE = 3; // days
//
//		int idx = getTransactionIndexForDate(tx.getDate().addDays(-TOLERANCE));
//
//		for (; idx < this.transactions.size(); ++idx) {
//			GenericTxn t = this.transactions.get(idx);
//			int diff = t.getDate().subtract(tx.getDate());
//			if (diff > TOLERANCE) {
//				break;
//			}
//			if (-diff > TOLERANCE) {
//				continue;
//			}
//
//			txns.add(t);
//		}
//
//		txns.sort(new Comparator<SimpleTxn>() {
//			public int compare(SimpleTxn o1, SimpleTxn o2) {
//				int diff1 = Math.abs(o1.getDate().subtract(tx.getDate()));
//				int diff2 = Math.abs(o2.getDate().subtract(tx.getDate()));
//
//				return diff1 - diff2;
//			}
//		});
//
//		return txns;
//	}

	/**
	 * Gather transactions that might belong to a statement.<br>
	 * i.e., They do not yet belong to a statement and their date is on or before
	 * the statement closing date.
	 */
	public List<GenericTxn> gatherTransactionsForStatement(Statement s) {
		List<GenericTxn> txns = new ArrayList<>();

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

	public String matches(Account other) {
		if (!this.name.equals(other.name) //
				|| (this.type != other.type) //
				|| !Common.isEffectivelyEqual(this.balance, other.balance) //
				|| !Common.isEffectivelyEqual(this.clearedBalance, other.clearedBalance) //
				|| ((this.closeDate == null) != (other.closeDate == null)) //
				|| ((this.closeDate != null) && !this.closeDate.equals(other.closeDate)) //
				|| !Common.safeEquals(this.description, other.description) //
				|| (this.statementFrequency != other.statementFrequency) //
				|| (this.statementDayOfMonth != other.statementDayOfMonth) //
		) {
			return "generalInfo";
		}

		int num1 = getNumTransactions();
		int num2 = other.getNumTransactions();

		if (num1 != num2) {
//			Common.reportWarning(String.format(//
//					"Warning: %s transaction count %d vs %d", //
//					this.name, num1, num2));
//			return "numtxn";
		}

		int idx1 = 0;
		int idx2 = 0;
		while (idx1 < num1 && idx2 < num2) {
			GenericTxn tx1 = this.transactions.get(idx1);
			GenericTxn tx2 = other.transactions.get(idx2);

			if (tx1.getAction() == TxAction.STOCKSPLIT) {
				++idx1;
				continue;
			}

			if (tx2.getAction() == TxAction.STOCKSPLIT) {
				++idx2;
				continue;
			}

			String ret = tx1.matches(tx2);
			if (ret != null) {
				return String.format("Tx%d:%s", idx1, ret);
			}

			++idx1;
			++idx2;
		}

		while ((idx2 < num2) //
				&& (other.transactions.get(idx2).getAction() == TxAction.STOCKSPLIT)) {
			++idx2;
		}

		if (idx1 < num1 || idx2 < num2) {
			return "unmatchedTx";
		}

		String res = this.securities.matches(other.securities);
		if (res != null) {
			return "holdings:" + res;
		}

		return null;
	}

	private boolean datesAreClose(SimpleTxn t1, SimpleTxn t2) {
		int TOLERANCE = 5; // days
		QDate d1 = t1.getDate();
		QDate d2 = t2.getDate();
		int diff = Math.abs(d1.subtract(d2));
		return (Math.abs(t1.getDate().subtract(t2.getDate())) <= TOLERANCE);
	}

	public boolean isApproximateMatch(SimpleTxn tx1, SimpleTxn tx2) {
		if (!datesAreClose(tx1, tx2)) {
			return false;
		}

		if (!tx1.getAction().isEquivalentTo(tx2.getAction())) {
			return false;
		}

		if (!Common.isEffectivelyEqual( //
				tx1.getAmount().abs(), //
				tx2.getAmount().abs())) {
			return false;
		}

		if (tx1.isCredit() != tx2.isCredit()) {
			return false;
		}

		if (tx1 instanceof InvestmentTxn) {
			if (!(tx2 instanceof InvestmentTxn)) {
				return false;
			}

			InvestmentTxn itx1 = (InvestmentTxn) tx1;
			InvestmentTxn itx2 = (InvestmentTxn) tx2;

			if (!itx1.getSecurityName().equals(itx2.getSecurityName())) {
				return false;
			}
		}

		return true;
	}

	/**
	 * TODO This duplicates TransactionCleaner findMatchesForTransfer()<br>
	 * Find existing transaction(s) that match a transaction being loaded.<br>
	 * Date is close, amount matches (or the amount of a split).
	 */
	public List<SimpleTxn> findMatchingTransactions(SimpleTxn tx, boolean dummy) {
		List<SimpleTxn> txns = new ArrayList<>();

		int idx = getTransactionIndexForDate(tx.getDate());
		for (; idx > 0; --idx) {
			if (!datesAreClose(tx, this.transactions.get(idx - 1))) {
				break;
			}
		}

		for (; idx < this.transactions.size(); ++idx) {
			GenericTxn t = this.transactions.get(idx);
			if (!datesAreClose(tx, t)) {
				break;
			}

			// Match scenarios:
			// Amount matches win txn
			// Amount matches split in win txn
			// Amount matches split in xfer with win txn

			if (isApproximateMatch(tx, t)) {
				txns.add(t);
			}

			if (t.hasSplits()) {
				for (SplitTxn st : t.getSplits()) {
					if (isApproximateMatch(tx, st)) {
						txns.add(st);
					} else if (st.hasSplits()) {
						for (SplitTxn sst : st.getSplits()) {
							if (isApproximateMatch(tx, sst)) {
								txns.add(sst);
							}
						}
					}
				}
			}

			// TODO wtf? Match split in win xfer txn
			SimpleTxn xt = t.getCashTransferTxn();
			if (xt != null) {
				if (isApproximateMatch(tx, xt)) {
					// txns.add(xt);
				} else if (xt.hasSplits()) {
					for (SplitTxn st : xt.getSplits()) {
						if (isApproximateMatch(tx, st)) {
							// txns.add(st);
						}
					}
				}
			}
		}

		if (txns.isEmpty()) {
			// System.out.println("No matches for " + tx.toString()); // TODO xyzzy
		}
		txns.sort(new Comparator<SimpleTxn>() {
			public int compare(SimpleTxn o1, SimpleTxn o2) {
				int diff1 = Math.abs(o1.getDate().subtract(tx.getDate()));
				int diff2 = Math.abs(o2.getDate().subtract(tx.getDate()));

				return diff1 - diff2;
			}
		});

		return txns;
	}
}
