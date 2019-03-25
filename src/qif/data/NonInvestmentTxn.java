package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Transaction for non-investment account */
public class NonInvestmentTxn extends GenericTxn {
	public String chkNumber;

//	public List<String> address;
	public List<SimpleTxn> splits;

	public NonInvestmentTxn(int acctid) {
		super(acctid);

		this.chkNumber = "";

//		this.address = new ArrayList<String>();
		this.splits = new ArrayList<SimpleTxn>();
	}

	public int getCheckNumber() {
		if ((this.chkNumber == null) || (this.chkNumber.length() == 0) //
				|| !Character.isDigit(this.chkNumber.charAt(0))) {
			return 0;
		}

		return Integer.parseInt(this.chkNumber);
	}

	public boolean hasSplits() {
		return !this.splits.isEmpty();
	}

	public List<SimpleTxn> getSplits() {
		return this.splits;
	}

	/** Do sanity check of splits */
	public void verifySplit() {
		if (this.splits.isEmpty()) {
			return;
		}

		BigDecimal dec = BigDecimal.ZERO;

		for (final SimpleTxn txn : this.splits) {
			dec = dec.add(txn.getAmount());
		}

		if (!dec.equals(getAmount())) {
			Common.reportError("Total(" + getAmount() + ") does not match split total (" + dec + ")");
		}
	}

	public String formatValue() {
		String ret = String.format("%10s %-30s %s  %13s  %-15s  %-10s", //
				getDate().toString(), //
				this.getPayee(), //
				((isCleared()) ? "C" : " "), //
				Common.formatAmount(getAmount()), //
				getCategory(), //
				getMemo());

		if (hasSplits()) {
			for (Iterator<SimpleTxn> iter = getSplits().iterator(); iter.hasNext();) {
				SimpleTxn split = iter.next();
				ret += "\n";
				ret += split.formatValue();
			}
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
		String s = ((this.stmtdate != null) ? "*" : " ") + "Tx" + this.txid + ":";
		QDate d = getDate();
		s += " date=" + ((d != null) ? getDate().toString() : "null");
		Account a = Account.getAccountByID(this.acctid);
		s += " acct=" + ((a != null) ? a.name : "null");
		//s += " clr:" + this.clearedStatus;
		s += " num=" + this.chkNumber;
		s += " payee=" + getPayee();
		s += " amt=" + getAmount();
		s += " memo=" + getMemo();
		s += " xfer/cat=" + getCategory();
		s += " bal=" + this.runningTotal;

//		if ((this.address != null) && !this.address.isEmpty()) {
//			s += "\n  addr= ";
//			for (final String addr : this.address) {
//				s += "\n  " + addr;
//			}
//		}

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