package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import app.QifLoader;

public class Statement {
	public int domid;
	public int acctid;
	public Date date;

	public Statement prevStatement;

	public boolean isBalanced;

	// open/close balance are total balance, including cash and securities
	public BigDecimal closingBalance;

	// Separate closing balance for cash and securities
	public BigDecimal cashBalance;
	public SecurityPortfolio holdings;

	// Transactions included in this statement
	public List<GenericTxn> transactions;

	// Transactions that as of the closing date are not cleared
	public List<GenericTxn> unclearedTransactions;

	/** Whether this statement has been saved to the reconcile log file */
	boolean dirty = false;

	public Statement(int domid, int acctid) {
		this.isBalanced = false;
		this.domid = domid;
		this.acctid = acctid;
		this.date = null;
		this.prevStatement = null;
		this.closingBalance = null;
		this.cashBalance = null;
		this.holdings = new SecurityPortfolio();
	}

	// Create a copy of a statement - used when creating a new Dom from an
	// existing one.
	public Statement(QifDom dom, Statement other) {
		this(dom.domid, other.acctid);

		this.date = other.date;
		this.prevStatement = dom.findStatement(other.prevStatement);
		this.closingBalance = other.closingBalance;
		this.cashBalance = other.cashBalance;

		// TODO copy holdings
		// TODO copy details?
	}

	public static List<Statement> loadStatements(QFileReader qfr, QifDom dom) {
		final QFileReader.QLine qline = new QFileReader.QLine();
		final List<Statement> stmts = new ArrayList<Statement>();

		Statement currstmt = null;

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
				currstmt = null;
				break;
			}

			case StmtsMonthly: {
				final String[] ss = qline.value.split(" ");
				int ssx = 0;

				final String datestr = ss[ssx++];
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

				while (ssx < ss.length) {
					if (month > 12) {
						Common.reportError("Statements month wrapped to next year");
					}

					final BigDecimal bal = new BigDecimal(ss[ssx++]);
					final Date d = (day == 0) //
							? Common.getDateForEndOfMonth(year, month) //
							: Common.getDate(year, month, day);

					final Statement prevstmt = (stmts.isEmpty() ? null : stmts.get(stmts.size() - 1));

					currstmt = new Statement(dom.domid, dom.currAccount.acctid);
					currstmt.date = d;
					currstmt.closingBalance = currstmt.cashBalance = bal;
					if ((prevstmt != null) && (prevstmt.acctid == currstmt.acctid)) {
						currstmt.prevStatement = prevstmt;
					}

					stmts.add(currstmt);

					++month;
				}

				break;
			}

			case StmtsCash:
				currstmt.cashBalance = new BigDecimal(qline.value);
				break;

			case StmtsSecurity: {
				final String[] ss = qline.value.split(";");
				int ssx = 0;

				final String secStr = ss[ssx++];
				final String qtyStr = ss[ssx++];
				final String valStr = ss[ssx++];

				final Security sec = dom.findSecurity(secStr);
				if (sec == null) {
					Common.reportError("Unknown security: " + secStr);
				}

				final SecurityPortfolio h = currstmt.holdings;
				final SecurityPosition p = new SecurityPosition(sec);
				p.shares = new BigDecimal(qtyStr);
				p.value = new BigDecimal(valStr);

				h.positions.add(p);
				break;
			}

			default:
				Common.reportError("syntax error");
			}
		}
	}

	public BigDecimal getOpeningBalance() {
		final BigDecimal openingBalance = (this.prevStatement != null) //
				? this.prevStatement.closingBalance //
				: BigDecimal.ZERO;
		return openingBalance;
	}

	public BigDecimal getOpeningCashBalance() {
		final BigDecimal openingBalance = (this.prevStatement != null) //
				? this.prevStatement.cashBalance //
				: BigDecimal.ZERO;
		return openingBalance;
	}

	public void addTransaction(GenericTxn txn) {
		this.transactions.add(txn);
	}

	public void clearTransactions( //
			List<GenericTxn> txns, List<GenericTxn> unclearedTxns) {
		for (final GenericTxn t : txns) {
			t.stmtdate = this.date;
		}

		this.transactions = txns;
		this.unclearedTransactions = unclearedTxns;
	}

	// name;date;stmtBal;cashBal;numTx;numPos;[cashTx;][sec;numTx[txIdx;shareBal;]]
	public String formatForSave() {
		final Account a = QifDom.getDomById(this.domid).getAccount(this.acctid);

		String s = String.format("%s;%s;%5.2f;%5.2f;%d;%d", //
				a.name, //
				Common.getDateString(this.date), //
				this.closingBalance, //
				this.cashBalance, //
				this.transactions.size(), //
				this.holdings.positions.size());

		for (final GenericTxn t : this.transactions) {
			s += ";" + t.formatForSave();
		}

		for (final SecurityPosition p : this.holdings.positions) {
			s += ";" + p.formatForSave(this);
		}

		return s;
	}

	public static void parseStatementDetails(StatementDetails d, QifDom dom, String s, int version) {
		final String[] ss = s.split(";");
		int ssx = 0;

		final String acctname = ss[ssx++].trim();
		final String dateStr = ss[ssx++].trim();
		final String closeStr = ss[ssx++].trim();
		final String closeCashStr = ss[ssx++].trim();
		final String txCountStr = ss[ssx++].trim();
		final String secCountStr = (version > 1) ? ss[ssx++].trim() : "0";

		d.domid = dom.domid;
		d.acctid = dom.findAccount(acctname).acctid;
		d.date = Common.parseDate(dateStr);
		if (version < 3) {
			d.closingBalance = d.closingCashBalance = new BigDecimal(closeCashStr);
		} else {
			d.closingBalance = new BigDecimal(closeStr);
			d.closingCashBalance = new BigDecimal(closeCashStr);
		}

		final int txcount = Integer.parseInt(txCountStr);
		final int seccount = Integer.parseInt(secCountStr);

		for (int ii = 0; ii < txcount; ++ii) {
			final TxInfo txinfo = new TxInfo();

			final String txtypeStr = ss[ssx++].trim();
			final boolean isInvestmentTx = txtypeStr.equals("I");

			String tdateStr;
			String actStr = "";
			String secStr = "";
			String shrStr = "";
			String cknumStr = "0";
			if (isInvestmentTx) {
				// I;12/27/1999;BUY;ETMMTD;7024.50;-7024.50;
				tdateStr = ss[ssx++].trim();
				actStr = ss[ssx++].trim();
				secStr = ss[ssx++].trim();
				shrStr = ss[ssx++].trim();
			} else {
				tdateStr = txtypeStr;
				cknumStr = ss[ssx++].trim();
			}
			final String amtStr = ss[ssx++].trim();

			try {
				txinfo.date = Common.parseDate(tdateStr);
				txinfo.action = actStr;
				txinfo.cknum = Integer.parseInt(cknumStr);
				txinfo.cashAmount = new BigDecimal(amtStr);
				if (secStr.length() > 0) {
					txinfo.security = dom.findSecurity(secStr);
					txinfo.shares = new BigDecimal(shrStr);
				}
			} catch (final Exception e) {
				e.printStackTrace();
			}
			d.transactions.add(txinfo);
		}

		// sec;numtx[;txidx;bal]
		for (int ii = 0; ii < seccount; ++ii) {
			final String symStr = ss[ssx++].trim();
			final String numtxStr = ss[ssx++].trim();

			final Security sec = dom.findSecurity(symStr);

			final StatementPosition spos = new StatementPosition();
			spos.sec = sec;
			d.holdings.positions.add(spos);

			final int numtx = Integer.parseInt(numtxStr);

			for (int jj = 0; jj < numtx; ++jj) {
				final String txidxStr = ss[ssx++].trim();
				final String shrbalStr = ss[ssx++].trim();

				final StatementPositionTx tx = new StatementPositionTx();
				tx.txidx = Integer.parseInt(txidxStr);
				tx.shrbal = new BigDecimal(shrbalStr);

				spos.transactions.add(tx);
			}
		}
	}

	public boolean reconcile(Account a, String msg) {
		boolean needsReview = false;

		if (!this.isBalanced) {
			// Didn't load stmt info, try automatic reconciliation
			final List<GenericTxn> txns = a.gatherTransactionsForStatement(this);
			final List<GenericTxn> uncleared = new ArrayList<GenericTxn>();

			if (!cashMatches(txns)) {
				this.isBalanced = false;

//				Common.findSubsetTotaling(txns, uncleared, getCashDifference(txns));

				if (uncleared.isEmpty()) {
					Common.reportWarning("Can't automatically balance account: " + this);
				} else {
					txns.removeAll(uncleared);
				}
			}

			clearTransactions(txns, uncleared);

			this.isBalanced &= holdingsMatch();
			this.dirty = true;
			needsReview = true;
		}

		if (!this.isBalanced || needsReview) {
			review(msg, true);

			if (this.isBalanced) {
				this.holdings.captureTransactions(this);
				this.dirty = true;
			}
		}

		if (this.dirty && this.isBalanced) {
			this.holdings.purgeEmptyPositions();
		}

		return this.isBalanced;
	}

	/**
	 * Match up loaded transactions with statement details from log.
	 *
	 * @param a
	 *            Account
	 * @return True if all transactions are found
	 */
	public void getTransactionsFromDetails(Account a, StatementDetails d) {
		if (this.isBalanced) {
			return;
		}

		final List<GenericTxn> txns = a.gatherTransactionsForStatement(this);
		this.transactions = new ArrayList<GenericTxn>();
		final List<TxInfo> badinfo = new ArrayList<TxInfo>();

		for (final TxInfo info : d.transactions) {
			boolean found = false;

			for (int ii = 0; ii < txns.size(); ++ii) {
				final GenericTxn t = txns.get(ii);

				if (info.date.compareTo(t.getDate()) == 0) {
					if ((info.cknum == t.getCheckNumber()) //
							&& (info.cashAmount.compareTo(t.getCashAmount()) == 0)) {
						if (t.stmtdate != null) {
							Common.reportError("Reconciling transaction twice:\n" //
									+ t.toString());
						}

						this.transactions.add(t);
						txns.remove(ii);
						found = true;

						break;
					}
				}

				// TODO txinfos are not sorted by date - should they be?
				// final long infoms = info.date.getTime();
				// final long tranms = t.getDate().getTime();
				//
				// if (infoms < tranms) {
				// break;
				// }
			}

			if (!found) {
				badinfo.add(info);
			}
		}

		this.unclearedTransactions = txns;

		if (!badinfo.isEmpty()) {
			// d.transactions.removeAll(badinfo);
			Common.reportWarning( //
					"Can't find " + badinfo.size() + " reconciled transactions:\n" //
							+ badinfo.toString());
			return;
		}

		for (final GenericTxn t : this.transactions) {
			t.stmtdate = this.date;
		}

		this.isBalanced = true;
	}

	private void review(String msg) {
		review(msg, false);
	}

	private void review(String msg, boolean reconcileNeeded) {
		boolean done = false;
		boolean abort = false;
		boolean sort = true;

		while (!done && !abort) {
			if (sort) {
				arrangeTransactionsForDisplay(this.transactions);
				arrangeTransactionsForDisplay(this.unclearedTransactions);
			}

			displayReviewStatus(msg);

			if (!reconcileNeeded) {
				return;
			}

			System.out.print("CMD> ");

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
				if (cashMatches() && holdingsMatch()) {
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
					break;
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
				final String[] ss = s.substring(1).trim().split(" ");
				int ssx = 0;
				String token = "";

				while (ssx < ss.length) {
					try {
						final int[] range = new int[2];

						token = ss[ssx++].trim();
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

		this.isBalanced = done && !abort;
	}

	private static void parseRange(String s, int[] range) {
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

	// This represents the information stored in the statementLog file for each
	// transaction that is part of a statement.
	static class TxInfo {
		Date date;
		String action;
		int cknum;
		BigDecimal cashAmount;
		Security security;
		BigDecimal shares;

		public static TxInfo factory(GenericTxn tx) {
			if (tx instanceof NonInvestmentTxn) {
				return new TxInfo((NonInvestmentTxn) tx);
			}

			if (tx instanceof InvestmentTxn) {
				return new TxInfo((InvestmentTxn) tx);
			}

			return null;
		}

		public TxInfo() {
			this.cknum = 0;
			this.action = null;
			this.security = null;
			this.shares = null;
		}

		private TxInfo(GenericTxn tx) {
			this();

			this.cashAmount = tx.getCashAmount();
		}

		public TxInfo(NonInvestmentTxn tx) {
			this((GenericTxn) tx);

			this.cknum = tx.getCheckNumber();
		}

		public TxInfo(InvestmentTxn tx) {
			this((GenericTxn) tx);

			this.action = tx.getAction().toString();

			if (tx.security != null) {
				this.security = tx.security;
				this.shares = tx.getShares();
			}
		}

		public String toString() {
			return String.format("%s %5d %10.2f", //
					Common.getDateString(this.date), this.cknum, this.cashAmount);
		}
	}

	// [name;numtx[;txidx;shrbal]]
	static class StatementPositionTx {
		int txidx;
		BigDecimal shrbal;
	}

	static class StatementPosition {
		Security sec;
		List<StatementPositionTx> transactions = new ArrayList<StatementPositionTx>();
	}

	static class StatementHoldings {
		List<StatementPosition> positions = new ArrayList<StatementPosition>();
	}

	// StatementDetails represents a reconciled statement as stored in the
	// statements log file.
	static class StatementDetails {
		public static final int CURRENT_VERSION = 4;

		int domid;
		int acctid;
		Date date;

		// This is the cumulative account value
		BigDecimal closingBalance;

		// This is the net cash change
		BigDecimal closingCashBalance;

		// This is the change to security positions (and closing price)
		StatementHoldings holdings;

		List<TxInfo> transactions;
		List<TxInfo> unclearedTransactions;

		private StatementDetails() {
			this.closingCashBalance = BigDecimal.ZERO;
			this.transactions = new ArrayList<TxInfo>();
			this.unclearedTransactions = new ArrayList<TxInfo>();

			this.holdings = new StatementHoldings();
		}

		// Load details object from file
		public StatementDetails(QifDom dom, String s, int version) {
			this();

			parseStatementDetails(dom, s, version);
		}

		private void parseStatementDetails(QifDom dom, String s, int version) {
			final String[] ss = s.split(";");
			int ssx = 0;

			final String acctname = ss[ssx++].trim();
			final String dateStr = ss[ssx++].trim();
			final String closeStr = ss[ssx++].trim();
			final String closeCashStr = ss[ssx++].trim();
			final String txCountStr = ss[ssx++].trim();
			final String secCountStr = (version > 1) ? ss[ssx++].trim() : "0";

			this.domid = dom.domid;
			this.acctid = dom.findAccount(acctname).acctid;
			this.date = Common.parseDate(dateStr);
			if (version < 3) {
				this.closingBalance = this.closingCashBalance = new BigDecimal(closeCashStr);
			} else {
				this.closingBalance = new BigDecimal(closeStr);
				this.closingCashBalance = new BigDecimal(closeCashStr);
			}

			final int txcount = Integer.parseInt(txCountStr);
			final int seccount = Integer.parseInt(secCountStr);

			for (int ii = 0; ii < txcount; ++ii) {
				final TxInfo txinfo = new TxInfo();

				final String txtypeStr = ss[ssx++].trim();
				final boolean isInvestmentTx = txtypeStr.equals("I");

				String tdateStr;
				String actStr = "";
				String secStr = "";
				String shrStr = "";
				String cknumStr = "0";
				if (isInvestmentTx) {
					// I;12/27/1999;BUY;ETMMTD;7024.50;-7024.50;
					tdateStr = ss[ssx++].trim();
					actStr = ss[ssx++].trim();
					secStr = ss[ssx++].trim();
					shrStr = ss[ssx++].trim();
				} else {
					tdateStr = (txtypeStr.equals("T")) //
							? shrStr = ss[ssx++].trim() //
							: txtypeStr;
					cknumStr = ss[ssx++].trim();
				}
				final String amtStr = ss[ssx++].trim();

				txinfo.date = Common.parseDate(tdateStr);
				txinfo.action = actStr;
				txinfo.cknum = Integer.parseInt(cknumStr);
				txinfo.cashAmount = new BigDecimal(amtStr);
				if (secStr.length() > 0) {
					txinfo.security = dom.findSecurity(secStr);
					txinfo.shares = new BigDecimal(shrStr);
				}

				this.transactions.add(txinfo);
			}

			// sec;numtx[;txidx;bal]
			for (int ii = 0; ii < seccount; ++ii) {
				final String symStr = ss[ssx++].trim();
				final String numtxStr = ss[ssx++].trim();

				final Security sec = dom.findSecurity(symStr);

				final StatementPosition spos = new StatementPosition();
				spos.sec = sec;
				this.holdings.positions.add(spos);

				final int numtx = Integer.parseInt(numtxStr);

				for (int jj = 0; jj < numtx; ++jj) {
					final String txidxStr = ss[ssx++].trim();
					final String shrbalStr = ss[ssx++].trim();

					final StatementPositionTx tx = new StatementPositionTx();
					tx.txidx = Integer.parseInt(txidxStr);
					tx.shrbal = new BigDecimal(shrbalStr);

					spos.transactions.add(tx);
				}
			}
		}

		public String toString() {
			final String s = "" + this.domid + ":" + this.acctid + " " //
					+ Common.getDateString(this.date) //
					+ String.format("%10.2f  %10.2f %d tx", //
							this.closingBalance, //
							this.closingCashBalance, //
							this.transactions.size());

			return s;
		}
	}

	private void displayReviewStatus(String msg) {
		System.out.println();
		System.out.println("-------------------------------------------------------");
		System.out.println(msg);
		System.out.println("-------------------------------------------------------");
		System.out.println(toString());
		displayHoldingsComparison();
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

	private void displayHoldingsComparison() {
		final BigDecimal newCash = getCashDelta();

		String s = String.format("\n  %-25s %10.2f", //
				"Cash", newCash);
		if (!cashMatches()) {
			s += " [" + getCashDifference() + "] *************";
		}
		System.out.println(s);

		final SecurityPortfolio newHoldings = getPortfolioDelta();

		for (final SecurityPosition p : newHoldings.positions) {
			s = String.format("  %-25s %10.2f", //
					p.security.getName(), p.shares);

			final SecurityPosition op = this.holdings.getPosition(p.security);
			final BigDecimal opShares = (op != null) ? op.shares : BigDecimal.ZERO;

			if (!Common.isEffectivelyEqual(p.shares, opShares)) {
				s += " [" + opShares.subtract(p.shares) + "] *************";
			}

			System.out.println(s);
		}

		boolean first = true;
		for (final SecurityPosition p : this.holdings.positions) {
			if (null == this.holdings.findPosition(p.security)) {
				if (first) {
					first = false;
					System.out.println("  Unexpected securities:");
				}

				System.out.println(String.format("  %s %10.2f", p.security.getName(), p.shares));
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

	/**
	 * Check cleared transactions against expected cash balance
	 *
	 * @return True if balance matches
	 */
	private boolean cashMatches() {
		return cashMatches(this.transactions);
	}

	/**
	 * Check cleared transactions against expected cash balance
	 *
	 * @param txns
	 *            The transactions to check
	 * @return True if balance matches
	 */
	private boolean cashMatches(List<GenericTxn> txns) {
		return getCashDifference(txns).signum() == 0;
	}

	/**
	 * Check list of transactions against expected cash balance
	 *
	 * @return Difference from expected value
	 */
	private BigDecimal getCashDifference() {
		return getCashDifference(this.transactions);
	}

	/**
	 * Check list of transactions against expected cash balance
	 *
	 * @param txns
	 *            The transactions to check
	 * @return Difference from expected balance
	 */
	private BigDecimal getCashDifference(List<GenericTxn> txns) {
		final BigDecimal cashTotal = Common.sumCashAmounts(txns);
		final BigDecimal cashExpected = this.cashBalance.subtract(getOpeningCashBalance());
		BigDecimal cashDiff = cashTotal.subtract(cashExpected);

		final BigDecimal newbal = getCashDelta(txns);
		cashDiff = this.cashBalance.subtract(newbal);

		return cashDiff;
	}

	/**
	 * Calculate the resulting cash position from the previous balance and a
	 * list of transactions
	 *
	 * @param txns
	 *            The transactions
	 * @return The new cash balance
	 */
	public BigDecimal getCashDelta(List<GenericTxn> txns) {
		return getOpeningCashBalance().add(Common.sumCashAmounts(txns));
	}

	/**
	 * Calculate the resulting cash position from the previous balance and
	 * cleared transactions
	 *
	 * @return The new cash balance
	 */
	public BigDecimal getCashDelta() {
		return getCashDelta(this.transactions);
	}

	/**
	 * Check cleared transactions against expected security holdings
	 *
	 * @return True if they match
	 */
	private boolean holdingsMatch() {
		if (this.holdings.positions.isEmpty()) {
			return true;
		}

		final SecurityPortfolio delta = getPortfolioDelta();

		for (final SecurityPosition p : this.holdings.positions) {
			final SecurityPosition op = delta.getPosition(p.security);

			return Common.isEffectivelyEqual(p.shares, //
					(op != null) ? op.shares : BigDecimal.ZERO);
		}

		for (final SecurityPosition p : delta.positions) {
			final SecurityPosition op = this.holdings.getPosition(p.security);

			return Common.isEffectivelyEqual(p.shares, //
					(op != null) ? op.shares : BigDecimal.ZERO);
		}

		return true;
	}

	/**
	 * Build a Portfolio position from the previous holdings and a list of
	 * transactions
	 *
	 * @param txns
	 *            The transactions
	 * @return new portfolio holdings
	 */
	private SecurityPortfolio getPortfolioDelta(List<GenericTxn> txns) {
		final SecurityPortfolio clearedPositions = (this.prevStatement != null) //
				? new SecurityPortfolio(this.prevStatement.holdings) //
				: new SecurityPortfolio();

		for (final GenericTxn t : txns) {
			if (!(t instanceof InvestmentTxn)) {
				continue;
			}

			final InvestmentTxn itx = (InvestmentTxn) t;

			clearedPositions.addTransaction(itx);
		}

		return clearedPositions;
	}

	/**
	 * Build a Portfolio position from the previous holdings and the current
	 * transactions
	 *
	 * @return Portfolio with holdings
	 */
	public SecurityPortfolio getPortfolioDelta() {
		return getPortfolioDelta(this.transactions);
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
				+ "  " + this.closingBalance //
				+ " tran=" + ((this.transactions != null) ? this.transactions.size() : null);

		return s;
	}
};
