
package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class SimpleTxn {
	private static final List<SimpleTxn> NOSPLITS = new ArrayList<SimpleTxn>();

	protected static long nextid = 1;
	public final long id;

	public short acctid;
	protected BigDecimal amount;
	public String memo;

	public short xacct = 0;
	public short catid; // >0: CategoryID; <0 AccountID
	public SimpleTxn xtxn = null;

	public SimpleTxn(short acctid) {
		this.id = nextid++;
		this.acctid = acctid;
	}

	public boolean hasSplits() {
		return false;
	}

	public List<SimpleTxn> getSplits() {
		return NOSPLITS;
	}

	public short getXferAcctid() {
		return (short) ((this.catid < 0) ? -this.catid : 0);
	}

	public BigDecimal getXferAmount() {
		return this.amount;
	}

	public String toString() {
		String s = "Tx" + this.id + ":";
		s += " acct=" + QifDom.thedom.accounts.get(this.acctid).name;
		s += " amt=" + this.amount;
		s += " memo=" + this.memo;
		if (this.xacct < (short) 0) {
			s += " xacct=" + QifDom.thedom.accounts.get(-this.xacct).name;
		}
		if (this.catid < (short) 0) {
			s += " xcat=" + QifDom.thedom.accounts.get(-this.catid).name;
		} else if (this.catid > (short) 0) {
			s += " cat=" + QifDom.thedom.categories.get(this.catid).name;
		}

		return s;
	}
};

class MultiSplitTxn extends SimpleTxn {
	public List<SimpleTxn> subsplits = new ArrayList<SimpleTxn>();

	public MultiSplitTxn(short acctid) {
		super(acctid);
	}
};

public abstract class GenericTxn extends SimpleTxn {
	private Date date;
	public String clearedStatus;
	public Date stmtdate;

	public GenericTxn(short acctid) {
		super(acctid);

		this.date = null;
		this.clearedStatus = null;
		this.stmtdate = null;
	}

	public boolean isCleared() {
		return this.stmtdate != null;
	}

	public void clear(Statement s) {
		this.stmtdate = s.date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Date getDate() {
		return this.date;
	}

	public String toStringShort() {
		// TODO implement
		return "";
	}
};

class NonInvestmentTxn extends GenericTxn {
	public String chkNumber;
	public String payee;

	public List<String> address;
	public List<SimpleTxn> split;

	public NonInvestmentTxn(short acctid) {
		super(acctid);

		clearedStatus = "";
		chkNumber = "";
		payee = "";
		memo = "";
		catid = 0;
		address = new ArrayList<String>();
		this.split = new ArrayList<SimpleTxn>();
	}

	public boolean hasSplits() {
		return !this.split.isEmpty();
	}

	public List<SimpleTxn> getSplits() {
		return this.split;
	}

	public static NonInvestmentTxn load(QFileReader qfr, QifDom dom) {
		QFileReader.QLine qline = new QFileReader.QLine();

		NonInvestmentTxn txn = new NonInvestmentTxn(dom.currAccount.id);
		SimpleTxn cursplit = null;

		for (;;) {
			qfr.nextTxnLine(qline);

			switch (qline.type) {
			case EndOfSection:
				return txn;

			case TxnCategory:
				txn.catid = dom.findCategoryID(qline.value);

				if (txn.catid == 0) {
					Common.reportError("Can't find xtxn: " + qline.value);
				}
				break;
			case TxnAmount: {
				BigDecimal amt = Common.getDecimal(qline.value);

				if (txn.amount != null) {
					if (!txn.amount.equals(amt)) {
						Common.reportError("Inconsistent amount: " + qline.value);
					}
				} else {
					txn.amount = amt;
				}

				break;
			}
			case TxnMemo:
				txn.memo = qline.value;
				break;

			case TxnDate:
				txn.setDate(Common.GetDate(qline.value));
				break;
			case TxnClearedStatus:
				txn.clearedStatus = qline.value;
				break;
			case TxnNumber:
				txn.chkNumber = qline.value;
				break;
			case TxnPayee:
				txn.payee = qline.value;
				break;
			case TxnAddress:
				txn.address.add(qline.value);
				break;

			case TxnSplitCategory:
				if (cursplit == null || cursplit.catid != 0) {
					cursplit = new SimpleTxn(txn.acctid);
					txn.split.add(cursplit);
				}

				if (qline.value == null || qline.value.trim().isEmpty()) {
					qline.value = "Fix Me";
				}
				cursplit.catid = dom.findCategoryID(qline.value);

				if (cursplit.catid == 0) {
					Common.reportError("Can't find xtxn: " + qline.value);
				}
				break;
			case TxnSplitAmount:
				if (cursplit == null || cursplit.amount != null) {
					txn.split.add(cursplit);
					cursplit = new SimpleTxn(txn.acctid);
				}

				cursplit.amount = Common.getDecimal(qline.value);
				break;
			case TxnSplitMemo:
				if (cursplit != null) {
					cursplit.memo = qline.value;
				}
				break;

			default:
				Common.reportError("syntax error; txn: " + qline);
			}
		}
	}

	public void verifySplit() {
		if (this.split.isEmpty()) {
			return;
		}

		BigDecimal dec = new BigDecimal(0);

		for (SimpleTxn txn : this.split) {
			dec = dec.add(txn.amount);
		}

		if (!dec.equals(this.amount)) {
			Common.reportError("Total(" + this.amount + ") does not match split total (" + dec + ")");
		}
	}

	public String toString() {
		return toStringLong();
	}

	public String toStringShort() {
		String s = Common.getDateString(getDate());
		s += " " + this.chkNumber;
		s += " " + this.amount;
		s += " " + this.payee;

		return s;
	}

	public String toStringLong() {
		String s = "Tx" + this.id + ":";
		s += " acct=" + QifDom.thedom.accounts.get(this.acctid).name;
		s += " date=" + Common.getDateString(getDate());
		s += " clr:" + this.clearedStatus;
		s += " num=" + this.chkNumber;
		s += " payee=" + this.payee;
		s += " amt=" + this.amount;
		s += " memo=" + this.memo;
		if (this.catid < (short) 0) {
			s += " xacct=[" + QifDom.thedom.accounts.get(-this.catid).name + "]";
		} else if (this.catid > (short) 0) {
			s += " cat=" + QifDom.thedom.categories.get(this.catid).name;
		}

		if (!this.address.isEmpty()) {
			s += "\n  addr= ";
			for (String a : this.address) {
				s += "\n  " + a;
			}
		}

		// assert this.SplitCategories.size() == this.SplitAmounts.size();
		// assert this.SplitMemos.size() == this.SplitAmounts.size();

		if (!this.split.isEmpty()) {
			s += "\n  splits \n";

			for (SimpleTxn txn : this.split) {
				if (txn.catid < (short) 0) {
					s += " [" + QifDom.thedom.accounts.get(-txn.catid).name + "]";
				}
				if (txn.catid > (short) 0) {
					s += " " + QifDom.thedom.categories.get(txn.catid).name;
				}
				s += " " + txn.amount;
				if (txn.memo != null) {
					s += " " + txn.memo;
				}
				s += "\n";
			}
		}

		return s;
	}

	// static void Export(StreamWriter writer, List<BasicTransaction> list) {
	// if ((list == null) || (list.Count == 0)) {
	// return;
	// }
	//
	// write(this.header);
	//
	// foreach (BasicTransaction item in list) {
	// write(Date, item.Date.ToShortDateString());
	//
	// foreach (int i in item.Address.Keys) {
	// write(Address, item.Address[i]);
	// }
	//
	// write(Amount, item.Amount.ToString(CultureInfo.CurrentCulture));
	// writeIfSet(Category, item.Category);
	// writeIfSet(ClearedStatus, item.ClearedStatus);
	// writeIfSet(Memo, item.Memo);
	// writeIfSet(Number, item.Number);
	// writeIfSet(Payee, item.Payee);
	//
	// foreach (int i in item.SplitCategories.Keys) {
	// write(SplitCategory, item.SplitCategories[i]);
	// write(SplitAmount, item.SplitAmounts[i]);
	// writeIfSet(SplitMemo, item.SplitMemos[i]);
	// }
	//
	// write(EndOfEntry);
	// }
	// }
};

class InvestmentTxn extends GenericTxn {
	public String action;
	public String security;
	public BigDecimal price;
	public BigDecimal quantity;
	public String textFirstLine;
	public BigDecimal commission;
	public String accountForTransfer;
	public BigDecimal amountTransferred;

	public InvestmentTxn(short acctid) {
		super(acctid);

		this.action = "";
		this.security = "";
		this.textFirstLine = "";
		this.memo = "";
		this.accountForTransfer = "";
	}

	public static InvestmentTxn load(QFileReader qfr, QifDom dom) {
		QFileReader.QLine qline = new QFileReader.QLine();

		InvestmentTxn txn = new InvestmentTxn(dom.currAccount.id);

		for (;;) {
			qfr.nextInvLine(qline);

			switch (qline.type) {
			case EndOfSection:
				return txn;

			case InvTransactionAmt: {
				BigDecimal amt = Common.getDecimal(qline.value);

				if (txn.amount != null) {
					if (!txn.amount.equals(amt)) {
						Common.reportError("Inconsistent amount: " + qline.value);
					}
				} else {
					txn.amount = amt;
				}

				break;
			}
			case InvAction:
				txn.action = qline.value;
				break;
			case InvClearedStatus:
				txn.clearedStatus = qline.value;
				break;
			case InvCommission:
				txn.commission = Common.getDecimal(qline.value);
				break;
			case InvDate:
				txn.setDate(Common.GetDate(qline.value));
				break;
			case InvMemo:
				txn.memo = qline.value;
				break;
			case InvPrice:
				txn.price = Common.getDecimal(qline.value);
				break;
			case InvQuantity:
				txn.quantity = Common.getDecimal(qline.value);
				break;
			case InvSecurity:
				txn.security = qline.value;
				break;
			case InvFirstLine:
				txn.textFirstLine = qline.value;
				break;
			case InvXferAmt:
				txn.amountTransferred = Common.getDecimal(qline.value);
				break;
			case InvXferAcct:
				txn.accountForTransfer = qline.value;
				txn.xacct = dom.findCategoryID(qline.value);
				break;

			default:
				Common.reportError("syntax error; txn: " + qline);
			}
		}
	}

	public BigDecimal getXferAmount() {
		return (this.amountTransferred != null) //
				? this.amountTransferred //
				: super.getXferAmount();
	}

	public short getXferAcctid() {
		return (short) -this.xacct;
	}

	public String toString() {
		String s = "InvTx:";
		s += " dt=" + Common.getDateString(getDate());
		s += " act=" + this.action;
		s += " sec=" + this.security;
		s += " price=" + this.price;
		s += " qty=" + this.quantity;
		s += " amt=" + this.amount;
		s += " clr=" + this.clearedStatus;
		s += " txt=" + this.textFirstLine;
		s += " memo=" + this.memo;
		s += " comm=" + this.commission;
		s += " xact=" + this.accountForTransfer;
		s += " xamt=" + this.amountTransferred;
		s += "\n";

		return s;
	}

	// static void Export(StreamWriter writer, List<InvestmentTxn> list) {
	// if ((list == null) || (list.Count == 0)) {
	// return;
	// }
	//
	// writer.WriteLine(Headers.Investment);
	//
	// foreach (InvestmentTxn item in list) {
	// writeIfSet(AccountForTransfer, item.AccountForTransfer);
	// writeIfSet(Action, item.Action);
	// writer.WriteLine(AmountTransferred +
	// item.AmountTransferred.ToString(CultureInfo.CurrentCulture));
	// writeIfSet(ClearedStatus, item.ClearedStatus);
	// writer.WriteLine(Commission +
	// item.Commission.ToString(CultureInfo.CurrentCulture));
	// writer.WriteLine(Date + item.Date.ToShortDateString());
	// writeIfSet(Memo, item.Memo);
	// writer.WriteLine(Price +
	// item.Price.ToString(CultureInfo.CurrentCulture));
	// writer.WriteLine(Quantity +
	// item.Quantity.ToString(CultureInfo.CurrentCulture));
	// writeIfSet(Security, item.Security);
	// writeIfSet(TextFirstLine, item.TextFirstLine);
	// writer.WriteLine(TransactionAmount +
	// item.TransactionAmount.ToString(CultureInfo.CurrentCulture));
	// writer.WriteLine(EndOfEntry);
	// }
	// }
};
