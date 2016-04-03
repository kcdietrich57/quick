package qif.data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class Statement {
	public short acctid;
	public Date date;
	public BigDecimal credits;
	public BigDecimal debits;
	public BigDecimal balance;

	public List<GenericTxn> transactions;

	public Statement(short acctid) {
		this.acctid = 0;
		this.date = null;
		this.credits = this.debits = this.balance = null;
	}

	public Statement(Statement other) {
		this.acctid = other.acctid;
		this.date = other.date;
		this.credits = other.credits;
		this.debits = other.debits;
		this.balance = other.balance;
	}

	public static Statement load(QFileReader qfr, short acctid) {
		final QFileReader.QLine qline = new QFileReader.QLine();

		final Statement acct = new Statement(acctid);

		for (;;) {
			qfr.nextStatementLine(qline);

			switch (qline.type) {
			case EndOfSection:
				return acct;

			case StmtDate:
				acct.date = Common.parseDate(qline.value);
				break;
			case StmtCredits:
				acct.credits = Common.getDecimal(qline.value);
				break;
			case StmtDebits:
				acct.debits = Common.getDecimal(qline.value);
				break;
			case StmtBalance:
				acct.balance = Common.getDecimal(qline.value);
				break;

			default:
				Common.reportError("syntax error");
			}
		}
	}

	public void addTransaction(GenericTxn txn) {
		this.transactions.add(txn);
	}

	public String toString() {
		final String s = "Statement" //
				+ " dt=" + this.date //
				+ " cr=" + this.credits //
				+ " db=" + this.debits //
				+ " bal=" + this.balance //
				+ "\n";

		return s;
	}
};
