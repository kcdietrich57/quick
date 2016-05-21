package qif.data;

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
	public BigDecimal openingBalance;
	public BigDecimal balance;
	StatementDetails details;

	public List<GenericTxn> transactions;
	public List<GenericTxn> unclearedTransactions;

	public Statement(int domid, int acctid) {
		this.acctid = acctid;
		this.domid = domid;
		this.date = null;
		this.openingBalance = null;
		this.balance = null;
		this.details = null;
	}

	public Statement(Statement other) {
		this.acctid = other.acctid;
		this.date = other.date;
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
			case StmtDebits:
				// TODO not used
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

					final Statement stmt = new Statement(dom.domid, dom.currAccount.acctid);
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

	public boolean reconcile(BigDecimal curbal, Account a, String msg) {
		boolean needsReconcile = false;

		if (!getTransactionsFromDetails(a)) {
			// Didn't load stmt info, try automatic reconciliation
			final List<GenericTxn> txns = a.gatherTransactionsForStatement(this);
			final List<GenericTxn> uncleared = new ArrayList<GenericTxn>();

			final BigDecimal totaltx = Common.sumAmounts(txns);
			BigDecimal diff = totaltx.add(curbal).subtract(this.balance);

			if (diff.signum() != 0) {
				Common.findSubsetTotaling(txns, uncleared, diff);

				if (uncleared.isEmpty()) {
					System.out.println("Can't automatically balance account: " + this);
				} else {
					diff = BigDecimal.ZERO;
					txns.removeAll(uncleared);
				}
			}

			clearTransactions(curbal, txns, uncleared);

			needsReconcile = (this.details == null) || (diff.signum() != 0);
		}

		final boolean isBalanced = review(needsReconcile, msg);

		if (isBalanced && (this.details == null)) {
			this.details = new StatementDetails(this);
		}

		return isBalanced;
	}

	private boolean getTransactionsFromDetails(Account a) {
		if (this.details == null) {
			return false;
		}

		final List<GenericTxn> txns = a.gatherTransactionsForStatement(this);
		this.transactions = new ArrayList<GenericTxn>();

		for (final TxInfo info : this.details.transactions) {
			for (int ii = 0; ii < txns.size(); ++ii) {
				final GenericTxn t = txns.get(ii);

				if (info.date.compareTo(t.getDate()) == 0) {
					if ((info.cknum == t.getCheckNumber()) //
							&& (info.amount.compareTo(t.getAmount()) == 0)) {
						this.transactions.add(t);
						t.stmtdate = this.date;

						txns.remove(ii);
						break;
					}
				}

				final long infoms = info.date.getTime();
				final long tranms = t.getDate().getTime();

				if (infoms < tranms) {
					return false;
				}
			}
		}

		this.unclearedTransactions = txns;

		return true;
	}

	private boolean review(boolean reconcileNeeded, String msg) {
		boolean done = false;
		boolean abort = false;
		boolean sort = true;

		while (!done && !abort) {
			if (sort) {
				arrangeTransactionsForDisplay(this.transactions);
				arrangeTransactionsForDisplay(this.unclearedTransactions);
			}
			print(msg);

			if (!reconcileNeeded) {
				return true;
			}

			final BigDecimal diff = checkBalance();
			final boolean ok = diff.signum() == 0;
			final String dflt = "(" + String.format("%3.2f", diff) + ")";

			System.out.print("CMD" + dflt + "> ");

			String s = QifLoader.scn.nextLine();

			if ((s == null) || (s.length() == 0)) {
				s = "q";
			}
			s = s.trim();

			switch (s.charAt(0)) {
			case 'a':
				abort = true;
				break;

			case 'q':
				if (ok) {
					done = true;
				}
				break;

			case 's':
				if (s.startsWith("sort")) {
					sort = true;
				}
				break;

			case 'n':
				if (s.startsWith("nosort")) {
					sort = false;
				}
				break;

			case 'r':
			case 'u': {
				if (s.startsWith("rall")) {
					clearAllTransactions();
					break;
				}
				if (s.startsWith("uall")) {
					unclearAllTransactions();
					break;
				}

				final boolean isReconcile = s.charAt(0) == 'r';
				final List<GenericTxn> lst = (isReconcile) //
						? this.unclearedTransactions //
						: this.transactions;
				final List<GenericTxn> txns = new ArrayList<GenericTxn>();
				final StringTokenizer toker = new StringTokenizer(s.substring(1));
				String token = "";

				while (toker.hasMoreTokens()) {
					try {
						final int[] range = new int[2];

						token = toker.nextToken();
						parseRange(token, range);

						final int begin = range[0];
						final int end = range[1];

						for (int n = begin; (n > 0) && (n <= end) && (n <= lst.size()); ++n) {
							final GenericTxn t = lst.get(n - 1);
							txns.add(t);
						}
					} catch (final Exception e) {
						System.out.println("Bad arg: " + token);
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

	private void parseRange(String s, int[] range) {
		range[0] = range[1] = 0;
		final int dash = s.indexOf('-');

		final String s1 = (dash >= 0) ? s.substring(0, dash) : s;
		final String s2 = (dash >= 0) ? s.substring(dash + 1) : s1;

		if ((s1.length() == 0) || !Character.isDigit(s1.charAt(0)) || //
				(s2.length() == 0) || !Character.isDigit(s2.charAt(0))) {
			return;
		}

		range[0] = Integer.parseInt(s1);
		range[1] = Integer.parseInt(s2);
	}

	static class TxInfo {
		Date date;
		int cknum;
		BigDecimal amount;

		public TxInfo() {
		}

		public TxInfo(GenericTxn tx) {
			this.date = tx.getDate();
			this.cknum = tx.getCheckNumber();
			this.amount = tx.getAmount();
		}

		public String toString() {
			return String.format("%s %5d %10.2f", //
					Common.getDateString(this.date), this.cknum, this.amount);
		}
	}

	static class StatementDetails {
		int domid;
		int acctid;
		Date date;
		BigDecimal open;
		BigDecimal close;
		List<TxInfo> transactions = new ArrayList<TxInfo>();
		boolean dirty;

		// Create details from statement object
		public StatementDetails(Statement stat) {
			this.domid = stat.domid;
			this.acctid = stat.acctid;

			// Mark this dirty so we will save it to file later
			this.dirty = true;

			captureDetails(stat);
		}

		// Load details object from file
		public StatementDetails(QifDom dom, String s) {
			parseStatementDetails(dom, s);

			// Since it came from file, we needn't save it later
			this.dirty = false;
		}

		private void captureDetails(Statement stat) {
			this.date = stat.date;
			this.open = stat.openingBalance;
			this.close = stat.balance;

			for (final GenericTxn t : stat.transactions) {
				final TxInfo info = new TxInfo(t);

				this.transactions.add(info);
			}
		}

		private void parseStatementDetails(QifDom dom, String s) {
			final StringTokenizer toker = new StringTokenizer(s, ";");

			final String acctname = toker.nextToken().trim();
			final String dateStr = toker.nextToken().trim();
			final String openStr = toker.nextToken().trim();
			final String closeStr = toker.nextToken().trim();
			final String txcountStr = toker.nextToken().trim();

			this.domid = dom.domid;
			this.acctid = dom.findAccount(acctname).acctid;
			this.date = Common.parseDate(dateStr);
			this.open = new BigDecimal(openStr);
			this.close = new BigDecimal(closeStr);

			final int txcount = Integer.parseInt(txcountStr);

			for (int ii = 0; ii < txcount; ++ii) {
				final String tdateStr = toker.nextToken().trim();
				final String cknumStr = toker.nextToken().trim();
				final String amtStr = toker.nextToken().trim();

				final TxInfo txinfo = new TxInfo();

				txinfo.date = Common.parseDate(tdateStr);
				txinfo.cknum = Integer.parseInt(cknumStr);
				txinfo.amount = new BigDecimal(amtStr);

				this.transactions.add(txinfo);
			}
		}

		public String formatForSave(QifDom dom, Account a) {
			String s = String.format("%s;%s;%5.2f;%5.2f;%d", //
					a.name, Common.getDateString(this.date), //
					this.open, this.close, this.transactions.size());

			for (final TxInfo t : this.transactions) {
				s += String.format(";%s;%s;%5.2f", //
						Common.getDateString(t.date), //
						t.cknum, t.amount);
			}

			return s;
		}

		public String toString() {
			final String s = "" + this.domid + ":" + this.acctid + " " //
					+ Common.getDateString(this.date) //
					+ String.format("%10.2f  %10.2f %d tx", //
							this.open, this.close, this.transactions.size());

			return s;
		}
	}

	public void print(String msg) {
		final QifDom dom = QifDom.getDomById(this.domid);
		final Account a = dom.getAccount(this.acctid);

		System.out.println();
		System.out.println("-------------------------------------------------------");
		System.out.println(msg);
		System.out.println("  " + Common.getDateString(this.date) //
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

	private void clearAllTransactions() {
		clearTransactions(this.unclearedTransactions);
	}

	private void unclearAllTransactions() {
		unclearTransactions(this.transactions);
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

		this.unclearedTransactions.addAll(txns);
		this.transactions.removeAll(txns);
	}

	private BigDecimal checkBalance() {
		final BigDecimal txsum = Common.sumAmounts(this.transactions);
		final BigDecimal newbal = this.openingBalance.add(txsum);

		return newbal.subtract(this.balance);
	}

	private void arrangeTransactionsForDisplay(List<GenericTxn> txns) {
		Collections.sort(txns, (o1, o2) -> {
			int diff = o1.getCheckNumber() - o2.getCheckNumber();
			if (diff != 0) {
				return diff;
			}

			diff = o1.getDate().compareTo(o1.getDate());
			if (diff != 0) {
				return diff;
			}

			return o1.txid - o2.txid;
		});
	}

	public String toString() {
		final String s = Common.getDateString(this.date) //
				+ "  " + this.balance //
				+ " tran=" + ((this.transactions != null) ? this.transactions.size() : null);

		return s;
	}
};
