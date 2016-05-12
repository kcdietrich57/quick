package qif.data;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.StringTokenizer;

import qif.data.QFileReader.SectionType;
import qif.data.Security.SplitInfo;

public class QifDomReader {
	private QFileReader rdr = null;
	private QifDom dom = null;
	private int nextAccountID = 1;
	private int nextCategoryID = 1;
	private int nextSecurityID = 1;

	public static QifDom loadDom(String[] qifFiles) {
		final QifDomReader rdr = new QifDomReader();
		final QifDom dom = new QifDom();

		for (final String fn : qifFiles) {
			rdr.load(dom, fn);
		}

		rdr.postLoad(new File(qifFiles[0]).getParentFile());

		return dom;
	}

	public QifDomReader() {
	}

	public QifDom load(String fileName) {
		return load(null, fileName);
	}

	public QifDom load(QifDom refdom, String fileName) {
		if (!new File(fileName).exists()) {
			if (new File("c:" + fileName).exists()) {
				fileName = "c:" + fileName;
			} else {
				Common.reportError("Input file '" + fileName + "' does not exist");

				return null;
			}
		}

		init(fileName, refdom);

		processFile();

		this.dom.cleanUpTransactions();
		this.dom.validateStatements();

		return this.dom;
	}

	public void postLoad(File dirFile) {
		final File d = new File(dirFile, "quotes");
		loadSecurityPriceHistory(d);

		this.dom.processSecurities();
		this.dom.fixPortfolios();

		final File dd = new File(dirFile, "statements");
		loadStatements(dd);

		this.dom.balanceStatements();
	}

	private void loadSecurityPriceHistory(File quoteDirectory) {
		if (!quoteDirectory.isDirectory()) {
			return;
		}

		final File quoteFiles[] = quoteDirectory.listFiles();

		for (final File f : quoteFiles) {
			String symbol = f.getName();
			symbol = symbol.replaceFirst(".csv", "");
			final Security sec = this.dom.findSecurityBySymbol(symbol);

			if (sec != null) {
				loadQuoteFile(sec, f);
			}
		}
	}

	public static void loadQuoteFile(Security sec, File f) {
		if (!f.getName().endsWith(".csv")) {
			return;
		}

		final List<Price> prices = sec.prices;
		final List<SplitInfo> splits = sec.splits;

		assert prices.isEmpty() && splits.isEmpty();
		prices.clear();
		splits.clear();

		FileReader fr;
		String line;
		LineNumberReader rdr;

		boolean isSplitAdjusted = false;
		boolean isWeekly = false;
		boolean dateprice = false;
		boolean chlvd = false;
		boolean dohlcv = false;

		Date splitDate = null;

		try {
			fr = new FileReader(f);

			rdr = new LineNumberReader(fr);

			line = rdr.readLine();

			while (line != null) {
				boolean isHeader = false;

				if (line.startsWith("split adjusted")) {
					isSplitAdjusted = true;
					isHeader = true;
				} else if (line.startsWith("weekly")) {
					isWeekly = true;
					isHeader = true;
				} else if (line.startsWith("date")) {
					chlvd = false;
					dohlcv = false;
					dateprice = true;
					isHeader = true;
				} else if (line.startsWith("price")) {
					chlvd = false;
					dohlcv = false;
					dateprice = false;
					isHeader = true;
				} else if (line.startsWith("chlvd")) {
					chlvd = true;
					dohlcv = false;
					dateprice = false;
					isHeader = true;
				} else if (line.startsWith("dohlcv")) {
					chlvd = false;
					dohlcv = true;
					dateprice = false;
					isHeader = true;
				} else if (line.startsWith("split")) {
					final StringTokenizer toker = new StringTokenizer(line, " ");
					toker.nextToken();
					final String newshrStr = toker.nextToken();
					final String oldshrStr = toker.nextToken();
					final String dateStr = toker.nextToken();

					final BigDecimal splitAdjust = new BigDecimal(newshrStr).divide(new BigDecimal(oldshrStr));
					splitDate = Common.parseDate(dateStr);

					final SplitInfo si = new SplitInfo();
					si.splitDate = splitDate;
					si.splitRatio = splitAdjust;

					splits.add(si);
					Collections.sort(splits, (o1, o2) -> o1.splitDate.compareTo(o2.splitDate));

					isHeader = true;
				}

				if (isHeader) {
					line = rdr.readLine();
					continue;
				}

				final StringTokenizer toker = new StringTokenizer(line, ",");

				String pricestr;
				String datestr;

				if (chlvd) {
					pricestr = toker.nextToken();
					toker.nextToken();
					toker.nextToken();
					toker.nextToken();
					datestr = toker.nextToken();
				} else if (dohlcv) {
					datestr = toker.nextToken();
					toker.nextToken();
					toker.nextToken();
					toker.nextToken();
					pricestr = toker.nextToken();
				} else if (dateprice) {
					datestr = toker.nextToken();
					pricestr = toker.nextToken();
				} else {
					pricestr = toker.nextToken();
					datestr = toker.nextToken();
				}

				Date date = Common.parseDate(datestr);
				BigDecimal price = null;
				try {
					price = new BigDecimal(pricestr);
				} catch (final Exception e) {
					e.printStackTrace();
				}

				final Price p = new Price();

				if (isWeekly) {
					final Calendar cal = new GregorianCalendar();
					cal.setTime(date);
					final LocalDate d = LocalDate.of( //
							cal.get(Calendar.YEAR), //
							cal.get(Calendar.MONTH) + 1, //
							cal.get(Calendar.DAY_OF_MONTH));
					final LocalDate d2 = d.plusDays(4);
					final Instant instant = d2.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
					date = Date.from(instant);
				}
				p.date = date;

				final BigDecimal splitRatio = sec.getSplitRatioForDate(date);

				if (isSplitAdjusted) {
					p.splitAdjustedPrice = price;
					p.price = price.multiply(splitRatio);
				} else {
					p.splitAdjustedPrice = price.divide(splitRatio);
					p.price = price;
				}

				prices.add(p);

				line = rdr.readLine();
			}

			rdr.close();
		} catch (final Exception e) {
			e.printStackTrace();
			return;
		}

		Collections.sort(prices, (o1, o2) -> o1.date.compareTo(o2.date));
	}

	private void init(String filename, QifDom refdom) {
		final File f = new File(filename);
		if (!f.exists()) {
			Common.reportError("File '" + filename + "' does not exist");
		}

		this.rdr = new QFileReader(f);

		if (refdom != null) {
			this.dom = refdom;
			this.nextAccountID = this.dom.getNextAccountID();
			this.nextCategoryID = this.dom.getNextCategoryID();
		} else {
			this.dom = new QifDom();
			this.nextAccountID = 1;
			this.nextCategoryID = 1;
		}
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
				loadStatements(this.rdr);
				break;

			case Statements:
				// System.out.println("Loading statements");
				loadStatements2(this.rdr);
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
				acct.type = Common.parseAccountType(qline.value);
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
			final String s = this.rdr.peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			final Security sec = loadSecurity();
			if (sec == null) {
				break;
			}

			final Security existing = (sec.symbol != null) //
					? this.dom.findSecurityBySymbol(sec.symbol) //
					: this.dom.findSecurityByName(sec.getName());

			if (existing != null) {
				// TODO verify
				if (!existing.names.contains(sec.getName())) {
					existing.names.add(sec.getName());
				}
			} else {
				sec.id = this.nextSecurityID++;
				this.dom.addSecurity(sec);
			}
		}
	}

	public Security loadSecurity() {
		final QFileReader.QLine qline = new QFileReader.QLine();

		String symbol = null;
		String name = null;
		String type = null;
		String goal = null;

		loop: for (;;) {
			this.rdr.nextSecurityLine(qline);

			switch (qline.type) {
			case EndOfSection:
				break loop;

			case SecName:
				name = qline.value;
				break;
			case SecSymbol:
				symbol = qline.value;
				break;
			case SecType:
				type = qline.value;
				break;
			case SecGoal:
				goal = qline.value;
				break;

			default:
				Common.reportError("syntax error");
			}
		}

		if (symbol == null) {
			Common.reportWarning("Security '" + name + "' does not specify a ticker symbol.");
		}

		final Security security = new Security(symbol);
		security.names.add(name);
		security.type = type;
		security.goal = goal;

		return security;
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

			if ("[[ignore]]".equals(txn.memo)) {
				continue;
			}

			if ((txn.security != null) && (txn.price != null)) {
				txn.security.addTransaction(txn);
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
				txn.action = Common.parseAction(qline.value);
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
				if (txn.security == null) {
					txn.security = this.dom.findSecurityByName(qline.value);
					Common.reportWarning("Txn for acct " + txn.acctid + ". " //
							+ "No security '" + qline.value + "' was found.");
				}
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

			if ("[[ignore]]".equals(txn.memo)) {
				continue;
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
				txn.setPayee(qline.value);
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
			sec.addPrice(price, true);
		}
	}

	private void loadStatements(File stmtDirectory) {
		if (!stmtDirectory.isDirectory()) {
			return;
		}

		final File stmtFiles[] = stmtDirectory.listFiles();

		for (final File f : stmtFiles) {
			if (!f.getName().endsWith(".qif")) {
				continue;
			}

			try {
				load(this.dom, f.getAbsolutePath());
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void loadStatements(QFileReader qfr) {
		for (;;) {
			final String s = qfr.peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			final Statement stmt = Statement.load(qfr, this.dom.domid, this.dom.currAccount.id);
			if (stmt == null) {
				break;
			}

			this.dom.currAccount.statements.add(stmt);
		}
	}

	private void loadStatements2(QFileReader qfr) {
		for (;;) {
			final String s = qfr.peekLine();
			if ((s == null) || ((s.length() > 0) && (s.charAt(0) == '!'))) {
				break;
			}

			final List<Statement> stmts = Statement.loadStatements(qfr, this.dom.domid, this.dom.currAccount.id);
			for (final Statement stmt : stmts) {
				this.dom.currAccount.statements.add(stmt);
			}
		}
	}
}