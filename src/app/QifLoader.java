package app;

import java.util.Date;
import java.util.Scanner;
import java.util.StringTokenizer;

import moneymgr.io.Reconciler;
import moneymgr.io.qif.QifDomReader;
import moneymgr.model.Account;
import moneymgr.report.AccountReporter;
import moneymgr.report.CashFlowReporter;
import moneymgr.report.NetWorthReporter;
import moneymgr.report.QifReporter;
import moneymgr.report.ReconcileStatusReporter;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/** Old (obsolete) text-based frontend to quicken model */
public class QifLoader {
	public static Scanner scn;

	private static void usage() {
		String msg = "\n" //
				+ "<date> - Balances for date" //
				+ "accts - Show all account balances\n" //
				+ "a <acct> - Summarize account\n" //
				+ "c [ <date> [ <date> ] ] - Cash flow\n" //
				+ "act <date> <date> - activity for period" //
				+ "ma [<date> [<date>]] - Monthly activity (start/end date)" //
				+ "mnw - Monthly net worth\n" //
				+ "s - Show statistics\n" //
				+ "u - Usage message\n" //
				+ "ys - Yearly status\n" //
				+ "\n" //
				+ "g <acct> - Generate monthly statements\n" //
				+ "relog - Regenerate statement log\n" //
				+ "";

		System.out.println(msg);
	}

	public static void main(String[] args) {
		scn = new Scanner(System.in);

		QifDomReader.loadDom(new String[] { "qif/DIETRICH.QIF" });

		for (;;) {
			QifReporter.showStatistics();

			final String s = scn.nextLine();

			if (s.startsWith("q")) {
				break;
			}

			if (s.startsWith("u")) {
				usage();
			} else if (s.startsWith("s")) {
				ReconcileStatusReporter.reportStatus();
			} else if (s.startsWith("a")) {
				if (s.startsWith("act")) {
					StringTokenizer toker = new StringTokenizer(s);
					String cmd = toker.nextToken();
					assert cmd.equals("act");
					String d1str = (toker.hasMoreTokens()) ? toker.nextToken() : "1/1/1970";
					String d2str = (toker.hasMoreTokens()) ? toker.nextToken() : null;
					Date d1 = Common.parseDate(d1str);
					Date d2 = (d2str != null) ? Common.parseDate(d2str) : new Date();

					QifReporter.reportActivity(d1, d2);
				} else if (s.startsWith("accts")) {
					NetWorthReporter.reportCurrentNetWorth();
				} else {
					final String aname = s.substring(1).trim();

					final Account a = Account.findAccount(aname);
					if (a != null) {
						AccountReporter.reportStatus(a, "m");
					}
				}
			} else if (s.startsWith("c")) {
				Date d1 = new Date();
				Date d2 = null;

				String ss = s.substring(1).trim();

				if (ss.length() > 0) {
					int idx = ss.indexOf(' ');
					if (idx > 0) {
						String ss2 = ss.substring(idx + 1).trim();
						ss = ss.substring(0, idx);

						d2 = Common.parseDate(ss2);
					}

					d1 = Common.parseDate(ss);
				}

				CashFlowReporter.reportCashFlow(d1, d2);
			} else if (s.startsWith("g")) {
				final String aname = s.substring(1).trim();

				Account a = Account.findAccount(aname);
				if (a != null) {
					QifReporter.generateMonthlyStatements(a);
				}
			} else if (s.startsWith("m")) {
				if (s.startsWith("ma")) {
					/*
					 * StringTokenizer toker = new StringTokenizer(s); String cmd =
					 * toker.nextToken(); assert cmd.equals("ma"); String d1str =
					 * (toker.hasMoreTokens()) ? toker.nextToken() : "1/1/1970"; String d2str =
					 * (toker.hasMoreTokens()) ? toker.nextToken() : null; Date d1 =
					 * Common.parseDate(d1str); Date d2 = (d2str != null) ? Common.parseDate(d2str)
					 * : new Date();
					 * 
					 * for (int id = 1; id <= dom.getNumAccounts(); ++id) { Account a =
					 * dom.getAccount(id);
					 * 
					 * AccountPosition apos1 = a.getStatus(d1); AccountPosition apos2 =
					 * a.getStatus(d2); }
					 * 
					 * dom.reportMonthlyActivity(d1, d2);
					 */ } else if (s.startsWith("mnw")) {
					// NetWorthReporter.reportMonthlyNetWorth();
				}
			} else if (s.startsWith("r")) {
				if (s.startsWith("relog")) {
					Reconciler.rewriteStatementLogFile();
				}
			} else if (s.startsWith("y")) {
				if (s.startsWith("ys")) {
					// NetWorthReporter.reportYearlyNetWorth();
				}
			} else {
				final QDate d = Common.parseQDate(s);
				if (d != null) {
					NetWorthReporter.reportNetWorthForDate(d);
				}
			}
		}

		scn.close();
	}
}
