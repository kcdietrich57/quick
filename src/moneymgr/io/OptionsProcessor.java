package moneymgr.io;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.math.BigDecimal;
import java.util.List;
import java.util.StringTokenizer;

import app.QifDom;
import moneymgr.model.Account;
import moneymgr.model.GenericTxn;
import moneymgr.model.InvestmentTxn;
import moneymgr.model.Security;
import moneymgr.model.StockOption;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/**
 * Process stock option activity.<br>
 * This uses QIF data along with an additional 'options.txt' input file that
 * provides details not captured in Quicken.
 */
public class OptionsProcessor {
	private final static String OPTIONS_DATA_FILENAME = "options.txt";

	/** Load information about stock options from additional data file */
	public static void loadStockOptions() {
		LineNumberReader rdr = null;

		try {
			File optfile = new File(QifDom.qifDir, OPTIONS_DATA_FILENAME);
			assert optfile.isFile() && optfile.canRead();

			rdr = new LineNumberReader(new FileReader(optfile));

			String line = rdr.readLine();
			while (line != null) {
				line = line.trim();
				if (line.isEmpty() || line.charAt(0) == '#') {
					line = rdr.readLine();
					continue;
				}

				StringTokenizer toker = new StringTokenizer(line, "\t");

				String datestr = toker.nextToken();
				QDate date = Common.parseQDate(datestr);
				String op = toker.nextToken();

				if (op.equals("ESPP")) {
					// 09/30/90 ESPP "ISI ESPP Stock" 241 4.04 4.75 973.64 1144.75
					String secname = toker.nextToken();
					Security sec = Security.findSecurity(secname);
					String acctname = toker.nextToken(); // .replaceAll("_", " ");
					Account acct = Account.findAccount(acctname);
					BigDecimal shares = new BigDecimal(toker.nextToken());
					BigDecimal buyPrice = new BigDecimal(toker.nextToken());
					BigDecimal cost = new BigDecimal(toker.nextToken());
					BigDecimal mktPrice = new BigDecimal(toker.nextToken());
					BigDecimal value = new BigDecimal(toker.nextToken());

					StockOption opt = StockOption.esppPurchase(date, //
							acct.acctid, sec.secid, //
							shares, buyPrice, cost, mktPrice, value);

					Common.reportInfo("ESPP: " + opt.toString());
				} else {
					String name = toker.nextToken();

					if (op.equals("GRANT")) {
						// 05/23/91 GRANT 2656 ASCL ISI_Options 500 6.00 Y 4 10y
						String secname = toker.nextToken();
						Security sec = Security.findSecurity(secname);
						String acctname = toker.nextToken(); // .replaceAll("_", " ");
						Account acct = Account.findAccount(acctname);
						BigDecimal shares = new BigDecimal(toker.nextToken());
						BigDecimal price = new BigDecimal(toker.nextToken());
						String vestPeriod = toker.nextToken();
						int vestPeriodMonths = (vestPeriod.charAt(0) == 'Y') ? 12 : 3;
						int vestCount = Integer.parseInt(toker.nextToken());

						StockOption opt = StockOption.grant(name, date, //
								acct.acctid, sec.secid, //
								shares, price, vestPeriodMonths, vestCount, 0);

						Common.reportInfo("Granted: " + opt.toString());
					} else if (op.equals("VEST")) {
						// 05/23/92 VEST 2656 1
						int vestNumber = Integer.parseInt(toker.nextToken());

						StockOption opt = StockOption.vest(name, date, vestNumber);

						Common.reportInfo("Vested: " + opt.toString());

					} else if (op.equals("SPLIT")) {
						// 09/16/92 SPLIT 2656 2 1 [1000/3.00]
						int newShares = Integer.parseInt(toker.nextToken());
						int oldShares = Integer.parseInt(toker.nextToken());

						StockOption opt = StockOption.split(name, date, newShares, oldShares);

						Common.reportInfo("Split: " + opt.toString());
					} else if (op.equals("EXPIRE")) {
						// 05/23/01 EXPIRE 2656
						StockOption opt = StockOption.expire(name, date);

						if (opt != null) {
							Common.reportInfo("Expire: " + opt.toString());
						}
					} else if (op.equals("CANCEL")) {
						// 05/23/01 CANCEL 2656
						StockOption opt = StockOption.cancel(name, date);

						if (opt != null) {
							Common.reportInfo("Cancel: " + opt.toString());
						}
					} else if (op.equals("EXERCISE")) {
						// 09/19/95 EXERCISE 2656 2000 32.75
						BigDecimal shares = new BigDecimal(toker.nextToken());
						// BigDecimal price = new BigDecimal(toker.nextToken());

						StockOption opt = StockOption.exercise(name, date, shares);

						Common.reportInfo("Exercise: " + opt.toString());
					}
				}

				line = rdr.readLine();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (rdr != null) {
				try {
					rdr.close();
				} catch (IOException e) {
				}
			}
		}

		List<StockOption> openOptions = StockOption.getOpenOptions();
		Common.reportInfo("\nOpen Stock Options:\n" + openOptions.toString());
	}

	/** Post-load processing for stock options */
	public static void matchOptionsWithTransactions() {
		matchOptionsWithTransactions(GenericTxn.getAllTransactions());

		// TODO seems we are processing transactions twice here.
		for (Account a : Account.getAccounts()) {
			if (a.isInvestmentAccount()) {
				matchOptionsWithTransactions(a.getTransactions());
			}
		}
	}

	/** Process options (either global for all txns, or one account's txns) */
	private static void matchOptionsWithTransactions(List<GenericTxn> txns) {
		for (GenericTxn gtxn : txns) {
			if (!(gtxn instanceof InvestmentTxn) //
					|| (((InvestmentTxn) gtxn).security == null)) {
				continue;
			}

			InvestmentTxn txn = (InvestmentTxn) gtxn;

			// SecurityPosition pos = port.getPosition(txn.security);
			// pos.transactions.add(txn);

			switch (txn.getAction()) {
			case SHRS_IN:
			case REINV_DIV:
			case REINV_LG:
			case REINV_SH:
			case REINV_INT:
			case SHRS_OUT:
			case SELL:
			case SELLX:
				// pos.shares = pos.shares.add(txn.getShares());
				break;

			case BUY:
			case BUYX:
				StockOption.processEspp(txn);
				break;

			case GRANT:
				StockOption.processGrant(txn);
				break;

			case VEST:
				StockOption.processVest(txn);
				break;

			case EXERCISE:
			case EXERCISEX:
				StockOption.processExercise(txn);
				break;

			case EXPIRE:
				StockOption.processExpire(txn);
				break;

			case STOCKSPLIT:
				// StockOption.processSplit(txn);
				//
				// pos.shares = pos.shares.multiply(txn.getShares());
				// pos.shares = pos.shares.divide(BigDecimal.TEN);
				break;

			case CASH:
			case DIV:
			case INT_INC:
			case MISC_INCX:
				break;

			default:
				break;
			}
		}
	}
}