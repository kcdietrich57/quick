package qif.data;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Account {
	enum AccountType { //
		Bank, CCard, Cash, Asset, Liability, Invest, InvPort, Inv401k, InvMutual;
	}

	public int domid;
	public int acctid;

	public String name;
	public AccountType type;
	public String description;
	public BigDecimal creditLimit;
	public BigDecimal balance;
	public BigDecimal clearedBalance;

	public List<GenericTxn> transactions;
	public List<Statement> statements;
	public SecurityPortfolio securities;

	public Account(QifDom dom) {
		this.domid = dom.domid;
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

	public Account(int id, QifDom dom) {
		this(dom);

		this.acctid = id;
	}

	// Create a copy of an account - used when creating a new Dom from an
	// existing one.
	public Account(Account other, QifDom dom) {
		this(other.acctid, dom);

		this.name = other.name;
		this.type = other.type;
		this.description = other.description;
		this.creditLimit = other.creditLimit;

		for (final GenericTxn t : other.transactions) {
			this.transactions.add(GenericTxn.clone(dom.domid, t));
		}

		for (final Statement s : this.statements) {
			this.statements.add(new Statement(dom, s));
		}
	}

	public Statement getStatement(Date date, BigDecimal balance) {
		for (final Statement s : this.statements) {
			if (s.date.compareTo(date) > 0) {
				break;
			}

			if ((s.date.compareTo(date) == 0) //
					&& (s.closingBalance.compareTo(balance) == 0)) {
				return s;
			}
		}

		Common.reportError("Can't find statement: " //
				+ this.name //
				+ Common.getDateString(date) //
				+ " " + String.format("%10.2f", balance));
		return null;
	}

	public Date getFirstTransactionDate() {
		return (this.transactions.isEmpty()) ? null : this.transactions.get(0).getDate();
	}

	public Date getLastTransactionDate() {
		return (this.transactions.isEmpty()) ? null : this.transactions.get(this.transactions.size() - 1).getDate();
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

	public int findFirstNonClearedTransaction() {
		for (int ii = 0; ii < this.transactions.size(); ++ii) {
			final GenericTxn t = this.transactions.get(ii);

			if (!t.isCleared()) {
				return ii;
			}
		}

		return -1;
	}

	public void reconcileStatements(PrintWriter pw) {
		if (this.statements.isEmpty()) {
			return;
		}

		for (int ii = 0; ii < this.statements.size(); ++ii) {
			final Statement s = this.statements.get(ii);

			final String msg = //
					"Reconciling " + this.name + " statement " + (ii + 1) //
							+ " of " + this.statements.size();

			s.reconcile(this, msg);
			if (!s.isBalanced) {
				break;
			}

			if (s.dirty) {
				final String logStr = s.formatForSave();
				pw.println(logStr);
				pw.flush();

				s.dirty = false;
			}

			this.balance = s.closingBalance;
		}
	}

	public void reportStatus(String interval) {
		if (this.statements.isEmpty()) {
			System.out.println("No statements for " + getDisplayName(36));
		} else {
			System.out.println("Statements for " + getDisplayName(36) + ": " //
					+ this.statements.size());

			final int nn = Math.max(0, this.statements.size() - 12);

			for (int ii = nn; ii < this.statements.size(); ++ii) {
				final Statement s = this.statements.get(ii);
				System.out.println(String.format("  %s  %3d tx  %10.2f", //
						Common.getDateString(s.date), s.transactions.size(), s.closingBalance));
			}

			System.out.println("Uncleared transactions as of last statement:");

			for (final GenericTxn t : this.statements.get(this.statements.size() - 1).unclearedTransactions) {
				System.out.println(String.format("  %s  %10.2f  %s", //
						Common.getDateString(t.getDate()), //
						t.getAmount(), t.getPayee()));
			}

			int unclearedCount = 0;

			for (final GenericTxn t : this.transactions) {
				if (t.stmtdate == null) {
					++unclearedCount;
				}
			}

			System.out.println("Total uncleared transactions: " + unclearedCount);
		}

		System.out.println(String.format("Current value: %10.2f", getCurrentValue()));
	}

	public BigDecimal reportStatusForDate(Date d) {
		final BigDecimal acctValue = getValueForDate(d);

		if (acctValue.compareTo(BigDecimal.ZERO) != 0) {
			System.out.println(String.format("  %-36s : %10.2f", //
					getDisplayName(36), acctValue));
			reportPortfolioForDate(d);
		}

		return acctValue;
	}

	String getDisplayName(int length) {
		String nn = this.name;
		if (nn.length() > 36) {
			nn = nn.substring(0, 33) + "...";
		}

		return nn;
	}

	BigDecimal getCurrentValue() {
		return getValueForDate(new Date());
	}

	BigDecimal getFinalValue() {
		final Date d = (this.transactions.isEmpty()) //
				? new Date() //
				: this.transactions.get(this.transactions.size() - 1).getDate();
		return getValueForDate(d);
	}

	BigDecimal getValueForDate(Date d) {
		BigDecimal acctValue = BigDecimal.ZERO;

		final int idx = Common.findLastTransactionOnOrBeforeDate(this.transactions, d);
		if (idx >= 0) {
			final GenericTxn tx = this.transactions.get(idx);

			final BigDecimal cashBal = tx.runningTotal;

			if (cashBal != null) {
				acctValue = cashBal;
			}
		}

		acctValue = acctValue.add(this.securities.getPortfolioValueForDate(d));

		acctValue = acctValue.setScale(2, RoundingMode.HALF_UP);

		return acctValue;
	}

	public BigDecimal reportPortfolioForDate(Date d) {
		BigDecimal portValue = BigDecimal.ZERO;

		for (final SecurityPosition pos : this.securities.positions) {
			portValue = portValue.add(pos.reportSecurityPositionForDate(d));
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
}
