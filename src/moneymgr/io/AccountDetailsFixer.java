package moneymgr.io;

import moneymgr.model.Account;
import moneymgr.model.AccountType;
import moneymgr.model.MoneyMgrModel;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/** Helper class to correct account information after QIF load */
public class AccountDetailsFixer {
	private final MoneyMgrModel model;

	public AccountDetailsFixer(MoneyMgrModel model) {
		this.model = model;
	}

	/** Repair Quicken's confusion about specific account types */
	public AccountType fixType(String name, AccountType type) {
		if (name.endsWith("Checking")) {
			type = AccountType.Bank;
		} else if (name.equals("UnionNationalCD") //
				|| name.equals("Waddell & Reed") //
				|| name.equals("IBM Future Health")) {
			type = AccountType.Invest;
		} else if (name.equals("Deferred 401k Match")) {
			type = AccountType.Inv401k;
		} else if (name.equals("ATT 401k") //
				|| name.equals("CapFed IRA") //
				|| name.equals("Fidelity HSA XX5575") //
				|| name.equals("GD IRA (E*Trade)") //
				|| name.equals("GD IRA (Scottrade)") //
				|| name.equals("HEC 401k Profit Sharing") //
				|| name.equals("HEC Pension") //
				|| name.equals("HEC 401k Profit Sharing") //
				|| name.equals("Invest IRA") //
				|| name.equals("TD IRA (E*Trade)") //
				|| name.equals("TD IRA (Scottrade)") //
				|| name.equals("GD IRA Ameritrade") //
				|| name.equals("TD IRA Ameritrade") //
				|| name.equals("GD IRA Schwab") //
				|| name.equals("TD IRA Schwab") //
				|| name.equals("IBM Pension")) {
			type = AccountType.Inv401k;
		} else if (name.equals("CapCheck")) {
			type = AccountType.Bank;
		}

		if (type == null) {
			Account existing = this.model.findAccount(name);
			type = (existing == null) ? AccountType.Bank : existing.type;
		}

		return type;
	}

	/**
	 * When encountering an account again during data load, compare the two and
	 * report issues as necessary, or update account properties where appropriate.
	 */
	public void updateAccount( //
			Account acct, QDate closeDate, int freq, int dom) {
		if (acct.type == null) {
			Common.reportError("Account type is null: " //
					+ "acct name '" + acct.name + "'");
		}

		acct.setCloseDate(closeDate);
		acct.setStatementFrequency(freq, dom);
	}
}