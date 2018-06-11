package qif.importer;

import qif.data.Account;
import qif.data.AccountType;

class QKludge {

	// Repair Quicken's confusion about these accounts
	public static void fixAccount(Account acct) {
		String name = acct.getName();

		// Fixups based on account name
		if (name.endsWith("Checking")) {
			acct.type = AccountType.Bank;
		} else if (name.equals("UnionNationalCD") //
				|| name.equals("Waddell & Reed")) {
			acct.type = AccountType.Invest;
		} else if (name.equals("Deferred 401k Match")) {
			acct.type = AccountType.Inv401k;
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
			acct.type = AccountType.Inv401k;
		} else if (name.equals("CapCheck")) {
			acct.type = AccountType.Bank;
		}
	}
}