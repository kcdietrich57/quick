
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
// Merge/compare files
//
//Code review/cleanup
//Point-in time positions (net worth)
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
	}

	public String toString() {
		String s = "";

		s += "Categories: " + this.categories;
		s += "Accounts: " + this.accounts_bytime;
		s += "Securities: " + this.securities;

		return s;
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

	public void addAccount(Account acct) {
		Account existing = findAccount(acct.name);

		if (existing != null) {
			updateAccount(existing, acct);

			acct = existing;
		} else {
			while (this.accounts.size() <= acct.id) {
				this.accounts.add(null);
			}

			this.accounts.set(acct.id, acct);
			this.accounts_bytime.add(acct);
		}

		this.currAccount = acct;
	}

	public void addCategory(Category cat) {
		Category existing = findCategory(cat.name);

		if (existing != null) {
			// TODO skip
		} else {
			while (this.categories.size() <= cat.id) {
				this.categories.add(null);
			}

			this.categories.set(cat.id, cat);
		}
	}

	private void updateAccount(Account existing, Account newacct) {
		// TODO compare and/or update
		if (existing.type != newacct.type) {
			Common.reportWarning("Account type mismatch: " //
					+ existing.type + " vs " + newacct.type);
		}

		// System.out.println("Updating account:\n" //
		// + " " + existing //
		// + " " + newacct);
	}
};
