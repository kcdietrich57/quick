package moneymgr.model.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

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
import moneymgr.model.InvestmentTxn;
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.QPrice;
import moneymgr.model.Security;
import moneymgr.model.SecurityPortfolio;
import moneymgr.model.SecurityPosition;
import moneymgr.model.SecurityPosition.PositionInfo;
import moneymgr.model.TxAction;
import moneymgr.util.QDate;

class SecurityPortfolioTest {

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
	void testSecurityPortfolio() {
		assertNotNull(this.portfolio);

		//fail("Not yet implemented");
	}

	@Test
	void testInitializeTransactions() {
		Assert.assertFalse(this.portfolio.isEmpty());
		this.portfolio.initializeTransactions();
		Assert.assertTrue(this.portfolio.isEmpty());

		//fail("Not yet implemented");
	}

	@Test
	void testAddTransaction() {
		fail("Not yet implemented");
	}

	@Test
	void testRemoveTransaction() {
		Assert.assertFalse(this.portfolio.isEmpty());
		this.portfolio.removeTransaction(this.tx);
		Assert.assertTrue(this.portfolio.isEmpty());

		//fail("Not yet implemented");
	}

	@Test
	void testFindPosition() {
		SecurityPosition pos = this.portfolio.findPosition(this.stock);
		Assert.assertNotNull(pos);

		//fail("Not yet implemented");
	}

	@Test
	void testCreatePosition() {
		this.portfolio.createPosition(this.stock.secid);

		//fail("Not yet implemented");
	}

	@Test
	void testGetPositions() {
		Assert.assertFalse(this.portfolio.isEmpty());
		List<SecurityPosition> plist = this.portfolio.getPositions();
		Assert.assertNotNull(plist);

		//fail("Not yet implemented");
	}

	@Test
	void testGetPositionInt() {
		SecurityPosition pos = this.portfolio.getPosition(this.stock.secid);
		Assert.assertNotNull(pos);

		//fail("Not yet implemented");
	}

	@Test
	void testGetPositionSecurity() {
		SecurityPosition pos = this.portfolio.getPosition(this.stock);
		Assert.assertNotNull(pos);

		//fail("Not yet implemented");
	}

	@Test
	void testPurgeEmptyPositions() {
		this.portfolio.purgeEmptyPositions();

		//fail("Not yet implemented");
	}

	@Test
	void testGetOpenPositionsForDate() {
		Map<Security, PositionInfo> pmap = this.portfolio.getOpenPositionsForDate(this.today);
		Assert.assertNotNull(pmap);

		//fail("Not yet implemented");
	}

	@Test
	void testGetOpenPositionsForDateByAccount() {
		Map<Account, PositionInfo> pmap = this.portfolio.getOpenPositionsForDateByAccount(this.stock, this.today);
		Assert.assertNotNull(pmap);

		//fail("Not yet implemented");
	}

	@Test
	void testGetPortfolioValueForDate() {
		BigDecimal v = this.portfolio.getPortfolioValueForDate(this.today);
		Assert.assertNotNull(v);

		//fail("Not yet implemented");
	}

	@Test
	void testComparisonTo() {
		fail("Not yet implemented");
	}

	@Test
	void testSize() {
		Assert.assertTrue(this.portfolio.size() > 0);

		//fail("Not yet implemented");
	}

	@Test
	void testIsEmpty() {
		Assert.assertFalse(this.portfolio.isEmpty());

		//fail("Not yet implemented");
	}

	@Test
	void testIsEmptyForDate() {
		Assert.assertTrue(this.portfolio.isEmptyForDate(this.today.addDays(-1)));
		Assert.assertFalse(this.portfolio.isEmptyForDate(this.today));
		Assert.assertFalse(this.portfolio.isEmptyForDate(this.today.addDays(1)));

		//fail("Not yet implemented");
	}

	@Test
	void testToString() {
		String s = this.portfolio.toString();
		Assert.assertNotNull(s);

		//fail("Not yet implemented");
	}

	@Test
	void testGetFirstTransactionDate() {
		Assert.assertEquals(this.today, this.portfolio.getFirstTransactionDate());

		//fail("Not yet implemented");
	}

	@Test
	void testMatches() {
		Assert.assertNull(this.portfolio.matches(this.portfolio));

		SecurityPortfolio newp = new SecurityPortfolio(this.invest, null);
		Assert.assertNotNull(this.portfolio.matches(newp));

		//fail("Not yet implemented");
	}

}
