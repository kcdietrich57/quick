package qif.importer;

import qif.data.Account;
import qif.data.AccountType;
import qif.data.Common;

class AccountProcessor {
	private int nextAccountID = Account.getNextAccountID();
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

			Account existing = Account.findAccount(acct.getName());

			if (existing != null) {
				updateAccount(existing, acct);
				Account.setCurrAccount(existing);
			} else {
				acct.acctid = this.nextAccountID++;
				Account.addAccount(acct);
			}
		}
	}

	private Account loadAccount() {
		final QFileReader.QLine qline = new QFileReader.QLine();

		Account acct = new Account();

		for (;;) {
			this.qrdr.getFileReader().nextAccountLine(qline);

			switch (qline.type) {
			case EndOfSection:
				QKludge.fixAccount(acct);

				return acct;

			case AcctType:
				if (acct.type == null) {
					acct.type = AccountType.parseAccountType(qline.value);
				}
				break;
			case AcctCreditLimit:
				// acct.creditLimit = Common.getDecimal(qline.value);
				break;
			case AcctDescription:
				acct.description = qline.value;
				break;
			case AcctName:
				acct.setName(qline.value);
				break;
			case AcctStmtDate:
				// acct.stmtDate = Common.GetDate(qline.value);
				break;
			case AcctStmtBal:
				// acct.stmtBalance = Common.getDecimal(qline.value);
				break;
			case AcctCloseDate:
				acct.closeDate = Common.parseQDate(qline.value);
				break;
			case AcctStmtFrequency:
				acct.statementFrequency = Integer.parseInt(qline.value);
				break;
			case AcctStmtDay:
				acct.statementDayOfMonth = Integer.parseInt(qline.value);
				break;

			default:
				Common.reportError("syntax error");
			}
		}
	}

	private void updateAccount(Account oldacct, Account newacct) {
		if ((oldacct.type != null) && (newacct.type != null) //
				&& (oldacct.type != newacct.type)) {
			String msg = "Account type mismatch: " //
					+ oldacct.type + " vs " + newacct.type;

			if (oldacct.isInvestmentAccount() != newacct.isInvestmentAccount()) {
				// Common.reportError(msg);
			}

			if (newacct.type != AccountType.Invest) {
				Common.reportWarning(msg);
			}
		}

		if (oldacct.closeDate == null) {
			oldacct.closeDate = newacct.closeDate;
		}

		if (oldacct.type == null) {
			oldacct.type = newacct.type;
		}

		oldacct.statementFrequency = newacct.statementFrequency;
		oldacct.statementDayOfMonth = newacct.statementDayOfMonth;
	}
}