package moneymgr.model;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import app.QifDom;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/** Information about statements */
public class Statement {
	/** Info about reconciled statements goes into this file. */
	private static String stmtLogFile = "statementLog.dat";
	private static String stmtLogFileCsv = "statementLog.csv.dat";

	public static File getStatementLogFileForModel(MoneyMgrModel model) {
		String name = (model.name.equals(MoneyMgrModel.CSV_MODEL_NAME)) //
				? stmtLogFileCsv //
				: stmtLogFile;

		return new File(QifDom.qifDir, name);
	}

	public final MoneyMgrModel model;
	public final int acctid;
	public final QDate date;

	public final Statement prevStatement;

	private boolean isBalanced;

	/** Total closing balance, including cash and securities */
	public final BigDecimal closingBalance;

	/** Cash closing balance */
	private BigDecimal cashBalance;

	/** Information about security activity in the statement period */
	public final SecurityPortfolio holdings;

	/** Transactions included in this statement */
	public final List<GenericTxn> transactions;

	/** Transactions during the statement period that are not cleared afterwards */
	public final List<GenericTxn> unclearedTransactions;

	/** Whether this statement has been saved to the statement file */
	public boolean dirty = false;

	public Statement(int acctid, QDate date, //
			BigDecimal closebal, BigDecimal cashbal, Statement prevstat) {
		this.model = MoneyMgrModel.currModel;

		this.isBalanced = false;
		this.acctid = acctid;
		this.date = date;

		this.transactions = new ArrayList<>();
		this.unclearedTransactions = new ArrayList<>();

		this.holdings = new SecurityPortfolio(this, //
				(prevstat != null) ? prevstat.holdings : null);

		this.prevStatement = prevstat;
		this.closingBalance = closebal;
		this.cashBalance = cashbal;
	}

	public Statement(int acctid, QDate date, Statement prevstat) {
		this(acctid, date, BigDecimal.ZERO, BigDecimal.ZERO, prevstat);
	}

	public Account getAccount() {
		return this.model.getAccountByID(this.acctid);
	}

	public boolean isBalanced() {
		return this.isBalanced;
	}

	public void setIsBalanced(boolean yesno) {
		this.isBalanced = yesno;
	}

	/** Add transactions to this statement's cleared list */
	public void addTransactions(Collection<GenericTxn> txns) {
		addTransactions(txns, false);
	}

	/** Add transactions to the cleared list, optionally checking their date */
	public void addTransactions(Collection<GenericTxn> txns, boolean checkDate) {
		// TODO should some/all txns go into unclearedTransactions?
		for (GenericTxn t : txns) {
			if (!checkDate || (t.getDate().compareTo(this.date) <= 0)) {
				addTransaction(t);
			}
		}
	}

	public void sanityCheck() {
		// this.holdings.sanityCheck();
	}

	public BigDecimal getCashBalance() {
		return (this.cashBalance != null) //
				? this.cashBalance //
				: this.closingBalance;
	}

	public void setCashBalance(BigDecimal val) {
		if (this.isBalanced) {
			Common.reportWarning("Changing cash balance of reconciled statement");
		}
		if (this.cashBalance != null) {
			Common.reportError("Statement cash balance is immutable");
		}

		this.cashBalance = val;
	}

	/** Add a transaction to the cleared transaction list */
	public void addTransaction(GenericTxn txn) {
		this.transactions.add(txn);

		if (txn instanceof InvestmentTxn) {
			InvestmentTxn itxn = (InvestmentTxn) txn;
			Security sec = itxn.getSecurity();

			if (sec != null) {
				if (this.holdings.findPosition(sec) == null) {
					this.holdings.createPosition(sec.secid);
				}

				this.holdings.addTransaction(txn);
			}
		}

		sanityCheck();
	}

	public QDate getOpeningDate() {
		return (this.prevStatement != null) //
				? this.prevStatement.date //
				: getAccount().getFirstTransactionDate();
	}

	public BigDecimal getOpeningBalance() {
		return (this.prevStatement != null) //
				? this.prevStatement.closingBalance //
				: BigDecimal.ZERO;
	}

	public BigDecimal getOpeningCashBalance() {
		return (this.prevStatement != null) //
				? this.prevStatement.getCashBalance() //
				: BigDecimal.ZERO;
	}

	/** Calculate closing cash balance from opening balance and cleared txns */
	public BigDecimal getClearedCashBalance() {
		BigDecimal bal = getOpeningCashBalance();

		for (GenericTxn txn : this.transactions) {
			bal = bal.add(txn.getCashAmount());
		}

		return bal;
	}

	/** Calculate sum of cleared credit transactions */
	public BigDecimal getCredits() {
		BigDecimal cr = new BigDecimal(0);

		for (GenericTxn s : this.transactions) {
			BigDecimal amt = s.getCashAmount();

			if (amt.compareTo(new BigDecimal(0)) >= 0) {
				cr = cr.add(amt);
			}
		}

		return cr;
	}

	/** Calculate sum of cleared debit transactions */
	public BigDecimal getDebits() {
		BigDecimal db = new BigDecimal(0);

		for (GenericTxn s : this.transactions) {
			BigDecimal amt = s.getCashAmount();

			if (amt.compareTo(new BigDecimal(0)) < 0) {
				db = db.add(amt.negate());
			}
		}

		return db;
	}

	/** Get all transactions (cleared and uncleared) that could be reconciled */
	public List<GenericTxn> getTransactionsForReconcile() {
		List<GenericTxn> txns = new ArrayList<>(this.transactions);
		txns.addAll(this.unclearedTransactions);
		return txns;
	}

	/** Toggle the cleared state of a transaction */
	public void toggleCleared(GenericTxn txn) {
		if (this.unclearedTransactions.contains(txn)) {
			clearTransaction(txn);
		} else {
			unclearTransaction(txn);
		}
	}

	/** Add a transaction to the cleared list */
	public void clearTransaction(GenericTxn txn) {
		if (this.unclearedTransactions.remove(txn)) {
			txn.setStatementDate(this.date);

			addTransaction(txn);
		}
	}

	/** Remove a transaction from the cleared list */
	public void unclearTransaction(GenericTxn txn) {
		if (this.transactions.remove(txn)) {
			txn.setStatementDate(null);

			this.holdings.removeTransaction(txn);
			this.unclearedTransactions.add(txn);
		}
	}

	public void clearAllTransactions() {
		clearTransactions(this.unclearedTransactions);
	}

	public void unclearAllTransactions() {
		unclearTransactions(this.transactions);
	}

	/** Clear a group of transactions */
	public void clearTransactions(List<GenericTxn> txns) {
		for (int idx = txns.size() - 1; idx >= 0; --idx) {
			clearTransaction(txns.get(idx));
		}
	}

	/** Unclear a group of transactions */
	public void unclearTransactions(List<GenericTxn> txns) {
		for (int idx = txns.size() - 1; idx >= 0; --idx) {
			unclearTransaction(txns.get(idx));
		}
	}

	/** Check currently cleared transactions against expected cash balance */
	public boolean cashMatches() {
		return cashMatches(this.transactions);
	}

	/** Check a list of cleared transactions against expected cash balance */
	public boolean cashMatches(List<GenericTxn> txns) {
		return getCashDifference(txns).signum() == 0;
	}

	/** Compare balance for currently cleared txns against expected cash balance */
	public BigDecimal getCashDifference() {
		return getCashDifference(this.transactions);
	}

	/** Compare balance for list of transactions against expected cash balance */
	private BigDecimal getCashDifference(List<GenericTxn> txns) {
		final BigDecimal cashTotal = Common.sumCashAmounts(txns);
		final BigDecimal cashExpected = this.cashBalance.subtract(getOpeningCashBalance());
		BigDecimal cashDiff = cashTotal.subtract(cashExpected);

		final BigDecimal newbal = getCashDelta(txns);
		cashDiff = this.cashBalance.subtract(newbal);

		return cashDiff;
	}

	/** Calculate the change in cash position for a list of transactions */
	public BigDecimal getCashDelta(List<GenericTxn> txns) {
		return getOpeningCashBalance().add(Common.sumCashAmounts(txns));
	}

	/** Calculate the change in cash position from the currently cleared txns */
	public BigDecimal getCashDelta() {
		return getCashDelta(this.transactions);
	}

	/** Check result of cleared transactions against expected security holdings */
	public boolean holdingsMatch() {
		if (this.holdings.isEmpty()) {
			return true;
		}

		SecurityPortfolio delta = getPortfolioDelta();

		for (SecurityPosition p : this.holdings.getPositions()) {
			SecurityPosition op = delta.getPosition(p.security);

			return Common.isEffectivelyEqual(p.getEndingShares(), //
					(op != null) ? op.getEndingShares() : BigDecimal.ZERO);
		}

		for (SecurityPosition p : delta.getPositions()) {
			SecurityPosition op = this.holdings.getPosition(p.security);

			return (op != null) //
					? Common.isEffectivelyEqual(p.getEndingShares(), op.getEndingShares()) //
					: Common.isEffectivelyZero(p.getEndingShares());
		}

		return true;
	}

	/** Build changes to previous Portfolio based on the cleared transactions */
	public SecurityPortfolio getPortfolioDelta() {
		return getPortfolioDelta(this.transactions);
	}

	/**
	 * TODO defunct<br>
	 * Build changes to the previous Portfolio position based on a list of cleared
	 * transactions
	 */
	public SecurityPortfolio getPortfolioDelta(List<GenericTxn> txns) {
		SecurityPortfolio clearedPositions = (this.prevStatement != null) //
				? new SecurityPortfolio(this, this.prevStatement.holdings) //
				: new SecurityPortfolio(this, null);

		for (GenericTxn t : txns) {
			if (t instanceof InvestmentTxn) {
				clearedPositions.addTransaction((InvestmentTxn) t);
			}
		}

		return clearedPositions;
	}

	public String toString() {
		List<String> sname = new ArrayList<String>();
		List<List<String>> sbal = new ArrayList<List<String>>();

		for (SecurityPosition pos : this.holdings.getPositions()) {
			BigDecimal pbal = BigDecimal.ZERO;
			if (this.prevStatement != null) {
				pbal = this.prevStatement.holdings.getPosition(pos.security).getEndingShares();
			}
			sname.add(pos.security.getSymbol());
			List<String> bals = new ArrayList<String>();
			sbal.add(bals);
			bals.add("-:" + Common.formatAmount3(pbal).trim());

			for (InvestmentTxn txn : pos.getTransactions()) {
				BigDecimal shr = txn.getShares();
				pbal = pbal.add(shr);
				bals.add(Common.formatDate(txn.getDate()) + ":" + Common.formatAmount3(pbal).trim());
			}

			bals.add("close:" + Common.formatAmount3(pos.getEndingShares()).trim());
		}

		// TODO System.out.print() in toString()??????
		for (int idx = 0; idx < sname.size(); ++idx) {
			System.out.print(String.format("%20s  ", sname.get(idx)));
		}
		System.out.println();

		int balidx = 0;
		boolean empty;

		do {
			// Print another line with balances
			empty = true;

			for (int idx = 0; idx < sbal.size(); ++idx) {
				List<String> bals = sbal.get(idx);
				String bal = "-";

				if (balidx < bals.size()) {
					bal = bals.get(balidx);
					empty = false;
				}

				System.out.print(String.format("%20s  ", bal));
			}
			System.out.println();

			++balidx;
		} while (!empty);

		String s = ((this.isBalanced) ? "*" : " ") //
				+ this.date.toString() //
				+ "  " + getAccount().name //
				+ "  " + this.closingBalance //
				+ " tran=" + ((this.transactions != null) ? this.transactions.size() : null);

		return s;
	}

	public String matches(Statement other) {
		return matches(other, false);
	}

	public String matches(Statement other, boolean deep) {
		if ((this.acctid != other.acctid)) {
			return String.format("acctid(%d/%d)", this.acctid, other.acctid);
		}
		if (!this.date.equals(other.date)) {
			return String.format("date(%s/%s)", //
					this.date.toString(), other.date.toString());
		}
		if (!Common.isEffectivelyEqual(this.closingBalance, other.closingBalance)) {
			return String.format("bal(%s/%s)", //
					this.closingBalance.toString(), //
					other.closingBalance.toString());
		}
		if (!Common.isEffectivelyEqual(getCashBalance(), other.getCashBalance())) {
			return String.format("cash(%s/%s)", //
					getCashBalance().toString(), //
					other.getCashBalance().toString());
		}
		if ((this.isBalanced != other.isBalanced)) {
			return String.format("isbal(%s/%s)", //
					new Boolean(this.isBalanced).toString(), //
					new Boolean(other.isBalanced).toString());
		}
		if ((this.transactions.size() != other.transactions.size())) {
			return String.format("numtx(%d/%d)", //
					this.transactions.size(), other.transactions.size());
		}
		if (((this.prevStatement == null) != (other.prevStatement == null))) {
			return String.format("prevstat(%d/%d)", //
					new Boolean(this.prevStatement == null).toString(), //
					new Boolean(other.prevStatement == null).toString());
		}

		for (int ii = 0; ii < this.transactions.size(); ++ii) {
			GenericTxn txn = this.transactions.get(ii);
			GenericTxn otxn = other.transactions.get(ii);

			String res = txn.matches(otxn);
			if (res != null) {
				return "txn:" + res;
			}
		}

		String res = this.holdings.matches(other.holdings);
		if (res != null) {
			this.holdings.matches(other.holdings);
			return "Holdings:" + res;
		}

		if (deep) {
			if (this.prevStatement != null) {
				res = this.prevStatement.matches(other.prevStatement, deep);
				if (res != null) {
					return "prevstmt:" + res;
				}
			}
		}

		return null;
	}
}
