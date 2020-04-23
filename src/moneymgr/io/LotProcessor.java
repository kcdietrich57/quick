package moneymgr.io;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import moneymgr.model.InvestmentTxn;
import moneymgr.model.Lot;
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.Security;
import moneymgr.model.TxAction;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/** Create lots to track shares as they move through the system */
public class LotProcessor {
	/**
	 * Comparator for sorting transactions for a given security.<br>
	 * Ordered by date, then whether shares are added or removed from the acct.
	 */
	private static final Comparator<InvestmentTxn> sortTransactionsForLots = //
			new Comparator<InvestmentTxn>() {
				public int compare(InvestmentTxn t1, InvestmentTxn t2) {
					int dif = t1.getDate().compareTo(t2.getDate());
					if (dif != 0) {
						return dif;
					}

					// Could be transfer?
					// Different accounts - remove comes first
					// Same account - add comes first
					if (t1.removesShares() != t2.removesShares()) {
						// TODO WTF? Why the opposite order depending on same acct or not?
						if (t1.getAccount().acctid != t2.getAccount().acctid) {
							return (t1.removesShares()) ? -1 : 1;
						} else {
							return (t1.removesShares()) ? 1 : -1;
						}
					}

					return 0;
				}
			};

	/** Create lots for all security transactions, all accounts */
	public static void setupSecurityLots() {
		for (Security sec : MoneyMgrModel.currModel.getSecurities()) {
			List<InvestmentTxn> txns = new ArrayList<InvestmentTxn>(sec.getTransactions());
			Collections.sort(txns, sortTransactionsForLots);

			// We process splits once for all accounts;
			// So we don't need split tx in each
			purgeDuplicateSplitTransactions(txns);

			List<Lot> thelots = new ArrayList<Lot>();

			createLotsForTransactions(thelots, txns);
			logLotsHistory(sec, thelots, false);

			sec.setLots(thelots);
		}

		logLotInfo();
	}

	/** Analyze a list of transactions, building lots for the investments */
	private static void createLotsForTransactions(List<Lot> thelots, List<InvestmentTxn> txns) {
		for (int txIdx = 0; txIdx < txns.size();) {
//			String txstr = txns.get(txIdx).toString();

			int newTxIdx = createLotsForTransaction(thelots, txns, txIdx);

//			System.out.println("=============================");
//			for (int ii = txIdx; ii < newTxIdx; ++ii) {
//				InvestmentTxn tx = txns.get(ii);
//				System.out.println();
//				System.out.println(tx.formatValue());
//			}

//			for (Lot lot : this.lots) {
//				if (lot.expireTransaction == null) {
//					System.out.println("***" + lot.print(""));
//				}
//			}
//
//			System.out.print(txstr);
//			System.out.println("========= ");

			txIdx = newTxIdx;
		}
	}

	/** Analyze a transaction, updating lot information */
	private static int createLotsForTransaction(List<Lot> thelots, List<InvestmentTxn> txns, int txIdx) {
		// TODO should these be sets? It would simplify gather func
		List<InvestmentTxn> srcTxns = new ArrayList<InvestmentTxn>();
		List<InvestmentTxn> dstTxns = new ArrayList<InvestmentTxn>();

		InvestmentTxn txn = txns.get(txIdx);
		boolean isXfer = !txn.getSecurityTransferTxns().isEmpty();

		if (isXfer) {
			txIdx = gatherXferTransactions(txns, txIdx, srcTxns, dstTxns);
		} else {
			if (txn.removesShares()) {
				srcTxns.add(txn);
			} else {
				dstTxns.add(txn);
			}

			++txIdx;
		}

//		if (!srcTxns.isEmpty()) {
//			int acctid = srcTxns.get(0).acctid;
//			System.out.println("====== Open lots for " + acctid);
//			System.out.println(printOpenLots(acctid));
//		}

		List<Lot> srcLots = null;
		if (!srcTxns.isEmpty()) {
			srcLots = getSrcLots(thelots, srcTxns);
		}

		switch (txn.getShareAction()) {
		case NEW_SHARES:
			addShares(thelots, txn);
			break;
		case DISPOSE_SHARES:
			removeShares(thelots, txn, srcLots);
			break;
		case TRANSFER_IN:
		case TRANSFER_OUT:
			transferShares(thelots, srcTxns, dstTxns, srcLots);
			break;
		case SPLIT:
			processSplit(thelots, txn);
			break;

		case NO_ACTION:
			Common.reportInfo("Skipping transaction: " + txn.toString());
			break;
		}

		return txIdx;
	}

	/** Insert a lot into a list sorted by date acquired */
	private static void addLot(List<Lot> thelots, Lot newLot) {
		int idx = 0;
		for (; idx < thelots.size(); ++idx) {
			Lot lot = thelots.get(idx);
			if (lot.disposingTransaction != null) {
				continue;
			}

			if (lot.getAcquisitionDate().compareTo(newLot.getAcquisitionDate()) > 0) {
				break;
			}
		}

		thelots.add(idx, newLot);
	}

	/** Create a lot for new shares created by a transaction */
	private static void addShares(List<Lot> thelots, InvestmentTxn txn) {
		Lot lot = new Lot(txn.getAccountID(), txn.getDate(), txn.getSecurityId(), //
				txn.getShares(), txn.getShareCost(), txn);
		addLot(thelots, lot);

		txn.lots.add(lot);
	}

	/** Get open lots for account */
	private static List<Lot> getOpenLots(List<Lot> thelots, int acctid) {
		List<Lot> ret = new ArrayList<Lot>();

		for (Lot lot : thelots) {
			if (lot.isOpen() && (lot.acctid == acctid)) {
				ret.add(lot);
			}
		}

		return ret;
	}

	/** Get open lots to satisfy txns that consume lots (sell/xfer out/split) */
	private static List<Lot> getSrcLots(List<Lot> thelots, List<InvestmentTxn> txns) {
		List<Lot> lots = getOpenLots(thelots, txns.get(0).getAccountID());
		List<Lot> ret = new ArrayList<Lot>();
		BigDecimal sharesRequired = BigDecimal.ZERO;

		for (InvestmentTxn txn : txns) {
			sharesRequired = sharesRequired.add(txn.getShares().abs());
		}

		BigDecimal sharesRemaining = sharesRequired;

		while ((sharesRemaining.signum() > 0) && !lots.isEmpty()) {
			Lot lot = lots.remove(0);
			ret.add(lot);

			if (lot.shares.compareTo(sharesRemaining) >= 0) {
				return ret;
			}

			sharesRemaining = sharesRemaining.subtract(lot.shares);
		}

		Common.reportError(String.format("Insufficient open lots: required %s, shortfall %s", //
				Common.formatAmount3(sharesRequired).trim(), //
				Common.formatAmount3(sharesRemaining).trim()));
		printOpenLots(thelots, txns.get(0).getAccountID());

		return null;
	}

	/**
	 * Get/remove the best lot to supply shares to remove (prefer size match).<br>
	 * NOTE: This removes the lot from the source list.
	 */
	private static Lot getBestLot(BigDecimal shares, List<Lot> lots) {
		for (Lot lot : lots) {
			// TODO why do we prefer this rather than the oldest lot?
			if (lot.shares.equals(shares)) {
				lots.remove(lot);
				return lot;
			}
		}

		return lots.remove(0);
	}

	/**
	 * Dispose lot(s) removed by a transaction.<br>
	 * Split the last lot if partially removed.
	 */
	private static void removeShares(List<Lot> thelots, InvestmentTxn txn, List<Lot> srcLots) {
		BigDecimal sharesRemaining = txn.getShares().abs();

		while (!Common.isEffectivelyZero(sharesRemaining)) {
			Lot srcLot = getBestLot(sharesRemaining, srcLots);

			// Split the src lot if it has shares in excess of what is
			// required for the current dst transaction
			if (srcLot.shares.compareTo(sharesRemaining) > 0) {
				Lot[] splitLot = srcLot.split(txn, sharesRemaining);

				addLot(thelots, splitLot[0]);
				addLot(thelots, splitLot[1]);

				srcLot = splitLot[0];
			}

			// Consume source lot
			srcLot.disposingTransaction = txn;
			txn.lotsDisposed.add(srcLot);
			txn.lots.add(srcLot);

			sharesRemaining = sharesRemaining.subtract(srcLot.shares);
		}
	}

	/** Build a formatted string describing the open lots in an account */
	private static String printOpenLots(List<Lot> thelots, int acctid) {
		String s = "";
		BigDecimal bal = BigDecimal.ZERO;
		// TODO since lot.addshares is always true, this means "not first lot"
		boolean addshares = false;
		QDate curdate = null;
		TxAction curaction = null;

		for (Lot lot : getOpenLots(thelots, acctid)) {
			bal = bal.add(lot.shares);

			if (curdate == null || //
					!(addshares == lot.addshares //
							&& curdate.equals(lot.createDate) //
							&& curaction.equals(lot.createTransaction.getAction()))) {
				s += String.format("\n%s %s %s", //
						lot.createDate.longString, //
						lot.createTransaction.getAction().toString(), //
						Common.formatAmount(lot.shares));
			} else {
				s += String.format(" %s", //
						Common.formatAmount(lot.shares));
			}

			addshares = lot.addshares;
			curdate = lot.createDate;
			curaction = lot.createTransaction.getAction();
		}

		s += "\nBalance: " + Common.formatAmount(bal);

		Common.reportInfo(s);

		return s;
	}

	/**
	 * Transfer lot(s) between accounts for a set of transactions.<br>
	 * Split the last lot if partially transferred.
	 */
	private static void transferShares( //
			List<Lot> thelots, //
			List<InvestmentTxn> srcTxns, //
			List<InvestmentTxn> dstTxns, //
			List<Lot> srcLots) {
		InvestmentTxn dstTxn = null;
		// Note this will be >= 0 (credit shares)
		BigDecimal sharesLeftInDstTxn = BigDecimal.ZERO;

		for (InvestmentTxn srcTxn : srcTxns) {
			// Note this will be negative (debit shares)
			BigDecimal sharesLeftInSrcTxn = srcTxn.getShares();
			assert sharesLeftInSrcTxn.signum() < 0;
			sharesLeftInSrcTxn = sharesLeftInSrcTxn.abs();

			while (sharesLeftInSrcTxn.signum() > 0) {
				if (sharesLeftInDstTxn.signum() <= 0) {
					if (dstTxns.isEmpty()) {
						Common.reportError("Transfer ran out of dest shares:" //
								+ " shares still required: " + Common.formatAmount3(sharesLeftInSrcTxn));
						break;
					}

					// Move on to the next destination transaction
					dstTxn = dstTxns.remove(0);
					sharesLeftInDstTxn = dstTxn.getShares();
					assert sharesLeftInDstTxn.signum() > 0;
				}

				// Consume the next available shares in the source account
				Lot srcLot = getBestLot(sharesLeftInDstTxn, srcLots);
				if (srcLot == null) {
					Common.reportError("Transfer ran out of src lots:" //
							+ " shares still required: " + Common.formatAmount3(sharesLeftInSrcTxn));
					break;
				}

				// Split the src lot if it has shares in excess of what is
				// required for the current dst transaction
				if (srcLot.shares.compareTo(sharesLeftInSrcTxn) > 0) {
					Lot[] splitLot = srcLot.split(srcTxn, sharesLeftInSrcTxn);

					addLot(thelots, splitLot[0]);
					addLot(thelots, splitLot[1]);

					srcLot = splitLot[0];
				}

				// Consume the entire source lot
				Lot newDstLot = new Lot(srcLot, dstTxn.getAccountID(), srcTxn, dstTxn);
				addLot(thelots, newDstLot);

				sharesLeftInSrcTxn = sharesLeftInSrcTxn.subtract(newDstLot.shares);

				srcLot.disposingTransaction = srcTxn;
				srcTxn.lots.add(srcLot);

				if (sharesLeftInDstTxn.compareTo(newDstLot.shares) > 0) {
					sharesLeftInDstTxn = sharesLeftInDstTxn.subtract(newDstLot.shares);
				} else {
					// Don't let it go negative
					sharesLeftInDstTxn = BigDecimal.ZERO;
				}
			}
		}
	}

	/** Find the first open lot in a list matching an account/number of shares. */
	private static Lot getFirstOpenLot(List<Lot> thelots, int acctid, BigDecimal sharesToMatch) {
		int idx = 0;
		for (Lot lot : thelots) {
			if (lot.isOpen() //
					&& ((acctid == 0) || (lot.acctid == acctid))) {
				break;
			}

			++idx;
		}

		if (idx >= thelots.size()) {
			return null;
		}

		Lot lot = thelots.get(idx);

		if (sharesToMatch != null) {
			for (int idx2 = idx; idx2 < thelots.size(); ++idx2) {
				Lot lot2 = thelots.get(idx2);

				if (lot2.getAcquisitionDate().compareTo(lot.getAcquisitionDate()) > 0) {
					break;
				}

				if (lot2.shares.equals(sharesToMatch)) {
					return lot2;
				}
			}
		}

		return thelots.get(idx);
	}

	/** Apply a split to all open lots in all accounts */
	private static void processSplit(List<Lot> thelots, InvestmentTxn txn) {
		List<Lot> newLots = new ArrayList<Lot>();

		for (;;) {
			Lot oldlot = getFirstOpenLot(thelots, 0, null);
			if (oldlot == null) {
				break;
			}

			InvestmentTxn t = (txn.getAccountID() == oldlot.acctid) //
					? txn //
					: new InvestmentTxn(oldlot.acctid, txn);

			Lot newLot = new Lot(oldlot, oldlot.acctid, t, t);

			newLots.add(newLot);
		}

		for (Lot newLot : newLots) {
			addLot(thelots, newLot);
		}
	}

	/** Remove duplicate stock splits (based on date) from the list of txns */
	private static void purgeDuplicateSplitTransactions(List<InvestmentTxn> txns) {
		for (int ii = 0; ii < txns.size(); ++ii) {
			InvestmentTxn txn = txns.get(ii);
			if (txn.getAction() != TxAction.STOCKSPLIT) {
				continue;
			}

			// Remove duplicate stock splits from different accounts
			while (ii < txns.size() - 1) {
				InvestmentTxn txn2 = txns.get(ii + 1);
				if (txn2.getAction() != TxAction.STOCKSPLIT //
						|| !txn.getDate().equals(txn2.getDate())) {
					break;
				}

				txns.remove(ii + 1);
			}
		}
	}

	/**
	 * Starting with one transaction that transfers shares, collect all the txns
	 * from both accounts that are involved in the transfer.<br>
	 * The associated transaction(s) must immediately following the first
	 * transaction.
	 * 
	 * @param txns     List of txns to search through
	 * @param startIdx Starting index in list for search (the original txn)
	 * @param srcTxns  Source of transfer
	 * @param dstTxns  Destination of transfer
	 * @return Updated list index following last xfer txn
	 */
	private static int gatherXferTransactions(List<InvestmentTxn> txns, int startIdx, //
			List<InvestmentTxn> srcTxns, List<InvestmentTxn> dstTxns) {
		InvestmentTxn starttx = txns.get(startIdx);

		if (starttx.removesShares()) {
			srcTxns.add(starttx);
		} else {
			dstTxns.add(starttx);
		}

		int maxidx = startIdx;
		int ntran = srcTxns.size() + dstTxns.size();

		for (boolean complete = false; !complete;) {
			maxidx = collectTransfers(txns, startIdx, maxidx, srcTxns, dstTxns);
			maxidx = collectTransfers(txns, startIdx, maxidx, dstTxns, srcTxns);

			// Continue until we find no more new related transactions
			int newNtran = srcTxns.size() + dstTxns.size();
			complete = ntran == newNtran;
			ntran = newNtran;
		}

		// All the xfer txns should be contiguous in the source list
		for (int followingIdx = startIdx; followingIdx <= maxidx; ++followingIdx) {
			if (!dstTxns.contains(txns.get(followingIdx)) && !srcTxns.contains(txns.get(followingIdx))) {
				Common.reportError(String.format( //
						"Unhandled xfer transaction in list: index %d (start %d, end %d)", //
						followingIdx, startIdx, maxidx));
			}
		}

		return maxidx + 1;
	}

	/**
	 * Add txns connected via transfer to txns in txnList1 into txnList2<br>
	 * The group of transactions starts at startIdx in the txns.
	 * 
	 * @return The index of the last txn in the main list involved in the transfer
	 */
	private static int collectTransfers( //
			List<InvestmentTxn> txns, //
			int startIdx, //
			int maxidx, //
			List<InvestmentTxn> txnList1, //
			List<InvestmentTxn> txnList2) {
		for (InvestmentTxn txn1 : txnList1) {
			for (InvestmentTxn xferTxn : txn1.getSecurityTransferTxns()) {
				if (!txnList2.contains(xferTxn)) {
					txnList2.add(xferTxn);

					int idx = txns.indexOf(xferTxn);
					if (idx <= startIdx) {
						Common.reportError(String.format( //
								"Src/dst transfer index less than start: (%d <= %d)", //
								idx, startIdx));
					}

					maxidx = Math.max(maxidx, idx);
				}
			}
		}

		return maxidx;
	}

	/** Output lot information and balance summary for all securities to the log. */
	private static void logLotInfo() {
		boolean SUMMARY_ONLY = true;
		boolean FULL_DETAILS = !SUMMARY_ONLY;

		Common.reportInfo("\nSummary of open lots:");

		for (Security sec : MoneyMgrModel.currModel.getSecurities()) {
			logLotsHistory(sec, sec.getLots(), SUMMARY_ONLY);
		}
	}

	/**
	 * Print a summary of security lots and balance<br>
	 * and (optionally) a map of the ancestry of all lots
	 * 
	 * @param sec         The security to log
	 * @param origlots    Lots for the security
	 * @param summaryOnly If true, log summary only without detailed history
	 * @return Final share balance
	 */
	private static BigDecimal logLotsHistory( //
			Security sec, List<Lot> origlots, boolean summaryOnly) {
		if (origlots.isEmpty()) {
			return BigDecimal.ZERO;
		}

		List<Lot> toplots = new ArrayList<Lot>();

		for (Lot lot : origlots) {
			if (lot.sourceLot == null) {
				toplots.add(lot);
			}
		}

		StringBuilder sb = new StringBuilder();

		if (!summaryOnly) {
			logSecurityHistoryDetails(sb, sec, toplots);
		}

		BigDecimal balance = calculateOpenLotsBalance(sb, origlots, summaryOnly);

		if (!Common.isEffectivelyZero(balance)) {
			if (summaryOnly) {
				sb.append(String.format("  %-6s: ", sec.getSymbol()));
			}

			sb.append(String.format("  bal=%12s", Common.formatAmount3(balance)));
		}

		Common.reportInfo(sb.toString());

		return balance;
	}

	/**
	 * Calculate share balance for a security from lots with optional logging of
	 * detailed info
	 */
	private static BigDecimal calculateOpenLotsBalance( //
			StringBuilder sb, List<Lot> lots, boolean summaryOnly) {
		BigDecimal balance = BigDecimal.ZERO;
		int lotcount = 0;

		sb.append("\nOpen lots(");
		int idx = sb.length();
		sb.append("):\n");

		if (summaryOnly) {
			sb.append("  -- details skipped --");
		}

		for (Lot lot : lots) {
			if (lot.isOpen()) {
				balance = balance.add(lot.shares);
				++lotcount;

				if (!summaryOnly) {
					sb.append("  " + lot.toString() + " " + Common.formatAmount3(balance));
				}
			}
		}

		sb.insert(idx, lotcount);

		return balance;
	}

	/** Log details of lot ancestry */
	private static void logSecurityHistoryDetails( //
			StringBuilder sb, Security sec, List<Lot> toplots) {
		sb.append("\n--------------------------------");
		sb.append("Lots for security " + sec.getSymbol());

		logSecurityChildHierarchy(sb, toplots, "");
	}

	/** Print a hierarchical representation of the history of lots */
	private static void logSecurityChildHierarchy(StringBuilder sb, List<Lot> lots, String indent) {
		for (Lot lot : lots) {
			sb.append(indent + "- " + lot.toString());

			logSecurityChildHierarchy(sb, lot.childLots, indent + "  ");
		}
	}
}