package qif.importer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import qif.data.Account;
import qif.data.Common;
import qif.data.GenericTxn;
import qif.data.InvestmentTxn;
import qif.data.NonInvestmentTxn;
import qif.data.QDate;
import qif.data.QifDom;
import qif.data.Security;
import qif.data.SecurityPosition;
import qif.data.Statement;

// StatementDetails represents a reconciled statement as stored in the
// statements log file.
public class StatementDetails {
	public static final int CURRENT_VERSION = 4;

	public int acctid;
	public QDate date;

	// This is the cumulative account value
	public BigDecimal closingBalance;

	// This is the net cash change
	BigDecimal closingCashBalance;

	// This is the change to security positions (and closing price)
	StatementHoldings holdings;

	public List<TxInfo> transactions;
	public List<TxInfo> unclearedTransactions;

	// name;date;stmtBal;cashBal;numTx;numPos;[cashTx;][sec;numTx[txIdx;shareBal;]]
	public static String formatStatementForSave(Statement stmt) {
		Account a = Account.getAccountByID(stmt.acctid);

		String s = String.format("%s;%s;%5.2f;%5.2f;%d;%d", //
				a.getName(), //
				stmt.date.toString(), //
				stmt.closingBalance, //
				stmt.cashBalance, //
				stmt.transactions.size(), //
				stmt.holdings.positions.size());

		for (final GenericTxn t : stmt.transactions) {
			s += ";" + t.formatForSave();
		}

		for (final SecurityPosition p : stmt.holdings.positions) {
			s += ";" + p.formatForSave(stmt);
		}

		return s;
	}

	private StatementDetails() {
		this.closingCashBalance = BigDecimal.ZERO;
		this.transactions = new ArrayList<TxInfo>();
		this.unclearedTransactions = new ArrayList<TxInfo>();

		this.holdings = new StatementHoldings();
	}

	// Load details object from file
	public StatementDetails(QifDom dom, String s, int version) {
		this();

		parseStatementDetails(dom, s, version);
	}

	private void parseStatementDetails(QifDom dom, String s, int version) {
		final String[] ss = s.split(";");
		int ssx = 0;

		final String acctname = ss[ssx++].trim();
		final String dateStr = ss[ssx++].trim();
		final String closeStr = ss[ssx++].trim();
		final String closeCashStr = ss[ssx++].trim();
		final String txCountStr = ss[ssx++].trim();
		final String secCountStr = (version > 1) ? ss[ssx++].trim() : "0";

		this.acctid = Account.findAccount(acctname).acctid;
		this.date = Common.parseQDate(dateStr);
		if (version < 3) {
			this.closingBalance = this.closingCashBalance = new BigDecimal(closeCashStr);
		} else {
			this.closingBalance = new BigDecimal(closeStr);
			this.closingCashBalance = new BigDecimal(closeCashStr);
		}

		final int txcount = Integer.parseInt(txCountStr);
		final int seccount = Integer.parseInt(secCountStr);

		for (int ii = 0; ii < txcount; ++ii) {
			final TxInfo txinfo = new TxInfo();

			String txtypeStr = "";
			try {
				txtypeStr = ss[ssx++].trim();
			} catch (Exception e) {
				System.out.println("*** ERROR: parsing statement details");
			}
			final boolean isInvestmentTx = txtypeStr.equals("I");

			String tdateStr;
			String actStr = "";
			String secStr = "";
			String shrStr = "";
			String cknumStr = "0";

			if (isInvestmentTx) {
				// I;12/27/1999;BUY;ETMMTD;7024.50;-7024.50;
				tdateStr = ss[ssx++].trim();
				actStr = ss[ssx++].trim();
				secStr = ss[ssx++].trim();
				shrStr = ss[ssx++].trim();
			} else {
				// FIXME this makes no sense? It is always "T"
				tdateStr = (txtypeStr.equals("T")) //
						? shrStr = ss[ssx++].trim() //
						: txtypeStr;
				cknumStr = ss[ssx++].trim();
			}

			final String amtStr = ss[ssx++].trim();

			txinfo.date = Common.parseQDate(tdateStr);
			txinfo.action = actStr;
			txinfo.cknum = Integer.parseInt(cknumStr);
			txinfo.cashAmount = new BigDecimal(amtStr);
			if (secStr.length() > 0) {
				txinfo.security = Security.findSecurity(secStr);
				txinfo.shares = (shrStr.length() > 0) ? Common.parseDecimal(shrStr) : BigDecimal.ZERO;
			}

			this.transactions.add(txinfo);
		}

		// sec;numtx[;txidx;bal]
		for (int ii = 0; ii < seccount; ++ii) {
			final String symStr = ss[ssx++].trim();
			final String numtxStr = ss[ssx++].trim();

			Security sec = Security.findSecurity(symStr);

			final StatementPosition spos = new StatementPosition();
			spos.sec = sec;
			this.holdings.positions.add(spos);

			final int numtx = Integer.parseInt(numtxStr);

			for (int jj = 0; jj < numtx; ++jj) {
				final String txidxStr = ss[ssx++].trim();
				final String shrbalStr = ss[ssx++].trim();

				final StatementPositionTx tx = new StatementPositionTx();
				tx.txidx = Integer.parseInt(txidxStr);
				tx.shrbal = new BigDecimal(shrbalStr);

				spos.transactions.add(tx);
			}
		}
	}

	public String toString() {
		final String s = "" + this.acctid + " " //
				+ this.date.toString() //
				+ String.format("%s  %s %d tx", //
						Common.formatAmount(this.closingBalance), //
						Common.formatAmount(this.closingCashBalance), //
						this.transactions.size());

		return s;
	}
}

// This represents the information stored in the statementLog file for each
// transaction that is part of a statement.
class TxInfo {
	QDate date;
	String action;
	int cknum;
	BigDecimal cashAmount;
	Security security;
	BigDecimal shares;

	public static TxInfo factory(GenericTxn tx) {
		if (tx instanceof NonInvestmentTxn) {
			return new TxInfo((NonInvestmentTxn) tx);
		}

		if (tx instanceof InvestmentTxn) {
			return new TxInfo((InvestmentTxn) tx);
		}

		return null;
	}

	public TxInfo() {
		this.cknum = 0;
		this.action = null;
		this.security = null;
		this.shares = null;
	}

	private TxInfo(GenericTxn tx) {
		this();

		this.cashAmount = tx.getCashAmount();
	}

	public TxInfo(NonInvestmentTxn tx) {
		this((GenericTxn) tx);

		this.cknum = tx.getCheckNumber();
	}

	public TxInfo(InvestmentTxn tx) {
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

// [name;numtx[;txidx;shrbal]]
class StatementPositionTx {
	int txidx;
	BigDecimal shrbal;
}

class StatementPosition {
	Security sec;
	List<StatementPositionTx> transactions = new ArrayList<StatementPositionTx>();
}

class StatementHoldings {
	List<StatementPosition> positions = new ArrayList<StatementPosition>();
}
