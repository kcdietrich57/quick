package qif.data;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import app.QifLoader;
import qif.importer.StatementDetails;
import qif.ui.ReviewDialog;

public class Reconciler {

	// Process unreconciled statements for each account, matching statements
	// with transactions and logging the results.
	public static void reconcileStatements() {
		PrintWriter pw = null;

		try {
			pw = new PrintWriter(new FileWriter(Statement.stmtLogFile, true));

			for (Account a : Account.accounts) {
				Reconciler.reconcileStatements(a, pw);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			if (pw != null) {
				pw.close();
			}
		}
	}

	public static void reconcileStatements(Account a, PrintWriter pw) {
		if (a.statements.isEmpty()) {
			return;
		}

		for (int ii = 0; ii < a.statements.size(); ++ii) {
			final Statement s = a.statements.get(ii);

			final String msg = //
					"Reconciling " + a.getName() + " statement " + (ii + 1) //
							+ " of " + a.statements.size();

			if (!reconcileStatement(a, s, msg)) {
				break;
			}

			if (s.dirty) {
				final String logStr = StatementDetails.formatStatementForSave(s);
				pw.println(logStr);
				pw.flush();

				s.dirty = false;
			}

			a.clearedBalance = s.closingBalance;
		}
	}

	private static boolean reconcileStatement(Account a, Statement s, String msg) {
		boolean needsReview = false;

		if (!s.isBalanced) {
			// Didn't load stmt info, try automatic reconciliation
			final List<GenericTxn> txns = a.gatherTransactionsForStatement(s);
			final List<GenericTxn> uncleared = new ArrayList<GenericTxn>();

			if (!s.cashMatches(txns)) {
				s.isBalanced = false;

				// Common.findSubsetTotaling(txns, uncleared,
				// getCashDifference(txns));

				if (uncleared.isEmpty()) {
					Common.reportWarning("Can't automatically balance statement: " + s);
				} else {
					txns.removeAll(uncleared);
				}
			}

			s.clearTransactions(txns, uncleared);

			s.isBalanced &= s.holdingsMatch();
			s.dirty = true;
			needsReview = true;
		}

		if (!s.isBalanced || needsReview) {
			review(s, msg, true);

			if (s.isBalanced) {
				s.holdings.captureTransactions(s);
				s.dirty = true;
			}
		}

		if (s.dirty && s.isBalanced) {
			s.holdings.purgeEmptyPositions();
		}

		return s.isBalanced;
	}

	private static void review(Statement s, String msg, boolean reconcileNeeded) {
		boolean done = false;
		boolean abort = false;

		while (!done && !abort) {
			ReviewDialog.review(s);

			List<GenericTxn> alltx = new ArrayList<GenericTxn>(s.transactions);
			List<GenericTxn> unclearedtx = new ArrayList<GenericTxn>(s.unclearedTransactions);

			arrangeTransactionsForDisplay(alltx);
			arrangeTransactionsForDisplay(unclearedtx);

			displayReviewStatus(s, msg, 1);

			if (!reconcileNeeded) {
				return;
			}

			System.out.print("CMD> ");

			String line = QifLoader.scn.nextLine();

			if ((line == null) || (line.length() == 0)) {
				line = "q";
			}
			line = line.trim();

			switch (line.charAt(0)) {
			case 'd':
				ReviewDialog.review(s);
				break;

			case 'a':
				if (line.startsWith("auto")) {
					final List<GenericTxn> subset = new ArrayList<GenericTxn>();

					Common.findSubsetTotaling(unclearedtx, subset, s.getCashDifference());

					if (!subset.isEmpty()) {
						System.out.println("success");
					}
				} else {
					s.unclearAllTransactions();
					abort = true;
				}
				break;

			case 'q':
				if (s.cashMatches() //
				// FIXME && holdingsMatch()
				) {
					done = true;
				}
				break;

			case 'r':
			case 'u': {
				if (line.startsWith("rall")) {
					s.clearAllTransactions();
					break;
				}
				if (line.startsWith("uall")) {
					s.unclearAllTransactions();
					break;
				}

				final boolean isReconcile = line.charAt(0) == 'r';
				final List<GenericTxn> lst = (isReconcile) //
						? unclearedtx //
						: alltx;
				final List<GenericTxn> txns = new ArrayList<GenericTxn>();
				final String[] ss = line.substring(1).trim().split(" ");
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
					s.clearTransactions(txns);
				} else {
					s.unclearTransactions(txns);
				}
				break;
			}
			}
		}

		s.isBalanced = done && !abort;
	}

	private static void arrangeTransactionsForDisplay(List<GenericTxn> txns) {
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

	private static void displayReviewStatus(Statement s, String msg, int columns) {
		System.out.println();
		System.out.println("-------------------------------------------------------");
		System.out.println(msg);
		System.out.println("-------------------------------------------------------");
		System.out.println(s.toString());
		displayHoldingsComparison(s);
		System.out.println("-------------------------------------------------------");

		int rows = (s.transactions.size() + columns - 1) / columns;

		final int maxlength = 80 / columns;

		BigDecimal cashtot = BigDecimal.ZERO;
		for (int ii = 0; ii < rows; ++ii) {
			for (int jj = 0; jj < columns; ++jj) {
				final int idx = jj * rows + ii;
				if (idx >= s.transactions.size()) {
					break;
				}

				final GenericTxn t = s.transactions.get(idx);

				String line = (columns > 1) ? t.toStringShort(true) : t.toStringShort(false);
				line = String.format("(%4.2f) ", t.getCashAmount()) + line;
				cashtot = cashtot.add(t.getCashAmount());
				if (line.length() > maxlength) {
					line = line.substring(0, maxlength);
				}

				if (jj > 0) {
					System.out.print("   ");
				}

				while ((columns > 1) && (line.length() < maxlength)) {
					line += " ";
				}

				System.out.print(String.format("%3d %-20s", idx + 1, line));
			}

			System.out.println();
		}

		System.out.println(String.format("(%4.2f) Total cash amount", cashtot));
		System.out.println();
		System.out.println("Uncleared transactions:");

		rows = (s.unclearedTransactions.size() + columns - 1) / columns;

		for (int ii = 0; ii < rows; ++ii) {
			for (int jj = 0; jj < columns; ++jj) {
				final int idx = jj * rows + ii;
				if (idx >= s.unclearedTransactions.size()) {
					break;
				}

				final GenericTxn t = s.unclearedTransactions.get(idx);

				String line = t.toStringShort(true);
				if (line.length() > maxlength) {
					line = line.substring(0, maxlength);
				}

				if (jj > 0) {
					System.out.print("   ");
				}

				System.out.print(String.format("%3d %-20s", idx + 1, s));
			}

			System.out.println();
		}
	}

	private static void displayHoldingsComparison(Statement stmt) {
		final BigDecimal newCash = stmt.getCashDelta();

		String s = String.format("\n  %-25s %s", //
				"Cash", Common.formatAmount(newCash));
		if (!stmt.cashMatches()) {
			s += " [" + stmt.getCashDifference() + "] *************";
		}
		System.out.println(s);

		final SecurityPortfolio newHoldings = stmt.getPortfolioDelta();

		for (final SecurityPosition p : newHoldings.positions) {
			final SecurityPosition posExpected = stmt.holdings.getPosition(p.security);
			final BigDecimal expectedShares = //
					((posExpected != null) && (posExpected.shares != null)) //
							? posExpected.shares //
							: BigDecimal.ZERO;
			final BigDecimal expectedValue = //
					((posExpected != null) && (posExpected.value != null)) //
							? posExpected.value //
							: BigDecimal.ZERO;
			final BigDecimal pValue = (p.value != null) ? p.value : BigDecimal.ZERO;

			s = String.format("  %-25s %s(%5.2f) %s(%5.2f)", //
					p.security.getName(), //
					Common.formatAmount(p.shares), //
					expectedShares.subtract(p.shares), //
					Common.formatAmount(pValue), //
					expectedValue.subtract(pValue));

			if (!Common.isEffectivelyEqual(p.shares, expectedShares) //
			// FIXME || !Common.isEffectivelyEqual(pValue, opValue)
			) {
				s += " ********";
			}

			System.out.println(s);
		}

		boolean first = true;
		for (final SecurityPosition p : stmt.holdings.positions) {
			if (null == stmt.holdings.findPosition(p.security)) {
				if (first) {
					first = false;
					System.out.println("  Unexpected securities:");
				}

				System.out.println(String.format("  %s %s", //
						p.security.getName(), Common.formatAmount(p.shares)));
			}
		}
	}

}