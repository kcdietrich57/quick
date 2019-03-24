package qif.persistence;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import qif.data.Account;
import qif.data.Common;
import qif.data.GenericTxn;
import qif.data.QifDom;
import qif.data.Statement;
import qif.importer.StatementDetails;
import qif.importer.StatementTxInfo;

public class Reconciler {

	// Read statement log file, filling in statement details.
	public static void processStatementLog() {
		if (!Statement.stmtLogFile.isFile()) {
			return;
		}

		LineNumberReader stmtLogReader = null;
		List<StatementDetails> details = new ArrayList<StatementDetails>();

		try {
			stmtLogReader = new LineNumberReader(new FileReader(Statement.stmtLogFile));

			String s = stmtLogReader.readLine();
			if (s == null) {
				return;
			}

			QifDom.loadedStatementsVersion = Integer.parseInt(s.trim());

			s = stmtLogReader.readLine();
			while (s != null) {
				StatementDetails d = //
						new StatementDetails(s, QifDom.loadedStatementsVersion);

				details.add(d);

				s = stmtLogReader.readLine();
			}

			processStatementDetails(details);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (stmtLogReader != null) {
				try {
					stmtLogReader.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/** Update statements with their reconciliation information */
	public static void processStatementDetails(List<StatementDetails> details) {
		for (StatementDetails d : details) {
			Account a = Account.getAccountByID(d.acctid);

			Statement s = a.getStatement(d.date, d.closingBalance);
			if (s == null) {
				Common.reportError("Can't find statement for details: " //
						+ a.name //
						+ "  " + d.date.toString() //
						+ "  " + d.closingBalance);
			}

			getTransactionsFromDetails(a, s, d);

			if (!s.isBalanced) {
				getTransactionsFromDetails(a, s, d);
				Common.reportError("Can't reconcile statement from log.\n" //
						+ " a=" + a.name //
						+ " s=" + s.toString());
			}
		}
	}

	/** Match up loaded transactions with statement details from log. */
	private static void getTransactionsFromDetails(Account a, Statement s, StatementDetails d) {
		if (s.isBalanced) {
			return;
		}

		s.transactions.clear();
		s.unclearedTransactions.clear();

		List<GenericTxn> txns = a.gatherTransactionsForStatement(s);
		List<StatementTxInfo> badinfo = new ArrayList<StatementTxInfo>();

		for (StatementTxInfo info : d.transactions) {
			boolean found = false;

			for (int ii = 0; ii < txns.size(); ++ii) {
				GenericTxn t = txns.get(ii);

				if (info.date.compareTo(t.getDate()) == 0) {
					if ((info.cknum == t.getCheckNumber()) //
							&& (info.cashAmount.compareTo(t.getCashAmount()) == 0)) {
						if (t.stmtdate != null) {
							Common.reportError("Reconciling transaction twice:\n" //
									+ t.toString());
						}

						s.transactions.add(t);
						txns.remove(ii);
						found = true;

						break;
					}
				}
			}

			if (!found) {
				badinfo.add(info);
			}
		}

		s.unclearedTransactions.addAll(txns);

		if (!badinfo.isEmpty()) {
			// d.transactions.removeAll(badinfo);
			Common.reportWarning( //
					"Can't find " + badinfo.size() + " reconciled transactions" //
							+ " for acct " + a.name + ":\n" //
							+ badinfo.toString() + "\n"); // + toString());
			return;
		}

		for (GenericTxn t : s.transactions) {
			t.stmtdate = s.date;
		}

		s.isBalanced = true;
	}

	public static void saveReconciledStatement(Statement stat) {
		PrintWriter pw = null;
		try {
			pw = openStatementsLogFile();

			if (stat.dirty) {
				String logStr = StatementDetails.formatStatementForSave(stat);
				pw.println(logStr);
				pw.flush();

				stat.dirty = false;
			}
		} catch (Exception e) {

		} finally {
			if (pw != null) {
				pw.close();
			}
		}
	}

	// Recreate log file when we have changed the format from the previous
	// version
	// Save the previous file as <name>.N
	public static void rewriteStatementLogFile() {
		String basename = Statement.stmtLogFile.getName();
		File tmpLogFile = new File(QifDom.qifDir, basename + ".tmp");

		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new FileWriter(tmpLogFile));
		} catch (IOException e) {
			Common.reportError("Can't open tmp stmt log file: " //
					+ Statement.stmtLogFile.getAbsolutePath());
			return;
		}

		pw.println("" + StatementDetails.CURRENT_VERSION);
		for (Account a : Account.getAccounts()) {
			for (Statement s : a.statements) {
				pw.println(StatementDetails.formatStatementForSave(s));
			}
		}

		try {
			if (pw != null) {
				pw.close();
			}
		} catch (Exception e) {
		}

		File logFileBackup = null;

		for (int ii = 1;; ++ii) {
			logFileBackup = new File(QifDom.qifDir, basename + "." + ii);
			if (!logFileBackup.exists()) {
				break;
			}
		}

		Statement.stmtLogFile.renameTo(logFileBackup);
		if (logFileBackup.exists() && tmpLogFile.exists() //
				&& !Statement.stmtLogFile.exists()) {
			tmpLogFile.renameTo(Statement.stmtLogFile);
		}

		assert (logFileBackup.exists() && !Statement.stmtLogFile.exists());

		QifDom.loadedStatementsVersion = StatementDetails.CURRENT_VERSION;
	}

	private static PrintWriter openStatementsLogFile() {
		try {
			return new PrintWriter(new FileWriter(Statement.stmtLogFile, true));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
}