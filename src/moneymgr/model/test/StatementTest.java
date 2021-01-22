package moneymgr.model.test;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
import moneymgr.model.QPrice;
import moneymgr.model.Security;
import moneymgr.model.Statement;
import moneymgr.model.TxAction;
import moneymgr.util.Common;
import moneymgr.util.QDate;

class StatementTest {

	MoneyMgrModel model;
	Account bank;
	Account invest;
	QDate today;
	NonInvestmentTxn ntx;
	InvestmentTxn itx;
	Security stock;
	Statement nstat;
	Statement istat;
	Statement tmpstat;

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

		this.bank = new Account("bank", AccountType.Bank);
		this.model.addAccount(this.bank);

		this.invest = new Account("invest", AccountType.Invest);
		this.model.addAccount(this.invest);

		this.today = QDate.today();
		this.model.setCurrentDate(this.today);
		this.model.setAsOfDate(this.today);

		this.ntx = new NonInvestmentTxn(this.bank.acctid);
		this.ntx.setDate(today);
		this.ntx.setAmount(new BigDecimal("1.23"));

		bank.addTransaction(this.ntx);

		stock = new Security("FOO", "Foo, Inc");
		stock.addPrice(new QPrice(today, stock.secid, new BigDecimal("1.23")));

		this.itx = new InvestmentTxn(this.invest.acctid);
		this.itx.setAction(TxAction.BUY);
		this.itx.setDate(today);
		this.itx.setSecurity(stock);
		this.itx.setQuantity(new BigDecimal("1.0"));

		this.invest.addTransaction(this.itx);

		this.nstat = new Statement(this.bank.acctid, today, //
				new BigDecimal("1.23"), new BigDecimal("1.23"), null);
		this.nstat.addTransaction(this.ntx);
		this.bank.addStatement(this.nstat);

		this.istat = new Statement(this.invest.acctid, today, null);
		// TODO this.istat.addTransaction(this.itx);
		this.invest.addStatement(this.istat);

		this.tmpstat = new Statement(this.bank.acctid, today, //
				new BigDecimal("1.23"), new BigDecimal("1.23"), null);
	}

	@AfterEach
	void tearDown() throws Exception {
		MoneyMgrModel.deleteModel("test-model");
	}

	@Test
	void testStatementIntQDateBigDecimalBigDecimalStatement() {
		Statement s = new Statement(this.bank.acctid, this.today, BigDecimal.ZERO, BigDecimal.ZERO, null);
		Assert.assertNotNull(s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testStatementIntQDateStatement() {
		Statement s = new Statement(this.bank.acctid, this.today, null);
		Assert.assertNotNull(s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testAddTransactionsCollectionOfGenericTxn() {
		List<GenericTxn> txns = new ArrayList<>();
		GenericTxn tx = new NonInvestmentTxn(bank.acctid);
		txns.add(tx);
		this.tmpstat.addTransactions(txns);

		// Assert.assertTrue(this.tmpstat.get);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testAddTransactionsCollectionOfGenericTxnBoolean() {
		List<GenericTxn> txns = new ArrayList<>();
		GenericTxn tx = new NonInvestmentTxn(bank.acctid);
		tx.setDate(this.today.addDays(1));
		txns.add(tx);

		Assert.assertTrue(this.tmpstat.getTransactionsForReconcile().isEmpty());
		this.tmpstat.addTransactions(txns, true);
		Assert.assertTrue(this.tmpstat.getTransactionsForReconcile().isEmpty());

		tx.setDate(this.today);
		this.tmpstat.addTransactions(txns, true);
		Assert.assertTrue(this.tmpstat.getTransactionsForReconcile().contains(tx));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testSanityCheck() {
		this.nstat.sanityCheck();
		this.tmpstat.sanityCheck();

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetSetCashBalance() {
		Statement stat;
		
		stat = new Statement(this.bank.acctid, this.today, null, null, null);
		Assert.assertNotNull(stat);
		Assert.assertNull(stat.getCashBalance());

		BigDecimal val = new BigDecimal("1.23");
		stat.setCashBalance(val);

		Assert.assertTrue(Common.isEffectivelyEqual(val, stat.getCashBalance()));
		
		stat = new Statement(this.bank.acctid, this.today, val, null, null);
		Assert.assertNotNull(stat);
		Assert.assertTrue(Common.isEffectivelyEqual(val, stat.getCashBalance()));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testAddTransaction() {
		GenericTxn tx = new NonInvestmentTxn(bank.acctid);
		this.tmpstat.addTransaction(tx);

		// fail("Not yet implemented");
	}

	@Test
	void testGetOpeningDate() {
		QDate open = this.tmpstat.getOpeningDate();
		Assert.assertNotNull(open);
		Assert.assertEquals(0, open.compareTo(this.today));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetOpeningBalance() {
		BigDecimal bal = this.nstat.getOpeningBalance();
		Assert.assertNotNull(bal);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetOpeningCashBalance() {
		BigDecimal bal = this.nstat.getOpeningCashBalance();
		Assert.assertNotNull(bal);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetClearedCashBalance() {
		BigDecimal bal = this.nstat.getClearedCashBalance();
		Assert.assertNotNull(bal);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetCredits() {
		BigDecimal bal = this.nstat.getCredits();
		Assert.assertNotNull(bal);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetDebits() {
		BigDecimal bal = this.nstat.getDebits();
		Assert.assertNotNull(bal);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetTransactionsForReconcile() {
		List<GenericTxn> txns = this.nstat.getTransactionsForReconcile();
		Assert.assertNotNull(txns);
		Assert.assertFalse(txns.isEmpty());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testToggleCleared() {
		Assert.assertNull(this.ntx.getStatementDate());
		this.nstat.toggleCleared(this.ntx);
		Assert.assertNotNull(this.ntx.getStatementDate());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testClearTransaction() {
		Assert.assertNull(this.ntx.getStatementDate());
		this.nstat.clearTransaction(this.ntx);
		Assert.assertNotNull(this.ntx.getStatementDate());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testUnclearTransaction() {
		this.nstat.clearTransaction(this.ntx);
		Assert.assertNotNull(this.ntx.getStatementDate());

		this.nstat.unclearTransaction(this.ntx);
		Assert.assertNull(this.ntx.getStatementDate());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testClearAllTransactions() {
		this.nstat.clearAllTransactions();
		Assert.assertNotNull(this.ntx.getStatementDate());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testUnclearAllTransactions() {
		this.nstat.clearTransaction(this.ntx);
		Assert.assertNotNull(this.ntx.getStatementDate());

		this.nstat.unclearAllTransactions();
		Assert.assertNull(this.ntx.getStatementDate());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testClearTransactions() {
		Assert.assertNull(this.ntx.getStatementDate());

		List<GenericTxn> txns = new ArrayList<>();
		txns.add(this.ntx);
		this.nstat.clearTransactions(txns);
		Assert.assertNotNull(this.ntx.getStatementDate());

		this.nstat.unclearAllTransactions();
		Assert.assertNull(this.ntx.getStatementDate());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testUnclearTransactions() {
		Assert.assertNull(this.ntx.getStatementDate());

		this.nstat.clearTransaction(this.ntx);
		Assert.assertNotNull(this.ntx.getStatementDate());

		List<GenericTxn> txns = new ArrayList<>();
		txns.add(this.ntx);
		this.nstat.unclearTransactions(txns);
		Assert.assertNull(this.ntx.getStatementDate());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testCashMatches() {
		Assert.assertTrue(this.nstat.cashMatches());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testCashMatchesListOfGenericTxn() {
		List<GenericTxn> txns = new ArrayList<>();
		txns.add(this.ntx);
		Assert.assertTrue(this.nstat.cashMatches(txns));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetCashDifference() {
		BigDecimal amt = this.nstat.getCashDifference();
		Assert.assertNotNull(amt);
		Assert.assertTrue(Common.isEffectivelyZero(amt));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetCashDeltaListOfGenericTxn() {
		List<GenericTxn> txns = new ArrayList<>();
		txns.add(this.ntx);

		BigDecimal amt = this.nstat.getCashDelta(txns);
		Assert.assertNotNull(amt);
		Assert.assertFalse(Common.isEffectivelyZero(amt));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetCashDelta() {
		BigDecimal amt = this.nstat.getCashDelta();
		Assert.assertNotNull(amt);
		Assert.assertFalse(Common.isEffectivelyZero(amt));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testHoldingsMatch() {
		fail("Not yet implemented");
	}

	@Test
	void testGetPortfolioDelta() {
		fail("Not yet implemented");
	}

	@Test
	void testGetPortfolioDeltaListOfGenericTxn() {
		fail("Not yet implemented");
	}

	@Test
	void testToString() {
		String s = this.nstat.toString();
		Assert.assertNotNull(s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testMatchesStatement() {
		Assert.assertNull(this.nstat.matches(this.nstat));
		Assert.assertNotNull(this.nstat.matches(this.istat));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testMatchesStatementBoolean() {
		Assert.assertNull(this.nstat.matches(this.nstat, false));
		Assert.assertNull(this.nstat.matches(this.nstat, true));
		Assert.assertNotNull(this.nstat.matches(this.istat, false));
		Assert.assertNotNull(this.nstat.matches(this.istat, true));

		// TODO fail("Not yet implemented");
	}

}
