package qif.data;

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
	public int id;

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
		this.id = 0;

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

		this.id = id;
	}

	public Account(Account other, QifDom dom) {
		this(other.id, dom);

		this.name = other.name;
		this.type = other.type;
		this.description = other.description;
		this.creditLimit = other.creditLimit;

		for (final GenericTxn t : other.transactions) {
			this.transactions.add(GenericTxn.clone(dom.domid, t));
		}

		for (final Statement s : this.statements) {
			this.statements.add(new Statement(s));
		}
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

	public void reportStatus(String interval) {
		System.out.println(String.format("Statements for  %-36s:", //
				getDisplayName(36)));

		for (final Statement s : this.statements) {
			System.out.println(String.format("  %s  %3d tx  %10.2f", //
					s.date, s.transactions.size(), s.balance));
		}

		System.out.println("Uncleared transactions:");

		for (final GenericTxn t : this.transactions) {
			if (t.stmtdate != null) {
				continue;
			}

			System.out.println(String.format("  %s  %10.2f  %s", //
					Common.getDateString(t.getDate()), //
					t.getAmount(), t.getPayee()));
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
		String s = "Account" + this.id + ": " + this.name //
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

	public boolean balanceStatement(BigDecimal curbal, Statement s) {
		final List<GenericTxn> txns = gatherTransactionsForStatement(s);
		List<GenericTxn> uncleared = null;

		final BigDecimal totaltx = sumAmounts(txns);
		final BigDecimal diff = totaltx.add(curbal).subtract(s.balance);

		System.out.println(" Stmt: " + this.name + " - " + s);

		if (diff.signum() != 0) {
			uncleared = findSubsetTotaling(txns, diff);
			if ((uncleared == null) || uncleared.isEmpty()) {
				System.out.println();
				System.out.println("Can't balance account: " + this);

				listTransactions(txns, 20);
				Common.reportWarning("Statement balance failed");
				return false;
			}

			txns.removeAll(uncleared);
		}

		s.clearTransactions(txns, uncleared);

		if ((uncleared != null) && (uncleared.size() >= SUBSET_WARNING)) {
			Common.reportWarning("Large number of uncommitted transactions");

			System.out.println("Cleared:");
			for (GenericTxn t : txns) {
				System.out.println(String.format("%10.2f %s %s", //
						t.getAmount(), Common.getDateString(t.getDate()), //
						Common.getCheckNumString(t)));
			}

			System.out.println("Uncleared:");
			for (GenericTxn t : uncleared) {
				System.out.println(String.format("%10.2f %s %s", //
						t.getAmount(), Common.getDateString(t.getDate()), //
						Common.getCheckNumString(t)));
			}

			s.print();
		}

		return true;
	}

	void listTransactions(List<GenericTxn> txns, int max) {
		System.out.println("Transaction list");

		for (int ii = Math.max(0, txns.size() - max); ii < txns.size(); ++ii) {
			final GenericTxn t = txns.get(ii);
			String cknum = "";
			if (t instanceof NonInvestmentTxn) {
				cknum = ((NonInvestmentTxn) t).chkNumber;
			}
			System.out.println(String.format("%s  %5s %10.2f  %10.2f", //
					Common.getDateString(t.getDate()), //
					cknum, t.getAmount(), t.runningTotal));
		}
	}

	static int maxsubset = 0;
	// TODO figure out a better algorithm for balancing?
	static final int SUBSET_LIMIT = 20;
	static final int SUBSET_WARNING = 10;

	private List<GenericTxn> findSubsetTotaling(List<GenericTxn> txns, BigDecimal diff) {
		// First try removing one transaction, then two, ...
		// Return a list of the fewest transactions totaling the desired amount
		// Limit how far back we go.
		int lowlimit = Math.max(0, txns.size() - 50);

		for (int nn = 1; (nn <= txns.size()) && (nn < SUBSET_LIMIT); ++nn) {
			final List<GenericTxn> subset = findSubsetTotaling(txns, diff, nn, lowlimit, txns.size());

			if (subset != null) {
				int n = subset.size();
				if (n > maxsubset) {
					maxsubset = n;
				}

				return subset;
			}

			if (nn == SUBSET_WARNING) {
				Common.reportWarning("Large number of uncommitted transactions");
			}
		}

		return null;
	}

	// Try combinations of nn transactions, indexes between min and max-1.
	// Return the first that adds up to tot.
	private List<GenericTxn> findSubsetTotaling( //
			List<GenericTxn> txns, BigDecimal tot, int nn, int min, int max) {
		if (nn > (max - min)) {
			return null;
		}

		// Remove one transaction, starting with the most recent
		for (int ii = max - 1; ii >= min; --ii) {
			final GenericTxn t = txns.get(ii);
			final BigDecimal newtot = tot.subtract(t.getAmount());

			if ((nn == 1) && (newtot.signum() == 0)) {
				// We are looking for one transaction and found it
				final List<GenericTxn> ret = new ArrayList<GenericTxn>();
				ret.add(t);

				return ret;
			}

			if ((nn > 1) && (nn <= ii)) {
				// We need n-1 more transactions - we have already considered
				// combinations with transactions after index ii, so start
				// before that, looking for n-1 transactions adding up to the
				// adjusted total.
				final List<GenericTxn> ret = findSubsetTotaling(txns, newtot, nn - 1, min, ii);

				if (ret != null) {
					ret.add(t);

					return ret;
				}
			}
		}

		return null;
	}

	private BigDecimal sumAmounts(List<GenericTxn> txns) {
		BigDecimal totaltx = BigDecimal.ZERO;
		for (final GenericTxn t : txns) {
			totaltx = totaltx.add(t.getAmount());
		}

		return totaltx;
	}

	private List<GenericTxn> gatherTransactionsForStatement(Statement s) {
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

		return txns;
	}
}
