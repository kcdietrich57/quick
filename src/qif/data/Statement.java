package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

public class Statement {
	public int domid;
	public int acctid;
	public Date date;
	public BigDecimal credits;
	public BigDecimal debits;
	public BigDecimal balance;

	public List<GenericTxn> transactions;
	public List<GenericTxn> unclearedTransactions;

	public Statement(int domid, int acctid) {
		this.acctid = acctid;
		this.domid = domid;
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

	public static Statement load(QFileReader qfr, int domid, int acctid) {
		final QFileReader.QLine qline = new QFileReader.QLine();

		final Statement stmt = new Statement(domid, acctid);

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

	public static List<Statement> loadStatements(QFileReader qfr, QifDom dom) {
		final QFileReader.QLine qline = new QFileReader.QLine();
		final List<Statement> stmts = new ArrayList<Statement>();

		for (;;) {
			qfr.nextStatementsLine(qline);

			switch (qline.type) {
			case EndOfSection:
				return stmts;

			case StmtsAccount: {
				String aname = qline.value;
				Account a = dom.findAccount(aname);
				if (a == null) {
					Common.reportError("Can't find account: " + aname);
				}

				dom.currAccount = a;
				break;
			}

			case StmtsMonthly: {
				final StringTokenizer toker = new StringTokenizer(qline.value, " ");
				final String datestr = toker.nextToken();
				final int slash1 = datestr.indexOf('/');
				final int slash2 = (slash1 < 0) ? -1 : datestr.indexOf('/', slash1 + 1);
				int day = 0; // last day of month
				int month = 1;
				int year = 0;
				if (slash2 > 0) {
					month = Integer.parseInt(datestr.substring(0, slash1));
					day = Integer.parseInt(datestr.substring(slash1 + 1, slash2));
					year = Integer.parseInt(datestr.substring(slash2 + 1));
				} else if (slash1 >= 0) {
					month = Integer.parseInt(datestr.substring(0, slash1));
					year = Integer.parseInt(datestr.substring(slash1 + 1));
				} else {
					year = Integer.parseInt(datestr);
				}

				while (toker.hasMoreTokens()) {
					if (month > 12) {
						Common.reportError("Statements month wrapped to next year");
					}

					final BigDecimal bal = new BigDecimal(toker.nextToken());
					final Date d = (day == 0) //
							? Common.getDateForEndOfMonth(year, month) //
							: Common.getDate(year, month, day);

					final Statement stmt = new Statement(dom.domid, dom.currAccount.id);
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

	public void clearTransactions(List<GenericTxn> txns, List<GenericTxn> unclearedTxns) {
		for (final GenericTxn t : txns) {
			t.stmtdate = this.date;
		}

		this.transactions = txns;
		this.unclearedTransactions = unclearedTxns;

		if (QifDom.verbose()) {
			print();
		}
	}

	public void print() {
		final QifDom dom = QifDom.getDomById(this.domid);
		final Account a = dom.getAccount(this.acctid);

		System.out.println();
		System.out.println("-------------------------------------------------------");
		System.out.println("Reconciled statement: " + a.name //
				+ " " + Common.getDateString(this.date) //
				+ " cr=" + this.credits + " db=" + this.debits //
				+ " bal=" + this.balance);

		List<NonInvestmentTxn> checks = new ArrayList<NonInvestmentTxn>();

		for (final GenericTxn t : this.transactions) {
			NonInvestmentTxn check = null;

			if (t instanceof NonInvestmentTxn) {
				check = (NonInvestmentTxn) t;
				if ((check.chkNumber == null) //
						|| (check.chkNumber.length() < 1) //
						|| !Character.isDigit(check.chkNumber.charAt(0))) {
					check = null;
				}
			}

			if (check == null) {
				System.out.println(t.toStringShort());
			} else {
				checks.add(check);
			}
		}

		Collections.sort(checks, new Comparator<NonInvestmentTxn>() {
			public int compare(NonInvestmentTxn o1, NonInvestmentTxn o2) {
				return Integer.parseInt(o1.chkNumber) - Integer.parseInt(o2.chkNumber);
			}
		});

		for (final NonInvestmentTxn t : checks) {
			System.out.println(t.toStringShort());
		}

		System.out.println();
		System.out.println("Uncleared transactions:");

		if (this.unclearedTransactions != null) {
			for (final GenericTxn t : this.unclearedTransactions) {
				System.out.println(t.toString());
			}
		}
	}

	public String toString() {
		final String s = Common.getDateString(this.date) //
				// + " cr=" + this.credits //
				// + " db=" + this.debits //
				+ "  " + this.balance //
				+ " tran=" + ((this.transactions != null) ? this.transactions.size() : null);

		return s;
	}
};
