package moneymgr.io;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import moneymgr.model.Account;
import moneymgr.model.GenericTxn;
import moneymgr.model.InvestmentTxn;
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.Security;
import moneymgr.model.SecurityPosition;
import moneymgr.model.Statement;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/** Represents a reconciled statement as stored in the statements log file */
public class StatementDetails {
	public static final int CURRENT_VERSION = 4;

	// [name;numtx[;txidx;shrbal]]
	private static class StatementPositionTx {
		int txidx;
		BigDecimal shrbal;
	}

	private static class StatementPosition {
		Security sec;
		List<StatementPositionTx> transactions = new ArrayList<StatementPositionTx>();
	}

	private static class StatementHoldings {
		List<StatementPosition> positions = new ArrayList<StatementPosition>();
	}

	/**
	 * Create a string representation of the statement for saving<br>
	 * Format:<br>
	 * acctname;date;bal;cashBal;numTx;numPos;[cashTx;][sec;numTx[txIdx;shareBal;]]
	 */
	public static String formatStatementForSave(Statement stmt) {
		Account a = MoneyMgrModel.currModel.getAccountByID(stmt.acctid);

		String s = String.format("%s;%s;%5.2f;%5.2f;%d;%d", //
				a.name, //
				stmt.date.toString(), //
				stmt.closingBalance, //
				stmt.getCashBalance(), //
				stmt.transactions.size(), //
				stmt.holdings.size());

		for (GenericTxn t : stmt.transactions) {
			s += ";" + formatTransactionForSave(t);
		}

		for (SecurityPosition p : stmt.holdings.getPositions()) {
			s += ";" + formatPositionForSave(stmt, p);
		}

		return s;
	}

	/**
	 * Construct a string for persisting a generic transaction to a file<br>
	 * Format: T;DATE;CKNUM;AMT
	 */
	public static String formatTransactionForSave(GenericTxn txn) {
		if (txn instanceof InvestmentTxn) {
			return formatInvestmentTransactionForSave((InvestmentTxn) txn);
		}

		String s = String.format("T;%s;%d;%5.2f", //
				txn.getDate().toString(), //
				txn.getCheckNumber(), //
				txn.getCashAmount());
		return s;
	}

	/**
	 * Construct a string for persisting an investment transaction to a file<br>
	 * Format: I;DATE;ACTION;[SEC];[QTY];AMT
	 */
	public static String formatInvestmentTransactionForSave(InvestmentTxn txn) {
		String secString = ";";

		if (txn.getSecurity() != null) {
			secString = txn.getSecuritySymbol() + ";";
			if (txn.getShares() != null) {
				secString += String.format("%5.2f", txn.getShares());
			}
		}

		String s = String.format("I;%s;%s;%s;%5.2f", //
				txn.getDate().toString(), //
				txn.getAction().toString(), //
				secString, //
				txn.getCashAmount());
		return s;
	}

	/**
	 * Encode security position transactions for save<br>
	 * Format: name;numtx[;txid;shrbal]
	 */
	public static String formatPositionForSave(Statement stat, SecurityPosition pos) {
		List<InvestmentTxn> txns = pos.getTransactions();
		int numtx = txns.size();
		String s = pos.security.getName() + ";" + numtx;
		List<BigDecimal> shrbal = pos.getShareBalances();

		for (int ii = 0; ii < numtx; ++ii) {
			InvestmentTxn t = txns.get(ii);
			int txidx = stat.transactions.indexOf(t);
			BigDecimal bal = shrbal.get(ii);

			assert txidx >= 0;

			s += String.format(";%d;%f", txidx, bal);
		}

		return s;
	}

	// TODO make fields final; use factory to construct
	public int acctid;
	public QDate date;

	/** Cumulative account value on closing date (cash + securities) */
	public BigDecimal closingBalance;

	/** Cash balance */
	BigDecimal closingCashBalance;

	/** Change to security positions (and closing price) since last statement */
	StatementHoldings holdings;

	/** Transactions covered by this statement */
	public List<StatementTxInfo> transactions;

	/** All Uncleared transactions as of closing date */
	public List<StatementTxInfo> unclearedTransactions;

	private StatementDetails() {
		this.closingCashBalance = BigDecimal.ZERO;
		this.transactions = new ArrayList<StatementTxInfo>();
		this.unclearedTransactions = new ArrayList<StatementTxInfo>();

		this.holdings = new StatementHoldings();
	}

	/** Construct details by loading from file */
	public StatementDetails(String s) {
		this();

		parseStatementDetails(s);
	}

	private void parseStatementDetails(String s) {
		String[] ss = s.split(";");
		int ssx = 0;

		String acctname = ss[ssx++].trim();
		String dateStr = ss[ssx++].trim();
		String closeStr = ss[ssx++].trim();
		String closeCashStr = ss[ssx++].trim();
		String txCountStr = ss[ssx++].trim();
		String secCountStr = ss[ssx++].trim();

		this.acctid = MoneyMgrModel.currModel.findAccount(acctname).acctid;
		this.date = Common.parseQDate(dateStr);
		this.closingBalance = new BigDecimal(closeStr);
		this.closingCashBalance = new BigDecimal(closeCashStr);

		int txcount = Integer.parseInt(txCountStr);
		int seccount = Integer.parseInt(secCountStr);

		for (int ii = 0; ii < txcount; ++ii) {
			StatementTxInfo txinfo = new StatementTxInfo();

			String txtypeStr = "";
			try {
				txtypeStr = ss[ssx++].trim();
			} catch (Exception e) {
				System.out.println("*** ERROR: parsing statement details");
			}

			String tdateStr;
			String actStr = "";
			String secStr = "";
			String shrStr = "";
			String cknumStr = "0";

			if (txtypeStr.equals("I")) {
				// I;12/27/1999;BUY;ETMMTD;7024.50;-7024.50;
				tdateStr = ss[ssx++].trim();
				actStr = ss[ssx++].trim();
				secStr = ss[ssx++].trim();
				shrStr = ss[ssx++].trim();
			} else if (txtypeStr.equals("T")) {
				// T;5/12/18;0;-35.19;
				tdateStr = shrStr = ss[ssx++].trim();
				cknumStr = ss[ssx++].trim();
			} else {
				// TODO statment log file format? <date>;cknum
				tdateStr = txtypeStr;
				cknumStr = ss[ssx++].trim();
			}

			String amtStr = ss[ssx++].trim();

			txinfo.date = Common.parseQDate(tdateStr);
			txinfo.action = actStr;
			txinfo.cknum = Integer.parseInt(cknumStr);
			txinfo.cashAmount = new BigDecimal(amtStr);
			if (secStr.length() > 0) {
				txinfo.security = MoneyMgrModel.currModel.findSecurity(secStr);
				txinfo.shares = (shrStr.length() > 0) ? Common.parseDecimal(shrStr) : BigDecimal.ZERO;
			}

			this.transactions.add(txinfo);
		}

		// sec;numtx[;txidx;bal]
		for (int ii = 0; ii < seccount; ++ii) {
			String symStr = ss[ssx++].trim();
			String numtxStr = ss[ssx++].trim();

			Security sec = MoneyMgrModel.currModel.findSecurity(symStr);

			StatementPosition spos = new StatementPosition();
			spos.sec = sec;
			this.holdings.positions.add(spos);

			int numtx = Integer.parseInt(numtxStr);

			for (int jj = 0; jj < numtx; ++jj) {
				String txidxStr = ss[ssx++].trim();
				String shrbalStr = ss[ssx++].trim();

				StatementPositionTx tx = new StatementPositionTx();
				tx.txidx = Integer.parseInt(txidxStr);
				tx.shrbal = new BigDecimal(shrbalStr);

				spos.transactions.add(tx);
			}
		}
	}

	public String toString() {
		String s = "" + this.acctid + " " //
				+ this.date.toString() //
				+ String.format("%s  %s %d tx", //
						Common.formatAmount(this.closingBalance), //
						Common.formatAmount(this.closingCashBalance), //
						this.transactions.size());

		return s;
	}
}
