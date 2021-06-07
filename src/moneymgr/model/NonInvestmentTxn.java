package moneymgr.model;

import java.util.Iterator;

import moneymgr.util.Common;
import moneymgr.util.QDate;

/** Transaction for non-investment account - i.e. All cash, no securities */
public class NonInvestmentTxn extends GenericTxn {
	public NonInvestmentTxn(int txid, int acctid) {
		super(txid, acctid);
	}

	public NonInvestmentTxn(int acctid) {
		this(0, acctid);
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
						(isCleared() ? "*" : " "), //
						getDate().toString(), //
						this.getCheckNumberString(), //
						Common.formatAmount(getAmount()), //
						getPayee());
	}

	public String toStringLong() {
		String s = "";

		s += (isCleared() ? "*" : " ");

		QDate d = getDate();
		s += ((d != null) ? d.toString() : "null");

		s += " Tx" + getTxid() + ":   ";

		Account a = getAccount();
		s += ((a != null) ? a.name : "null");

		String cknum = getCheckNumberString();
		if ((cknum != null) && !cknum.isEmpty()) {
			s += " num=" + cknum;
		}

		s += " " + Common.formatAmount(getAmount()).trim();
		s += " " + getPayee();
		s += " xfer/cat=" + getCategory();

		if (getCashTransferAcctid() > 0) {
			s += "(";
			s += "" + ((getCashTransferTxn() != null) ? getCashTransferTxn().getTxid() : "-");
			s += ")";
		}

		s += " memo=" + getMemo();
		s += " bal=" + this.getRunningTotal();

		if (hasSplits()) {
			s += "\n";

			for (SimpleTxn txn : getSplits()) {
				s += "  " + getCategory();
				s += " " + txn.getAmount();
				s += " " + txn.getMemo();

				s += "\n";
			}
		}

		return s;
	}
}