package qif.importer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import qif.data.Common;
import qif.data.InvestmentTxn;
import qif.data.Lot;
import qif.data.QDate;
import qif.data.Security;
import qif.data.TxAction;

class LotProcessor {
	/**
	 * For sorting transactions for a given security.<br>
	 * Ordered by date, then whether shares are added or removed from the acct.
	 */
	static Comparator<InvestmentTxn> sortTransactionsForLots = new Comparator<InvestmentTxn>() {
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

	private List<Lot> lots = null;

	public void setupSecurityLots() {
		for (Security sec : Security.getSecurities()) {
			List<InvestmentTxn> txns = new ArrayList<InvestmentTxn>(sec.transactions);
			Collections.sort(txns, sortTransactionsForLots);

			// Process splits once for all accounts; don't need split tx in each
			purgeDuplicateSplitTransactions(txns);

			this.lots = new ArrayList<Lot>();

			createLotsForTransactions(txns);

			mapLots(sec, this.lots, false);

			sec.setLots(this.lots);
			this.lots = null;
		}

		summarizeLots();
	}

	private void summarizeLots() {
		Common.reportInfo("\nSummary of open lots:");

		for (Security sec : Security.getSecurities()) {
			mapLots(sec, sec.getLots(), true);
		}
	}

	private void createLotsForTransactions(List<InvestmentTxn> txns) {
		for (int txIdx = 0; txIdx < txns.size();) {
//			String txstr = txns.get(txIdx).toString();

			int newTxIdx = createLotsForTransaction(txns, txIdx);

			System.out.println("=============================");
			for (int ii = txIdx; ii < newTxIdx; ++ii) {
				InvestmentTxn tx = txns.get(ii);
				System.out.println();
				System.out.println(tx.formatValue());
			}

			txIdx = newTxIdx;

//			for (Lot lot : this.lots) {
//				if (lot.expireTransaction == null) {
//					System.out.println("***" + lot.print(""));
//				}
//			}
//
//			System.out.print(txstr);
//			System.out.println("========= ");
		}
	}

	private int createLotsForTransaction(List<InvestmentTxn> txns, int txIdx) {
		// TODO should these be sets? It would simplify gather func
		List<InvestmentTxn> srcTxns = new ArrayList<InvestmentTxn>();
		List<InvestmentTxn> dstTxns = new ArrayList<InvestmentTxn>();

		InvestmentTxn txn = txns.get(txIdx);
		boolean isXfer = (txn.xferTxns != null) && !txn.xferTxns.isEmpty();

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

		if (!srcTxns.isEmpty()) {
			int acctid = srcTxns.get(0).acctid;
			System.out.println("====== Open lots for " + acctid);
			System.out.println(printOpenLots(acctid));
		}

		List<Lot> srcLots = null;
		if (!srcTxns.isEmpty()) {
			srcLots = getSrcLots(srcTxns);
		}

		switch (txn.getShareAction()) {
		case NEW_SHARES:
			addShares(txn);
			break;
		case DISPOSE_SHARES:
			removeShares(txn, srcLots);
			break;
		case TRANSFER_IN:
		case TRANSFER_OUT:
			transferShares(srcTxns, dstTxns, srcLots);
			break;
		case SPLIT:
			processSplit(txn);
			break;

		case NO_ACTION:
			Common.reportInfo("Skipping transaction: " + txn.toString());
			break;
		}

		return txIdx;
	}

	private void addLot(Lot newLot) {
		int idx = 0;
		for (; idx < this.lots.size(); ++idx) {
			Lot lot = this.lots.get(idx);
			if (lot.expireTransaction != null) {
				continue;
			}

			if (lot.getAcquisitionDate().compareTo(newLot.getAcquisitionDate()) > 0) {
				break;
			}
		}

		this.lots.add(idx, newLot);
	}

	/** Create a lot for new shares created by a transaction */
	private void addShares(InvestmentTxn txn) {
		Lot lot = new Lot(txn.acctid, txn.getDate(), txn.security.secid, //
				txn.getShares(), txn.getShareCost(), txn);
		addLot(lot);

		txn.lots.add(lot);
	}

	/** Get open lots for account */
	private List<Lot> getOpenLots(int acctid) {
		List<Lot> ret = new ArrayList<Lot>();

		for (Lot lot : this.lots) {
			if (lot.isOpen() && (lot.acctid == acctid)) {
				ret.add(lot);
			}
		}

		return ret;
	}

	/** Get open lots to satisfy txns that consume lots (sell/xfer out/split) */
	private List<Lot> getSrcLots(List<InvestmentTxn> txns) {
		List<Lot> lots = getOpenLots(txns.get(0).acctid);
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
		printOpenLots(txns.get(0).acctid);

		return null;
	}

	private Lot getBestLot(BigDecimal shares, List<Lot> lots) {
		for (Lot lot : lots) {
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
	private void removeShares(InvestmentTxn txn, List<Lot> srcLots) {
		BigDecimal sharesRemaining = txn.getShares().abs();

		while (!Common.isEffectivelyZero(sharesRemaining)) {
			Lot srcLot = getBestLot(sharesRemaining, srcLots);

			// Split the src lot if it has shares in excess of what is
			// required for the current dst transaction
			if (srcLot.shares.compareTo(sharesRemaining) > 0) {
				Lot[] splitLot = srcLot.split(txn, sharesRemaining);

				addLot(splitLot[0]);
				addLot(splitLot[1]);

				srcLot = splitLot[0];
			}

			// Consume source lot
			srcLot.expireTransaction = txn;
			txn.lotsDisposed.add(srcLot);
			txn.lots.add(srcLot);

			sharesRemaining = sharesRemaining.subtract(srcLot.shares);
		}
	}

	private String printOpenLots(int acctid) {
		String s = "";
		BigDecimal bal = BigDecimal.ZERO;
		boolean addshares = false;
		QDate curdate = null;
		TxAction curaction = null;

		for (Lot lot : getOpenLots(acctid)) {
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
	 * Transfer lot(s) between accounts for a transaction.<br>
	 * Split the last lot if partially transferred.
	 */
	private void transferShares( //
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

					addLot(splitLot[0]);
					addLot(splitLot[1]);

					srcLot = splitLot[0];
				}

				// Consume the entire source lot
				Lot newDstLot = new Lot(srcLot, dstTxn.acctid, srcTxn, dstTxn);
				addLot(newDstLot);

				sharesLeftInSrcTxn = sharesLeftInSrcTxn.subtract(newDstLot.shares);

				srcLot.expireTransaction = srcTxn;
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

	private Lot getFirstOpenLot(int acctid, BigDecimal sharesToMatch) {
		int idx = 0;
		for (Lot lot : this.lots) {
			if (lot.isOpen() //
					&& ((acctid == 0) || (lot.acctid == acctid))) {
				break;
			}

			++idx;
		}

		if (idx >= this.lots.size()) {
			return null;
		}

		Lot lot = this.lots.get(idx);

		if (sharesToMatch != null) {
			for (int idx2 = idx; idx2 < this.lots.size(); ++idx2) {
				Lot lot2 = this.lots.get(idx2);

				if (lot2.getAcquisitionDate().compareTo(lot.getAcquisitionDate()) > 0) {
					break;
				}

				if (lot2.shares.equals(sharesToMatch)) {
					return lot2;
				}
			}
		}

		return this.lots.get(idx);
	}

	/** Apply a split to all open shares in all accounts */
	private void processSplit(InvestmentTxn txn) {
		List<Lot> newLots = new ArrayList<Lot>();

		for (;;) {
			Lot oldlot = getFirstOpenLot(0, null);
			if (oldlot == null) {
				break;
			}

			InvestmentTxn t = (txn.acctid == oldlot.acctid) //
					? txn //
					: new InvestmentTxn(oldlot.acctid, txn);

			Lot newLot = new Lot(oldlot, oldlot.acctid, t, t);

			newLots.add(newLot);
		}

		for (Lot newLot : newLots) {
			addLot(newLot);
		}
	}

	/** Remove duplicate stock splits (based on date) from the list of txns */
	private void purgeDuplicateSplitTransactions(List<InvestmentTxn> txns) {
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
	 * that are involved in the transfer. The associated transaction(s) must
	 * immediately following the first transaction.
	 * 
	 * @param txns     List of txns to search through
	 * @param startIdx Starting index in list for search (the original txn)
	 * @param srcTxns  Source of transfer
	 * @param dstTxns  Destination of transfer
	 * @return Updated list index following last xfer txn
	 */
	private int gatherXferTransactions(List<InvestmentTxn> txns, int startIdx, //
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
	 * Collect txns in list2 connected via transfer to txns in list1<br>
	 * The group of transactions starts at startIdx in the list of txns.
	 * 
	 * @return The index of the last txn in the main list involved in the transfer
	 */
	private int collectTransfers( //
			List<InvestmentTxn> txns, //
			int startIdx, int maxidx, //
			List<InvestmentTxn> list1, //
			List<InvestmentTxn> list2) {
		for (InvestmentTxn txn1 : list1) {
			for (InvestmentTxn xferTxn : txn1.xferTxns) {
				if (!list2.contains(xferTxn)) {
					list2.add(xferTxn);

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

	/** Print a map of the ancestry of all lots */
	private BigDecimal mapLots(Security sec, List<Lot> origlots, boolean summary) {
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

		if (!summary) {
			sb.append("\n--------------------------------");
			sb.append("Lots for security " + sec.getSymbol());

			mapLotsRecursive(sb, toplots, "");

			sb.append("\nOpen lots:\n");
		}

		BigDecimal balance = BigDecimal.ZERO;
		int lotcount = 0;

		for (Lot lot : origlots) {
			if (lot.isOpen()) {
				balance = balance.add(lot.shares);
				++lotcount;

				if (!summary) {
					sb.append("  " + lot.toString() + " " + Common.formatAmount3(balance));
				}
			}
		}

		if (!Common.isEffectivelyZero(balance)) {
			if (summary) {
				sb.append(String.format("  %-6s: ", sec.getSymbol()));
			}

			sb.append(String.format("  lots=%3d  bal=%12s", //
					lotcount, Common.formatAmount3(balance)));
		}

		Common.reportInfo(sb.toString());

		return balance;
	}

	/** Print a hierarchical representation of the history of lots */
	private void mapLotsRecursive(StringBuilder sb, List<Lot> lots, String indent) {
		for (Lot lot : lots) {
			sb.append(indent + "- " + lot.toString());

			mapLotsRecursive(sb, lot.childLots, indent + "  ");
		}
	}
}