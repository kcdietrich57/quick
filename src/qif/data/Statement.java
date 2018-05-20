package qif.data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import app.QifLoader;
import qif.ui.ReviewDialog;

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
	public final List<GenericTxn> transactions;

	// Transactions that as of the closing date are not cleared
	public final List<GenericTxn> unclearedTransactions;

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

		this.transactions = new ArrayList<GenericTxn>();
		this.unclearedTransactions = new ArrayList<GenericTxn>();
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
						Common.reportError( //
								"Statements month wrapped to next year:\n" //
										+ qline.value);
					}

					String balStr = ss[ssx++];
					if (balStr.equals("x")) {
						balStr = "0.00";
					}

					final BigDecimal bal = new BigDecimal(balStr);
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
				currstmt.cashBalance = Common.parseDecimal(qline.value);
				break;

			case StmtsSecurity: {
				final String[] ss = qline.value.split(";");
				int ssx = 0;

				// S<SYM>;[<order>;]QTY;VALUE;PRICE
				final String secStr = ss[ssx++];

				String ordStr = ss[ssx];
				if ("qpv".indexOf(ordStr.charAt(0)) < 0) {
					ordStr = "qvp";
				} else {
					++ssx;
				}

				final int qidx = ordStr.indexOf('q');
				final int vidx = ordStr.indexOf('v');
				final int pidx = ordStr.indexOf('p');

				final String qtyStr = ((qidx >= 0) && (qidx + ssx < ss.length)) ? ss[qidx + ssx] : "x";
				final String valStr = ((vidx >= 0) && (vidx + ssx < ss.length)) ? ss[vidx + ssx] : "x";
				final String priceStr = ((pidx >= 0) && (pidx + ssx < ss.length)) ? ss[pidx + ssx] : "x";

				final Security sec = dom.findSecurity(secStr);
				if (sec == null) {
					Common.reportError("Unknown security: " + secStr);
				}

				final SecurityPortfolio h = currstmt.holdings;
				final SecurityPosition p = new SecurityPosition(sec);

				p.value = (valStr.equals("x")) ? null : new BigDecimal(valStr);
				p.shares = (qtyStr.equals("x")) ? null : new BigDecimal(qtyStr);
				BigDecimal price = (priceStr.equals("x")) ? null : new BigDecimal(priceStr);
				final BigDecimal price4date = sec.getPriceForDate(currstmt.date).price;

				// We care primarily about the number of shares. If that is not
				// present, the other two must be set for us to calculate the
				// number of shares. If the price is not present, we can use the
				// price on the day of the statement.
				// If we know two of the values, we can calculate the third.
				if (p.shares == null) {
					if (p.value != null) {
						if (price == null) {
							price = price4date;
						}

						p.shares = p.value.divide(price, RoundingMode.HALF_UP);
					}
				} else if (p.value == null) {
					if (p.shares != null) {
						if (price == null) {
							price = price4date;
						}

						p.value = price.multiply(p.shares);
					}
				} else if (price == null) {
					price = price4date;
				}

				if (p.shares == null) {
					Common.reportError("Missing security info in stmt");
				}

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

	public BigDecimal getCredits() {
		BigDecimal cr = new BigDecimal(0);

		for (GenericTxn s : this.transactions) {
			BigDecimal amt = s.getCashAmount();

			if (amt.compareTo(new BigDecimal(0)) >= 0) {
				cr = cr.add(amt);
			}
		}

		return cr;
	}

	public BigDecimal getDebits() {
		BigDecimal db = new BigDecimal(0);

		for (GenericTxn s : this.transactions) {
			BigDecimal amt = s.getCashAmount();

			if (amt.compareTo(new BigDecimal(0)) < 0) {
				db = db.add(amt.negate());
			}
		}

		return db;
	}

	public void addTransaction(GenericTxn txn) {
		this.transactions.add(txn);
	}

	public void clearTransactions( //
			List<GenericTxn> txns, List<GenericTxn> unclearedTxns) {
		for (final GenericTxn t : txns) {
			t.stmtdate = this.date;
		}
		for (final GenericTxn t : unclearedTxns) {
			t.stmtdate = null;
		}

		this.transactions.clear();
		this.unclearedTransactions.clear();

		this.transactions.addAll(txns);
		this.unclearedTransactions.addAll(unclearedTxns);
	}

	// name;date;stmtBal;cashBal;numTx;numPos;[cashTx;][sec;numTx[txIdx;shareBal;]]
	public String formatForSave() {
		final Account a = QifDom.getDomById(this.domid).getAccount(this.acctid);

		String s = String.format("%s;%s;%5.2f;%5.2f;%d;%d", //
				a.getName(), //
				Common.formatDate(this.date), //
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

	// public static void parseStatementDetails(StatementDetails d, QifDom dom,
	// String s, int version) {
	// final String[] ss = s.split(";");
	// int ssx = 0;
	//
	// final String acctname = ss[ssx++].trim();
	// final String dateStr = ss[ssx++].trim();
	// final String closeStr = ss[ssx++].trim();
	// final String closeCashStr = ss[ssx++].trim();
	// final String txCountStr = ss[ssx++].trim();
	// final String secCountStr = (version > 1) ? ss[ssx++].trim() : "0";
	//
	// d.domid = dom.domid;
	// d.acctid = dom.findAccount(acctname).acctid;
	// d.date = Common.parseDate(dateStr);
	// if (version < 3) {
	// d.closingBalance = d.closingCashBalance = new BigDecimal(closeCashStr);
	// } else {
	// d.closingBalance = new BigDecimal(closeStr);
	// d.closingCashBalance = new BigDecimal(closeCashStr);
	// }
	//
	// final int txcount = Integer.parseInt(txCountStr);
	// final int seccount = Integer.parseInt(secCountStr);
	//
	// for (int ii = 0; ii < txcount; ++ii) {
	// final TxInfo txinfo = new TxInfo();
	//
	// final String txtypeStr = ss[ssx++].trim();
	// final boolean isInvestmentTx = txtypeStr.equals("I");
	//
	// String tdateStr;
	// String actStr = "";
	// String secStr = "";
	// String shrStr = "";
	// String cknumStr = "0";
	// if (isInvestmentTx) {
	// // I;12/27/1999;BUY;ETMMTD;7024.50;-7024.50;
	// tdateStr = ss[ssx++].trim();
	// actStr = ss[ssx++].trim();
	// secStr = ss[ssx++].trim();
	// shrStr = ss[ssx++].trim();
	// } else {
	// tdateStr = txtypeStr;
	// cknumStr = ss[ssx++].trim();
	// }
	// final String amtStr = ss[ssx++].trim();
	//
	// try {
	// txinfo.date = Common.parseDate(tdateStr);
	// txinfo.action = actStr;
	// txinfo.cknum = Integer.parseInt(cknumStr);
	// txinfo.cashAmount = new BigDecimal(amtStr);
	// if (secStr.length() > 0) {
	// txinfo.security = dom.findSecurity(secStr);
	// txinfo.shares = new BigDecimal(shrStr);
	// }
	// } catch (final Exception e) {
	// e.printStackTrace();
	// }
	// d.transactions.add(txinfo);
	// }
	//
	// // sec;numtx[;txidx;bal]
	// for (int ii = 0; ii < seccount; ++ii) {
	// final String symStr = ss[ssx++].trim();
	// final String numtxStr = ss[ssx++].trim();
	//
	// final Security sec = dom.findSecurity(symStr);
	//
	// final StatementPosition spos = new StatementPosition();
	// spos.sec = sec;
	// d.holdings.positions.add(spos);
	//
	// final int numtx = Integer.parseInt(numtxStr);
	//
	// for (int jj = 0; jj < numtx; ++jj) {
	// final String txidxStr = ss[ssx++].trim();
	// final String shrbalStr = ss[ssx++].trim();
	//
	// final StatementPositionTx tx = new StatementPositionTx();
	// tx.txidx = Integer.parseInt(txidxStr);
	// tx.shrbal = new BigDecimal(shrbalStr);
	//
	// spos.transactions.add(tx);
	// }
	// }
	// }

	public boolean reconcile(Account a, String msg) {
		boolean needsReview = false;

		if (!this.isBalanced) {
			// Didn't load stmt info, try automatic reconciliation
			final List<GenericTxn> txns = a.gatherTransactionsForStatement(this);
			final List<GenericTxn> uncleared = new ArrayList<GenericTxn>();

			if (!cashMatches(txns)) {
				this.isBalanced = false;

				// Common.findSubsetTotaling(txns, uncleared,
				// getCashDifference(txns));

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

		this.transactions.clear();
		this.unclearedTransactions.clear();

		final List<GenericTxn> txns = a.gatherTransactionsForStatement(this);
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

		this.unclearedTransactions.addAll(txns);

		if (!badinfo.isEmpty()) {
			// d.transactions.removeAll(badinfo);
			Common.reportWarning( //
					"Can't find " + badinfo.size() + " reconciled transactions" //
							+ " for acct " + a.getName() + ":\n" //
							+ badinfo.toString() + "\n" + toString());
			return;
		}

		for (final GenericTxn t : this.transactions) {
			t.stmtdate = this.date;
		}

		this.isBalanced = true;
	}

	private void review(String msg, boolean reconcileNeeded) {
		boolean done = false;
		boolean abort = false;
		boolean sort = true;

		while (!done && !abort) {
			ReviewDialog.review(this);

			if (sort) {
				arrangeTransactionsForDisplay(this.transactions);
				arrangeTransactionsForDisplay(this.unclearedTransactions);
			}

			displayReviewStatus(msg, 1);

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
			case 'd':
				ReviewDialog.review(this);
				break;

			case 'a':
				if (s.startsWith("auto")) {
					final List<GenericTxn> subset = new ArrayList<GenericTxn>();
					Common.findSubsetTotaling(this.unclearedTransactions, subset, getCashDifference());
					if (!subset.isEmpty()) {
						System.out.println("success");
					}

				} else {
					unclearAllTransactions();
					abort = true;
				}
				break;

			case 'q':
				if (cashMatches() //
				// TODO && holdingsMatch()
				) {
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
					Common.formatDate(this.date), this.cknum, this.cashAmount);
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

				String txtypeStr = "";
				try {
					txtypeStr = ss[ssx++].trim();
				} catch (Exception e) {
					System.out.println("*** ERROR: parsing statement details");
				}
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
					// TODO this makes no sense? It is always "T"
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
					txinfo.shares = (shrStr.length() > 0) ? Common.parseDecimal(shrStr) : BigDecimal.ZERO;
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
					+ Common.formatDate(this.date) //
					+ String.format("%10.2f  %10.2f %d tx", //
							this.closingBalance, //
							this.closingCashBalance, //
							this.transactions.size());

			return s;
		}
	}

	private void displayReviewStatus(String msg, int columns) {
		System.out.println();
		System.out.println("-------------------------------------------------------");
		System.out.println(msg);
		System.out.println("-------------------------------------------------------");
		System.out.println(toString());
		displayHoldingsComparison();
		System.out.println("-------------------------------------------------------");

		int rows = (this.transactions.size() + columns - 1) / columns;

		final int maxlength = 80 / columns;

		BigDecimal cashtot = BigDecimal.ZERO;
		for (int ii = 0; ii < rows; ++ii) {
			for (int jj = 0; jj < columns; ++jj) {
				final int idx = jj * rows + ii;
				if (idx >= this.transactions.size()) {
					break;
				}

				final GenericTxn t = this.transactions.get(idx);

				String s = (columns > 1) ? t.toStringShort(true) : t.toStringShort(false);
				s = String.format("(%4.2f) ", t.getCashAmount()) + s;
				cashtot = cashtot.add(t.getCashAmount());
				if (s.length() > maxlength) {
					s = s.substring(0, maxlength);
				}

				if (jj > 0) {
					System.out.print("   ");
				}

				while ((columns > 1) && (s.length() < maxlength)) {
					s += " ";
				}

				System.out.print(String.format("%3d %-20s", idx + 1, s));
			}

			System.out.println();
		}

		System.out.println(String.format("(%4.2f) Total cash amount", cashtot));
		System.out.println();
		System.out.println("Uncleared transactions:");

		rows = (this.unclearedTransactions.size() + columns - 1) / columns;

		for (int ii = 0; ii < rows; ++ii) {
			for (int jj = 0; jj < columns; ++jj) {
				final int idx = jj * rows + ii;
				if (idx >= this.unclearedTransactions.size()) {
					break;
				}

				final GenericTxn t = this.unclearedTransactions.get(idx);

				String s = t.toStringShort(true);
				if (s.length() > maxlength) {
					s = s.substring(0, maxlength);
				}

				if (jj > 0) {
					System.out.print("   ");
				}

				System.out.print(String.format("%3d %-20s", idx + 1, s));
			}

			System.out.println();
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
			final SecurityPosition posExpected = this.holdings.getPosition(p.security);
			final BigDecimal expectedShares = //
					((posExpected != null) && (posExpected.shares != null)) //
							? posExpected.shares //
							: BigDecimal.ZERO;
			final BigDecimal expectedValue = //
					((posExpected != null) && (posExpected.value != null)) //
							? posExpected.value //
							: BigDecimal.ZERO;
			final BigDecimal pValue = (p.value != null) ? p.value : BigDecimal.ZERO;

			s = String.format("  %-25s %10.2f(%5.2f) %10.2f(%5.2f)", //
					p.security.getName(), //
					p.shares, expectedShares.subtract(p.shares), //
					pValue, expectedValue.subtract(pValue));

			if (!Common.isEffectivelyEqual(p.shares, expectedShares) //
			// TODO || !Common.isEffectivelyEqual(pValue, opValue)
			) {
				s += " ********";
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
	 * Calculate the resulting cash position from the previous balance and a list of
	 * transactions
	 *
	 * @param txns
	 *            The transactions
	 * @return The new cash balance
	 */
	public BigDecimal getCashDelta(List<GenericTxn> txns) {
		return getOpeningCashBalance().add(Common.sumCashAmounts(txns));
	}

	/**
	 * Calculate the resulting cash position from the previous balance and cleared
	 * transactions
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

			return (op != null) //
					? Common.isEffectivelyEqual(p.shares, op.shares) //
					: Common.isEffectivelyZero(p.shares);
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
		final String s = Common.formatDate(this.date) //
				+ "  " + this.closingBalance //
				+ " tran=" + ((this.transactions != null) ? this.transactions.size() : null);

		return s;
	}
};
