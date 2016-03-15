package qif.data;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import qif.data.Account.AccountType;
import qif.data.QFileReader.SectionType;
import qif.data.SimpleTxn.Action;

public class QifDomReader {
	private QFileReader rdr = null;
	private QifDom dom = null;
	// private QifDom refdom = null;
	private short nextAccountID = 1;
	private short nextCategoryID = 1;
	private short nextSecurityID = 1;

	public QifDomReader() {
	}

	public QifDom load(String fileName) {
		return load(null, fileName);
	}

	public QifDom load(QifDom refdom, String fileName) {
		if (new File("c:" + fileName).exists()) {
			fileName = "c:" + fileName;
		} else if (!new File(fileName).exists()) {
			Common.reportError("Input file '" + fileName + "' does not exist");

			return null;
		}

		init(fileName, refdom);

		processFile();
		cleanUpTransactions();
		validateStatements();

		Collections.sort(this.dom.accounts_bytime, new Comparator<Account>() {
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

		balanceStatements();

		return this.dom;
	}

	private void init(String filename, QifDom refdom) {
		File f = new File(filename);
		if (!f.exists()) {
			Common.reportError("File '" + filename + "' does not exist");
		}

		// this.refdom = refdom;

		this.dom = (refdom != null) ? new QifDom(refdom) : new QifDom();
		this.rdr = new QFileReader(f);

		this.nextAccountID = 1;
		this.nextCategoryID = 1;
	}

	private void processFile() {
		this.rdr.reset();

		for (SectionType sectype = this.rdr.findFirstSection(); //
		sectype != SectionType.EndOfFile; //
		sectype = this.rdr.nextSection()) {
			switch (sectype) {
			case Tag:
			case Category:
				System.out.println("Loading categories");
				loadCategories();
				break;

			case Account:
				System.out.println("Loading accounts");
				loadAccounts();
				break;

			case Asset:
			case Liability:
			case Cash:
			case CreditCard:
			case Bank:
				System.out.println("Loading transactions for " + this.dom.currAccount.name);
				loadNonInvestmentTransactions();
				break;

			case Investment:
				System.out.println("Loading transactions for " + this.dom.currAccount.name);
				loadInvestmentTransactions();
				break;

			case Statement:
				System.out.println("Loading statements");
				loadStatements();
				break;

			case Security:
				if (this.dom.securities.isEmpty()) {
					System.out.println("Loading securities");
				}
				loadSecurities();
				break;

			case Prices:
				// System.out.println("Loading prices");
				loadPrices();
				break;

			case QClass:
				Common.reportError("TODO not implemented");
				// dom.classes.addAll(Class.read(secbody));
				break;

			case MemorizedTransaction:
				Common.reportError("TODO not implemented");
				// dom.memorizedTxns.addAll(MemorizedTxn.read(secbody));
				break;

			default:
				break;
			}
		}
	}

	private void loadCategories() {
		for (;;) {
			String s = this.rdr.peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			Category cat = loadCategory();
			if (cat == null) {
				break;
			}

			Category existing = this.dom.findCategory(cat.name);
			if (existing != null) {
				// TODO verify
			} else {
				cat.id = this.nextCategoryID++;
				dom.addCategory(cat);
			}
		}

		if (null == dom.findCategory("Fix Me")) {
			Category cat = new Category(this.nextCategoryID++);
			cat.name = "Fix Me";
			dom.addCategory(cat);
		}
	}

	public Category loadCategory() {
		QFileReader.QLine qline = new QFileReader.QLine();

		Category cat = new Category();

		for (;;) {
			this.rdr.nextCategoryLine(qline);

			switch (qline.type) {
			case EndOfSection:
				return cat;

			case CatName:
				cat.name = qline.value;
				break;
			case CatDescription:
				cat.description = qline.value;
				break;
			case CatTaxRelated:
				cat.taxRelated = Common.GetBoolean(qline.value);
				break;
			case CatIncomeCategory:
				cat.incomeCategory = Common.GetBoolean(qline.value);
				break;
			case CatExpenseCategory:
				cat.expenseCategory = Common.GetBoolean(qline.value);
				break;
			case CatBudgetAmount:
				cat.budgetAmount = Common.getDecimal(qline.value);
				break;
			case CatTaxSchedule:
				break;

			default:
				Common.reportError("syntax error");
			}
		}
	}

	private void loadAccounts() {
		for (;;) {
			String s = this.rdr.peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			Account acct = loadAccount();
			if (acct == null) {
				break;
			}

			Account existing = this.dom.findAccount(acct.name);
			if (existing != null) {
				dom.updateAccount(existing, acct);
			} else {
				acct.id = this.nextAccountID++;
				dom.addAccount(acct);
			}
		}
	}

	public Account loadAccount() {
		QFileReader.QLine qline = new QFileReader.QLine();

		Account acct = new Account();

		for (;;) {
			this.rdr.nextAccountLine(qline);

			switch (qline.type) {
			case EndOfSection:
				return acct;

			case AcctType:
				acct.type = AccountType.parse(qline.value);
				break;
			case AcctCreditLimit:
				acct.creditLimit = Common.getDecimal(qline.value);
				break;
			case AcctDescription:
				acct.description = qline.value;
				break;
			case AcctName:
				acct.name = qline.value;
				break;
			case AcctStmtDate:
				// acct.stmtDate = Common.GetDate(qline.value);
				break;
			case AcctStmtBal:
				// acct.stmtBalance = Common.getDecimal(qline.value);
				break;

			default:
				Common.reportError("syntax error");
			}
		}
	}

	private void loadSecurities() {
		for (;;) {
			String s = this.rdr.peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			Security sec = loadSecurity();
			if (sec == null) {
				break;
			}

			Security existing = this.dom.findSecurityByName(sec.name);
			if (existing != null) {
				// TODO verify
			} else {
				sec.id = this.nextSecurityID++;
				dom.securities.add(sec);
			}
		}
	}

	public Security loadSecurity() {
		QFileReader.QLine qline = new QFileReader.QLine();

		Security security = new Security();

		for (;;) {
			this.rdr.nextSecurityLine(qline);

			switch (qline.type) {
			case EndOfSection:
				return security;

			case SecName:
				security.name = qline.value;
				break;
			case SecSymbol:
				security.symbol = qline.value;
				break;
			case SecType:
				security.type = qline.value;
				break;
			case SecGoal:
				security.goal = qline.value;
				break;

			default:
				Common.reportError("syntax error");
			}
		}
	}

	private void cleanUpTransactions() {
		sortTransactions();
		cleanUpSplits();
		calculateRunningTotals();
		connectTransfers();
	}

	private void calculateRunningTotals() {
		for (Account a : this.dom.accounts_bytime) {
			BigDecimal total = new BigDecimal(0);

			for (GenericTxn t : a.transactions) {
				switch (t.getAction()) {
				case ActionStockSplit:
					break;

				default:
					total = total.add(t.getTotalAmount());
					t.runningTotal = total;
					break;
				}
			}
		}
	}

	private void sortTransactions() {
		for (Account a : dom.accounts) {
			if (a == null) {
				continue;
			}

			Comparator<GenericTxn> cmptor = new Comparator<GenericTxn>() {
				public int compare(GenericTxn t1, GenericTxn t2) {
					int diff = t1.getDate().compareTo(t2.getDate());

					if (diff != 0) {
						return diff;
					}

					return (int) (t1.id - t2.id);
				}
			};

			Collections.sort(a.transactions, cmptor);
		}
	}

	private void cleanUpSplits() {
		for (Account a : dom.accounts) {
			if (a == null) {
				continue;
			}

			for (GenericTxn txn : a.transactions) {
				massageSplits(txn);
			}
		}
	}

	private void connectTransfers() {
		for (Account a : dom.accounts) {
			if (a == null) {
				continue;
			}

			for (GenericTxn txn : a.transactions) {
				connectTransfers(txn);
			}
		}
	}

	private void connectTransfers(GenericTxn txn) {
		if ((txn instanceof NonInvestmentTxn) && !((NonInvestmentTxn) txn).split.isEmpty()) {
			for (SimpleTxn stxn : ((NonInvestmentTxn) txn).split) {
				connectTransfers(stxn, txn.getDate());
			}
		} else if ((txn.catid < 0) && //
		// opening balance shows up as xfer to same acct
		(-txn.catid != txn.acctid)) {
			connectTransfers(txn, txn.getDate());
		}
	}

	private void validateStatements() {
		for (Account a : dom.accounts) {
			if (a == null) {
				continue;
			}

			BigDecimal bal = new BigDecimal(0);
			for (Statement s : a.statements) {
				bal = bal.add(s.credits);
				bal = bal.subtract(s.debits);

				if (!bal.equals(s.balance)) {
					Common.reportError("Statement check failed: " + s);
				}
			}
		}
	}

	private void balanceStatements() {
		for (Account a : dom.accounts) {
			if ((a == null) //
					// TODO partly unimplemented
					|| !a.isNonInvestmentAccount()) {
				continue;
			}

			BigDecimal balance = new BigDecimal(0);

			for (Statement s : a.statements) {
				if (!balanceStatement(a, balance, s)) {
					System.out.println("Can't balance account: " + a);
					System.out.println(" Stmt: " + s);
					break;
				}

				balance = s.balance;
			}
		}
	}

	private boolean balanceStatement(Account a, BigDecimal curbal, Statement s) {
		List<GenericTxn> txns = gatherTransactionsForStatement(a, s);
		List<GenericTxn> uncleared = null;

		// curbal + totaltx = s.balance
		BigDecimal totaltx = sumAmounts(txns);
		BigDecimal diff = totaltx.add(curbal).subtract(s.balance);

		if (diff.signum() != 0) {
			uncleared = findSubsetTotaling(txns, diff);
			if (uncleared.isEmpty()) {
				return false;
			}

			txns.removeAll(uncleared);
		}

		clearTransactions(txns, s);

		System.out.println();
		System.out.println("-------------------------------------------------------");
		System.out.println("Reconciled statement: " + a.name //
				+ " " + Common.getDateString(s.date) //
				+ " cr=" + s.credits + " db=" + s.debits //
				+ " bal=" + s.balance);

		for (GenericTxn t : txns) {
			System.out.println(t.toStringShort());
		}

		System.out.println();
		System.out.println("Uncleared transactions:");

		if (uncleared != null) {
			for (GenericTxn t : uncleared) {
				System.out.println(t.toString());
			}
		}

		return true;
	}

	private List<GenericTxn> findSubsetTotaling(List<GenericTxn> txns, BigDecimal diff) {
		List<List<GenericTxn>> matches = null;

		for (int nn = 1; nn < txns.size(); ++nn) {
			List<List<GenericTxn>> subsets = findSubsetsTotaling(txns, diff, nn, txns.size());

			if (!subsets.isEmpty()) {
				matches = subsets;
				break;
			}
		}

		return (matches == null) ? null : matches.get(0);
	}

	private List<List<GenericTxn>> findSubsetsTotaling( //
			List<GenericTxn> txns, BigDecimal tot, int nn, int max) {
		List<List<GenericTxn>> ret = new ArrayList<List<GenericTxn>>();
		if (nn >= txns.size()) {
			return ret;
		}

		List<GenericTxn> txns_work = new ArrayList<>();
		txns_work.addAll(txns);

		for (int ii = max - 1; ii >= 0; --ii) {
			BigDecimal newtot = tot.subtract(txns.get(ii).getAmount());
			GenericTxn t = txns_work.remove(ii);

			if ((nn == 1) && (newtot.signum() == 0)) {
				List<GenericTxn> l = new ArrayList<GenericTxn>();
				l.add(t);
				ret.add(l);

				return ret;
			}

			if (nn > 1 && nn <= ii) {
				List<List<GenericTxn>> subsets = findSubsetsTotaling(txns_work, newtot, nn - 1, ii);
				txns_work.add(ii, t);

				if (!subsets.isEmpty()) {
					for (List<GenericTxn> l : subsets) {
						l.add(t);
					}

					ret.addAll(subsets);

					return ret;
				}
			}
		}

		return ret;
	}

	private void clearTransactions(List<GenericTxn> txns, Statement s) {
		for (GenericTxn t : txns) {
			t.stmtdate = s.date;
		}
	}

	private BigDecimal sumAmounts(List<GenericTxn> txns) {
		BigDecimal totaltx = new BigDecimal(0);
		for (GenericTxn t : txns) {
			totaltx = totaltx.add(t.getAmount());
		}

		return totaltx;
	}

	private List<GenericTxn> gatherTransactionsForStatement(Account a, Statement s) {
		List<GenericTxn> txns = new ArrayList<GenericTxn>();

		int idx1 = a.findFirstNonClearedTransaction();
		if (idx1 < 0) {
			return txns;
		}

		for (int ii = idx1; ii <= a.transactions.size(); ++ii) {
			GenericTxn t = a.transactions.get(ii);

			if (t.getDate().compareTo(s.date) > 0) {
				break;
			}

			if (!t.isCleared()) {
				txns.add(t);
			}
		}

		return txns;
	}

	private void loadInvestmentTransactions() {
		for (;;) {
			String s = this.rdr.peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			InvestmentTxn txn = loadInvestmentTransaction();
			if (txn == null) {
				break;
			}

			dom.currAccount.addTransaction(txn);
		}
	}

	public InvestmentTxn loadInvestmentTransaction() {
		QFileReader.QLine qline = new QFileReader.QLine();

		InvestmentTxn txn = new InvestmentTxn(dom.currAccount.id);

		for (;;) {
			this.rdr.nextInvLine(qline);

			switch (qline.type) {
			case EndOfSection:
				txn.repair();
				return txn;

			case InvTransactionAmt: {
				BigDecimal amt = Common.getDecimal(qline.value);

				if (txn.getAmount() != null) {
					if (!txn.getAmount().equals(amt)) {
						Common.reportError("Inconsistent amount: " + qline.value);
					}
				} else {
					txn.setAmount(amt);
				}

				break;
			}
			case InvAction:
				txn.action = qline.value;
				break;
			case InvClearedStatus:
				txn.clearedStatus = qline.value;
				break;
			case InvCommission:
				txn.commission = Common.getDecimal(qline.value);
				break;
			case InvDate:
				txn.setDate(Common.GetDate(qline.value));
				break;
			case InvMemo:
				txn.memo = qline.value;
				break;
			case InvPrice:
				txn.price = Common.getDecimal(qline.value);
				break;
			case InvQuantity:
				txn.quantity = Common.getDecimal(qline.value);
				break;
			case InvSecurity:
				txn.security = qline.value;
				break;
			case InvFirstLine:
				txn.textFirstLine = qline.value;
				break;
			case InvXferAmt:
				txn.amountTransferred = Common.getDecimal(qline.value);
				break;
			case InvXferAcct:
				txn.accountForTransfer = qline.value;
				txn.xacctid = dom.findCategoryID(qline.value);
				break;

			default:
				Common.reportError("syntax error; txn: " + qline);
			}
		}
	}

	private void loadNonInvestmentTransactions() {
		for (;;) {
			String s = this.rdr.peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			NonInvestmentTxn txn = loadNonInvestmentTransaction();
			if (txn == null) {
				break;
			}

			txn.verifySplit();

			dom.currAccount.addTransaction(txn);
		}
	}

	public NonInvestmentTxn loadNonInvestmentTransaction() {
		QFileReader.QLine qline = new QFileReader.QLine();

		NonInvestmentTxn txn = new NonInvestmentTxn(dom.currAccount.id);
		SimpleTxn cursplit = null;

		for (;;) {
			this.rdr.nextTxnLine(qline);

			switch (qline.type) {
			case EndOfSection:
				return txn;

			case TxnCategory:
				txn.catid = dom.findCategoryID(qline.value);

				if (txn.catid == 0) {
					Common.reportError("Can't find xtxn: " + qline.value);
				}
				break;
			case TxnAmount: {
				BigDecimal amt = Common.getDecimal(qline.value);

				if (txn.getAmount() != null) {
					if (!txn.getAmount().equals(amt)) {
						Common.reportError("Inconsistent amount: " + qline.value);
					}
				} else {
					txn.setAmount(amt);
				}

				break;
			}
			case TxnMemo:
				txn.memo = qline.value;
				break;

			case TxnDate:
				txn.setDate(Common.GetDate(qline.value));
				break;
			case TxnClearedStatus:
				txn.clearedStatus = qline.value;
				break;
			case TxnNumber:
				txn.chkNumber = qline.value;
				break;
			case TxnPayee:
				txn.payee = qline.value;
				break;
			case TxnAddress:
				txn.address.add(qline.value);
				break;

			case TxnSplitCategory:
				if (cursplit == null || cursplit.catid != 0) {
					cursplit = new SimpleTxn(txn.acctid);
					txn.split.add(cursplit);
				}

				if (qline.value == null || qline.value.trim().isEmpty()) {
					qline.value = "Fix Me";
				}
				cursplit.catid = dom.findCategoryID(qline.value);

				if (cursplit.catid == 0) {
					Common.reportError("Can't find xtxn: " + qline.value);
				}
				break;
			case TxnSplitAmount:
				if (cursplit == null || cursplit.getAmount() != null) {
					txn.split.add(cursplit);
					cursplit = new SimpleTxn(txn.acctid);
				}

				cursplit.setAmount(Common.getDecimal(qline.value));
				break;
			case TxnSplitMemo:
				if (cursplit != null) {
					cursplit.memo = qline.value;
				}
				break;

			default:
				Common.reportError("syntax error; txn: " + qline);
			}
		}
	}

	private void loadPrices() {
		for (;;) {
			String s = this.rdr.peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			Price price = Price.load(this.rdr);
			if (price == null) {
				break;
			}

			Security sec = dom.findSecurityBySymbol(price.symbol);
			sec.addPrice(price);
		}
	}

	private void loadStatements() {
		for (;;) {
			String s = this.rdr.peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			Statement stmt = Statement.load(this.rdr, dom.currAccount.id);
			if (stmt == null) {
				break;
			}

			dom.currAccount.statements.add(stmt);
		}
	}

	private void massageSplits(GenericTxn txn) {
		if (!(txn instanceof NonInvestmentTxn)) {
			return;
		}

		NonInvestmentTxn nitxn = (NonInvestmentTxn) txn;

		for (int ii = 0; ii < nitxn.split.size(); ++ii) {
			SimpleTxn stxn = nitxn.split.get(ii);
			if (stxn.catid >= 0) {
				continue;
			}

			MultiSplitTxn mtxn = null;

			for (int jj = ii + 1; jj < nitxn.split.size(); ++jj) {
				SimpleTxn stxn2 = nitxn.split.get(jj);

				if (stxn.catid == stxn2.catid) {
					if (mtxn == null) {
						mtxn = new MultiSplitTxn(txn.acctid);
						nitxn.split.set(ii, mtxn);

						mtxn.setAmount(stxn.getAmount());
						mtxn.catid = stxn.catid;
						mtxn.subsplits.add(stxn);
					}

					mtxn.setAmount(mtxn.getAmount().add(stxn2.getAmount()));
					mtxn.subsplits.add(stxn2);

					nitxn.split.remove(jj);
					--jj;
				}
			}
		}
	}

	private static int totalXfers = 0;
	private static int failedXfers = 0;

	private void connectTransfers(SimpleTxn txn, Date date) {
		// Opening balance appears as a transfer to the same acct
		if ((txn.catid >= 0) || (txn.catid == -txn.getXferAcctid())) {
			return;
		}

		Account a = dom.accounts.get(-txn.catid);

		findMatches(a, txn, date, true);

		++totalXfers;

		if (this.matchingTxns.isEmpty()) {
			findMatches(a, txn, date, false); // SellX openingBal void
		}

		if (this.matchingTxns.isEmpty()) {
			++failedXfers;
			System.out.println("match not found for xfer: " + txn);
			System.out.println("  " + failedXfers + " of " + totalXfers + " failed");
			return;
		}

		SimpleTxn xtxn = null;
		if (this.matchingTxns.size() == 1) {
			xtxn = this.matchingTxns.get(0);
		} else {
			// TODO choose one more deliberately
			xtxn = this.matchingTxns.get(0);
		}

		txn.xtxn = xtxn;
		xtxn.xtxn = txn;
	}

	private final List<SimpleTxn> matchingTxns = new ArrayList<SimpleTxn>();

	private void findMatches(Account acct, SimpleTxn txn, Date date) {
		findMatches(acct, txn, date, false);
	}

	private void findMatches(Account acct, SimpleTxn txn, Date date, boolean strict) {
		this.matchingTxns.clear();

		int idx = findDateRange(acct, date);
		if (idx < 0) {
			return;
		}

		boolean datematch = false;

		for (int inc = 0; datematch || (inc < 10); ++inc) {
			datematch = false;

			if (idx + inc < acct.transactions.size()) {
				GenericTxn gtxn = acct.transactions.get(idx + inc);
				datematch = date.equals(gtxn.getDate());

				SimpleTxn match = checkMatch(txn, gtxn, strict);
				if (match != null) {
					this.matchingTxns.add(match);
				}
			}

			if (inc > 0 && idx >= inc) {
				GenericTxn gtxn = acct.transactions.get(idx - inc);
				datematch = datematch || date.equals(gtxn.getDate());

				SimpleTxn match = checkMatch(txn, gtxn, strict);
				if (match != null) {
					this.matchingTxns.add(match);
				}
			}
		}
	}

	private SimpleTxn checkMatch(SimpleTxn txn, GenericTxn gtxn) {
		return checkMatch(txn, gtxn, false);
	}

	private SimpleTxn checkMatch(SimpleTxn txn, GenericTxn gtxn, boolean strict) {
		assert -txn.catid == gtxn.acctid;

		if (!gtxn.hasSplits()) {
			if ((gtxn.getXferAcctid() == txn.acctid) //
					&& amountIsEqual(gtxn, txn, strict)) {
				return gtxn;
			}
		} else {
			for (SimpleTxn splitxn : gtxn.getSplits()) {
				if ((splitxn.getXferAcctid() == txn.acctid) //
						&& amountIsEqual(splitxn, txn, strict)) {
					return splitxn;
				}
			}
		}

		return null;
	}

	int cashok = 0;
	int cashbad = 0;

	private boolean amountIsEqual(SimpleTxn txn1, SimpleTxn txn2, boolean strict) {
		BigDecimal amt1 = txn1.getXferAmount();
		BigDecimal amt2 = txn2.getXferAmount();

		if (amt1.abs().compareTo(amt2.abs()) != 0) {
			return false;
		}

		if (BigDecimal.ZERO.compareTo(amt1) == 0) {
			return true;
		}

		// We know the magnitude is the same and non-zero
		// Check whether they are equal or negative of each other
		boolean eq = amt1.equals(amt2);

		boolean ret = !eq || !strict;

		if ((txn1.getAction() == Action.ActionCash) //
				|| (txn2.getAction() == Action.ActionCash)) {
			if (eq) {
				++cashbad;
			} else {
				++cashok;
			}

			System.out.println(txn1.toString());
			System.out.println(txn2.toString());
			System.out.println("Cash ok=" + cashok + " bad=" + cashbad);

			return ret;
		}

		return ret;
	}

	private static int findDateRange(Account acct, Date date) {
		if (acct.transactions.isEmpty()) {
			return -1;
		}

		int loidx = 0;
		int hiidx = acct.transactions.size() - 1;
		Date loval = acct.transactions.get(loidx).getDate();
		Date hival = acct.transactions.get(hiidx).getDate();
		if (loval.compareTo(date) >= 0) {
			return loidx;
		}
		if (hival.compareTo(date) <= 0) {
			return hiidx;
		}

		while (loidx < hiidx) {
			int idx = (loidx + hiidx) / 2;
			if (idx <= loidx || idx >= hiidx) {
				return idx;
			}
			Date val = acct.transactions.get(idx).getDate();

			if (val.compareTo(date) < 0) {
				loidx = idx;
			} else if (val.compareTo(date) > 0) {
				hiidx = idx;
			} else {
				while ((idx > 0) //
						&& (acct.transactions.get(idx - 1).getDate().equals(date))) {
					--idx;
				}

				return idx;
			}
		}

		return loidx;
	}

	// public static void Export(QifDom dom, String fileName) {
	// if (File.Exists(fileName)) {
	// File.SetAttributes(fileName, FileAttributes.Normal);
	// }
	//
	// StreamWriter writer = new StreamWriter(fileName);
	// writer.AutoFlush = true;
	//
	// Category.Export(writer, dom.categories);
	// Class.Export(writer, dom.classes);
	// Account.Export(writer, dom.accounts);
	// MemorizedTransaction.Export(writer, dom.memorizedTxns);
	//
	// Asset.Export(writer, dom.assetTxns);
	// BankTxn.Export(writer, dom.bankTxns);
	// CashTxn.Export(writer, dom.cashTxns);
	// CreditCardTxn.Export(writer, dom.creditCardTxns);
	// InvestmentTxn.Export(writer, dom.investmentTxns);
	// LiabilityTxn.Export(writer, dom.liabilityTxns);
	// }
}