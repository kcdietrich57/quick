package moneymgr.io.qif;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import app.QifDom;
import moneymgr.io.LotProcessor;
import moneymgr.io.mm.Persistence;
import moneymgr.model.Account;
import moneymgr.model.GenericTxn;
import moneymgr.model.InvestmentTxn;
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.MultiSplitTxn;
import moneymgr.model.NonInvestmentTxn;
import moneymgr.model.Security;
import moneymgr.model.SimpleTxn;
import moneymgr.model.SplitTxn;
import moneymgr.model.TxAction;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/**
 * The imported data does not connect transfers, so we need to do it. These keep
 * track of successful and unsuccessful attempts to connect transfers.
 */
public class TransactionCleaner {
	private TransactionCleaner() {
	}

	public static void cleanUpTransactionsFromQIF() {
		cleanUpSplits();
		// calculateRunningTotals();
		connectTransfers();
		connectSecurityTransfers();
		LotProcessor.setupSecurityLots();
	}

	public static void cleanUpTransactionsFromJSON() {
		cleanUpSplits();
		calculateRunningTotals();
		connectTransfers();
		connectSecurityTransfers();
//		LotProcessor.setupSecurityLots();
	}

	/**
	 * Go through acct txns and plug in running balance values.<br>
	 * Also update account current and cleared balance values.
	 */
	public static void calculateRunningTotals() {
		for (Account a : MoneyMgrModel.currModel.getAccounts()) {
			BigDecimal bal = BigDecimal.ZERO;
			BigDecimal cleared = BigDecimal.ZERO;

			for (GenericTxn t : a.getTransactions()) {
				BigDecimal amt = t.getCashAmount();

				if (!Common.isEffectivelyZero(amt)) {
					bal = bal.add(amt);

					if (t.isCleared()) {
						cleared = cleared.add(amt);
					}
				}

				t.setRunningTotal(bal);
			}

			a.setBalance(bal);
			a.setClearedBalance(cleared);
		}
	}

	/** Correct any issues with split transactions */
	private static void cleanUpSplits() {
		for (Account a : MoneyMgrModel.currModel.getAccounts()) {
			for (GenericTxn txn : a.getTransactions()) {
				massageSplits(txn);
			}
		}
	}

	/** A txn may have multiple splits transferring to/from another account. */
	private static void massageSplits(GenericTxn txn) {
		if (!txn.hasSplits()) {
			return;
		}

		List<SplitTxn> splits = txn.getSplits();

		for (int ii = 0; ii < splits.size(); ++ii) {
			SplitTxn stxn = splits.get(ii);
			if (stxn.getCatid() >= 0) {
				continue;
			}

			MultiSplitTxn mtxn = null;

			// Gather multiple splits into a MultiSplit if necessary
			for (int jj = ii + 1; jj < splits.size(); ++jj) {
				SplitTxn stxn2 = splits.get(jj);

				if (stxn.getCatid() == stxn2.getCatid()) {
					if (mtxn == null) {
						mtxn = new MultiSplitTxn(txn);
						MoneyMgrModel.currModel.addTransaction(mtxn);
						splits.set(ii, mtxn);

						// mtxn.setAmount(stxn.getAmount());
						mtxn.setCatid(stxn.getCatid());
						mtxn.addSplit(stxn);
					}

					// mtxn.setAmount(mtxn.getAmount().add(stxn2.getAmount()));
					mtxn.addSplit(stxn2);

					splits.remove(jj);
					--jj;
				}
			}
		}
	}

	/** Connect transfer transactions between accounts */
	private static void connectTransfers() {
		for (Account a : MoneyMgrModel.currModel.getAccounts()) {
			for (GenericTxn txn : a.getTransactions()) {
				connectTransfers(txn);
			}
		}
	}

	static int[] counts = { 0, 0 };

	/** Connect transfers for a transaction */
	private static void connectTransfers(GenericTxn txn) {
		if (txn.hasSplits()) {
			for (SimpleTxn stxn : ((NonInvestmentTxn) txn).getSplits()) {
				// TODO verify we don't connect subsplits, just multisplit
//				if (false && stxn instanceof MultiSplitTxn) {
//					for (SimpleTxn sstxn : stxn.getSplits()) {
//						connectTransfers(sstxn, txn.getDate());
//					}
//				} else
				{
					connectTransfers(stxn, txn.getDate());
				}
			}
		} else if ((txn.getCatid() < 0) && //
		// NB opening balance shows up as xfer to same acct
				(-txn.getCatid() != txn.getAccountID())) {
			connectTransfers(txn, txn.getDate());
		}

		Persistence.validateTransfers(txn, counts);
	}

	private static int multWarnCount = 0;

	/**
	 * Given a transaction that is a transfer, search the associated account's
	 * transactions for a suitable mate for this transaction.
	 */
	private static void connectTransfers(SimpleTxn txn, QDate date) {
		List<SimpleTxn> matchingTxns = new ArrayList<SimpleTxn>();

		int totalXfers = 0;
		int failedXfers = 0;

		if ((txn.getCatid() >= 0)) {
			return;
		}

		if ((txn.getCashTransferTxn() != null) //
				&& (txn.getCashTransferTxn().getCashTransferTxn() == txn)) {
			return;
		}

		Account a = MoneyMgrModel.currModel.getAccountByID(-txn.getCatid());

		findMatchesForTransfer(a, matchingTxns, txn, date, true);

		++totalXfers;

		if (matchingTxns.isEmpty()) {
			findMatchesForTransfer(a, matchingTxns, txn, date, false);
			// SellX openingBal void
		}

		if (matchingTxns.isEmpty()) {
			++failedXfers;

			Common.debugInfo("match not found for xfer: " + txn);
			Common.debugInfo("  " + failedXfers + " of " + totalXfers + " failed");

			return;
		}

		SimpleTxn xtxn = null;
		if (matchingTxns.size() == 1) {
			xtxn = matchingTxns.get(0);
		} else {
			if (QifDom.verbose) {
				++multWarnCount;
				Common.reportWarning("Multiple matching transactions (" //
						+ matchingTxns.size() //
						+ ") - using the first one. (" + multWarnCount + " occurrences)\n" //
						+ matchingTxns);
			}

			xtxn = matchingTxns.get(0);
		}

		txn.setCashTransferTxn(xtxn);
		xtxn.setCashTransferTxn(txn);
	}

	/** Look for transfer candidates in an account */
	private static void findMatchesForTransfer(//
			Account acct, List<SimpleTxn> matchingTxns, //
			SimpleTxn txn, QDate date, boolean strict) {
		matchingTxns.clear();

		List<GenericTxn> txns = acct.getTransactions();
		int ntran = acct.getNumTransactions();
		int tolerance = 2;

		int idx0 = MoneyMgrModel.currModel.getLastTransactionIndexOnOrBeforeDate(txns, date.addDays(-tolerance));
		if (idx0 < 0) {
			idx0 = 0;
		}
		int idx1 = MoneyMgrModel.currModel.getLastTransactionIndexOnOrBeforeDate(txns, date.addDays(tolerance));
		if (idx1 < 0) {
			idx1 = 0;
		}

		boolean exactDateMatch = false;

		for (int idx = idx0; idx <= idx1; ++idx) {
			GenericTxn gtxn = txns.get(idx);

			boolean dateeq = date.equals(gtxn.getDate());

			// Check nearby dates only if we haven't found a match
			if (dateeq || !exactDateMatch) {
				int xid = txn.getCashTransferAcctid();
				if ((xid <= 0) || (txn.getCashTransferAcctid() != gtxn.getAccountID())) {
					continue;
				}

				SimpleTxn match = checkMatchForTransfer(txn, gtxn, strict);
				if (match == null) {
//					System.out.println(txn.toString());
//					System.out.println(gtxn.toString());
//					System.out.println("no match");				
				} else if (match.getCashTransferTxn() == null) {
					if (dateeq && !exactDateMatch) {
						matchingTxns.clear();
					}

					exactDateMatch |= dateeq;
					matchingTxns.add(match);
				}
			}
		}
	}

	/**
	 * Locate a match for txn in gtxn (either gtxn itself, or a split)<br>
	 * Account/amount must match and not already associated with the xfer.
	 */
	private static SimpleTxn checkMatchForTransfer(SimpleTxn txn, GenericTxn gtxn, boolean strict) {
		assert -txn.getCatid() == gtxn.getAccountID();

		if (!gtxn.hasSplits()) {
			if ((gtxn.getCashTransferAcctid() == txn.getAccountID()) //
					&& (gtxn.getCashTransferTxn() == null) //
					&& gtxn.amountIsEqual(txn, strict)) {
				return gtxn;
			}
		} else {
			for (SimpleTxn splittTxn : gtxn.getSplits()) {
				if ((splittTxn.getCashTransferAcctid() == txn.getAccountID()) //
						&& (splittTxn.getCashTransferTxn() == null) //
						&& splittTxn.amountIsEqual(txn, strict)) {
					return splittTxn;
				}
			}
		}

		return null;
	}

	/** Process transfers of securities between accounts */
	private static void connectSecurityTransfers() {
		List<InvestmentTxn> xins = new ArrayList<InvestmentTxn>();
		List<InvestmentTxn> xouts = new ArrayList<InvestmentTxn>();

		// Gather all security transfers
		for (SimpleTxn txn : MoneyMgrModel.currModel.getAllTransactions()) {
			if (txn instanceof InvestmentTxn) {
				if ((txn.getAction() == TxAction.SHRS_IN)) {
					xins.add((InvestmentTxn) txn);
				} else if (txn.getAction() == TxAction.SHRS_OUT) {
					xouts.add((InvestmentTxn) txn);
				}
			}
		}

		// Connect the transfers
		connectSecurityTransfers(xins, xouts);
	}

	/** Go through security transfers and connect in/out transactions */
	private static void connectSecurityTransfers(List<InvestmentTxn> xins, List<InvestmentTxn> xouts) {
		Comparator<InvestmentTxn> cpr = (o1, o2) -> {
			int diff;

			diff = o1.getDate().compareTo(o2.getDate());
			if (diff != 0) {
				return diff;
			}

			diff = o1.getSecurityName().compareTo(o2.getSecurityName());
			if (diff != 0) {
				return diff;
			}

			diff = o1.getShares().compareTo(o2.getShares());
			if (diff != 0) {
				return diff;
			}

			return o2.getAction().ordinal() - o1.getAction().ordinal();
		};

		List<InvestmentTxn> txns = new ArrayList<InvestmentTxn>(xins);
		txns.addAll(xouts);
		Collections.sort(txns, cpr);

		Collections.sort(xins, cpr);
		Collections.sort(xouts, cpr);

		List<InvestmentTxn> ins = new ArrayList<InvestmentTxn>();
		List<InvestmentTxn> outs = new ArrayList<InvestmentTxn>();
		List<InvestmentTxn> unmatched = new ArrayList<InvestmentTxn>();

		BigDecimal inshrs;
		BigDecimal outshrs;

		while (!xins.isEmpty()) {
			ins.clear();
			outs.clear();

			InvestmentTxn t = xins.get(0);

			inshrs = gatherTransactionsForSecurityTransfer(ins, xins, null, t.getSecurity(), t.getDate());
			outshrs = gatherTransactionsForSecurityTransfer(outs, xouts, unmatched, t.getSecurity(), t.getDate());

			if (outs.isEmpty()) {
				unmatched.addAll(ins);
			} else {
				BigDecimal inshrs2 = inshrs.setScale(3, RoundingMode.HALF_UP);
				BigDecimal outshrs2 = outshrs.setScale(3, RoundingMode.HALF_UP);

				if (inshrs2.abs().compareTo(outshrs2.abs()) != 0) {
					Common.reportError("Mismatched security transfer");
				}

				for (InvestmentTxn inTx : ins) {
					inTx.setSecurityTransferTxns(outs);
				}
				for (InvestmentTxn outTx : outs) {
					outTx.setSecurityTransferTxns(ins);
				}
			}

			if (QifDom.verbose && !outs.isEmpty()) {
				String s = String.format(//
						"%-20s : %10s %8s %8s INSH=%10s (%3d txns) OUTSH=%10s (%d txns)", //
						t.getAccount().name, //
						t.getAction().toString(), //
						t.getSecuritySymbol(), //
						t.getDate().toString(), //
						Common.formatAmount3(inshrs), ins.size(), //
						Common.formatAmount3(outshrs), outs.size());
				Common.debugInfo(s);
			}
		}

		if (QifDom.verbose && !unmatched.isEmpty()) {
			for (InvestmentTxn t : unmatched) {
				String pad = (t.getAction() == TxAction.SHRS_IN) //
						? "" //
						: "           ";

				String s = String.format("%-20s : %10s %8s %8s SHR=%s%10s", //
						t.getAccount().name, //
						t.getAction().toString(), //
						t.getSecuritySymbol(), //
						t.getDate().toString(), //
						pad, //
						Common.formatAmount3(t.getShares()));
				Common.debugInfo(s);
			}
		}
	}

	/** Collect transactions for a security transfer on a given date */
	private static BigDecimal gatherTransactionsForSecurityTransfer( //
			List<InvestmentTxn> rettxns, // OUT txs for xfer
			List<InvestmentTxn> srctxns, // IN all remaining txs
			List<InvestmentTxn> unmatched, // OUT earlier txs that don't match
			Security s, // The security being transferred
			QDate d) { // The date of the transfer
		// Return number of shares collected

		BigDecimal numshrs = BigDecimal.ZERO;

		if (srctxns.isEmpty()) {
			return numshrs;
		}

		InvestmentTxn t = srctxns.get(0);

		// Skip earlier Txs, gathering in unmatched
		while (t.getDate().compareTo(d) < 0 || //
				(t.getDate().equals(d) //
						&& (t.getSecurityName().compareTo(s.getName()) < 0))) {
			unmatched.add(srctxns.remove(0));
			if (srctxns.isEmpty()) {
				break;
			}

			t = srctxns.get(0);
		}

		// Processing matching txs
		while (!srctxns.isEmpty()) {
			t = srctxns.get(0);

			if ((t.getSecurity() != s) || //
					(t.getDate().compareTo(d) != 0)) {
				break;
			}

			rettxns.add(t);
			numshrs = numshrs.add(t.getShares());

			srctxns.remove(0);
		}

		return numshrs;
	}
}