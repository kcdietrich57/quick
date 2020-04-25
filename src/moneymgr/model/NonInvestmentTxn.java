package moneymgr.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import moneymgr.io.TransactionInfo;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/** Transaction for non-investment account - i.e. All cash, no securities */
public class NonInvestmentTxn extends GenericTxn {
//	public List<String> address;
	// TODO move splits to SimpleTxn/GenericTxn?//
	// Why shouldn't an investment txn have splits?
	private final List<SplitTxn> splits;

	public NonInvestmentTxn(int txid, int acctid) {
		super(txid, acctid);

		this.chkNumber = "";

//		this.address = new ArrayList<String>();
		this.splits = new ArrayList<>();
	}

	public NonInvestmentTxn(int acctid) {
		this(MoneyMgrModel.currModel.createTxid(), acctid);
	}

	public int compareWith(TransactionInfo tuple, SimpleTxn othersimp) {
		int diff;

		diff = super.compareWith(tuple, othersimp);
		if (diff != 0) {
			return diff;
		}

		if (!(othersimp instanceof NonInvestmentTxn)) {
			return -1;
		}

		NonInvestmentTxn other = (NonInvestmentTxn) othersimp;

		if (hasSplits()) {
			if (!other.hasSplits()) {
				return 1;
			}

			// TODO compare splits
		}

		return 0;
	}

	public int getCheckNumber() {
		if ((this.chkNumber == null) || (this.chkNumber.length() == 0)) {
			return 0;
		}

		// Quicken puts other info (e.g. 'DEP') in this field as well
		if (!Character.isDigit(this.chkNumber.charAt(0))) {
			return 0;
		}

		try {
			return Integer.parseInt(this.chkNumber);
		} catch (Exception e) {
			return 0;
		}
	}

	public boolean hasSplits() {
		return (this.splits != null) && !this.splits.isEmpty();
	}

	public List<SplitTxn> getSplits() {
		return this.splits;
	}

	public void addSplit(SplitTxn txn) {
		this.splits.add(txn);
	}

	/** Do sanity check of splits */
	public void verifySplit() {
		if (this.splits.isEmpty()) {
			return;
		}

		BigDecimal dec = BigDecimal.ZERO;

		for (SimpleTxn txn : this.splits) {
			dec = dec.add(txn.getAmount());
		}

		if (!dec.equals(getAmount())) {
			Common.reportError("Total(" + getAmount() + ") does not match split total (" + dec + ")");
		}
	}

	public String formatValue() {
		String ret = String.format("%10s    %-30s %s", //
				getDate().toString(), //
				this.getPayee(), //
				((isCleared()) ? "C" : " "));

		if (hasSplits()) {
			for (Iterator<SplitTxn> iter = getSplits().iterator(); iter.hasNext();) {
				SplitTxn split = iter.next();

				ret += "\n";
				// ret += split.formatValue();
				ret += String.format("   %13s    %-25s   %s", //
						Common.formatAmount(split.getAmount()), //
						split.getCategory(), //
						split.getMemo());
			}
		} else {
			ret += "\n";
			ret += String.format("   %13s    %-25s   %s", //
					Common.formatAmount(getAmount()), //
					getCategory(), //
					getMemo());
		}

		return ret;
	}

	public String toStringShort(boolean veryshort) {
		return (veryshort) //
				? String.format("%s %s  %s", //
						getDate().shortString, //
						Common.formatAmount(getAmount()), //
						getPayee()) //
				: String.format("%s %s %5s %s %s", //
						((this.stmtdate != null) ? "*" : " "), //
						getDate().toString(), //
						this.chkNumber, //
						Common.formatAmount(getAmount()), //
						getPayee());
	}

	public String toStringLong() {
		String s = "";

		s += ((this.stmtdate != null) ? "*" : " ");
		QDate d = getDate();
		s += ((d != null) ? d.toString() : "null");
		s += " Tx" + this.txid + ":   ";
		Account a = MoneyMgrModel.currModel.getAccountByID(getAccountID());
		s += ((a != null) ? a.name : "null");
		if (this.chkNumber != null && !this.chkNumber.isEmpty()) {
			s += " num=" + this.chkNumber;
		}
		s += " " + Common.formatAmount(getAmount()).trim();
		s += " " + getPayee();
		s += " xfer/cat=" + getCategory();
		if (getCashTransferAcctid() > 0) {
			s += "(";
			s += "" + ((getCashTransferTxn() != null) ? getCashTransferTxn().txid : "-");
			s += ")";
		}
		s += " memo=" + getMemo();
		s += " bal=" + this.getRunningTotal();

		if ((this.splits != null) && !this.splits.isEmpty()) {
			s += "\n  splits \n";

			for (final SimpleTxn txn : this.splits) {
				s += "  " + getCategory();
				s += " " + txn.getAmount();
				s += " " + txn.getMemo();

				s += "\n";
			}
		}

		return s;
	}
}