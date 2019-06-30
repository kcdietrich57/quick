package moneymgr.io.qif;

import java.math.BigDecimal;

import moneymgr.model.Account;
import moneymgr.model.Category;
import moneymgr.model.InvestmentTxn;
import moneymgr.model.NonInvestmentTxn;
import moneymgr.model.Security;
import moneymgr.model.SplitTxn;
import moneymgr.model.TxAction;
import moneymgr.util.Common;

/** Inputs transactions from input QIF file */
public class TransactionProcessor {
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

	private final QifDomReader qrdr;

	public TransactionProcessor(QifDomReader qrdr) {
		this.qrdr = qrdr;
	}

	public void loadInvestmentTransactions() {
		if (!Account.currAccountBeingLoaded.isInvestmentAccount()) {
			loadNonInvestmentTransactions();
			return;
		}

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

		// TODO gather info and create transaction at the end
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
				// Ignore
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
					Common.reportWarning("Txn for acct " + txn.getAccountID() + ". " //
							+ "No security '" + qline.value + "' was found.");
				}
				break;
			case InvFirstLine:
				// txn.textFirstLine = qline.value;
				break;
			case InvXferAmt:
				txn.amountTransferred = Common.getDecimal(qline.value);
				break;
			case InvXferAcct: {
				int catid = findCategoryID(qline.value);
				if (catid < 0) {
					txn.accountForTransfer = qline.value;
				}
				txn.setCatid(catid);
			}
				break;

			default:
				Common.reportError("syntax error; txn: " + qline);
			}
		}
	}

	public void loadNonInvestmentTransactions() {
		if (Account.currAccountBeingLoaded.isInvestmentAccount()) {
			loadInvestmentTransactions();
			return;
		}

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

		// TODO gather info and create transaction at the end
		NonInvestmentTxn txn = new NonInvestmentTxn(Account.currAccountBeingLoaded.acctid);
		SplitTxn cursplit = null;

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
				// Ignore
				break;
			case TxnNumber:
				txn.chkNumber = qline.value;
				break;
			case TxnPayee:
				txn.setPayee(qline.value);
				break;
			case TxnSplitCategory:
				if (cursplit == null || cursplit.getCatid() != 0) {
					cursplit = new SplitTxn(txn);
					txn.addSplit(cursplit);
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
					txn.addSplit(cursplit);
					cursplit = new SplitTxn(txn);
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