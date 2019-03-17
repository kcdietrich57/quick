package qif.importer;

import qif.data.Account;
import qif.data.AccountType;
import qif.data.Common;
import qif.data.QDate;

class AccountProcessor {
	private QifDomReader qrdr;

	public AccountProcessor(QifDomReader qrdr) {
		this.qrdr = qrdr;
	}

	public void loadAccounts() {
		for (;;) {
			String s = this.qrdr.getFileReader().peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			Account acct = loadAccount();
			if (acct == null) {
				break;
			}

			Account.currAccountBeingLoaded = acct;
		}
	}

	private Account loadAccount() {
		final QFileReader.QLine qline = new QFileReader.QLine();

		AccountType type = null;
		String name = null;
		String desc = null;
		QDate closedate = null;
		int statfreq = -1;
		int statdom = -1;

		for (;;) {
			this.qrdr.getFileReader().nextAccountLine(qline);

			switch (qline.type) {
			case EndOfSection:
				return Account.makeAccount(name, type, desc, closedate, statfreq, statdom);

			case AcctType:
				if (type == null) {
					type = AccountType.parseAccountType(qline.value);
				}
				break;
			case AcctCreditLimit:
				// creditLimit = Common.getDecimal(qline.value);
				break;
			case AcctDescription:
				desc = qline.value;
				break;
			case AcctName:
				name = qline.value;
				break;
			case AcctStmtDate:
				// stmtDate = Common.GetDate(qline.value);
				break;
			case AcctStmtBal:
				// stmtBalance = Common.getDecimal(qline.value);
				break;
			case AcctCloseDate:
				closedate = Common.parseQDate(qline.value);
				break;
			case AcctStmtFrequency:
				statfreq = Integer.parseInt(qline.value);
				break;
			case AcctStmtDay:
				statdom = Integer.parseInt(qline.value);
				break;

			default:
				Common.reportError("syntax error");
			}
		}
	}
}