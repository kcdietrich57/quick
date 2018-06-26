package qif.persistence;

import java.io.FileWriter;
import java.io.PrintWriter;

import qif.data.Statement;
import qif.importer.StatementDetails;

public class Reconciler {

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

	private static PrintWriter openStatementsLogFile() {
		try {
			return new PrintWriter(new FileWriter(Statement.stmtLogFile, true));
		} catch (final Exception e) {
			e.printStackTrace();
		}

		return null;
	}
}