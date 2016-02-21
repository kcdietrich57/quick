package qif.data;

import static qif.data.Headers.ACCT_CREDITLIMIT;
import static qif.data.Headers.ACCT_DESCRIPTION;
import static qif.data.Headers.ACCT_STMTBAL;
import static qif.data.Headers.ACCT_STMTDATE;
import static qif.data.Headers.ACCT_TYPE;
import static qif.data.Headers.END;

import java.io.PrintWriter;
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

	private static short nextid = 1;
	public final short id;

	public String name;
	public AcctType type;
	public String description;
	public BigDecimal creditLimit;
	public Date stmtDate;
	public BigDecimal stmtBalance;
	List<Statement> statements;

	public List<GenericTxn> transactions;

	public Account() {
		this.id = nextid++;

		name = "";
		description = "";

		this.transactions = new ArrayList<GenericTxn>();
		this.statements = new ArrayList<Statement>();
	}

	public static Account load(QFileReader qfr) {
		QFileReader.QLine qline = new QFileReader.QLine();

		Account acct = new Account();

		for (;;) {
			qfr.nextAccountLine(qline);

			switch (qline.type) {
			case EndOfSection:
				return acct;

			case AcctType:
				acct.type = AcctType.parse(qline.value);
				break;
			case AcctCreditLimit:
				acct.creditLimit = Common.getDecimal(qline.value);
				break;
			case AcctDescription:
				acct.description = qline.value;
				break;
			case AcctName:
				acct.name = qline.value;
				break;
			case AcctStmtBal:
				acct.stmtBalance = Common.getDecimal(qline.value);
				break;
			case AcctStmtDate:
				acct.stmtDate = Common.GetDate(qline.value);
				break;

			default:
				Common.reportError("syntax error");
			}
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
				+ " desc=" + this.description //
				+ " limit=" + this.creditLimit //
				+ " bal=" + this.stmtBalance //
				+ " asof " + this.stmtDate //
				+ " #tx= " + this.transactions.size() //
				+ "\n";

		return s;
	}

	public static void export(PrintWriter pw, List<Account> list) {
		if ((list == null) || (list.size() == 0)) {
			return;
		}

		Common.writeln(pw, Headers.HdrAccount);

		for (Account acct : list) {
			Common.writeIfSet(pw, ACCT_TYPE, "" + acct.type);
			Common.write(pw, ACCT_CREDITLIMIT, acct.creditLimit.toString());
			Common.writeIfSet(pw, ACCT_DESCRIPTION, acct.description);
			Common.writeIfSet(pw, Headers.ACCT_NAME, acct.name);
			Common.write(pw, ACCT_STMTBAL, acct.stmtBalance.toString());
			Common.write(pw, ACCT_STMTDATE, acct.stmtDate.toString());
			Common.write(pw, END);
		}
	}
};
