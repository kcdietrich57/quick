package moneymgr.io.qif;

import java.math.BigDecimal;

import moneymgr.io.TransactionInfo;
import moneymgr.model.Account;
import moneymgr.model.InvestmentTxn;
import moneymgr.model.NonInvestmentTxn;
import moneymgr.model.Security;
import moneymgr.model.SplitTxn;
import moneymgr.model.TxAction;
import moneymgr.util.Common;

/** Inputs transactions from input QIF file */
public class TransactionProcessor {
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

		TransactionInfo tinfo = new TransactionInfo(Account.currAccountBeingLoaded);

		// TODO gather info and create transaction at the end
		InvestmentTxn txn = new InvestmentTxn(Account.currAccountBeingLoaded.acctid);

		for (;;) {
			this.qrdr.getFileReader().nextInvLine(qline);

			switch (qline.type) {
			case EndOfSection:
				txn.repair(tinfo);
				TransactionInfo.addWinInfo(tinfo, txn);
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
				tinfo.setValue(TransactionInfo.AMOUNT_IDX, qline.value);

				break;
			}
			case InvAction:
				txn.setAction(TxAction.parseAction(qline.value));
				tinfo.setValue(TransactionInfo.ACTION_IDX, qline.value);
				break;
			case InvClearedStatus:
				// Ignore
				break;
			case InvCommission:
				txn.commission = Common.getDecimal(qline.value);
				tinfo.setValue(TransactionInfo.COMMISSION_IDX, qline.value);
				break;
			case InvDate:
				txn.setDate(Common.parseQDate(qline.value));
				tinfo.setValue(TransactionInfo.DATE_IDX, qline.value);
				break;
			case InvMemo:
				txn.setMemo(qline.value);
				tinfo.setValue(TransactionInfo.MEMO_IDX, qline.value);
				break;
			case InvPrice:
				txn.price = Common.getDecimal(qline.value);
				tinfo.setValue(TransactionInfo.PRICE_IDX, qline.value);
				break;
			case InvQuantity:
				txn.setQuantity(Common.getDecimal(qline.value));
				tinfo.setValue(TransactionInfo.SHARES_IDX, qline.value);
				break;
			case InvSecurity:
				txn.security = Security.findSecurityByName(qline.value);
				if (txn.security == null) {
					txn.security = Security.findSecurityByName(qline.value);
					Common.reportWarning("Txn for acct " + txn.getAccountID() + ". " //
							+ "No security '" + qline.value + "' was found.");
				}
				tinfo.setValue(TransactionInfo.SECURITY_IDX, qline.value);
				break;
			case InvFirstLine:
				// txn.textFirstLine = qline.value;
				break;
			case InvXferAmt:
				txn.amountTransferred = Common.getDecimal(qline.value);
				tinfo.setValue(TransactionInfo.XAMOUNT_IDX, qline.value);
				break;
			case InvXferAcct: {
				int catid = Common.parseCategory(qline.value);
				if (catid < 0) {
					txn.accountForTransfer = qline.value;
				}
				txn.setCatid(catid);
				tinfo.setValue(TransactionInfo.CATEGORY_IDX, qline.value);
			}
			tinfo.setValue(TransactionInfo.XACCOUNT_IDX, qline.value);
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
			 String s = this.qrdr.getFileReader().peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			 NonInvestmentTxn txn = loadNonInvestmentTransaction();
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

		TransactionInfo tinfo = new TransactionInfo(Account.currAccountBeingLoaded);

		// TODO gather info and create transaction at the end
		NonInvestmentTxn txn = new NonInvestmentTxn(Account.currAccountBeingLoaded.acctid);
		SplitTxn cursplit = null;

		for (;;) {
			this.qrdr.getFileReader().nextTxnLine(qline);

			switch (qline.type) {
			case EndOfSection:
				TransactionInfo.addWinInfo(tinfo, txn);
				return txn;

			case TxnCategory: {
				int catid = Common.parseCategory(qline.value);

				if (catid == 0) {
					Common.reportError("Can't find xtxn: " + qline.value);
				}

				txn.setCatid(catid);
			}
				tinfo.setValue(TransactionInfo.CATEGORY_IDX, qline.value);
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
				tinfo.setValue(TransactionInfo.AMOUNT_IDX, qline.value);

				break;
			}
			case TxnMemo:
				txn.setMemo(qline.value);
				tinfo.setValue(TransactionInfo.MEMO_IDX, qline.value);
				break;

			case TxnDate:
				txn.setDate(Common.parseQDate(qline.value));
				tinfo.setValue(TransactionInfo.DATE_IDX, qline.value);
				break;
			case TxnClearedStatus:
				// Ignore
				break;
			case TxnNumber:
				txn.chkNumber = qline.value;
				tinfo.setValue(TransactionInfo.CHECKNUM_IDX, qline.value);
				break;
			case TxnPayee:
				txn.setPayee(qline.value);
				tinfo.setValue(TransactionInfo.PAYEE_IDX, qline.value);
				break;
			case TxnSplitCategory:
				if (cursplit == null || cursplit.getCatid() != 0) {
					cursplit = new SplitTxn(txn);
					txn.addSplit(cursplit);
				}

				if (qline.value == null || qline.value.trim().isEmpty()) {
					qline.value = "Fix Me";
				}
				cursplit.setCatid(Common.parseCategory(qline.value));

				if (cursplit.getCatid() == 0) {
					Common.reportError("Can't find xtxn: " + qline.value);
				}
				tinfo.addSplitCategory(qline.value);
				break;
			case TxnSplitAmount:
				if (cursplit == null || cursplit.getAmount() != null) {
					txn.addSplit(cursplit);
					cursplit = new SplitTxn(txn);
				}

				cursplit.setAmount(Common.getDecimal(qline.value));
				tinfo.addSplitAmount(qline.value);
				break;
			case TxnSplitMemo:
				if (cursplit != null) {
					cursplit.setMemo(qline.value);
				}
				tinfo.addSplitMemo(qline.value);
				break;

			default:
				Common.reportError("syntax error; txn: " + qline);
			}
		}
	}
}