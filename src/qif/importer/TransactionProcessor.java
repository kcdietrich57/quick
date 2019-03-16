package qif.importer;

import java.math.BigDecimal;

import qif.data.Account;
import qif.data.Category;
import qif.data.Common;
import qif.data.InvestmentTxn;
import qif.data.NonInvestmentTxn;
import qif.data.Security;
import qif.data.SimpleTxn;
import qif.data.TxAction;

class TransactionProcessor {
	private static int findCategoryID(String s) {
		if (s.startsWith("[")) {
			s = s.substring(1, s.length() - 1).trim();

			Account acct = Account.findAccount(s);

			return (short) ((acct != null) ? (-acct.acctid) : 0);
		}

		int slash = s.indexOf('/');
		if (slash >= 0) {
			// Throw away tag
			s = s.substring(slash + 1);
		}

		Category cat = Category.findCategory(s);

		return (cat != null) ? (cat.catid) : 0;
	}

	private QifDomReader qrdr;

	public TransactionProcessor(QifDomReader qrdr) {
		this.qrdr = qrdr;
	}

	public void loadInvestmentTransactions() {
		for (;;) {
			String s = this.qrdr.getFileReader().peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			InvestmentTxn txn = loadInvestmentTransaction();
			if (txn == null) {
				break;
			}

			if ("[[ignore]]".equals(txn.getMemo())) {
				continue;
			}

			if ((txn.security != null) && (txn.price != null)) {
				txn.security.addTransaction(txn);
			}

			Account.currAccountBeingLoaded.addTransaction(txn);
		}
	}

	private InvestmentTxn loadInvestmentTransaction() {
		QFileReader.QLine qline = new QFileReader.QLine();

		InvestmentTxn txn = new InvestmentTxn(Account.currAccountBeingLoaded.acctid);

		for (;;) {
			this.qrdr.getFileReader().nextInvLine(qline);

			switch (qline.type) {
			case EndOfSection:
				txn.repair();
				return txn;

			case InvTransactionAmt: {
				BigDecimal amt = Common.getDecimal(qline.value);

				if (txn.getAmount() != null) {
					if (!txn.getAmount().equals(amt)) {
						Common.reportError("Inconsistent amount: " + qline.value);
					}
				} else {
					txn.setAmount(amt);
				}

				break;
			}
			case InvAction:
				txn.setAction(TxAction.parseAction(qline.value));
				break;
			case InvClearedStatus:
				txn.clearedStatus = qline.value;
				break;
			case InvCommission:
				txn.commission = Common.getDecimal(qline.value);
				break;
			case InvDate:
				txn.setDate(Common.parseQDate(qline.value));
				break;
			case InvMemo:
				txn.setMemo(qline.value);
				break;
			case InvPrice:
				txn.price = Common.getDecimal(qline.value);
				break;
			case InvQuantity:
				txn.setQuantity(Common.getDecimal(qline.value));
				break;
			case InvSecurity:
				txn.security = Security.findSecurityByName(qline.value);
				if (txn.security == null) {
					txn.security = Security.findSecurityByName(qline.value);
					Common.reportWarning("Txn for acct " + txn.acctid + ". " //
							+ "No security '" + qline.value + "' was found.");
				}
				break;
			case InvFirstLine:
				//txn.textFirstLine = qline.value;
				break;
			case InvXferAmt:
				txn.amountTransferred = Common.getDecimal(qline.value);
				break;
			case InvXferAcct:
				txn.accountForTransfer = qline.value;
				txn.setCatid(findCategoryID(qline.value));
				break;

			default:
				Common.reportError("syntax error; txn: " + qline);
			}
		}
	}

	public void loadNonInvestmentTransactions() {
		for (;;) {
			final String s = this.qrdr.getFileReader().peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			final NonInvestmentTxn txn = loadNonInvestmentTransaction();
			if (txn == null) {
				break;
			}

			if ("[[ignore]]".equals(txn.getMemo())) {
				continue;
			}

			txn.verifySplit();

			Account.currAccountBeingLoaded.addTransaction(txn);
		}
	}

	private NonInvestmentTxn loadNonInvestmentTransaction() {
		QFileReader.QLine qline = new QFileReader.QLine();

		NonInvestmentTxn txn = new NonInvestmentTxn(Account.currAccountBeingLoaded.acctid);
		SimpleTxn cursplit = null;

		for (;;) {
			this.qrdr.getFileReader().nextTxnLine(qline);

			switch (qline.type) {
			case EndOfSection:
				return txn;

			case TxnCategory: {
				int catid = findCategoryID(qline.value);

				if (catid == 0) {
					Common.reportError("Can't find xtxn: " + qline.value);
				}

				txn.setCatid(catid);
			}
				break;

			case TxnAmount: {
				final BigDecimal amt = Common.getDecimal(qline.value);

				if (txn.getAmount() != null) {
					if (!txn.getAmount().equals(amt)) {
						Common.reportError("Inconsistent amount: " + qline.value);
					}
				} else {
					txn.setAmount(amt);
				}

				break;
			}
			case TxnMemo:
				txn.setMemo(qline.value);
				break;

			case TxnDate:
				txn.setDate(Common.parseQDate(qline.value));
				break;
			case TxnClearedStatus:
				txn.clearedStatus = qline.value;
				break;
			case TxnNumber:
				txn.chkNumber = qline.value;
				break;
			case TxnPayee:
				txn.setPayee(qline.value);
				break;
//			case TxnAddress:
//				txn.address.add(qline.value);
//				break;

			case TxnSplitCategory:
				if (cursplit == null || cursplit.getCatid() != 0) {
					cursplit = new SimpleTxn(txn.acctid);
					txn.split.add(cursplit);
				}

				if (qline.value == null || qline.value.trim().isEmpty()) {
					qline.value = "Fix Me";
				}
				cursplit.setCatid(findCategoryID(qline.value));

				if (cursplit.getCatid() == 0) {
					Common.reportError("Can't find xtxn: " + qline.value);
				}
				break;
			case TxnSplitAmount:
				if (cursplit == null || cursplit.getAmount() != null) {
					txn.split.add(cursplit);
					cursplit = new SimpleTxn(txn.acctid);
				}

				cursplit.setAmount(Common.getDecimal(qline.value));
				break;
			case TxnSplitMemo:
				if (cursplit != null) {
					cursplit.setMemo(qline.value);
				}
				break;

			default:
				Common.reportError("syntax error; txn: " + qline);
			}
		}
	}
}