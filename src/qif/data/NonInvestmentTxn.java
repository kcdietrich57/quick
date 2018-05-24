package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

class NonInvestmentTxn extends GenericTxn {
	public enum TransactionType {
		Check, Deposit, Payment, Investment, ElectronicPayee
	};

	public String chkNumber;

	public List<String> address;
	public List<SimpleTxn> split;

	public NonInvestmentTxn(int domid, int acctid) {
		super(domid, acctid);

		this.chkNumber = "";

		this.address = new ArrayList<String>();
		this.split = new ArrayList<SimpleTxn>();
	}

	public NonInvestmentTxn(int domid, NonInvestmentTxn other) {
		super(domid, other);

		this.chkNumber = other.chkNumber;

		this.address = new ArrayList<String>();
		this.split = new ArrayList<SimpleTxn>();

		for (final String a : other.address) {
			this.address.add(new String(a));
		}
		for (final SimpleTxn st : other.split) {
			this.split.add(st);
		}
	}

	public int getCheckNumber() {
		if ((this.chkNumber == null) || (this.chkNumber.length() == 0) //
				|| !Character.isDigit(this.chkNumber.charAt(0))) {
			return 0;
		}

		return Integer.parseInt(this.chkNumber);
	}

	public boolean hasSplits() {
		return !this.split.isEmpty();
	}

	public List<SimpleTxn> getSplits() {
		return this.split;
	}

	public void verifySplit() {
		if (this.split.isEmpty()) {
			return;
		}

		BigDecimal dec = BigDecimal.ZERO;

		for (final SimpleTxn txn : this.split) {
			dec = dec.add(txn.getAmount());
		}

		if (!dec.equals(getAmount())) {
			Common.reportError("Total(" + getAmount() + ") does not match split total (" + dec + ")");
		}
	}

	public String toStringShort(boolean veryshort) {

		return (veryshort) //
				? String.format("%s %s  %s", //
						Common.formatDateShort(getDate()), //
						Common.formatAmount(getAmount()), //
						getPayee()) //
				: String.format("%s %s %5s %s %s", //
						((this.stmtdate != null) ? "*" : " "), //
						Common.formatDate(getDate()), //
						this.chkNumber, //
						Common.formatAmount(getAmount()), //
						getPayee());
	}

	public String toStringLong() {
		final QifDom dom = QifDom.getDomById(this.domid);

		String s = ((this.stmtdate != null) ? "*" : " ") + "Tx" + this.txid + ":";
		s += " date=" + Common.formatDate(getDate());
		s += " acct=" + dom.getAccount(this.acctid).getName();
		s += " clr:" + this.clearedStatus;
		s += " num=" + this.chkNumber;
		s += " payee=" + getPayee();
		s += " amt=" + getAmount();
		s += " memo=" + this.memo;

		if (this.catid < (short) 0) {
			s += " xacct=[" + dom.getAccount(-this.catid).getName() + "]";
		} else if (this.catid > (short) 0) {
			s += " cat=" + dom.getCategory(this.catid).name;
		}

		s += " bal=" + this.runningTotal;

		if (!this.address.isEmpty()) {
			s += "\n  addr= ";
			for (final String a : this.address) {
				s += "\n  " + a;
			}
		}

		if (!this.split.isEmpty()) {
			s += "\n  splits \n";

			for (final SimpleTxn txn : this.split) {
				if (txn.catid < (short) 0) {
					s += " [" + dom.getAccount(-txn.catid).getName() + "]";
				} else if (txn.catid > (short) 0) {
					s += " " + dom.getCategory(txn.catid).name;
				}

				s += " " + txn.getAmount();

				if (txn.memo != null) {
					s += " " + txn.memo;
				}

				s += "\n";
			}
		}

		return s;
	}
}