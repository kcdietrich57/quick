package qif.importer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import qif.data.Account;
import qif.data.Common;
import qif.data.GenericTxn;
import qif.data.InvestmentTxn;
import qif.data.MultiSplitTxn;
import qif.data.NonInvestmentTxn;
import qif.data.QDate;
import qif.data.QifDom;
import qif.data.Security;
import qif.data.SimpleTxn;
import qif.data.TxAction;

class TransactionCleaner {
	// Housekeeping info while processing related transfer transactions.
	// The imported data does not connect transfers, so we need to do it.
	// These keep track of successful and unsuccessful attempts to connect
	// transfers.
	private static final List<SimpleTxn> matchingTxns = new ArrayList<SimpleTxn>();
	private static int totalXfers = 0;
	private static int failedXfers = 0;

	public void cleanUpTransactions() {
		// TODO transactions should already be sorted?
		sortAccountTransactionsByDate();
		cleanUpSplits();
		calculateRunningTotals();
		connectTransfers();
		connectSecurityTransfers();
		new LotProcessor().setupSecurityLots();
	}

	private void sortAccountTransactionsByDate() {
		for (Account a : Account.getAccounts()) {
			Common.sortTransactionsByDate(a.transactions);
		}
	}

	private void cleanUpSplits() {
		for (Account a : Account.getAccounts()) {
			for (GenericTxn txn : a.transactions) {
				massageSplits(txn);
			}
		}
	}

	// This handles the case where we have multiple splits that involve
	// transferring from another account. The other account may have a single
	// entry that corresponds to more than one split in the other account.
	// N.B. Alternatively, we could merge the splits into one.
	private void massageSplits(GenericTxn txn) {
		if (!(txn instanceof NonInvestmentTxn)) {
			return;
		}

		final NonInvestmentTxn nitxn = (NonInvestmentTxn) txn;

		for (int ii = 0; ii < nitxn.split.size(); ++ii) {
			final SimpleTxn stxn = nitxn.split.get(ii);
			if (stxn.getCatid() >= 0) {
				continue;
			}

			MultiSplitTxn mtxn = null;

			for (int jj = ii + 1; jj < nitxn.split.size(); ++jj) {
				final SimpleTxn stxn2 = nitxn.split.get(jj);

				if (stxn.getCatid() == stxn2.getCatid()) {
					if (mtxn == null) {
						mtxn = new MultiSplitTxn(txn.acctid);
						nitxn.split.set(ii, mtxn);

						mtxn.setAmount(stxn.getAmount());
						mtxn.setCatid(stxn.getCatid());
						mtxn.subsplits.add(stxn);
					}

					mtxn.setAmount(mtxn.getAmount().add(stxn2.getAmount()));
					mtxn.subsplits.add(stxn2);

					nitxn.split.remove(jj);
					--jj;
				}
			}
		}
	}

	private void calculateRunningTotals() {
		for (Account a : Account.getAccounts()) {
			a.clearedBalance = a.balance = BigDecimal.ZERO;

			for (GenericTxn t : a.transactions) {
				BigDecimal amt = t.getCashAmount();

				if (!amt.equals(BigDecimal.ZERO)) {
					a.balance = a.balance.add(amt);
					t.runningTotal = a.balance;

					if (t.isCleared()) {
						a.clearedBalance = a.clearedBalance.add(amt);
					}
				}
			}
		}
	}

	private void connectTransfers() {
		for (Account a : Account.getAccounts()) {
			for (GenericTxn txn : a.transactions) {
				connectTransfers(txn);
			}
		}
	}

	private void connectTransfers(GenericTxn txn) {
		if ((txn instanceof NonInvestmentTxn) && !((NonInvestmentTxn) txn).split.isEmpty()) {
			for (final SimpleTxn stxn : ((NonInvestmentTxn) txn).split) {
				connectTransfers(stxn, txn.getDate());
			}
		} else if ((txn.getCatid() < 0) && //
		// opening balance shows up as xfer to same acct
				(-txn.getCatid() != txn.acctid)) {
			connectTransfers(txn, txn.getDate());
		}
	}

	private void connectTransfers(SimpleTxn txn, QDate date) {
		// Opening balance appears as a transfer to the same acct
		if ((txn.getCatid() >= 0) || (txn.getCatid() == -txn.getXferAcctid())) {
			return;
		}

		final Account a = Account.getAccountByID(-txn.getCatid());

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
			Common.reportWarning("Multiple matching transactions - using the first one.");
			xtxn = matchingTxns.get(0);
		}

		txn.setXtxn(xtxn);
		xtxn.setXtxn(txn);
	}

	private void findMatchesForTransfer(Account acct, SimpleTxn txn, QDate date, boolean strict) {
		matchingTxns.clear();

		final int idx = GenericTxn.getLastTransactionIndexOnOrBeforeDate(acct.transactions, date);
		if (idx < 0) {
			return;
		}

		boolean datematch = false;

		for (int inc = 0; datematch || (inc < 10); ++inc) {
			datematch = false;

			if (idx + inc < acct.transactions.size()) {
				final GenericTxn gtxn = acct.transactions.get(idx + inc);
				datematch = date.equals(gtxn.getDate());

				final SimpleTxn match = checkMatchForTransfer(txn, gtxn, strict);
				if (match != null) {
					matchingTxns.add(match);
				}
			}

			if (inc > 0 && idx >= inc) {
				final GenericTxn gtxn = acct.transactions.get(idx - inc);
				datematch = datematch || date.equals(gtxn.getDate());

				final SimpleTxn match = checkMatchForTransfer(txn, gtxn, strict);
				if (match != null) {
					matchingTxns.add(match);
				}
			}
		}
	}

	private SimpleTxn checkMatchForTransfer(SimpleTxn txn, GenericTxn gtxn, boolean strict) {
		assert -txn.getCatid() == gtxn.acctid;

		if (!gtxn.hasSplits()) {
			if ((gtxn.getXferAcctid() == txn.acctid) //
					&& gtxn.amountIsEqual(txn, strict)) {
				return gtxn;
			}
		} else {
			for (final SimpleTxn splitxn : gtxn.getSplits()) {
				if ((splitxn.getXferAcctid() == txn.acctid) //
						&& splitxn.amountIsEqual(txn, strict)) {
					return splitxn;
				}
			}
		}

		return null;
	}

	private void connectSecurityTransfers() {
		List<InvestmentTxn> xins = new ArrayList<InvestmentTxn>();
		List<InvestmentTxn> xouts = new ArrayList<InvestmentTxn>();

		for (GenericTxn txn : GenericTxn.getAllTransactions()) {
			if (txn instanceof InvestmentTxn) {
				if ((txn.getAction() == TxAction.SHRS_IN)) {
					xins.add((InvestmentTxn) txn);
				} else if (txn.getAction() == TxAction.SHRS_OUT) {
					xouts.add((InvestmentTxn) txn);
				}
			}
		}

		connectSecurityTransfers(xins, xouts);
	}

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
			for (final InvestmentTxn t : unmatched) {
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