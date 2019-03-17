package qif.importer;

import qif.data.Account;
import qif.data.AccountType;
import qif.data.Common;
import qif.data.QDate;

public class AccountDetailsFixer {
	/** Repair Quicken's confusion about specific account types */
	public static AccountType fixType(String name, AccountType type) {
		if (name.endsWith("Checking")) {
			type = AccountType.Bank;
		} else if (name.equals("UnionNationalCD") //
				|| name.equals("Waddell & Reed")) {
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
				|| name.equals("IBM Pension")) {
			type = AccountType.Inv401k;
		} else if (name.equals("CapCheck")) {
			type = AccountType.Bank;
		}

		if (type == null) {
			Account existing = Account.findAccount(name);
			type = (existing == null) ? AccountType.Bank : existing.type;
		}

		return type;
	}

	/**
	 * When encountering an account again during data load, compare the two and
	 * report issues as necessary, or update account properties where appropriate.
	 */
	public static void updateAccount( //
			Account acct, QDate closeDate, int freq, int dom) {
		if (acct.type == null) {
			Common.reportError("Account type is null: " //
					+ "acct name '" + acct.name + "'");
		}

		if (acct.closeDate == null) {
			acct.closeDate = closeDate;
		}

		acct.statementFrequency = freq;
		acct.statementDayOfMonth = dom;
	}
}