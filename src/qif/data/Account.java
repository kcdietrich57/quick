package qif.data;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Account {
	public int acctid;

	private String name;
	public AccountType type;
	public String description;
	public Date closeDate;
	public BigDecimal creditLimit;
	public BigDecimal balance;
	public BigDecimal clearedBalance;

	public List<GenericTxn> transactions;
	public List<Statement> statements;
	public SecurityPortfolio securities;

	public Account(QifDom dom) {
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

	public Date getOpenDate() {
		if (this.transactions != null) {
			return this.transactions.get(0).getDate();
		}

		return null;
	}

	public boolean isOpenAsOf(Date d) {
		if (d == null) {
			d = new Date();
		}

		final Date openDate = getOpenDate();

		return (openDate == null) //
				|| ((d == null) || (openDate.compareTo(d) <= 0));
	}

	public boolean isClosedAsOf(Date d) {
		if (d == null) {
			d = new Date();
		}

		return (this.closeDate != null) && (this.closeDate.compareTo(d) <= 0);
	}

	public boolean isOpenOn(Date d) {
		return isOpenAsOf(d) && !isClosedAsOf(d);
	}

	public void setName(String name) {
		this.name = name;

		if (name.equals("Waddell & Reed")) {
			this.type = AccountType.Invest;
		} else if (name.equals("Deferred 401k Match")) {
			this.type = AccountType.Inv401k;
		} else if (name.equals("GD IRA (E*Trade)") //
				|| name.equals("GD IRA (Scottrade)") //
				|| name.equals("TD IRA (E*Trade)") //
				|| name.equals("TD IRA (Scottrade)") //
				|| name.equals("IBM Pension")) {
			this.type = AccountType.Inv401k;
		}
	}

	public String getName() {
		return this.name;
	}

	public Date getLastStatementDate() {
		return (this.statements.isEmpty()) //
				? null //
				: this.statements.get(this.statements.size() - 1).date;
	}

	public Statement getLastStatement() {
		return (this.statements.isEmpty()) //
				? null //
				: this.statements.get(this.statements.size() - 1);
	}

	public Statement getStatement(Date date) {
		return getStatement(date, null);
	}

	public Statement getStatement(Date date, BigDecimal balance) {
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
				+ Common.formatDate(date) + " " //
				+ Common.formatAmount(balance));
		return null;
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

	public Date getFirstTransactionDate() {
		return (this.transactions.isEmpty()) ? null : this.transactions.get(0).getDate();
	}

	public Date getLastTransactionDate() {
		return (this.transactions.isEmpty()) ? null : this.transactions.get(this.transactions.size() - 1).getDate();
	}

	public Date getFirstUnclearedTransactionDate() {
		final GenericTxn t = getFirstUnclearedTransaction();

		return (t == null) ? null : t.getDate();
	}

	public GenericTxn getFirstUnclearedTransaction() {
		for (final GenericTxn t : this.transactions) {
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

			if (!s.reconcile(this, msg)) {
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

	public void generateMonthlyStatements() {
		int lastyear = -1;
		int lastmonth = -1;
		GenericTxn lasttx = null;
		boolean first = true;

		System.out.println("\n!Account");
		System.out.println("N" + this.name);
		System.out.println("^");
		System.out.println("!Statements");

		for (final GenericTxn t : this.transactions) {
			final Calendar cal = Calendar.getInstance();
			cal.setTime(t.getDate());

			final int thisyear = cal.get(Calendar.YEAR);
			final int thismonth = cal.get(Calendar.MONTH);

			if ((lastyear != thisyear) || (lastmonth != thismonth)) {
				if (!first) {
					System.out.print(String.format(" %4.2f", lasttx.runningTotal));
				}

				if ((lastyear != thisyear) || (lastmonth + 1 != thismonth)) {
					if (!first) {
						System.out.println();
					}

					System.out.print("M" + Common.formatDateMonthYear(t.getDate()));
				}

				lastyear = thisyear;
				lastmonth = thismonth;
			}

			first = false;
			lasttx = t;
		}

		System.out.println();
		System.out.println("^");
	}

	private String getOpenCloseDateString() {
		String openstr = (this.transactions.isEmpty()) //
				? "??/??/????" //
				: Common.formatDate(this.transactions.get(0).getDate());
		openstr += "- ";
		openstr += (this.closeDate != null) //
				? Common.formatDate(this.closeDate) //
				: "  /  /  ";

		return openstr;
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

	BigDecimal getCashValueForDate(Date d) {
		BigDecimal cashBal = null;

		int idx = Common.findLastTransactionOnOrBeforeDate(this.transactions, d);
		while ((cashBal == null) && (idx >= 0)) {
			final GenericTxn tx = this.transactions.get(idx);
			--idx;

			cashBal = tx.runningTotal;
		}

		return (cashBal != null) ? cashBal : BigDecimal.ZERO;
	}

	// TODO unused, unfinished
	void getPositionsForDate(Date d) {
		this.securities.getPositionsForDate(d);
	}

	BigDecimal getValueForDate(Date d) {
		final BigDecimal cashBal = getCashValueForDate(d);
		final BigDecimal secBal = this.securities.getPortfolioValueForDate(d);

		BigDecimal acctValue = cashBal.add(secBal);

		acctValue = acctValue.setScale(2, RoundingMode.HALF_UP);

		return acctValue;
	}

	BigDecimal getSecuritiesValueForDate(Date d) {
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

		BigDecimal v1 = getCashValueForDate(d1);

		return apos;
	}
}
