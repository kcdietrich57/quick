package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

class SecurityPosition {
	Security security;
	BigDecimal shares;
	List<InvestmentTxn> transactions;

	public SecurityPosition(Security sec, BigDecimal shares) {
		this.security = sec;
		this.shares = shares;
		this.transactions = new ArrayList<InvestmentTxn>();
	}

	public SecurityPosition(Security sec) {
		this(sec, BigDecimal.ZERO);
	}

	public String toString() {
		String s = "Sec: " + this.security.name + //
				" Shares: " + this.shares + //
				" Txns: " + this.transactions.size();
		return s;
	}
}

class SecurityDetails {
	List<SecurityPosition> positions;

	public SecurityDetails() {
		this.positions = new ArrayList<SecurityPosition>();
	}

	public SecurityPosition getPosition(Security sec) {
		for (SecurityPosition pos : this.positions) {
			if (pos.security == sec) {
				return pos;
			}
		}

		SecurityPosition newpos = new SecurityPosition(sec);
		this.positions.add(newpos);

		return newpos;
	}
}

public class Account {
	enum AccountType { //
		Bank, CCard, Cash, Asset, Liability, Invest, InvPort, Inv401k, InvMutual;
	}

	public short domid;
	public short id;

	public String name;
	public AccountType type;
	public String description;
	public BigDecimal creditLimit;
	public BigDecimal balance;
	public BigDecimal clearedBalance;

	public List<GenericTxn> transactions;
	public List<Statement> statements;
	public SecurityDetails securities;

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
		this.securities = new SecurityDetails();
	}

	public Account(short id, QifDom dom) {
		this(dom);

		this.id = id;
	}

	public Account(Account other, QifDom dom) {
		this(other.id, dom);

		this.name = other.name;
		this.type = other.type;
		this.description = other.description;
		this.creditLimit = other.creditLimit;

		for (GenericTxn t : other.transactions) {
			this.transactions.add(GenericTxn.clone(dom.domid, t));
		}

		for (Statement s : this.statements) {
			this.statements.add(new Statement(s));
		}
	}

	public boolean isInvestmentAccount() {
		switch (this.type) {
		case Bank:
		case CCard:
		case Cash:
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
			GenericTxn t = this.transactions.get(ii);

			if (!t.isCleared()) {
				return ii;
			}
		}

		return -1;
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

		if (!this.securities.positions.isEmpty()) {
			s += "Securities Held:\n";

			for (SecurityPosition p : this.securities.positions) {
				s += p.security.name + " " + p.shares + "\n";
			}
		}
		return s;
	}
}
