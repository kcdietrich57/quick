package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Account {
	enum AcctType { //
		Bank, CCard, Cash, Asset, Liability, Invest, InvPort, Inv401k, InvMutual;

		public static AcctType parse(String s) {
			if (s.equals("Bank")) {
				return Bank;
			}
			if (s.equals("CCard")) {
				return CCard;
			}
			if (s.equals("Cash")) {
				return Cash;
			}
			if (s.equals("Oth A")) {
				return Asset;
			}
			if (s.equals("Oth L")) {
				return Liability;
			}
			if (s.equals("Port")) {
				return InvPort;
			}
			if (s.equals("Invst")) {
				return Invest;
			}
			if (s.equals("401(k)/403(b)")) {
				return Inv401k;
			}
			if (s.equals("Mutual")) {
				return InvMutual;
			}

			Common.reportError("Unknown account type: " + s);
			return AcctType.Bank;
		}
	};

	public short id;
	public String name;
	public AcctType type;
	public String description;
	public BigDecimal creditLimit;
	public Date stmtDate;
	public BigDecimal stmtBalance;

	public List<GenericTxn> transactions;
	public List<Statement> statements;

	public Account() {
		this.id = 0;

		this.transactions = new ArrayList<GenericTxn>();
		this.statements = new ArrayList<Statement>();

		name = "";
		description = "";
	}

	public Account(short id) {
		this();

		this.id = id;
	}

	public Account(Account other) {
		this(other.id);

		id = other.id;
		name = other.name;
		type = other.type;
		description = other.description;
		creditLimit = other.creditLimit;
		stmtDate = other.stmtDate;
		stmtBalance = other.stmtBalance;

//		for (GenericTxn t : other.transactions) {
//			this.transactions.add(GenericTxn.cloneTransaction(t));
//		}
//
//		for (Statement s : this.statements) {
//			this.statements.add(s);
//		}
	}

	public boolean isNonInvestmentAccount() {
		switch (this.type) {
		case Bank:
		case CCard:
		case Cash:
			return true;

		case Asset:
		case Liability:
			return false; // TODO for now

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
				+ " desc=" + this.description //
				+ " limit=" + this.creditLimit //
				+ " bal=" + this.stmtBalance //
				+ " asof " + this.stmtDate //
				+ " #tx= " + this.transactions.size() //
				+ "\n";

		return s;
	}
};
