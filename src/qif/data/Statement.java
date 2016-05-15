package qif.data;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import app.QifLoader;

public class Statement {
	public int domid;
	public int acctid;
	public Date date;
	public BigDecimal credits;
	public BigDecimal debits;
	public BigDecimal openingBalance;
	public BigDecimal balance;

	public List<GenericTxn> transactions;
	public List<GenericTxn> unclearedTransactions;

	public Statement(int domid, int acctid) {
		this.acctid = acctid;
		this.domid = domid;
		this.date = null;
		this.credits = this.debits = this.openingBalance = this.balance = null;
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
				final String aname = qline.value;
				final Account a = dom.findAccount(aname);
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

	public void clearTransactions(BigDecimal openingBalance, //
			List<GenericTxn> txns, List<GenericTxn> unclearedTxns) {
		this.openingBalance = openingBalance;

		for (final GenericTxn t : txns) {
			t.stmtdate = this.date;
		}

		this.transactions = txns;
		this.unclearedTransactions = unclearedTxns;
	}

	public boolean balance(BigDecimal curbal, Account a) {
		List<GenericTxn> txns = a.gatherTransactionsForStatement(this);
		List<GenericTxn> uncleared = null;

		final BigDecimal totaltx = Common.sumAmounts(txns);
		BigDecimal diff = totaltx.add(curbal).subtract(this.balance);

		if (diff.signum() != 0) {
			uncleared = Common.findSubsetTotaling(txns, diff);

			if (uncleared.isEmpty()) {
				System.out.println("Can't balance account: " + this);
			} else {
				diff = BigDecimal.ZERO;
				txns.removeAll(uncleared);
			}
		}

		clearTransactions(curbal, txns, uncleared);

		//System.out.println(" Stmt: " + a.name + " - " + this);
		boolean ok = (diff.signum() == 0) || adjust();

		if (ok) {
			logDetails(a);
		}

		return ok;
	}

	private boolean adjust() {
		boolean done = false;
		boolean abort = false;

		while (!done && !abort) {
			arrangeTransactionsForDisplay(this.transactions);
			print();

			final BigDecimal diff = checkBalance();
			final boolean ok = diff.signum() == 0;
			final String dflt = "(" + String.format("%3.2f", diff) + ")";

			System.out.print("CMD" + dflt + "> ");

			String s = QifLoader.scn.nextLine();

			if ((s == null) || (s.length() == 0)) {
				s = "q";
			}

			switch (s.charAt(0)) {
			case 'a':
				abort = true;
				break;

			case 'q':
				if (ok) {
					done = true;
				}
				break;

			case 'r':
			case 'u': {
				final boolean isReconcile = s.charAt(0) == 'r';
				List<GenericTxn> lst = (isReconcile) //
						? this.unclearedTransactions //
						: this.transactions;
				final List<GenericTxn> txns = new ArrayList<GenericTxn>();
				final StringTokenizer toker = new StringTokenizer(s.substring(1));

				while (toker.hasMoreTokens()) {
					try {
						final int n = Integer.parseInt(toker.nextToken());
						if ((n > 0) && (n <= lst.size())) {
							final GenericTxn t = lst.get(n - 1);
							txns.add(t);
						}
					} catch (Exception e) {
						// be charitable
					}
				}

				if (isReconcile) {
					clearTransactions(txns);
				} else {
					unclearTransactions(txns);
				}
				break;
			}
			}
		}

		return done && !abort;
	}

	private void logDetails(Account a) {
		PrintStream ps = System.out;

		ps.print(a.name + ";");
		ps.print(Common.getDateString(this.date) + ";");
		ps.print(String.format("%5.2f;%5.2f;%d", //
				this.openingBalance, this.balance, this.transactions.size()));

		for (GenericTxn t : this.transactions) {
			ps.print(";" + Common.getDateString(t.getDate()) + ";" //
					+ String.format("%5.2f", t.getAmount()));
		}

		ps.println();
	}

	public void print() {
		final QifDom dom = QifDom.getDomById(this.domid);
		final Account a = dom.getAccount(this.acctid);

		System.out.println();
		System.out.println("-------------------------------------------------------");
		System.out.println("Reconciled statement:" //
				+ "  " + Common.getDateString(this.date) //
				+ " " + this.balance //
				+ "  " + a.name);
		System.out.println("-------------------------------------------------------");

		for (int ii = 0; ii < this.transactions.size(); ++ii) {
			final GenericTxn t = this.transactions.get(ii);

			System.out.println(String.format("%3d: ", ii + 1) + t.toStringShort());
		}

		System.out.println();
		System.out.println("Uncleared transactions:");

		if (this.unclearedTransactions != null) {
			for (int ii = 0; ii < this.unclearedTransactions.size(); ++ii) {
				final GenericTxn t = this.unclearedTransactions.get(ii);

				System.out.println(String.format("%3d: ", ii + 1) + t.toString());
			}
		}
	}

	private void clearTransactions(List<GenericTxn> txns) {
		for (final GenericTxn t : txns) {
			t.stmtdate = this.date;
		}

		this.transactions.addAll(txns);
		this.unclearedTransactions.removeAll(txns);
	}

	private void unclearTransactions(List<GenericTxn> txns) {
		for (final GenericTxn t : txns) {
			t.stmtdate = null;
		}

		this.transactions.removeAll(txns);
		this.unclearedTransactions.addAll(txns);
	}

	private BigDecimal checkBalance() {
		final BigDecimal txsum = Common.sumAmounts(this.transactions);
		final BigDecimal newbal = this.openingBalance.add(txsum);

		return newbal.subtract(this.balance);
	}

	private void arrangeTransactionsForDisplay(List<GenericTxn> txns) {
		Collections.sort(txns, (o1, o2) -> {
			final int diff = o1.getCheckNumber() - o2.getCheckNumber();
			if (diff != 0) {
				return diff;
			}

			return o1.getDate().compareTo(o1.getDate());
		});
	}

	public String toString() {
		final String s = Common.getDateString(this.date) //
				+ "  " + this.balance //
				+ " tran=" + ((this.transactions != null) ? this.transactions.size() : null);

		return s;
	}
};
