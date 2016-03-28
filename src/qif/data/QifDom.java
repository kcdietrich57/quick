
package qif.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import qif.data.Account.AccountType;

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
// 2/28 Running cash balance
// 3/5 Non-investment register - running balance
// 3/27 Investment cash balance
// 3/27 Investment transactions - share balance
//
// Track investments - lots, share xfers, cost basis/gain/loss
//
// Merge/compare files
// Synchronize data with updated qif file
//
//Code review/cleanup - ids for more fields?
//Point-in time positions (net worth)
//Handle investment amounts and transfers
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
	private static short nextdomid = 1;
	private static List<QifDom> doms = new ArrayList<QifDom>();

	public static QifDom getDomById(int domid) {
		return ((domid >= 0) && (doms.size() > domid)) ? doms.get(domid) : null;
	}

	public final short domid;
	private List<Account> accounts;
	private List<Account> accounts_bytime;
	private List<Category> categories;
	private List<Security> securities;

	// public List<QClass> classes;
	// public List<MemorizedTxn> memorizedTxns;

	public Account currAccount = null;

	public QifDom() {
		this.domid = nextdomid++;

		while (doms.size() < this.domid) {
			doms.add(null);
		}

		doms.add(this);

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
				addAccount(new Account(a, this));
			}
		}
	}

	public int getNumAccounts() {
		return this.accounts.size() - 1;
	}

	public int getNumCategories() {
		return this.categories.size() - 1;
	}

	public int getNumSecurities() {
		return this.securities.size() - 1;
	}

	public Account getAccount(int acctid) {
		return this.accounts.get(acctid);
	}

	public Account getAccountByTime(int acctid) {
		return this.accounts_bytime.get(acctid);
	}

	public Category getCategory(int catid) {
		return this.categories.get(catid);
	}

	public void addAccount(Account acct) {
		while (this.accounts.size() <= acct.id) {
			this.accounts.add(null);
		}

		this.accounts.set(acct.id, acct);
		this.accounts_bytime.add(acct);

		this.currAccount = acct;

		Collections.sort(this.accounts_bytime, new Comparator<Account>() {
			public int compare(Account a1, Account a2) {
				if (a1 == null) {
					return (a2 == null) ? 0 : 1;
				} else if (a2 == null) {
					return -1;
				}

				// Order by firsttran, lasttran
				int ct1 = a1.transactions.size();
				int ct2 = a2.transactions.size();

				if (ct1 == 0) {
					return (ct2 == 0) ? 0 : -1;
				} else if (ct2 == 0) {
					return 1;
				}

				GenericTxn firsttxn1 = a1.transactions.get(0);
				GenericTxn lasttxn1 = a1.transactions.get(ct1 - 1);
				GenericTxn firsttxn2 = a2.transactions.get(0);
				GenericTxn lasttxn2 = a2.transactions.get(ct2 - 1);

				int diff = firsttxn1.getDate().compareTo(firsttxn2.getDate());
				if (diff != 0) {
					return diff;
				}

				diff = lasttxn1.getDate().compareTo(lasttxn2.getDate());
				if (diff != 0) {
					return diff;
				}

				return (a1.name.compareTo(a2.name));
			}
		});
	}

	public void updateAccount(Account oldacct, Account newacct) {
		String msg = "Account type mismatch: " //
				+ oldacct.type + " vs " + newacct.type;

		if (oldacct.type != newacct.type) {
			if (oldacct.isInvestmentAccount() != newacct.isInvestmentAccount()) {
				Common.reportError(msg);
			}

			if (newacct.type != AccountType.Invest) {
				Common.reportWarning(msg);
			}
		}

		this.currAccount = oldacct;
	}

	public Account getAccount(short acctid) {
		return this.accounts.get(acctid);
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

		s += "ID: " + this.domid;
		s += "Categories: " + this.categories;
		s += "Accounts: " + this.accounts_bytime;
		s += "Securities: " + this.securities;

		return s;
	}
};
