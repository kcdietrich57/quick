
package qif.data;

import java.util.ArrayList;
import java.util.List;

//--------------------------------------------------------------------
//TODO
//--------------------------------------------------------------------
// 2/9 Use Acct ID in transactions
// 2/9 Use Category ID in transactions
// 2/9 Create Split transactions with IDs
// 2/9 Verify split consistency
// 2/9 Sort transactions by date
// 2/11 Connect xfer transactions
// 2/11 Security, prices
// 2/12 Account type
// 2/12 Account summary
// 2/13 Non-Investment Statements
// 2/16 Balance Non-Investment Statements
// 2/20 Relaxed loading, fix load bugs, better errors, load old files
// 2/21 Add to git
// 2/22 Cloning Category/Security/Account info in new file (prep for merge)
// Merge/compare files
// Synchronize data with updated qif file
//
//Code review/cleanup
//Point-in time positions (net worth)
//Handle investment amounts and transfers
//Investment cash balance
//Investment transactions - share balance
// Non-investment register - running balance
//
// Encryption, security
// Persistence
// GUI Register
// Reports
// Graphs
//
// Look up transaction by id?
//--------------------------------------------------------------------

// Document Object Model for a QIF file.
public class QifDom {
	public List<Account> accounts;
	public List<Account> accounts_bytime;
	public List<Category> categories;
	public List<Security> securities;

	// public List<QClass> classes;
	// public List<MemorizedTxn> memorizedTxns;

	public Account currAccount = null;

	public QifDom() {
		this.categories = new ArrayList<Category>();
		this.accounts = new ArrayList<Account>();
		this.accounts_bytime = new ArrayList<Account>();
		this.securities = new ArrayList<Security>();

		// this.classes = new ArrayList<Class>();
		// this.memorizedTxns = new ArrayList<MemorizedTxn>();
	}

	public QifDom(QifDom other) {
		this();

		for (Category c : other.categories) {
			if (c != null) {
				addCategory(new Category(c));
			}
		}

		for (Security s : other.securities) {
			if (s != null) {
				addSecurity(new Security(s));
			}
		}

		for (Account a : other.accounts) {
			if (a != null) {
				addAccount(new Account(a));
			}
		}
	}

	public void addAccount(Account acct) {
		while (this.accounts.size() <= acct.id) {
			this.accounts.add(null);
		}

		this.accounts.set(acct.id, acct);
		this.accounts_bytime.add(acct);

		this.currAccount = acct;
	}

	public void updateAccount(Account oldacct, Account newacct) {
		// TODO better compare and/or update
		if (oldacct.type != newacct.type) {
			Common.reportWarning("Account type mismatch: " //
					+ oldacct.type + " vs " + newacct.type);
		}

		this.currAccount = oldacct;

		// System.out.println("Updating account:\n" //
		// + " " + existing //
		// + " " + newacct);
	}

	public void addCategory(Category cat) {
		Category existing = findCategory(cat.name);

		if (existing != null) {
			Common.reportError("Adding duplicate category");
		}

		while (this.categories.size() <= cat.id) {
			this.categories.add(null);
		}

		this.categories.set(cat.id, cat);
	}

	public void addSecurity(Security sec) {
		Security existing = findSecurityByName(sec.name);

		if (existing != null) {
			Common.reportError("Adding duplicate security");
		}

		while (this.securities.size() <= sec.id) {
			this.securities.add(null);
		}

		this.securities.set(sec.id, sec);
	}

	public short findCategoryID(String s) {
		if (s.startsWith("[")) {
			s = s.substring(1, s.length() - 1).trim();

			Account acct = findAccount(s);

			return (short) ((acct != null) ? (-acct.id) : 0);
		}

		Category cat = findCategory(s);

		return (short) ((cat != null) ? (cat.id) : 0);
	}

	public Security findSecurityByName(String name) {
		for (Security sec : this.securities) {
			if (sec != null && sec.name.equals(name)) {
				return sec;
			}
		}

		return null;
	}

	public Security findSecurityBySymbol(String sym) {
		for (Security sec : this.securities) {
			if (sec != null && sec.symbol.equals(sym)) {
				return sec;
			}
		}

		return null;
	}

	public Account findAccount(String name) {
		for (Account acct : this.accounts) {
			if (acct != null && acct.name.equals(name)) {
				return acct;
			}
		}

		return null;
	}

	public Category findCategory(String name) {
		for (Category cat : this.categories) {
			if ((cat != null) && cat.name.equals(name)) {
				return cat;
			}
		}

		return null;
	}

	public String toString() {
		String s = "";

		s += "Categories: " + this.categories;
		s += "Accounts: " + this.accounts_bytime;
		s += "Securities: " + this.securities;

		return s;
	}
};
