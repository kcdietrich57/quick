package qif.data;

import java.math.BigDecimal;
import java.math.MathContext;
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
		acctValue = acctValue.round(new MathContext(2));

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

		if (diff.signum() != 0) {
			listTransactions(txns);

			uncleared = findSubsetTotaling(txns, diff);
			if ((uncleared == null) || uncleared.isEmpty()) {
				System.out.println("Can't balance account: " + this);
				System.out.println(" Stmt: " + s);

				return false;
			}

			txns.removeAll(uncleared);
		}

		s.clearTransactions(txns, uncleared);

		return true;
	}

	void listTransactions(List<GenericTxn> txns) {
		System.out.println("Transaction list");

		for (final GenericTxn t : txns) {
			String cknum = "";
			if (t instanceof NonInvestmentTxn) {
				cknum = ((NonInvestmentTxn) t).chkNumber;
			}
			System.out.println(String.format("%s  %5s %10.2f  %10.2f", //
					Common.getDateString(t.getDate()), //
					cknum, t.getAmount(), t.runningTotal));
		}
	}

	private List<GenericTxn> findSubsetTotaling(List<GenericTxn> txns, BigDecimal diff) {
		List<List<GenericTxn>> matches = null;

		for (int nn = 1; nn <= txns.size(); ++nn) {
			final List<List<GenericTxn>> subsets = findSubsetsTotaling(txns, diff, nn, txns.size());

			if (!subsets.isEmpty()) {
				matches = subsets;
				break;
			}
		}

		return (matches == null) ? null : matches.get(0);
	}

	private List<List<GenericTxn>> findSubsetsTotaling( //
			List<GenericTxn> txns, BigDecimal tot, int nn, int max) {
		final List<List<GenericTxn>> ret = new ArrayList<List<GenericTxn>>();
		if (nn > txns.size()) {
			return ret;
		}

		final List<GenericTxn> txns_work = new ArrayList<>();
		txns_work.addAll(txns);

		for (int ii = max - 1; ii >= 0; --ii) {
			final BigDecimal newtot = tot.subtract(txns.get(ii).getAmount());
			final GenericTxn t = txns_work.remove(ii);

			if ((nn == 1) && (newtot.signum() == 0)) {
				final List<GenericTxn> l = new ArrayList<GenericTxn>();
				l.add(t);
				ret.add(l);

				return ret;
			}

			if (nn > 1 && nn <= ii) {
				final List<List<GenericTxn>> subsets = findSubsetsTotaling(txns_work, newtot, nn - 1, ii);
				txns_work.add(ii, t);

				if (!subsets.isEmpty()) {
					for (final List<GenericTxn> l : subsets) {
						l.add(t);
					}

					ret.addAll(subsets);

					return ret;
				}
			}
		}

		return ret;
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
