package moneymgr.io;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import app.QifDom;
import moneymgr.model.Account;
import moneymgr.model.GenericTxn;
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.SimpleTxn;
import moneymgr.model.Statement;
import moneymgr.util.Common;

/** Process statement info in statementLog.dat */
public class Reconciler {
	public final MoneyMgrModel model;

	public Reconciler(MoneyMgrModel model) {
		this.model = model;
	}

	/** After loading QIF data, read statement log file, filling in details. */
	public void processStatementLog() {
		File logfile = Statement.getStatementLogFileForModel(this.model);

		if (!logfile.isFile()) {
			return;
		}

		LineNumberReader stmtLogReader = null;
		List<StatementDetails> details = new ArrayList<StatementDetails>();

		try {
			stmtLogReader = new LineNumberReader(new FileReader(logfile));

			String s = stmtLogReader.readLine();
			if (s == null) {
				return;
			}

			QifDom.loadedStatementsVersion = Integer.parseInt(s.trim());

			// Ingest statements (each input line)
			s = stmtLogReader.readLine();
			while (s != null) {
				StatementDetails d = new StatementDetails(this.model, s);

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

		// TODO testing code
//		for (Account a : Account.getAccounts()) {
//			if (!a.isInvestmentAccount()) {
//				continue;
//			}
//
//			for (Statement s : a.statements) {
//				if (!s.isBalanced) {
//					continue;
//				}
//
//				Statement ps = s.prevStatement;
//				SecurityPortfolio pport = (ps != null) ? ps.holdings : null;
//
//				for (SecurityPosition pos : s.holdings.positions) {
//					if (pport != null) {
//						SecurityPosition ppos = pport.findPosition(pos.security);
//
//						if (ppos != null //
//								&& !Common.isEffectivelyEqual( //
//										ppos.getExpectedEndingShares(), //
//										pos.getStartingShares())) {
//							System.out.println("xyzzy");
//						}
//					}
//
//					if (!Common.isEffectivelyEqual(pos.getEndingShares(), pos.getExpectedEndingShares())) {
//						System.out.println("xyzzy");
//					}
//				}
//			}
//		}
	}

	/** Update statements with their reconciliation information */
	public void processStatementDetails(List<StatementDetails> details) {
		for (StatementDetails d : details) {
			Account a = this.model.getAccountByID(d.acctid);

			// We've loaded basic statement info (date, closing balance)
			// We must connect the statements with the associated transactions
			// that are specified in the statementLog (StatementDetails objects)
			Statement s = a.getStatement(d.date, d.closingBalance);
			if (s == null) {
				Common.reportError("Can't find statement for details: " //
						+ a.name //
						+ "  " + d.date.toString() //
						+ "  " + d.closingBalance);
				continue;
			}

			getTransactionsFromDetails(a, s, d);

			if (!s.isBalanced()) {
				// getTransactionsFromDetails(a, s, d);
//				Common.reportError("Can't reconcile statement from log.\n" //
//						+ " a=" + a.name //
//						+ " s=" + s.toString());
			}
		}
	}

	private boolean isMatch(SimpleTxn tx, StatementTxInfo txinfo) {
		if (!txinfo.date.equals(tx.getDate())) {
			return false;
		}

		if ((txinfo.cknum != tx.getCheckNumber()) //
				|| (txinfo.cashAmount.compareTo(tx.getCashAmount()) != 0)) {
			return false;
		}

		if (tx.isCleared()) {
			Common.reportError("Reconciling transaction twice:\n" //
					+ tx.toString());
		}

		return true;
	}

	/** Match up statement and its transactions using statement details. */
	private void getTransactionsFromDetails(Account a, Statement s, StatementDetails d) {
		if (s.isBalanced()) {
			return;
		}

		s.transactions.clear();
		s.unclearedTransactions.clear();
		s.holdings.initializeTransactions();

		List<GenericTxn> txns = a.gatherTransactionsForStatement(s);
		List<StatementTxInfo> badinfo = new ArrayList<StatementTxInfo>();

		for (StatementTxInfo info : d.transactions) {
			boolean found = false;

			for (int ii = 0; ii < txns.size(); ++ii) {
				GenericTxn t = txns.get(ii);

				if (isMatch(t, info)) {
					s.addTransaction(t);
					txns.remove(ii);
					found = true;

					break;
				}
			}

			if (!found) {
				badinfo.add(info);
			}
		}

		s.unclearedTransactions.addAll(txns);
boolean b = false;
		if (!badinfo.isEmpty()) {
			// d.transactions.removeAll(badinfo);
			Common.reportWarning(String.format( //
					"Statement: %s\n  %d missing transactions\n  %s", //
					s.toString(), badinfo.size(), badinfo.toString()));
			b = true;
		}

		for (GenericTxn t : s.transactions) {
			t.setStatementDate(s.date);
		}

		s.setIsBalanced(badinfo.isEmpty());
	}

	/** Add a reconciled statement's info to the statement log file */
	public void saveReconciledStatement(Statement stat) {
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

	/**
	 * Recreate log file when the format has changed from the previous version.<br>
	 * Save the previous file as <name>.N
	 */
	public void rewriteStatementLogFile() {
		File logfile = Statement.getStatementLogFileForModel(this.model);

		String basename = logfile.getName();
		File tmpLogFile = new File(QifDom.qifDir, basename + ".tmp");

		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new FileWriter(tmpLogFile));
		} catch (IOException e) {
			Common.reportError("Can't open tmp stmt log file: " + logfile.getAbsolutePath());
			return;
		}

		pw.println("" + StatementDetails.CURRENT_VERSION);
		for (Account a : this.model.getAccounts()) {
			for (Statement s : a.getStatements()) {
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

		logfile.renameTo(logFileBackup);
		if (logFileBackup.exists() && tmpLogFile.exists() && !logfile.exists()) {
			tmpLogFile.renameTo(logfile);
		}

		assert (logFileBackup.exists() && !logfile.exists());

		QifDom.loadedStatementsVersion = StatementDetails.CURRENT_VERSION;
	}

	/** Open the statement log file for appending */
	private PrintWriter openStatementsLogFile() {
		try {
			return new PrintWriter(new FileWriter( //
					Statement.getStatementLogFileForModel(this.model), true));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
}