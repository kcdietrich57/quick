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
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.Security;
import moneymgr.model.SimpleTxn;
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

	public final MoneyMgrModel model;

	public OptionsProcessor(MoneyMgrModel model) {
		this.model = model;
	}

	/** Load information about stock options from additional data file */
	public void loadStockOptions() {
		LineNumberReader rdr = null;

		try {
			File optfile = new File(QifDom.qifDir, OPTIONS_DATA_FILENAME);
			if (!(optfile.isFile() && optfile.canRead())) {
				return;
			}

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

				StockOption opt = null;

				if (op.equals("ESPP")) {
					// 09/30/90 ESPP "ISI ESPP Stock" 241 4.04 4.75 973.64 1144.75
					String secname = toker.nextToken();
					Security sec = this.model.findSecurity(secname);
					String acctname = toker.nextToken(); // .replaceAll("_", " ");
					Account acct = this.model.findAccount(acctname);
					BigDecimal shares = new BigDecimal(toker.nextToken());
					BigDecimal buyPrice = new BigDecimal(toker.nextToken());
					BigDecimal cost = new BigDecimal(toker.nextToken());
					BigDecimal mktPrice = new BigDecimal(toker.nextToken());
					BigDecimal value = new BigDecimal(toker.nextToken());

					opt = StockOption.esppPurchase(model, date, //
							acct.acctid, sec.secid, //
							shares, buyPrice, cost, mktPrice, value);

					Common.debugInfo("ESPP: " + opt.toString());
				} else {
					String name = toker.nextToken();

					if (op.equals("GRANT")) {
						// 05/23/91 GRANT 2656 ASCL ISI_Options 500 6.00 Y 4 10y
						String secname = toker.nextToken();
						Security sec = this.model.findSecurity(secname);
						String acctname = toker.nextToken(); // .replaceAll("_", " ");
						Account acct = this.model.findAccount(acctname);
						BigDecimal shares = new BigDecimal(toker.nextToken());
						BigDecimal price = new BigDecimal(toker.nextToken());
						String vestPeriod = toker.nextToken();
						int vestPeriodMonths = (vestPeriod.charAt(0) == 'Y') ? 12 : 3;
						int vestCount = Integer.parseInt(toker.nextToken());

						opt = StockOption.grant(this.model, name, date, //
								acct.acctid, sec.secid, //
								shares, price, vestPeriodMonths, vestCount, 0);

						Common.debugInfo("Granted: " + opt.toString());
					} else if (op.equals("VEST")) {
						// 05/23/92 VEST 2656 1
						int vestNumber = Integer.parseInt(toker.nextToken());

						opt = StockOption.vest(this.model, name, date, vestNumber);

						Common.debugInfo("Vested: " + opt.toString());

					} else if (op.equals("SPLIT")) {
						// 09/16/92 SPLIT 2656 2 1 [1000/3.00]
						int newShares = Integer.parseInt(toker.nextToken());
						int oldShares = Integer.parseInt(toker.nextToken());

						opt = StockOption.split(this.model, name, date, newShares, oldShares);

						Common.debugInfo("Split: " + opt.toString());
					} else if (op.equals("EXPIRE")) {
						// 05/23/01 EXPIRE 2656
						opt = StockOption.expire(this.model, name, date);

						if (opt != null) {
							Common.debugInfo("Expire: " + opt.toString());
						}
					} else if (op.equals("CANCEL")) {
						// 05/23/01 CANCEL 2656
						opt = StockOption.cancel(this.model, name, date);

						if (opt != null) {
							Common.debugInfo("Cancel: " + opt.toString());
						}
					} else if (op.equals("EXERCISE")) {
						// 09/19/95 EXERCISE 2656 2000 32.75
						BigDecimal shares = new BigDecimal(toker.nextToken());
						// BigDecimal price = new BigDecimal(toker.nextToken());

						opt = StockOption.exercise(this.model, name, date, shares);

						Common.debugInfo("Exercise: " + opt.toString());
					}
				}

				if (opt != null) {
					this.model.addStockOption(opt);
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

		List<StockOption> openOptions = StockOption.getOpenOptions(this.model);
		Common.debugInfo("\nOpen Stock Options:\n" + openOptions.toString());
	}

	/** Post-load processing for stock options */
	public void matchOptionsWithTransactions() {
		matchOptionsWithAllTransactions(this.model.getAllTransactions());

		// TODO seems we are processing transactions twice here.
		for (Account a : this.model.getAccounts()) {
			if (a.isInvestmentAccount()) {
				matchOptionsWithAccountTransactions(a.getTransactions());
			}
		}
	}

	/** Process options (either global for all txns, or one account's txns) */
	private void matchOptionsWithAccountTransactions(List<GenericTxn> txns) {
		for (GenericTxn gtxn : txns) {
			if ((gtxn != null) && (gtxn.getSecurity() != null)) {
				matchOptionsWithTransactions((InvestmentTxn) gtxn);
			}
		}
	}

	/** Process options (either global for all txns, or one account's txns) */
	private void matchOptionsWithAllTransactions(List<SimpleTxn> txns) {
		for (SimpleTxn gtxn : txns) {
			if ((gtxn != null) && (gtxn.getSecurity() != null)) {
				matchOptionsWithTransactions((InvestmentTxn) gtxn);
			}
		}
	}

	/** Process options (either global for all txns, or one account's txns) */
	private void matchOptionsWithTransactions(InvestmentTxn txn) {
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