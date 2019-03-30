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
	public static File stmtLogFile = new File(QifDom.qifDir, "statementLog.dat");

	public final int acctid;
	public final QDate date;

	public Statement prevStatement;

	public boolean isBalanced;

	/** Total closing balance, including cash and securities */
	public BigDecimal closingBalance;

	/** Cash closing balance */
	public BigDecimal cashBalance;

	/** Information about security activity in the statement period */
	public final SecurityPortfolio holdings;

	/** Transactions included in this statement */
	public final List<GenericTxn> transactions;

	/** Transactions during the statement period that are not cleared afterwards */
	public final List<GenericTxn> unclearedTransactions;

	/** Whether this statement has been saved to the statement file */
	public boolean dirty = false;

	public Statement(int acctid, QDate date) {
		this.isBalanced = false;
		this.acctid = acctid;
		this.date = date;

		this.transactions = new ArrayList<>();
		this.unclearedTransactions = new ArrayList<>();
		this.holdings = new SecurityPortfolio();

		this.prevStatement = null;
		this.closingBalance = null;
		this.cashBalance = null;
	}

	/** Add transactions to this statement's cleared list */
	public void addTransactions(Collection<GenericTxn> txns) {
		addTransactions(txns, false);
	}

	/** Add transactions to the cleared list, optionally checking their date */
	public void addTransactions(Collection<GenericTxn> txns, boolean checkDate) {
		// TODO should some/all txns go into unclearedTransactions?
		if (!checkDate) {
			this.transactions.addAll(txns);
		} else {
			for (GenericTxn t : txns) {
				if (t.getDate().compareTo(this.date) <= 0) {
					this.transactions.add(t);
				}
			}
		}
	}

	/** Add a transaction to the cleared transaction list */
	public void addTransaction(GenericTxn txn) {
		this.transactions.add(txn);
	}

	public BigDecimal getOpeningBalance() {
		return (this.prevStatement != null) //
				? this.prevStatement.closingBalance //
				: BigDecimal.ZERO;
	}

	public BigDecimal getOpeningCashBalance() {
		return (this.prevStatement != null) //
				? this.prevStatement.cashBalance //
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
			txn.stmtdate = this.date;

			this.transactions.add(txn);
		}
	}

	/** Remove a transaction from the cleared list */
	public void unclearTransaction(GenericTxn txn) {
		if (this.transactions.remove(txn)) {
			txn.stmtdate = null;

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
		if (this.holdings.positions.isEmpty()) {
			return true;
		}

		SecurityPortfolio delta = getPortfolioDelta();

		for (SecurityPosition p : this.holdings.positions) {
			SecurityPosition op = delta.getPosition(p.security);

			return Common.isEffectivelyEqual(p.endingShares, //
					(op != null) ? op.endingShares : BigDecimal.ZERO);
		}

		for (final SecurityPosition p : delta.positions) {
			SecurityPosition op = this.holdings.getPosition(p.security);

			return (op != null) //
					? Common.isEffectivelyEqual(p.endingShares, op.endingShares) //
					: Common.isEffectivelyZero(p.endingShares);
		}

		return true;
	}

	/** Build changes to previous Portfolio based on the cleared transactions */
	public SecurityPortfolio getPortfolioDelta() {
		return getPortfolioDelta(this.transactions);
	}

	/**
	 * Build changes to the previous Portfolio position based on a list of cleared
	 * transactions
	 */
	public SecurityPortfolio getPortfolioDelta(List<GenericTxn> txns) {
		SecurityPortfolio clearedPositions = (this.prevStatement != null) //
				? new SecurityPortfolio(this.prevStatement.holdings) //
				: new SecurityPortfolio();

		for (GenericTxn t : txns) {
			if (!(t instanceof InvestmentTxn)) {
				continue;
			}

			InvestmentTxn itx = (InvestmentTxn) t;

			clearedPositions.addTransaction(itx);
		}

		return clearedPositions;
	}

	public String toString() {
		final String s = this.date.toString() //
				+ "  " + this.closingBalance //
				+ " tran=" + ((this.transactions != null) ? this.transactions.size() : null);

		return s;
	}
}
