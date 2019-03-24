package qif.importer;

import java.math.BigDecimal;

import qif.data.Common;
import qif.data.GenericTxn;
import qif.data.InvestmentTxn;
import qif.data.NonInvestmentTxn;
import qif.data.QDate;
import qif.data.Security;

/** Represents a transaction persisted to the statement log file */
public class StatementTxInfo {
	public QDate date;
	public String action;
	public int cknum;
	public BigDecimal cashAmount;
	public Security security;
	public BigDecimal shares;

	public static StatementTxInfo factory(GenericTxn tx) {
		if (tx instanceof NonInvestmentTxn) {
			return new StatementTxInfo((NonInvestmentTxn) tx);
		}

		if (tx instanceof InvestmentTxn) {
			return new StatementTxInfo((InvestmentTxn) tx);
		}

		return null;
	}

	public StatementTxInfo() {
		this.cknum = 0;
		this.action = null;
		this.security = null;
		this.shares = null;
	}

	private StatementTxInfo(GenericTxn tx) {
		this();

		this.cashAmount = tx.getCashAmount();
	}

	public StatementTxInfo(NonInvestmentTxn tx) {
		this((GenericTxn) tx);

		this.cknum = tx.getCheckNumber();
	}

	public StatementTxInfo(InvestmentTxn tx) {
		this((GenericTxn) tx);

		this.action = tx.getAction().toString();

		if (tx.security != null) {
			this.security = tx.security;
			this.shares = tx.getShares();
		}
	}

	public String toString() {
		return String.format("%s %5d %s", //
				this.date.toString(), this.cknum, //
				Common.formatAmount(this.cashAmount));
	}
}