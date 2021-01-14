package moneymgr.model.test;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import moneymgr.model.Account;
import moneymgr.model.AccountType;
import moneymgr.model.InvestmentTxn;
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.QPrice;
import moneymgr.model.Security;
import moneymgr.model.SecurityPortfolio;
import moneymgr.model.SecurityPosition;
import moneymgr.model.TxAction;
import moneymgr.util.Common;
import moneymgr.util.QDate;

class SecurityPositionTest {

	MoneyMgrModel model;
	Account invest;
	QDate today;
	InvestmentTxn tx;
	Security stock;
	SecurityPortfolio portfolio;
	SecurityPosition position;

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

		this.invest = new Account("asset", AccountType.Invest);
		this.model.addAccount(this.invest);

		this.today = QDate.today();
		this.model.setCurrentDate(this.today);
		this.model.setAsOfDate(this.today);

		stock = new Security("FOO", "Foo, Inc");
		stock.addPrice(new QPrice(today, stock.secid, new BigDecimal("1.23")));

		this.tx = new InvestmentTxn(this.invest.acctid);
		this.tx.setAction(TxAction.BUY);
		this.tx.setDate(today);
		this.tx.setSecurity(stock);
		this.tx.setQuantity(new BigDecimal("1.0"));

		invest.addTransaction(this.tx);

		this.portfolio = new SecurityPortfolio(MoneyMgrModel.currModel, null);

		this.position = new SecurityPosition(this.portfolio, this.stock);
		this.position.addTransaction(this.tx);
	}

	@AfterEach
	void tearDown() throws Exception {
		MoneyMgrModel.deleteModel("test-model");
	}

	@Test
	void testSecurityPositionConstructors() {

		SecurityPosition pos = new SecurityPosition(this.portfolio, this.stock);
		assertNotNull(pos);

		SecurityPosition newpos = new SecurityPosition(this.portfolio, pos);
		assertNotNull(newpos);

		fail("Not yet implemented");
	}

	@Test
	void testIsEmpty() {
		assertFalse(this.position.isEmpty());

		fail("Not yet implemented");
	}

	@Test
	void testIsEmptyForDate() {
		assertFalse(this.position.isEmptyForDate(this.today));

		fail("Not yet implemented");
	}

	@Test
	void testGetEndingValue() {
		BigDecimal val = this.position.getEndingValue();
		assertTrue(Common.isEffectivelyZero(val));

		fail("Not yet implemented");
	}

	@Test
	void testGetStartingShares() {
		BigDecimal shares = this.position.getStartingShares();
		assertTrue(Common.isEffectivelyZero(shares));

		fail("Not yet implemented");
	}

	@Test
	void testGetEndingShares() {
		BigDecimal shares = this.position.getEndingShares();
		assertTrue(Common.isEffectivelyZero(shares));

		fail("Not yet implemented");
	}

	@Test
	void testGetExpectedEndingShares() {
		BigDecimal shares = this.position.getExpectedEndingShares();
		assertTrue(Common.isEffectivelyZero(shares));

		fail("Not yet implemented");
	}

	@Test
	void testSetExpectedEndingShares() {
		BigDecimal newshares = new BigDecimal("1.23");

		BigDecimal shares = this.position.getExpectedEndingShares();
		assertTrue(Common.isEffectivelyZero(shares));

		this.position.setExpectedEndingShares(newshares);

		shares = this.position.getExpectedEndingShares();
		assertTrue(Common.isEffectivelyEqual(newshares, shares));

		fail("Not yet implemented");
	}

	@Test
	void testSetEndingValue() {
		BigDecimal newval = new BigDecimal("100.00");

		this.position.setEndingValue(newval);

		BigDecimal val = this.position.getEndingValue();
		assertTrue(Common.isEffectivelyEqual(val, newval));

		fail("Not yet implemented");
	}

	@Test
	void testGetShareBalances() {
		List<BigDecimal> bals = this.position.getShareBalances();
		assertNotNull(bals);
		assertFalse(bals.isEmpty());

		fail("Not yet implemented");
	}

	@Test
	void testInitializeTransactions() {
		List<InvestmentTxn> txns = this.position.getTransactions();
		assertNotNull(txns);
		assertFalse(txns.isEmpty());

		List<BigDecimal> bals = this.position.getShareBalances();
		assertNotNull(bals);
		assertFalse(bals.isEmpty());

		BigDecimal val = this.position.getEndingValue();
		assertFalse(Common.isEffectivelyZero(val));

		this.position.initializeTransactions();

		txns = this.position.getTransactions();
		assertNotNull(txns);
		assertTrue(txns.isEmpty());

		bals = this.position.getShareBalances();
		assertNotNull(bals);
		assertTrue(bals.isEmpty());

		val = this.position.getEndingValue();
		assertTrue(Common.isEffectivelyZero(val));

		fail("Not yet implemented");
	}

	@Test
	void testGetFirstTransactionDate() {
		QDate date = this.position.getFirstTransactionDate();
		assertNotNull(date);
		assertEquals(this.today, date);

		fail("Not yet implemented");
	}

	@Test
	void testGetTransactions() {
		List<InvestmentTxn> txns = this.position.getTransactions();
		assertNotNull(txns);
		assertFalse(txns.isEmpty());

		fail("Not yet implemented");
	}

	@Test
	void testAddTransaction() {
		fail("Not yet implemented");
	}

	@Test
	void testRemoveTransaction() {
		fail("Not yet implemented");
	}

	@Test
	void testSetTransactions() {
		fail("Not yet implemented");
	}

	@Test
	void testUpdateShareBalances() {
		fail("Not yet implemented");
	}

	@Test
	void testGetValueForDate() {
		fail("Not yet implemented");
	}

	@Test
	void testGetSharesForDate() {
		fail("Not yet implemented");
	}

	@Test
	void testGetPositionForDate() {
		fail("Not yet implemented");
	}

	@Test
	void testToString() {
		fail("Not yet implemented");
	}

}
