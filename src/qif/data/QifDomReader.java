package qif.data;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

	private final List<SimpleTxn> matchingTxns = new ArrayList<SimpleTxn>();

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

		processSecurities();
		balanceStatements();

		fixPortfolios();

		return this.dom;
	}

	private void init(String filename, QifDom refdom) {
		final File f = new File(filename);
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
				// System.out.println("Loading categories");
				loadCategories();
				break;

			case Account:
				// System.out.println("Loading accounts");
				loadAccounts();
				break;

			case Asset:
			case Liability:
			case Cash:
			case CreditCard:
			case Bank:
				// System.out.println("Loading transactions for " +
				// this.dom.currAccount.name);
				loadNonInvestmentTransactions();
				break;

			case Investment:
				// System.out.println("Loading transactions for " +
				// this.dom.currAccount.name);
				loadInvestmentTransactions();
				break;

			case Statement:
				// System.out.println("Loading statements");
				loadStatements();
				break;

			case Security:
				// if (this.dom.securities.isEmpty()) {
				// System.out.println("Loading securities");
				// }
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
			final String s = this.rdr.peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			final Category cat = loadCategory();
			if (cat == null) {
				break;
			}

			final Category existing = this.dom.findCategory(cat.name);
			if (existing != null) {
				// TODO verify
			} else {
				cat.id = this.nextCategoryID++;
				this.dom.addCategory(cat);
			}
		}

		if (null == this.dom.findCategory("Fix Me")) {
			final Category cat = new Category(this.nextCategoryID++);
			cat.name = "Fix Me";
			this.dom.addCategory(cat);
		}
	}

	public Category loadCategory() {
		final QFileReader.QLine qline = new QFileReader.QLine();

		final Category cat = new Category();

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
				cat.taxRelated = Common.parseBoolean(qline.value);
				break;
			case CatIncomeCategory:
				cat.incomeCategory = Common.parseBoolean(qline.value);
				break;
			case CatExpenseCategory:
				cat.expenseCategory = Common.parseBoolean(qline.value);
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
			final String s = this.rdr.peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			final Account acct = loadAccount();
			if (acct == null) {
				break;
			}

			final Account existing = this.dom.findAccount(acct.name);
			if (existing != null) {
				this.dom.updateAccount(existing, acct);
			} else {
				acct.id = this.nextAccountID++;
				this.dom.addAccount(acct);
			}
		}
	}

	public Account loadAccount() {
		final QFileReader.QLine qline = new QFileReader.QLine();

		final Account acct = new Account(this.dom);

		for (;;) {
			this.rdr.nextAccountLine(qline);

			switch (qline.type) {
			case EndOfSection:
				return acct;

			case AcctType:
				acct.type = parseAccountType(qline.value);
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

	public static AccountType parseAccountType(String s) {
		switch (s.charAt(0)) {
		case 'B':
			if (s.equals("Bank")) {
				return AccountType.Bank;
			}
			break;
		case 'C':
			if (s.equals("CCard")) {
				return AccountType.CCard;
			}
			if (s.equals("Cash")) {
				return AccountType.Cash;
			}
			break;
		case 'I':
			if (s.equals("Invst")) {
				return AccountType.Invest;
			}
			break;
		case 'M':
			if (s.equals("Mutual")) {
				return AccountType.InvMutual;
			}
			break;
		case 'O':
			if (s.equals("Oth A")) {
				return AccountType.Asset;
			}
			if (s.equals("Oth L")) {
				return AccountType.Liability;
			}
			break;
		case 'P':
			if (s.equals("Port")) {
				return AccountType.InvPort;
			}
			break;
		case '4':
			if (s.equals("401(k)/403(b)")) {
				return AccountType.Inv401k;
			}
			break;
		}

		Common.reportError("Unknown account type: " + s);
		return AccountType.Bank;
	}

	private void loadSecurities() {
		for (;;) {
			final String s = this.rdr.peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			final Security sec = loadSecurity();
			if (sec == null) {
				break;
			}

			final Security existing = this.dom.findSecurityByName(sec.name);
			if (existing != null) {
				// TODO verify
			} else {
				sec.id = this.nextSecurityID++;
				this.dom.addSecurity(sec);
			}
		}
	}

	public Security loadSecurity() {
		final QFileReader.QLine qline = new QFileReader.QLine();

		final Security security = new Security();

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
		connectSecurityTransfers();
	}

	private void calculateRunningTotals() {
		for (int idx = 0; idx < this.dom.getNumAccounts(); ++idx) {
			final Account a = this.dom.getAccountByTime(idx);

			a.clearedBalance = a.balance = BigDecimal.ZERO;

			for (final GenericTxn t : a.transactions) {
				BigDecimal amt = t.getTotalAmount();
				if (t instanceof InvestmentTxn) {
					switch (t.getAction()) {
					case BUY:
					case SELL:
						break;

					case SHRS_IN:
					case SHRS_OUT: // no xfer info?
					case BUYX:
					case SELLX:
					case REINV_DIV:
					case REINV_INT:
					case REINV_LG:
					case REINV_SH:
					case GRANT:
					case VEST:
					case EXERCISEX:
					case EXPIRE:
					case STOCKSPLIT:
						// No net cash change
						continue;

					case CASH:
					case CONTRIBX:
					case DIV:
					case INT_INC:
					case MISC_INCX:
					case OTHER:
					case REMINDER:
					case WITHDRAWX:
					case XIN:
					case XOUT:
						break;
					}
				}

				switch (t.getAction()) {
				case STOCKSPLIT:
					break;

				case BUY:
					// TODO take care of this in transaction instead?
					amt = amt.negate();

					// fall through

				default:
					a.balance = a.balance.add(amt);
					t.runningTotal = a.balance;

					if (t.isCleared()) {
						a.clearedBalance = a.clearedBalance.add(amt);
					}
					break;
				}
			}
		}
	}

	private void sortTransactions() {
		for (int acctid = 1; acctid <= this.dom.getNumAccounts(); ++acctid) {
			final Account a = this.dom.getAccount(acctid);
			if (a == null) {
				continue;
			}

			final Comparator<GenericTxn> cmptor = (t1, t2) -> {
				final int diff = t1.getDate().compareTo(t2.getDate());

				if (diff != 0) {
					return diff;
				}

				return t1.id - t2.id;
			};

			Collections.sort(a.transactions, cmptor);
		}
	}

	private void cleanUpSplits() {
		for (int acctid = 1; acctid <= this.dom.getNumAccounts(); ++acctid) {
			final Account a = this.dom.getAccount(acctid);
			if (a == null) {
				continue;
			}

			for (final GenericTxn txn : a.transactions) {
				massageSplits(txn);
			}
		}
	}

	private void connectTransfers() {
		for (int acctid = 1; acctid <= this.dom.getNumAccounts(); ++acctid) {
			final Account a = this.dom.getAccount(acctid);
			if (a == null) {
				continue;
			}

			for (final GenericTxn txn : a.transactions) {
				connectTransfers(txn);
			}
		}
	}

	private void connectTransfers(GenericTxn txn) {
		if ((txn instanceof NonInvestmentTxn) && !((NonInvestmentTxn) txn).split.isEmpty()) {
			for (final SimpleTxn stxn : ((NonInvestmentTxn) txn).split) {
				connectTransfers(stxn, txn.getDate());
			}
		} else if ((txn.catid < 0) && //
		// opening balance shows up as xfer to same acct
		(-txn.catid != txn.acctid)) {
			connectTransfers(txn, txn.getDate());
		}
	}

	private void fixPortfolios() {
		fixPortfolio(this.dom.portfolio);

		for (int acctid = 1; acctid <= this.dom.getNumAccounts(); ++acctid) {
			final Account a = this.dom.getAccount(acctid);

			if (a.isInvestmentAccount()) {
				fixPortfolio(a.securities);
			}
		}
	}

	private void fixPortfolio(SecurityPortfolio port) {
		for (final SecurityPosition pos : port.positions) {
			fixPosition(pos);
		}
	}

	private void fixPosition(SecurityPosition p) {
		BigDecimal shrbal = BigDecimal.ZERO;
		p.shrBalance.clear();

		for (final InvestmentTxn t : p.transactions) {
			if (t.getAction() == Action.STOCKSPLIT) {
				shrbal = shrbal.multiply(t.quantity);
				shrbal = shrbal.divide(BigDecimal.TEN);
			} else if (t.quantity != null) {
				shrbal = shrbal.add(t.quantity);
			}

			p.shrBalance.add(shrbal);
		}
	}

	private void connectSecurityTransfers() {
		final List<InvestmentTxn> xins = new ArrayList<InvestmentTxn>();
		final List<InvestmentTxn> xouts = new ArrayList<InvestmentTxn>();

		for (int acctid = 1; acctid <= this.dom.getNumAccounts(); ++acctid) {
			final Account a = this.dom.getAccount(acctid);
			if (!a.isInvestmentAccount()) {
				continue;
			}

			for (final GenericTxn txn : a.transactions) {
				if (!(txn instanceof InvestmentTxn)) {
					continue;
				}

				if ((txn.getAction() == Action.SHRS_IN)) {
					xins.add((InvestmentTxn) txn);
				} else if (txn.getAction() == Action.SHRS_OUT) {
					xouts.add((InvestmentTxn) txn);
				}
			}
		}

		connectSecurityTransfers(xins, xouts);
	}

	private void connectSecurityTransfers(List<InvestmentTxn> xins, List<InvestmentTxn> xouts) {
		final Comparator<InvestmentTxn> cpr = (o1, o2) -> {
			int diff;

			diff = o1.getDate().compareTo(o2.getDate());
			if (diff != 0) {
				return diff;
			}

			diff = o1.security.name.compareTo(o2.security.name);
			if (diff != 0) {
				return diff;
			}

			return o2.getAction().ordinal() - o1.getAction().ordinal();
		};

		final List<InvestmentTxn> txns = new ArrayList<InvestmentTxn>(xins);
		txns.addAll(xouts);
		Collections.sort(txns, cpr);
		for (final InvestmentTxn t : txns) {
			System.out.println(t);
		}

		Collections.sort(xins, cpr);
		Collections.sort(xouts, cpr);

		final List<InvestmentTxn> ins = new ArrayList<InvestmentTxn>();
		final List<InvestmentTxn> outs = new ArrayList<InvestmentTxn>();
		final List<InvestmentTxn> unmatched = new ArrayList<InvestmentTxn>();

		BigDecimal inshrs;
		BigDecimal outshrs;

		while (!xins.isEmpty()) {
			ins.clear();
			outs.clear();

			final InvestmentTxn t = xins.get(0);
			inshrs = gatherTransactionsForSecurityTransfer(ins, xins, null, t.security, t.getDate());
			outshrs = gatherTransactionsForSecurityTransfer(outs, xouts, unmatched, t.security, t.getDate());

			if (outs.isEmpty()) {
				unmatched.addAll(ins);
			} else {
				final BigDecimal inshrs2 = inshrs.setScale(3, RoundingMode.HALF_UP);
				final BigDecimal outshrs2 = outshrs.setScale(3, RoundingMode.HALF_UP);

				if (inshrs2.abs().compareTo(outshrs2.abs()) != 0) {
					Common.reportError("Mismatched security transfer");
				}

				for (final InvestmentTxn t2 : ins) {
					t2.xferInv = outs;
				}
				for (final InvestmentTxn t2 : outs) {
					t2.xferInv = ins;
				}
			}

			final String s = String.format(//
					"%-20s : %5s(%2d) %s INSH=%10.3f (%2d txns) OUTSH=%10.3f (%2d txns)", //
					t.getAccount().name, t.security.symbol, t.security.id, //
					Common.getDateString(t.getDate()), //
					inshrs, ins.size(), outshrs, outs.size());
			System.out.println(s);
		}

		for (final InvestmentTxn t : unmatched) {
			final String pad = (t.getAction() == Action.SHRS_IN) //
					? "" //
					: "                          ";

			final String s = String.format("%-20s : %5s(%2d) %s %s SHR=%10.3f", //
					t.getAccount().name, t.security.symbol, t.security.id, //
					Common.getDateString(t.getDate()), pad, t.quantity);
			System.out.println(s);
		}
	}

	private BigDecimal gatherTransactionsForSecurityTransfer( //
			List<InvestmentTxn> rettxns, //
			List<InvestmentTxn> srctxns, //
			List<InvestmentTxn> unmatched, //
			Security s, Date d) {
		BigDecimal numshrs = BigDecimal.ZERO;

		if (srctxns.isEmpty()) {
			return numshrs;
		}

		InvestmentTxn t = srctxns.get(0);

		while (t.getDate().compareTo(d) < 0 || //
				(t.getDate().equals(d) && (t.security.name.compareTo(s.name) < 0))) {
			unmatched.add(srctxns.remove(0));
			if (srctxns.isEmpty()) {
				break;
			}

			t = srctxns.get(0);
		}

		while (!srctxns.isEmpty()) {
			t = srctxns.get(0);

			if ((t.security != s) || //
					(t.getDate().compareTo(d) != 0)) {
				break;
			}

			rettxns.add(t);
			numshrs = numshrs.add(t.quantity);

			if (srctxns.isEmpty()) {
				break;
			}

			srctxns.remove(0);
		}

		return numshrs;
	}

	private void validateStatements() {
		for (int acctid = 1; acctid <= this.dom.getNumAccounts(); ++acctid) {
			final Account a = this.dom.getAccount(acctid);

			BigDecimal bal = BigDecimal.ZERO;
			for (final Statement s : a.statements) {
				bal = bal.add(s.credits);
				bal = bal.subtract(s.debits);

				if (!bal.equals(s.balance)) {
					Common.reportError("Statement check failed: " + s);
				}
			}
		}
	}

	private void balanceStatements() {
		for (int acctid = 1; acctid <= this.dom.getNumAccounts(); ++acctid) {
			final Account a = this.dom.getAccount(acctid);
			if (!a.isNonInvestmentAccount()) {
				continue;
			}

			BigDecimal balance = BigDecimal.ZERO;

			for (final Statement s : a.statements) {
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
		final List<GenericTxn> txns = gatherTransactionsForStatement(a, s);
		List<GenericTxn> uncleared = null;

		final BigDecimal totaltx = sumAmounts(txns);
		final BigDecimal diff = totaltx.add(curbal).subtract(s.balance);

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

		for (final GenericTxn t : txns) {
			System.out.println(t.toStringShort());
		}

		System.out.println();
		System.out.println("Uncleared transactions:");

		if (uncleared != null) {
			for (final GenericTxn t : uncleared) {
				System.out.println(t.toString());
			}
		}

		return true;
	}

	private List<GenericTxn> findSubsetTotaling(List<GenericTxn> txns, BigDecimal diff) {
		List<List<GenericTxn>> matches = null;

		for (int nn = 1; nn < txns.size(); ++nn) {
			final List<List<GenericTxn>> subsets = findSubsetsTotaling(txns, diff, nn, txns.size());

			if (!subsets.isEmpty()) {
				matches = subsets;
				break;
			}
		}

		return (matches == null) ? null : matches.get(0);
	}

	private List<List<GenericTxn>> findSubsetsTotaling( //
			List<GenericTxn> txns, BigDecimal tot, int nn, int max) {
		final List<List<GenericTxn>> ret = new ArrayList<List<GenericTxn>>();
		if (nn >= txns.size()) {
			return ret;
		}

		final List<GenericTxn> txns_work = new ArrayList<>();
		txns_work.addAll(txns);

		for (int ii = max - 1; ii >= 0; --ii) {
			final BigDecimal newtot = tot.subtract(txns.get(ii).getAmount());
			final GenericTxn t = txns_work.remove(ii);

			if ((nn == 1) && (newtot.signum() == 0)) {
				final List<GenericTxn> l = new ArrayList<GenericTxn>();
				l.add(t);
				ret.add(l);

				return ret;
			}

			if (nn > 1 && nn <= ii) {
				final List<List<GenericTxn>> subsets = findSubsetsTotaling(txns_work, newtot, nn - 1, ii);
				txns_work.add(ii, t);

				if (!subsets.isEmpty()) {
					for (final List<GenericTxn> l : subsets) {
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
		for (final GenericTxn t : txns) {
			t.stmtdate = s.date;
		}
	}

	private BigDecimal sumAmounts(List<GenericTxn> txns) {
		BigDecimal totaltx = BigDecimal.ZERO;
		for (final GenericTxn t : txns) {
			totaltx = totaltx.add(t.getAmount());
		}

		return totaltx;
	}

	private List<GenericTxn> gatherTransactionsForStatement(Account a, Statement s) {
		final List<GenericTxn> txns = new ArrayList<GenericTxn>();

		final int idx1 = a.findFirstNonClearedTransaction();
		if (idx1 < 0) {
			return txns;
		}

		for (int ii = idx1; ii <= a.transactions.size(); ++ii) {
			final GenericTxn t = a.transactions.get(ii);

			if (t.getDate().compareTo(s.date) > 0) {
				break;
			}

			if (!t.isCleared()) {
				txns.add(t);
			}
		}

		return txns;
	}

	private void processSecurities() {
		processSecurities2(this.dom.portfolio, this.dom.getAllTransactions());

		for (int acctid = 1; acctid <= this.dom.getNumAccounts(); ++acctid) {
			final Account a = this.dom.getAccount(acctid);
			if ((a == null) || !a.isInvestmentAccount()) {
				continue;
			}

			processSecurities2(a.securities, a.transactions);
		}
	}

	private void processSecurities2(SecurityPortfolio port, List<GenericTxn> txns) {
		for (final GenericTxn gtxn : txns) {
			if (!(gtxn instanceof InvestmentTxn)) {
				continue;
			}

			final InvestmentTxn txn = (InvestmentTxn) gtxn;
			if (txn.security == null) {
				continue;
			}

			final SecurityPosition pos = port.getPosition(txn.security);
			pos.transactions.add(txn);

			switch (txn.getAction()) {
			case BUY:
			case SHRS_IN:
			case REINV_DIV:
			case REINV_LG:
			case REINV_SH:
			case GRANT:
			case EXPIRE:
				// if (txn.quantity == null) {
				// // TODO what to do about this?
				// System.out.println("NULL quantities: " +
				// ++nullQuantities);
				// break;
				// }
			case BUYX:
			case REINV_INT:
			case VEST:
			case SHRS_OUT:
			case SELL:
			case SELLX:
			case EXERCISEX:
				pos.shares = pos.shares.add(txn.quantity);
				break;
			// pos.shares = pos.shares.subtract(txn.quantity);
			// break;

			case STOCKSPLIT:
				pos.shares = pos.shares.multiply(txn.quantity);
				pos.shares = pos.shares.divide(BigDecimal.TEN);
				break;

			case CASH:
			case DIV:
			case INT_INC:
			case MISC_INCX:
				break;

			default:
				break;
			}
		}
	}

	private void loadInvestmentTransactions() {
		for (;;) {
			final String s = this.rdr.peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			final InvestmentTxn txn = loadInvestmentTransaction();
			if (txn == null) {
				break;
			}

			this.dom.currAccount.addTransaction(txn);
		}
	}

	public InvestmentTxn loadInvestmentTransaction() {
		final QFileReader.QLine qline = new QFileReader.QLine();

		final InvestmentTxn txn = new InvestmentTxn(this.dom.domid, this.dom.currAccount.id);

		for (;;) {
			this.rdr.nextInvLine(qline);

			switch (qline.type) {
			case EndOfSection:
				txn.repair();
				return txn;

			case InvTransactionAmt: {
				final BigDecimal amt = Common.getDecimal(qline.value);

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
				txn.action = parseAction(qline.value);
				break;
			case InvClearedStatus:
				txn.clearedStatus = qline.value;
				break;
			case InvCommission:
				txn.commission = Common.getDecimal(qline.value);
				break;
			case InvDate:
				txn.setDate(Common.parseDate(qline.value));
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
				txn.security = this.dom.findSecurityByName(qline.value);
				break;
			case InvFirstLine:
				txn.textFirstLine = qline.value;
				break;
			case InvXferAmt:
				txn.amountTransferred = Common.getDecimal(qline.value);
				break;
			case InvXferAcct:
				txn.accountForTransfer = qline.value;
				txn.xacctid = this.dom.findCategoryID(qline.value);
				break;

			default:
				Common.reportError("syntax error; txn: " + qline);
			}
		}
	}

	private Action parseAction(String s) {
		if ("StkSplit".equals(s)) {
			return Action.STOCKSPLIT;
		}
		if ("Cash".equals(s)) {
			return Action.CASH;
		}
		if ("XIn".equals(s)) {
			return Action.XIN;
		}
		if ("XOut".equals(s)) {
			return Action.XOUT;
		}
		if ("Buy".equals(s)) {
			return Action.BUY;
		}
		if ("BuyX".equals(s)) {
			return Action.BUYX;
		}
		if ("Sell".equals(s)) {
			return Action.SELL;
		}
		if ("SellX".equals(s)) {
			return Action.SELLX;
		}
		if ("ShrsIn".equals(s)) {
			return Action.SHRS_IN;
		}
		if ("ShrsOut".equals(s)) {
			return Action.SHRS_OUT;
		}
		if ("Grant".equals(s)) {
			return Action.GRANT;
		}
		if ("Vest".equals(s)) {
			return Action.VEST;
		}
		if ("ExercisX".equals(s)) {
			return Action.EXERCISEX;
		}
		if ("Expire".equals(s)) {
			return Action.EXPIRE;
		}
		if ("WithdrwX".equals(s)) {
			return Action.WITHDRAWX;
		}
		if ("IntInc".equals(s)) {
			return Action.INT_INC;
		}
		if ("MiscIncX".equals(s)) {
			return Action.MISC_INCX;
		}
		if ("Div".equals(s)) {
			return Action.DIV;
		}
		if ("ReinvDiv".equals(s)) {
			return Action.REINV_DIV;
		}
		if ("ReinvLg".equals(s)) {
			return Action.REINV_LG;
		}
		if ("ReinvSh".equals(s)) {
			return Action.REINV_SH;
		}
		if ("ReinvInt".equals(s)) {
			return Action.REINV_INT;
		}
		if ("ContribX".equals(s)) {
			return Action.CONTRIBX;
		}
		if ("Reminder".equals(s)) {
			return Action.REMINDER;
		}

		return Action.OTHER;
	}

	private void loadNonInvestmentTransactions() {
		for (;;) {
			final String s = this.rdr.peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			final NonInvestmentTxn txn = loadNonInvestmentTransaction();
			if (txn == null) {
				break;
			}

			txn.verifySplit();

			this.dom.currAccount.addTransaction(txn);
		}
	}

	public NonInvestmentTxn loadNonInvestmentTransaction() {
		final QFileReader.QLine qline = new QFileReader.QLine();

		final NonInvestmentTxn txn = new NonInvestmentTxn(this.dom.domid, this.dom.currAccount.id);
		SimpleTxn cursplit = null;

		for (;;) {
			this.rdr.nextTxnLine(qline);

			switch (qline.type) {
			case EndOfSection:
				return txn;

			case TxnCategory:
				txn.catid = this.dom.findCategoryID(qline.value);

				if (txn.catid == 0) {
					Common.reportError("Can't find xtxn: " + qline.value);
				}
				break;
			case TxnAmount: {
				final BigDecimal amt = Common.getDecimal(qline.value);

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
				txn.setDate(Common.parseDate(qline.value));
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
					cursplit = new SimpleTxn(this.dom.domid, txn.acctid);
					txn.split.add(cursplit);
				}

				if (qline.value == null || qline.value.trim().isEmpty()) {
					qline.value = "Fix Me";
				}
				cursplit.catid = this.dom.findCategoryID(qline.value);

				if (cursplit.catid == 0) {
					Common.reportError("Can't find xtxn: " + qline.value);
				}
				break;
			case TxnSplitAmount:
				if (cursplit == null || cursplit.getAmount() != null) {
					txn.split.add(cursplit);
					cursplit = new SimpleTxn(this.dom.domid, txn.acctid);
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
			final String s = this.rdr.peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			final Price price = Price.load(this.rdr);
			if (price == null) {
				break;
			}

			final Security sec = this.dom.findSecurityBySymbol(price.symbol);
			sec.addPrice(price);
		}
	}

	private void loadStatements() {
		for (;;) {
			final String s = this.rdr.peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			final Statement stmt = Statement.load(this.rdr, this.dom.currAccount.id);
			if (stmt == null) {
				break;
			}

			this.dom.currAccount.statements.add(stmt);
		}
	}

	private void massageSplits(GenericTxn txn) {
		if (!(txn instanceof NonInvestmentTxn)) {
			return;
		}

		final NonInvestmentTxn nitxn = (NonInvestmentTxn) txn;

		for (int ii = 0; ii < nitxn.split.size(); ++ii) {
			final SimpleTxn stxn = nitxn.split.get(ii);
			if (stxn.catid >= 0) {
				continue;
			}

			MultiSplitTxn mtxn = null;

			for (int jj = ii + 1; jj < nitxn.split.size(); ++jj) {
				final SimpleTxn stxn2 = nitxn.split.get(jj);

				if (stxn.catid == stxn2.catid) {
					if (mtxn == null) {
						mtxn = new MultiSplitTxn(this.dom.domid, txn.acctid);
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

		final Account a = this.dom.getAccount(-txn.catid);

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

	private void findMatches(Account acct, SimpleTxn txn, Date date, boolean strict) {
		this.matchingTxns.clear();

		final int idx = Common.findLastTransactionOnOrBeforeDate(acct.transactions, date);
		if (idx < 0) {
			return;
		}

		boolean datematch = false;

		for (int inc = 0; datematch || (inc < 10); ++inc) {
			datematch = false;

			if (idx + inc < acct.transactions.size()) {
				final GenericTxn gtxn = acct.transactions.get(idx + inc);
				datematch = date.equals(gtxn.getDate());

				final SimpleTxn match = checkMatch(txn, gtxn, strict);
				if (match != null) {
					this.matchingTxns.add(match);
				}
			}

			if (inc > 0 && idx >= inc) {
				final GenericTxn gtxn = acct.transactions.get(idx - inc);
				datematch = datematch || date.equals(gtxn.getDate());

				final SimpleTxn match = checkMatch(txn, gtxn, strict);
				if (match != null) {
					this.matchingTxns.add(match);
				}
			}
		}
	}

	// private SimpleTxn checkMatch(SimpleTxn txn, GenericTxn gtxn) {
	// return checkMatch(txn, gtxn, false);
	// }

	private SimpleTxn checkMatch(SimpleTxn txn, GenericTxn gtxn, boolean strict) {
		assert -txn.catid == gtxn.acctid;

		if (!gtxn.hasSplits()) {
			if ((gtxn.getXferAcctid() == txn.acctid) //
					&& amountIsEqual(gtxn, txn, strict)) {
				return gtxn;
			}
		} else {
			for (final SimpleTxn splitxn : gtxn.getSplits()) {
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
		final BigDecimal amt1 = txn1.getXferAmount();
		final BigDecimal amt2 = txn2.getXferAmount();

		if (amt1.abs().compareTo(amt2.abs()) != 0) {
			return false;
		}

		if (BigDecimal.ZERO.compareTo(amt1) == 0) {
			return true;
		}

		// We know the magnitude is the same and non-zero
		// Check whether they are equal or negative of each other
		final boolean eq = amt1.equals(amt2);

		final boolean ret = !eq || !strict;

		if ((txn1.getAction() == Action.CASH) //
				|| (txn2.getAction() == Action.CASH)) {
			if (eq) {
				++this.cashbad;
			} else {
				++this.cashok;
			}

			System.out.println(txn1.toString());
			System.out.println(txn2.toString());
			System.out.println("Cash ok=" + this.cashok + " bad=" + this.cashbad);

			return ret;
		}

		return ret;
	}

	// private static int findDateRange(Account acct, Date date) {
	// if (acct.transactions.isEmpty()) {
	// return -1;
	// }
	//
	// int loidx = 0;
	// int hiidx = acct.transactions.size() - 1;
	// final Date loval = acct.transactions.get(loidx).getDate();
	// final Date hival = acct.transactions.get(hiidx).getDate();
	// if (loval.compareTo(date) >= 0) {
	// return loidx;
	// }
	// if (hival.compareTo(date) <= 0) {
	// return hiidx;
	// }
	//
	// while (loidx < hiidx) {
	// int idx = (loidx + hiidx) / 2;
	// if (idx <= loidx || idx >= hiidx) {
	// return idx;
	// }
	// final Date val = acct.transactions.get(idx).getDate();
	//
	// if (val.compareTo(date) < 0) {
	// loidx = idx;
	// } else if (val.compareTo(date) > 0) {
	// hiidx = idx;
	// } else {
	// while ((idx > 0) //
	// && (acct.transactions.get(idx - 1).getDate().equals(date))) {
	// --idx;
	// }
	//
	// return idx;
	// }
	// }
	//
	// return loidx;
	// }
}