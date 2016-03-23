package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Account {
	enum AccountType { //
		Bank, CCard, Cash, Asset, Liability, Invest, InvPort, Inv401k, InvMutual;

		public static AccountType parse(String s) {
			switch (s.charAt(0)) {
			case 'B':
				if (s.equals("Bank")) {
					return Bank;
				}
				break;
			case 'C':
				if (s.equals("CCard")) {
					return CCard;
				}
				if (s.equals("Cash")) {
					return Cash;
				}
				break;
			case 'I':
				if (s.equals("Invst")) {
					return Invest;
				}
				break;
			case 'M':
				if (s.equals("Mutual")) {
					return InvMutual;
				}
				break;
			case 'O':
				if (s.equals("Oth A")) {
					return Asset;
				}
				if (s.equals("Oth L")) {
					return Liability;
				}
				break;
			case 'P':
				if (s.equals("Port")) {
					return InvPort;
				}
				break;
			case '4':
				if (s.equals("401(k)/403(b)")) {
					return Inv401k;
				}
				break;
			}

			Common.reportError("Unknown account type: " + s);
			return AccountType.Bank;
		}
	};

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
				+ " clbal=" + this.clearedBalance //
				+ " bal=" + this.balance //
				+ " desc=" + this.description //
				+ " limit=" + this.creditLimit //
				+ " #tx= " + this.transactions.size() //
				+ "\n";

		return s;
	}
};
