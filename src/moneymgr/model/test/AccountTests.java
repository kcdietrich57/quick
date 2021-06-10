package moneymgr.model.test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import moneymgr.model.Account;
import moneymgr.model.AccountType;
import moneymgr.model.GenericTxn;
import moneymgr.model.InvestmentTxn;
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.NonInvestmentTxn;
import moneymgr.model.Security;
import moneymgr.model.SecurityPosition.PositionInfo;
import moneymgr.model.SimpleTxn;
import moneymgr.model.Statement;
import moneymgr.util.Common;
import moneymgr.util.QDate;

class AccountTests {

	MoneyMgrModel model;
	QDate today;

	Account asset;
	Account bank;
	Account cash;
	Account ccard;
	Account retire;
	Account invest;
	Account mutual;
	Account port;
	Account liability;

	Security stock;
	Statement nstat;
	Statement istat;
	NonInvestmentTxn ntx;
	InvestmentTxn itx;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
		new MoneyMgrModel("test-model");
		this.model = MoneyMgrModel.changeModel("test-model");

		this.asset = new Account("asset", AccountType.Asset);
		this.model.addAccount(this.asset);

		this.bank = new Account("bank", AccountType.Bank);
		this.model.addAccount(this.bank);

		this.cash = new Account("cash", AccountType.Cash);
		this.model.addAccount(this.cash);

		this.ccard = new Account("ccard", AccountType.CCard);
		this.model.addAccount(this.ccard);

		this.retire = new Account("401k", AccountType.Inv401k);
		this.model.addAccount(this.retire);

		this.invest = new Account("invest", AccountType.Invest);
		this.model.addAccount(this.invest);

		this.mutual = new Account("mutual", AccountType.InvMutual);
		this.model.addAccount(this.mutual);

		this.port = new Account("port", AccountType.InvPort);
		this.model.addAccount(this.port);

		this.liability = new Account("liability", AccountType.Liability);
		this.model.addAccount(this.liability);

		this.today = QDate.today();
		this.model.setCurrentDate(this.today);
		this.model.setAsOfDate(this.today);

		this.ntx = new NonInvestmentTxn(this.bank.acctid);
		this.ntx.setDate(this.today);
		this.ntx.setAmount(new BigDecimal("9.99"));
		// System.out.println("setup tx is " + this.ntx.toString());
		// System.out.println("setup tx.amt is " + this.ntx.getAmount());
		this.bank.addTransaction(this.ntx);

		this.stock = new Security("FOO", "Foo, Inc");

		this.nstat = new Statement(this.bank.acctid, today, //
				new BigDecimal("1.23"), new BigDecimal("1.23"), null);
		this.nstat.addTransaction(this.ntx);
		this.bank.addStatement(this.nstat);

		this.istat = new Statement(this.invest.acctid, today, null);
		// TODO this.istat.addTransaction(this.itx);
		this.invest.addStatement(this.istat);
	}

	@AfterEach
	void tearDown() throws Exception {
		MoneyMgrModel.deleteModel("test-model");
	}

	@Test
	void testModelFunctions() {
		MoneyMgrModel m = this.model;

		List<Account> accts = m.getAccounts();
		List<Account> acctsById = m.getAccountsById();
		Assert.assertNotNull(accts);
		Assert.assertNotNull(acctsById);
		Assert.assertTrue(accts.size() < acctsById.size());

		int n = m.getNumAccounts();
		Assert.assertEquals(n, accts.size());
		Assert.assertTrue(n > 0);

		Assert.assertNotNull(m.getAccountByID(1));
		Assert.assertNull(m.getAccountByID(99));

		Account a = m.makeAccount( //
				"new-account", AccountType.Bank, "new-account-desc", //
				today, 30, 5);
		Assert.assertNotNull(a);

		Assert.assertEquals(n + 1, m.getNumAccounts());
		Assert.assertEquals(n + 1, accts.size());
		Assert.assertEquals(a, m.findAccount("new-account"));

		List<Account> acctsSorted = m.getSortedAccounts(false);
		Assert.assertNotNull(acctsSorted);
		acctsSorted = m.getSortedAccounts(true);
		Assert.assertNotNull(acctsSorted);
	}

	@Test
	void testFindMatchingTransactions() {
		SimpleTxn tx = bank.getFirstUnclearedTransaction();
		Assert.assertNotNull(tx);
		System.out.println("Txn: " + tx.toString());
		System.out.println("amt: " + tx.getAmount());

		List<SimpleTxn> txns = bank.findMatchingTransactions(tx, false);
		Assert.assertNotNull(txns);
		Assert.assertFalse(txns.isEmpty());
	}

	@Test
	void testAccountIntStringStringAccountTypeIntInt() {
		new Account(this.model, 99, "new-account", "new-account-desc", AccountType.Bank, 30, 1);
		// TODO fail("Not yet implemented");
	}

	@Test
	void testAccountStringAccountTypeStringQDateIntInt() {
		new Account("new-account", AccountType.Bank, "new-account-desc", null, 30, 1);
		new Account("new-account", AccountType.Bank, "new-account-desc", this.today, 30, 1);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testAccountStringAccountType() {
		new Account("new-account", AccountType.Bank);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetSetStatementFrequency() {
		Assert.assertEquals(30, bank.getStatementFrequency());

		bank.setStatementFrequency(90, 0);
		Assert.assertEquals(90, bank.getStatementFrequency());
	}

	@Test
	void testGetStatementDay() {
		Assert.assertEquals(30, bank.getStatementDay());

		bank.setStatementFrequency(90, 5);
		Assert.assertEquals(5, bank.getStatementDay());
	}

	@Test
	void testGetSetBalance() {
		BigDecimal bal = bank.getBalance();
		Assert.assertTrue(Common.isEffectivelyZero(bal));

		bal = new BigDecimal("100.00");
		bank.setBalance(bal);

		Assert.assertTrue(Common.isEffectivelyEqual(bal, bank.getBalance()));
	}

	@Test
	void testGetSetClearedBalance() {
		BigDecimal bal = bank.getClearedBalance();
		Assert.assertTrue(Common.isEffectivelyZero(bal));

		bal = new BigDecimal("100.00");
		bank.setClearedBalance(bal);

		Assert.assertTrue( //
				Common.isEffectivelyEqual(bal, bank.getClearedBalance()));
	}

	@Test
	void testGetDisplayName() {
		String s = bank.getDisplayName(20);
		System.out.println("Display name '" + s + "'");
	}

	@Test
	void testIsLiability() {
		Assert.assertFalse(bank.isLiability());
		Assert.assertFalse(invest.isLiability());
		Assert.assertTrue(liability.isLiability());
	}

	@Test
	void testIsAsset() {
		Assert.assertTrue(bank.isAsset());
		Assert.assertTrue(invest.isAsset());
		Assert.assertFalse(liability.isAsset());
	}

	@Test
	void testIsInvestmentAccount() {
		Assert.assertFalse(bank.isInvestmentAccount());
		Assert.assertTrue(invest.isInvestmentAccount());
	}

	@Test
	void testIsCashAccount() {
		Assert.assertTrue(bank.isCashAccount());
		Assert.assertFalse(invest.isCashAccount());
	}

	@Test
	void testIsNonInvestmentAccount() {
		Assert.assertTrue(bank.isNonInvestmentAccount());
		Assert.assertFalse(invest.isNonInvestmentAccount());
	}

	@Test
	void testGetOpenDate() {
		Assert.assertEquals(asset.getOpenDate(), today);
		Assert.assertEquals(bank.getOpenDate(), today);

		QDate open = today.addDays(-5);

		NonInvestmentTxn tx = new NonInvestmentTxn(bank.acctid);
		tx.setDate(open);
		bank.addTransaction(tx);

		Assert.assertEquals(bank.getOpenDate(), open);
	}

	@Test
	void testGetSetCloseDate() {
		Assert.assertNull(bank.getCloseDate());

		QDate close;

		// TODO error?
		close = bank.getOpenDate().addDays(-1);
		bank.setCloseDate(close);
		Assert.assertEquals(bank.getCloseDate(), bank.getOpenDate());
		Assert.assertTrue( //
				bank.getOpenDate().compareTo(bank.getCloseDate()) <= 0);

		bank.setCloseDate(today);
		Assert.assertEquals(bank.getCloseDate(), today);
		Assert.assertTrue( //
				bank.getOpenDate().compareTo(bank.getCloseDate()) <= 0);

		close = today.addDays(1);
		bank.setCloseDate(close);
		Assert.assertEquals(bank.getCloseDate(), close);
		Assert.assertTrue( //
				bank.getOpenDate().compareTo(bank.getCloseDate()) <= 0);
	}

	@Test
	void testIsOpenOn() {
		QDate today = QDate.today();

		Assert.assertFalse(bank.isOpenOn(today.addDays(-1)));
		Assert.assertTrue(bank.isOpenOn(today));

		// TODO error?
		System.out.println("closed " + bank.getCloseDate());
		bank.setCloseDate(today.addDays(-1));
		System.out.println("closed " + bank.getCloseDate());
		System.out.println("openasof " + bank.isOpenAsOf(today) + " closedasof " + bank.isClosedAsOf(today));
		Assert.assertTrue(bank.isOpenOn(today));

		bank.setCloseDate(today);
		Assert.assertTrue(bank.isOpenOn(today));

		bank.setCloseDate(today.addDays(1));
		System.out.println("open " + bank.getOpenDate() + " close " + bank.getCloseDate());
		Assert.assertTrue(bank.isOpenOn(today));

		Assert.assertTrue(cash.isOpenOn(today));
	}

	@Test
	void testIsOpenDuring() {
		QDate begin = today.addDays(-5);
		QDate end = today;
		Assert.assertTrue(bank.isOpenDuring(begin, end));

		end = today.addDays(-1);
		Assert.assertFalse(bank.isOpenDuring(begin, end));

		begin = today;
		end = today.addDays(5);
		Assert.assertTrue(bank.isOpenDuring(begin, end));

		begin = today.addDays(1);
		Assert.assertTrue(bank.isOpenDuring(begin, end));

		bank.setCloseDate(today.addDays(5));

		QDate open = bank.getOpenDate();
		QDate close = bank.getCloseDate();

		Assert.assertFalse(bank.isOpenDuring(open.addDays(-5), open.addDays(-1)));
		Assert.assertFalse(bank.isOpenDuring(close.addDays(1), close.addDays(5)));
		Assert.assertTrue(bank.isOpenDuring(open.addDays(-5), open));
		Assert.assertTrue(bank.isOpenDuring(close, close.addDays(5)));
	}

	@Test
	void testIsOpenAsOf() {
		Assert.assertFalse(bank.isOpenAsOf(today.addDays(-1)));
		Assert.assertTrue(bank.isOpenAsOf(today));

		// TODO error?
		bank.setCloseDate(today.addDays(-1));
		Assert.assertTrue(bank.isOpenAsOf(today));

		bank.setCloseDate(today);
		Assert.assertTrue(bank.isOpenAsOf(today));
		Assert.assertTrue(bank.isOpenAsOf(today.addDays(1)));

		bank.setCloseDate(today.addDays(1));
		Assert.assertTrue(bank.isOpenAsOf(today));

		Assert.assertTrue(asset.isOpenAsOf(today));
	}

	@Test
	void testAddTransaction() {
		int num = bank.getNumTransactions();

		NonInvestmentTxn tx = new NonInvestmentTxn(bank.acctid);
		tx.setDate(today.addDays(-1));
		bank.addTransaction(tx);

		Assert.assertEquals(num + 1, bank.getNumTransactions());
	}

	@Test
	void testGetNumTransactions() {
		Assert.assertEquals(1, bank.getNumTransactions());
		Assert.assertEquals(0, cash.getNumTransactions());
	}

	@Test
	void testGetTransactions() {
		List<GenericTxn> txns;

		txns = cash.getTransactions();
		Assert.assertTrue(txns.isEmpty());

		txns = bank.getTransactions();
		Assert.assertFalse(txns.isEmpty());
	}

	@Test
	void testGetTransactionsQDateQDate() {
		// TODO why is this simple tx, and getTxns() is generic tx?
		List<SimpleTxn> txns;

		txns = cash.getTransactions(today, today);
		Assert.assertTrue(txns.isEmpty());

		txns = bank.getTransactions(today, today);
		Assert.assertFalse(txns.isEmpty());
	}

	@Test
	void testGetLastStatement() {
		Statement s = bank.getLastStatement();
		Assert.assertNotNull(s);
		Assert.assertEquals(this.nstat, s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetLastBalancedStatementDate() {
		QDate d = bank.getLastBalancedStatementDate();
		Assert.assertNull(d);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetFirstUnbalancedStatement() {
		Statement s = bank.getFirstUnbalancedStatement();
		Assert.assertNotNull(s);
		Assert.assertEquals(this.nstat, s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testIsStatementDue() {
		Assert.assertTrue(bank.isStatementDue());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testIsStatementOverdue() {
		Assert.assertFalse(bank.isStatementOverdue(5));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetDaysUntilStatementIsDue() {
		int days = bank.getDaysUntilStatementIsDue();
		Assert.assertTrue(days < 0);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetNumStatements() {
		int n = bank.getNumStatements();
		Assert.assertEquals(1, n);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetStatements() {
		List<Statement> ss = bank.getStatements();
		Assert.assertNotNull(ss);
		Assert.assertFalse(ss.isEmpty());
		Assert.assertEquals(1, ss.size());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testAddStatement() {
		Statement s = new Statement(bank.acctid, today, null);
		bank.addStatement(s);
		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetStatementQDate() {
		List<Statement> stmts = this.bank.getStatements();
		Assert.assertNotNull(stmts);
		System.out.println("Statements: " + stmts.toString());

		Statement s = this.bank.getStatement(today);
		Assert.assertNotNull(s);
		Assert.assertTrue(s == this.nstat);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetStatementQDateBigDecimal() {
		Statement s = bank.getStatement(this.nstat.date, this.nstat.closingBalance);
		Assert.assertNotNull(s);
		Assert.assertEquals(this.nstat, s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetNextStatementDate() {
		QDate d = bank.getNextStatementDate();
		// System.out.println("Next statement date is " + d.toString());
		Assert.assertNotNull(d);
		Assert.assertTrue(today.compareTo(d) < 0);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetNextStatementToReconcile() {
		Statement s = bank.getNextStatementToReconcile();
		Assert.assertNotNull(s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetUnclearedStatement() {
		Statement s = bank.getUnclearedStatement();
		Assert.assertNull(s);

		s = bank.createUnclearedStatement(null);
		this.bank.addStatement(s);
		s = bank.getUnclearedStatement();
		// Assert.assertNotNull(s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testCreateUnclearedStatement() {
		Statement s = bank.createUnclearedStatement(null);
		Assert.assertNotNull(s);
	}

	@Test
	void testGetFirstTransactionDate() {
		QDate d = bank.getFirstTransactionDate();
		Assert.assertNotNull(d);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetFirstUnclearedTransactionDate() {
		QDate d = bank.getFirstUnclearedTransactionDate();
		System.out.println("1st uncleared tx date: " + d.toString());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetFirstUnclearedTransaction() {
		GenericTxn tx = bank.getFirstUnclearedTransaction();
		Assert.assertNotNull(tx);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetUnclearedTransactionCount() {
		int n = bank.getUnclearedTransactionCount();
		Assert.assertTrue(n > 0);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetCurrentValue() {
		BigDecimal val = bank.getCurrentValue();
		Assert.assertTrue(Common.isEffectivelyZero(val));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetValueForDate() {
		BigDecimal val = bank.getValueForDate(today);
		Assert.assertTrue(Common.isEffectivelyZero(val));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetOptionsValueForDate() {
		BigDecimal val = bank.getOptionsValueForDate(today);
		Assert.assertTrue(Common.isEffectivelyZero(val));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetSecuritiesValueForDate() {
		BigDecimal val = bank.getSecuritiesValueForDate(today);
		Assert.assertTrue(Common.isEffectivelyZero(val));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetSecurityValueForDate() {
		PositionInfo pos = bank.getSecurityValueForDate(this.stock, today);
		Assert.assertNotNull(pos);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGatherTransactionsForStatement() {
		Statement s = new Statement(bank.acctid, this.today, null);
		bank.gatherTransactionsForStatement(s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetOpenPositionsForDate() {
		Map<Security, PositionInfo> positions = bank.getOpenPositionsForDate(today);
		Assert.assertNotNull(positions);
		Assert.assertTrue(positions.isEmpty());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testToString() {
		String s = bank.toString();
		Assert.assertNotNull(s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testMatches() {
		Assert.assertNotNull(bank.matches(asset));

		// TODO fail("Not yet implemented");
	}

}
