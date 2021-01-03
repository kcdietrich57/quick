package moneymgr.model.test;

import java.math.BigDecimal;
import java.util.List;

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
import moneymgr.model.StockOption;
import moneymgr.model.TxAction;
import moneymgr.util.QDate;

class StockOptionTest {

	MoneyMgrModel model;
	Account invest;
	QDate today;
	InvestmentTxn itx;
	Security stock;
	StockOption opt;

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

		this.invest = new Account("invest", AccountType.Invest);
		this.model.addAccount(this.invest);

		this.today = QDate.today();
		this.model.setCurrentDate(this.today);
		this.model.setAsOfDate(this.today);

		stock = new Security("FOO", "Foo, Inc");
		stock.addPrice(new QPrice(today, stock.secid, new BigDecimal("1.23")));

		this.itx = new InvestmentTxn(this.invest.acctid);
		this.itx.setAction(TxAction.BUY);
		this.itx.setDate(today);
		this.itx.setSecurity(stock);
		this.itx.setQuantity(new BigDecimal("1.0"));

		this.invest.addTransaction(this.itx);

		BigDecimal v = new BigDecimal("1.00");

		this.opt = new StockOption(null, 99, "new-option", today, //
				this.invest.acctid, this.stock.secid, //
				v, v, v, v, v, //
				120, 480, 4);
		MoneyMgrModel.currModel.addStockOption(opt);
	}

	@AfterEach
	void tearDown() throws Exception {
		MoneyMgrModel.deleteModel("test-model");
	}

	@Test
	void testEsppPurchase() {
		StockOption o = StockOption.esppPurchase( //
				today, this.invest.acctid, this.stock.secid, //
				new BigDecimal("1.0"), new BigDecimal("1.23"), //
				new BigDecimal("1.0"), new BigDecimal("2.0"), //
				new BigDecimal("2.0"));
		Assert.assertNotNull(o);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGrant() {
		StockOption o = StockOption.grant( //
				"the-option", today, this.invest.acctid, this.stock.secid, //
				new BigDecimal("1.00"), new BigDecimal("2.0"), //
				48, 4, 120);
		Assert.assertNotNull(o);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testVest() {
		StockOption o = StockOption.vest("the-option", today, 1);
		Assert.assertNotNull(o);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testSplit() {
		StockOption o = StockOption.split("the-option", this.today, 2, 1);
		Assert.assertNotNull(o);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testExpire() {
		StockOption o = StockOption.expire("the-option", this.today);
		Assert.assertNotNull(o);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testCancel() {
		StockOption o = StockOption.cancel("the-option", this.today);
		Assert.assertNotNull(o);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testExercise() {
		StockOption o = StockOption.exercise("the-option", today, new BigDecimal("0.23"));
		Assert.assertNotNull(o);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testProcessEspp() {
		StockOption.processEspp(this.itx);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testProcessGrant() {
		StockOption.processGrant(this.itx);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testProcessVest() {
		StockOption.processVest(this.itx);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testProcessSplit() {
		StockOption.processSplit(this.itx);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testProcessExercise() {
		StockOption.processExercise(this.itx);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testProcessExpire() {
		StockOption.processExpire(this.itx);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetOpenOption() {
		StockOption o = StockOption.getOpenOption("the-option");
		Assert.assertNotNull(o);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetOpenOptions() {
		List<StockOption> opts = StockOption.getOpenOptions();
		Assert.assertNotNull(opts);
		Assert.assertTrue(opts.isEmpty());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetOpenOptionsAccountQDate() {
		List<StockOption> opts = StockOption.getOpenOptions(this.invest, this.today);
		Assert.assertNotNull(opts);
		Assert.assertTrue(opts.isEmpty());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testStockOption() {
		BigDecimal shares = new BigDecimal("1.00");
		BigDecimal strike = new BigDecimal("1.00");
		BigDecimal cost = new BigDecimal("1.00");
		BigDecimal mkt = new BigDecimal("1.00");
		BigDecimal origmkt = new BigDecimal("1.00");

		StockOption o = new StockOption(null, 99, "new-option", today, //
				this.invest.acctid, this.stock.secid, //
				shares, strike, cost, mkt, origmkt, //
				120, 480, 4);
		Assert.assertNotNull(o);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testIsLiveOn() {
		Assert.assertFalse(this.opt.isLiveOn(this.today));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetDiscount() {
		BigDecimal amt = this.opt.getDiscount();
		Assert.assertNotNull(amt);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetBasisPrice() {
		BigDecimal amt = this.opt.getBasisPrice(this.today);
		Assert.assertNotNull(amt);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetValueForDate() {
		BigDecimal amt = this.opt.getValueForDate(this.today);
		Assert.assertNotNull(amt);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetAvailableShares() {
		BigDecimal amt = this.opt.getAvailableShares();
		Assert.assertNotNull(amt);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetAvailableSharesQDate() {
		BigDecimal amt = this.opt.getAvailableShares(this.today);
		Assert.assertNotNull(amt);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetAvailableSharesBoolean() {
		BigDecimal amt = this.opt.getAvailableShares(false);
		Assert.assertNotNull(amt);

		amt = this.opt.getAvailableShares(true);
		Assert.assertNotNull(amt);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetGrantDate() {
		QDate date = this.opt.getGrantDate();
		Assert.assertNotNull(date);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetVestNum() {
		int num = this.opt.getVestNum(this.today);
		Assert.assertEquals(0, num);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testToString() {
		String s = this.opt.toString();
		Assert.assertNotNull(s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testFormatInfo() {
		String s = this.opt.formatInfo(this.today);
		Assert.assertNotNull(s);

		// TODO fail("Not yet implemented");
	}
}
