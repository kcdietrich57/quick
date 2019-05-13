package moneymgr.io.qif;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import app.QifDom;
import moneymgr.io.LotProcessor;
import moneymgr.model.Account;
import moneymgr.model.GenericTxn;
import moneymgr.model.InvestmentTxn;
import moneymgr.model.MultiSplitTxn;
import moneymgr.model.NonInvestmentTxn;
import moneymgr.model.Security;
import moneymgr.model.SimpleTxn;
import moneymgr.model.TxAction;
import moneymgr.util.Common;
import moneymgr.util.QDate;

public class TransactionCleaner {
	/**
	 * Housekeeping info for processing related transfer transactions.<br>
	 * The imported data does not connect transfers, so we need to do it. These keep
	 * track of successful and unsuccessful attempts to connect transfers.
	 */
	private static final List<SimpleTxn> matchingTxns = new ArrayList<SimpleTxn>();
	private static int totalXfers = 0;
	private static int failedXfers = 0;

	public void cleanUpTransactions() {
		// TODO transactions should already be sorted?
		// sortAccountTransactionsByDate();
		cleanUpSplits();
		calculateRunningTotals();
		connectTransfers();
		connectSecurityTransfers();
		LotProcessor.setupSecurityLots();
	}

	private void sortAccountTransactionsByDate() {
		for (Account a : Account.getAccounts()) {
			Common.sortTransactionsByDate(a.transactions);
		}
	}

	/** Correct any issues with split transactions */
	private void cleanUpSplits() {
		for (Account a : Account.getAccounts()) {
			for (GenericTxn txn : a.transactions) {
				massageSplits(txn);
			}
		}
	}

	/** A txn may have multiple splits transferring to/from another account. */
	private void massageSplits(GenericTxn txn) {
		if (!(txn instanceof NonInvestmentTxn)) {
			return;
		}

		NonInvestmentTxn nitxn = (NonInvestmentTxn) txn;

		for (int ii = 0; ii < nitxn.splits.size(); ++ii) {
			SimpleTxn stxn = nitxn.splits.get(ii);
			if (stxn.getCatid() >= 0) {
				continue;
			}

			MultiSplitTxn mtxn = null;

			// Gather multiple splits into a MultiSplit if necessary
			for (int jj = ii + 1; jj < nitxn.splits.size(); ++jj) {
				SimpleTxn stxn2 = nitxn.splits.get(jj);

				if (stxn.getCatid() == stxn2.getCatid()) {
					if (mtxn == null) {
						mtxn = new MultiSplitTxn(txn.acctid);
						nitxn.splits.set(ii, mtxn);

						mtxn.setAmount(stxn.getAmount());
						mtxn.setCatid(stxn.getCatid());
						mtxn.subsplits.add(stxn);
					}

					mtxn.setAmount(mtxn.getAmount().add(stxn2.getAmount()));
					mtxn.subsplits.add(stxn2);

					nitxn.splits.remove(jj);
					--jj;
				}
			}
		}
	}

	/**
	 * Go through acct txns and plug in running balance values.<br>
	 * Also update account current and cleared balance values.
	 */
	private void calculateRunningTotals() {
		for (Account a : Account.getAccounts()) {
			a.clearedBalance = a.balance = BigDecimal.ZERO;

			for (GenericTxn t : a.transactions) {
				BigDecimal amt = t.getCashAmount();

				if (!amt.equals(BigDecimal.ZERO)) {
					a.balance = a.balance.add(amt);

					if (t.isCleared()) {
						a.clearedBalance = a.clearedBalance.add(amt);
					}
				}

				t.runningTotal = a.balance;
			}
		}
	}

	/** Connect transfer transactions between accounts */
	private void connectTransfers() {
		for (Account a : Account.getAccounts()) {
			for (GenericTxn txn : a.transactions) {
				connectTransfers(txn);
			}
		}
	}

	/** Connect transfers for a transaction */
	private void connectTransfers(GenericTxn txn) {
		if (txn.hasSplits()) {
			for (SimpleTxn stxn : ((NonInvestmentTxn) txn).splits) {
				connectTransfers(stxn, txn.getDate());
			}
		} else if ((txn.getCatid() < 0) && //
		// NB opening balance shows up as xfer to same acct
				(-txn.getCatid() != txn.acctid)) {
			connectTransfers(txn, txn.getDate());
		}
	}

	private static int multWarnCount = 0;

	/**
	 * Given a transaction that is a transfer, search the associated account's
	 * transactions for a suitable mate for this transaction.
	 */
	private void connectTransfers(SimpleTxn txn, QDate date) {
		if ((txn.getCatid() >= 0)) {
			return;
		}

		Account a = Account.getAccountByID(-txn.getCatid());

		findMatchesForTransfer(a, txn, date, true);

		++totalXfers;

		if (matchingTxns.isEmpty()) {
			findMatchesForTransfer(a, txn, date, false); // SellX openingBal void
		}

		if (matchingTxns.isEmpty()) {
			++failedXfers;

			Common.reportInfo("match not found for xfer: " + txn);
			Common.reportInfo("  " + failedXfers + " of " + totalXfers + " failed");

			return;
		}

		SimpleTxn xtxn = null;
		if (matchingTxns.size() == 1) {
			xtxn = matchingTxns.get(0);
		} else {
			if ((multWarnCount++ % 100) == 0) {
				Common.reportWarning("Multiple matching transactions (" //
						+ matchingTxns.size() //
						+ ") - using the first one. (" + multWarnCount + " occurrences)");
			}

			// TODO use the earliest choice
			xtxn = matchingTxns.get(0);
		}

		txn.setXtxn(xtxn);
		xtxn.setXtxn(txn);
	}

	/** Look for transfer candidates in an account */
	private void findMatchesForTransfer(Account acct, SimpleTxn txn, QDate date, boolean strict) {
		matchingTxns.clear();

		int idx = GenericTxn.getLastTransactionIndexOnOrBeforeDate(acct.transactions, date);
		if (idx < 0) {
			// TODO we can't do this, as we will look forwards from the date
			// return;
		}

		boolean exactDateMatch = false;

		for (int inc = 0; inc < 10; ++inc) {
			// Check matching/preceding transactions
			if (idx >= inc) {
				GenericTxn gtxn = acct.transactions.get(idx - inc);

				boolean dateeq = date.equals(gtxn.getDate());

				// Check earlier date only if we haven't found a match
				if (dateeq || !exactDateMatch) {
					SimpleTxn match = checkMatchForTransfer(txn, gtxn, strict);

					if (match != null) {
						matchingTxns.add(match);
						exactDateMatch |= dateeq;
					}
				}
			}

			// Check following transaction (date must be later)
			// Skip if exact match has already been found
			if (!exactDateMatch && (idx + inc < acct.transactions.size())) {
				GenericTxn gtxn = acct.transactions.get(idx + inc);

				SimpleTxn match = checkMatchForTransfer(txn, gtxn, strict);
				if (match != null) {
					matchingTxns.add(match);
				}
			}
		}
	}

	/** Locate a match for txn in gtxn (either gtxn itself, or a split) */
	private SimpleTxn checkMatchForTransfer(SimpleTxn txn, GenericTxn gtxn, boolean strict) {
		assert -txn.getCatid() == gtxn.acctid;

		if (!gtxn.hasSplits()) {
			if ((gtxn.getXferAcctid() == txn.acctid) //
					&& (gtxn.getXtxn() == null) //
					&& gtxn.amountIsEqual(txn, strict)) {
				return gtxn;
			}
		} else {
			for (SimpleTxn splittTxn : gtxn.getSplits()) {
				if ((splittTxn.getXferAcctid() == txn.acctid) //
						&& (splittTxn.getXtxn() == null) //
						&& splittTxn.amountIsEqual(txn, strict)) {
					return splittTxn;
				}
			}
		}

		return null;
	}

	/** Process transfers of securities between accounts */
	private void connectSecurityTransfers() {
		List<InvestmentTxn> xins = new ArrayList<InvestmentTxn>();
		List<InvestmentTxn> xouts = new ArrayList<InvestmentTxn>();

		// Gather all transfers
		for (GenericTxn txn : GenericTxn.getAllTransactions()) {
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
	private void connectSecurityTransfers(List<InvestmentTxn> xins, List<InvestmentTxn> xouts) {
		Comparator<InvestmentTxn> cpr = (o1, o2) -> {
			int diff;

			diff = o1.getDate().compareTo(o2.getDate());
			if (diff != 0) {
				return diff;
			}

			diff = o1.security.getName().compareTo(o2.security.getName());
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

			inshrs = gatherTransactionsForSecurityTransfer(ins, xins, null, t.security, t.getDate());
			outshrs = gatherTransactionsForSecurityTransfer(outs, xouts, unmatched, t.security, t.getDate());

			if (outs.isEmpty()) {
				unmatched.addAll(ins);
			} else {
				BigDecimal inshrs2 = inshrs.setScale(3, RoundingMode.HALF_UP);
				BigDecimal outshrs2 = outshrs.setScale(3, RoundingMode.HALF_UP);

				if (inshrs2.abs().compareTo(outshrs2.abs()) != 0) {
					Common.reportError("Mismatched security transfer");
				}

				for (InvestmentTxn inTx : ins) {
					inTx.xferTxns = new ArrayList<InvestmentTxn>(outs);
				}
				for (InvestmentTxn outTx : outs) {
					outTx.xferTxns = new ArrayList<InvestmentTxn>(ins);
				}
			}

			if (QifDom.verbose && !outs.isEmpty()) {
				String s = String.format(//
						"%-20s : %10s %8s %8s INSH=%10s (%3d txns) OUTSH=%10s (%d txns)", //
						t.getAccount().name, //
						t.getAction().toString(), //
						t.security.symbol, //
						t.getDate().toString(), //
						Common.formatAmount3(inshrs), ins.size(), //
						Common.formatAmount3(outshrs), outs.size());
				Common.reportInfo(s);
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
						t.security.symbol, //
						t.getDate().toString(), //
						pad, //
						Common.formatAmount3(t.getShares()));
				Common.reportInfo(s);
			}
		}
	}

	/** Collect transactions for a security transfer on a given date */
	private BigDecimal gatherTransactionsForSecurityTransfer( //
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
						&& (t.security.getName().compareTo(s.getName()) < 0))) {
			unmatched.add(srctxns.remove(0));
			if (srctxns.isEmpty()) {
				break;
			}

			t = srctxns.get(0);
		}

		// Processing matching txs
		while (!srctxns.isEmpty()) {
			t = srctxns.get(0);

			if ((t.security != s) || //
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