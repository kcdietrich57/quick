
package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import qif.data.Account.AccountType;

//--------------------------------------------------------------------
//TODO
//--------------------------------------------------------------------
// 2/9/16 Use Acct ID in transactions
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
// 3/28 Verify all transaction types - Dividends, Splits
// 3/29 ShrsIn/ShrsOut (xfer)
// 4/2 Share balance in account positions
// 4/2 Security positions (all accounts)
// 4/3 Security position by account and security for any date
// Security price history
//
// Splits
// Dump portfolio for each month (positions)
// Portfolio market value
// Associate security sales with purchases (lots)
// ShrsIn/ShrsOut - add/remove
// Track cost basis/gain/loss
// ESPP grants
// Extra ESPP tax info
// Options - Grant, Vest, Exercise, Expire
// Include vested options in portfolio
// Optionally include non-vested options in portfolio (separately, perhaps)
// Exclude expired options
// Statements - store separately
// Specify expected statements per account
// Prompt for info for missing statements; persist information
// Investment statement with additional info for securities
// Persist info in extended QIF files
//
// Assets
// Loans
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
	private final List<Category> categories;
	private final List<Security> securities;

	private final List<Account> accounts;
	private final List<Account> accounts_bytime;

	SecurityPortfolio portfolio;

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

		this.portfolio = new SecurityPortfolio();

		// this.classes = new ArrayList<Class>();
		// this.memorizedTxns = new ArrayList<MemorizedTxn>();
	}

	public QifDom(QifDom other) {
		this();

		for (final Category c : other.categories) {
			if (c != null) {
				addCategory(new Category(c));
			}
		}

		for (final Security s : other.securities) {
			if (s != null) {
				addSecurity(new Security(s));
			}
		}

		for (final Account a : other.accounts) {
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

	public List<GenericTxn> getAllTransactions() {
		final List<GenericTxn> txns = new ArrayList<GenericTxn>();

		for (final Account a : this.accounts) {
			if (a == null) {
				continue;
			}
			txns.addAll(a.transactions);

			final Comparator<GenericTxn> cpr = (o1, o2) -> {
				return o1.getDate().compareTo(o2.getDate());
			};

			Collections.sort(txns, cpr);
		}

		return txns;
	}

	public void addAccount(Account acct) {
		while (this.accounts.size() <= acct.id) {
			this.accounts.add(null);
		}

		this.accounts.set(acct.id, acct);
		this.accounts_bytime.add(acct);

		this.currAccount = acct;

		Collections.sort(this.accounts_bytime, (a1, a2) -> {
			if (a1 == null) {
				return (a2 == null) ? 0 : 1;
			} else if (a2 == null) {
				return -1;
			}

			// Order by firsttran, lasttran
			final int ct1 = a1.transactions.size();
			final int ct2 = a2.transactions.size();

			if (ct1 == 0) {
				return (ct2 == 0) ? 0 : -1;
			} else if (ct2 == 0) {
				return 1;
			}

			final GenericTxn firsttxn1 = a1.transactions.get(0);
			final GenericTxn lasttxn1 = a1.transactions.get(ct1 - 1);
			final GenericTxn firsttxn2 = a2.transactions.get(0);
			final GenericTxn lasttxn2 = a2.transactions.get(ct2 - 1);

			int diff = firsttxn1.getDate().compareTo(firsttxn2.getDate());
			if (diff != 0) {
				return diff;
			}

			diff = lasttxn1.getDate().compareTo(lasttxn2.getDate());
			if (diff != 0) {
				return diff;
			}

			return (a1.name.compareTo(a2.name));
		});
	}

	public void updateAccount(Account oldacct, Account newacct) {
		final String msg = "Account type mismatch: " //
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
		final Category existing = findCategory(cat.name);

		if (existing != null) {
			Common.reportError("Adding duplicate category");
		}

		while (this.categories.size() <= cat.id) {
			this.categories.add(null);
		}

		this.categories.set(cat.id, cat);
	}

	public void addSecurity(Security sec) {
		final Security existing = findSecurityByName(sec.name);

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

			final Account acct = findAccount(s);

			return (short) ((acct != null) ? (-acct.id) : 0);
		}

		final Category cat = findCategory(s);

		return (cat != null) ? (cat.id) : 0;
	}

	public Security findSecurityByName(String name) {
		for (final Security sec : this.securities) {
			if (sec != null && sec.name.equals(name)) {
				return sec;
			}
		}

		return null;
	}

	public Security findSecurityBySymbol(String sym) {
		for (final Security sec : this.securities) {
			if (sec != null && sec.symbol.equalsIgnoreCase(sym)) {
				return sec;
			}
		}

		return null;
	}

	public Account findAccount(String name) {
		for (final Account acct : this.accounts) {
			if (acct != null && acct.name.equals(name)) {
				return acct;
			}
		}

		return null;
	}

	public Category findCategory(String name) {
		for (final Category cat : this.categories) {
			if ((cat != null) && cat.name.equals(name)) {
				return cat;
			}
		}

		return null;
	}

	public void reportStatusForDate(Date d, boolean itemizeAccounts) {
		System.out.println();
		System.out.println("Global status for date: " + Common.getDateString(d));
		System.out.println("----------------------------------");
		System.out.println(String.format("  %-36s : %10s", "Account", "Balance"));

		BigDecimal netWorth = BigDecimal.ZERO;

		final AccountType atypes[] = { //
				AccountType.Bank, AccountType.CCard, AccountType.Cash, //
				AccountType.Asset, AccountType.Liability, AccountType.Invest, //
				AccountType.InvPort, AccountType.Inv401k, AccountType.InvMutual //
		};

		for (final AccountType at : atypes) {
			System.out.println("======== " + at + " accounts ========");

			for (final Account a : this.accounts) {
				if ((a != null) && (a.type == at)) {
					netWorth = netWorth.add(a.reportStatusForDate(d));
				}
			}
		}

		System.out.println("Balance: " + netWorth);
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
