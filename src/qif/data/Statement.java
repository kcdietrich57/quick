package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

public class Statement {
	public short acctid;
	public Date date;
	public BigDecimal credits;
	public BigDecimal debits;
	public BigDecimal balance;

	public List<GenericTxn> transactions;

	public Statement(int acctid) {
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

	public static Statement load(QFileReader qfr, int acctid) {
		final QFileReader.QLine qline = new QFileReader.QLine();

		final Statement stmt = new Statement(acctid);

		for (;;) {
			qfr.nextStatementLine(qline);

			switch (qline.type) {
			case EndOfSection:
				return stmt;

			case StmtDate:
				stmt.date = Common.parseDate(qline.value);
				break;
			case StmtCredits:
				stmt.credits = Common.getDecimal(qline.value);
				break;
			case StmtDebits:
				stmt.debits = Common.getDecimal(qline.value);
				break;
			case StmtBalance:
				stmt.balance = Common.getDecimal(qline.value);
				break;

			default:
				Common.reportError("syntax error");
			}
		}
	}

	public static List<Statement> loadStatements(QFileReader qfr, int acctid) {
		final QFileReader.QLine qline = new QFileReader.QLine();
		final List<Statement> stmts = new ArrayList<Statement>();

		for (;;) {
			qfr.nextStatementsLine(qline);

			switch (qline.type) {
			case EndOfSection:
				return stmts;

			case StmtsMonthly: {
				final StringTokenizer toker = new StringTokenizer(qline.value, " ");
				final String datestr = toker.nextToken();
				final int slash = datestr.indexOf('/');
				int month = 1;
				int year = 0;
				if (slash >= 0) {
					month = Integer.parseInt(datestr.substring(0, slash));
					year = Integer.parseInt(datestr.substring(slash + 1));
				} else {
					year = Integer.parseInt(datestr);
				}

				while (toker.hasMoreTokens()) {
					if (month > 12) {
						Common.reportError("Statements month wrapped to next year");
					}

					final BigDecimal bal = new BigDecimal(toker.nextToken());
					final Date d = Common.getDateForEndOfMonth(year, month);

					final Statement stmt = new Statement(acctid);
					stmt.date = d;
					stmt.balance = bal;

					stmts.add(stmt);

					++month;
				}

				break;
			}

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
