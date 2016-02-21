
package qif.data;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
//
// Non-investment running balance
// Investment cash balance
// Investment share balance
//
// Encryption, security
// Persistence
// Register
// Reports
// Statements
// Net worth
// Graphics
//
// Look up transaction by id?
//--------------------------------------------------------------------

// Document Object Model for a QIF file.
public class QifDom {
	public static void main(String[] args) {
		String file = "/tmp/dietrich.qif";
		file = "/tmp/jk.qif";
		if (new File("c:" + file).exists()) {
			file = "c:" + file;
		}

		if (!new File(file).exists()) {
			System.out.println("Input file '" + file + "' does not exist");
			;
			return;
		}

		QifDomReader rdr = new QifDomReader();
		rdr.load(file);

		QifDom.thedom.reportAccounts();

		// System.out.println(dom);
	}

	public static QifDom thedom = null;

	public List<Account> accounts;
	public List<Account> accounts_bytime;
	public List<Category> categories;
	public List<Security> securities;

	// public List<QClass> classes;
	// public List<MemorizedTxn> memorizedTxns;

	public Account currAccount = null;

	public QifDom() {
		thedom = this;

		this.accounts = new ArrayList<Account>();
		this.accounts_bytime = new ArrayList<Account>();
		this.categories = new ArrayList<Category>();
		this.securities = new ArrayList<Security>();

		// this.classes = new ArrayList<Class>();
		// this.memorizedTxns = new ArrayList<MemorizedTxn>();
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

	private void reportAccounts() {
		System.out.println("============================");
		System.out.println("Accounts");
		System.out.println("============================");

		for (Account a : this.accounts_bytime) {
			if (a == null) {
				continue;
			}

			switch (a.type) {
			case Bank:
			case CCard:
			case Cash:
			case Asset:
			case Liability:
				reportNonInvestmentAccount(a, true);
				break;

			case Inv401k:
			case InvMutual:
			case InvPort:
			case Invest:
				reportInvestmentAccount(a);
				break;

			default:
				break;
			}
		}
	}

	private void reportNonInvestmentAccount(Account a, boolean includePseudoStatements) {
		System.out.println("----------------------------");
		System.out.println(a.name + " " + a.type + " " + a.description);
		int ntran = a.transactions.size();

		// System.out.println(" " + ntran + " transactions");

		if (ntran > 0) {
			GenericTxn ft = a.transactions.get(0);
			GenericTxn lt = a.transactions.get(ntran - 1);

			System.out.println("    Date range: " //
					+ Common.getDateString(ft.getDate()) //
					+ " - " + Common.getDateString(lt.getDate()));
			if (includePseudoStatements) {
				System.out.println("----------------------------");
			}

			int curNumTxn = 0;
			int curYear = -1;
			int curMonth = -1;
			BigDecimal bal = new BigDecimal(0);
			Calendar cal = Calendar.getInstance();

			for (GenericTxn t : a.transactions) {
				Date d = t.getDate();
				cal.setTime(d);

				if (includePseudoStatements) {
					if ((cal.get(Calendar.YEAR) != curYear) //
							|| (cal.get(Calendar.MONTH) != curMonth)) {
						System.out.println(Common.getDateString(t.getDate()) //
								+ ": " + bal //
								+ " " + curNumTxn + " transactions");

						curNumTxn = 0;
						curYear = cal.get(Calendar.YEAR);
						curMonth = cal.get(Calendar.MONTH);
					}

					++curNumTxn;
				}

				bal = bal.add(t.amount);
			}

			System.out.println("    " + ntran + " transactions");
			System.out.println("    Final: " + bal);
			// System.out.println(a.name + " " + a.type + " " + a.description);
		}

		System.out.println("----------------------------");
	}

	private void reportInvestmentAccount(Account a) {
		System.out.println("----------------------------");
		System.out.println(a.name + " " + a.type + " " + a.description);
		int ntran = a.transactions.size();

		System.out.println("" + ntran + " transactions");

		if (ntran > 0) {
			GenericTxn ft = a.transactions.get(0);
			GenericTxn lt = a.transactions.get(ntran - 1);

			System.out.println("Date range: " //
					+ Common.getDateString(ft.getDate()) //
					+ " - " + Common.getDateString(lt.getDate()));
			System.out.println("----------------------------");
		}

		System.out.println("----------------------------");
	}
};
