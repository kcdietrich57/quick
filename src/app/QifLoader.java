package app;

import java.util.Date;
import java.util.Scanner;
import java.util.StringTokenizer;

import qif.data.Account;
import qif.data.Common;
import qif.data.QifDom;
import qif.importer.QifDomReader;
import qif.report.NetWorthReporter;
import qif.report.QifReporter;
import qif.report.StatusReporter;

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

		final QifDom dom = QifDomReader.loadDom(new String[] { "qif/DIETRICH.QIF" });

		final Date firstTxDate = dom.getFirstTransactionDate();
		final Date lastTxDate = dom.getLastTransactionDate();

		for (;;) {
			System.out.println( //
					"First/last tx date (1): " + Common.formatDate(firstTxDate) //
							+ " " + Common.formatDate(lastTxDate));

			dom.showStatistics();

			final String s = scn.nextLine();

			if (s.startsWith("q")) {
				break;
			}

			if (s.startsWith("u")) {
				usage();
			} else if (s.startsWith("s")) {
				StatusReporter.reportStatus();
			} else if (s.startsWith("a")) {
				if (s.startsWith("act")) {
					StringTokenizer toker = new StringTokenizer(s);
					String cmd = toker.nextToken();
					assert cmd.equals("act");
					String d1str = (toker.hasMoreTokens()) ? toker.nextToken() : "1/1/1970";
					String d2str = (toker.hasMoreTokens()) ? toker.nextToken() : null;
					Date d1 = Common.parseDate(d1str);
					Date d2 = (d2str != null) ? Common.parseDate(d2str) : new Date();

					// dom.reportActivity(d1, d2);
				} else if (s.startsWith("accts")) {
					NetWorthReporter.reportCurrentNetWorth();
				} else {
					final String aname = s.substring(1).trim();

					final Account a = dom.findAccount(aname);
					if (a != null) {
						QifReporter.reportStatus(a, "m");
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

				QifReporter.reportCashFlow(d1, d2);
			} else if (s.startsWith("g")) {
				final String aname = s.substring(1).trim();

				final Account a = dom.findAccount(aname);
				if (a != null) {
					a.generateMonthlyStatements();
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
					QifReporter.reportMonthlyNetWorth();
				}
			} else if (s.startsWith("r")) {
				if (s.startsWith("relog")) {
					dom.rewriteStatementLogFile();
				}
			} else if (s.startsWith("y")) {
				if (s.startsWith("ys")) {
					QifReporter.reportYearlyStatus();
				}
			} else {
				final Date d = Common.parseDate(s);
				if (d != null) {
					NetWorthReporter.reportNetWorthForDate(d);
				}
			}
		}

		scn.close();
	}
}
