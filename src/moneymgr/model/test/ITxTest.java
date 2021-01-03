package moneymgr.model.test;

import static org.junit.jupiter.api.Assertions.fail;

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
import moneymgr.model.Lot;
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.QPrice;
import moneymgr.model.Security;
import moneymgr.model.TxAction;
import moneymgr.model.InvestmentTxn.ShareAction;
import moneymgr.util.Common;
import moneymgr.util.QDate;

class ITxTest {

	MoneyMgrModel model;
	Account invest;
	QDate today;
	InvestmentTxn tx;
	Security stock;

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

		this.tx = new InvestmentTxn(this.invest.acctid);
		this.tx.setAction(TxAction.BUY);
		this.tx.setDate(today);
		this.tx.setSecurity(stock);
		this.tx.setQuantity(new BigDecimal("1.0"));

		invest.addTransaction(this.tx);
	}

	@AfterEach
	void tearDown() throws Exception {
		MoneyMgrModel.deleteModel("test-model");
	}

	@Test
	void testCompareWith() {
		// this.tx.compareWith(txInfo, tx);

		fail("Not yet implemented");
	}

	@Test
	void testRemovesShares() {
		Assert.assertFalse(this.tx.removesShares());
		Assert.assertFalse(this.tx.addsShares());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetAction() {
		Assert.assertEquals(TxAction.BUY, this.tx.getAction());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testSetAction() {
		Assert.assertEquals(TxAction.BUY, this.tx.getAction());

		this.tx.setAction(TxAction.SELL);
		Assert.assertEquals(TxAction.SELL, this.tx.getAction());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetSecurityTransferTxns() {
		List<InvestmentTxn> txns = this.tx.getSecurityTransferTxns();
		Assert.assertNotNull(txns);
		Assert.assertTrue(txns.isEmpty());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetCashTransferAmount() {
		BigDecimal amt = this.tx.getCashTransferAmount();
		Assert.assertNotNull(amt);
		Assert.assertTrue(Common.isEffectivelyZero(amt));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetCashAmount() {
		BigDecimal amt = this.tx.getCashAmount();
		Assert.assertNotNull(amt);
		Assert.assertTrue(Common.isEffectivelyZero(amt));
		Assert.assertTrue(Common.isEffectivelyEqual(new BigDecimal("-1.23"), amt));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetSecurity() {
		Security sec = this.tx.getSecurity();
		Assert.assertEquals(this.stock, sec);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testFormatValue() {
		String s = this.tx.formatValue();
		Assert.assertNotNull(s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testToStringShort() {
		String s = this.tx.toStringShort(false);
		Assert.assertNotNull(s);

		s = this.tx.toStringShort(true);
		Assert.assertNotNull(s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testToStringLong() {
		String s = this.tx.toStringLong();
		Assert.assertNotNull(s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testRepair() {
		// this.tx.repair(txinfo);

		fail("Not yet implemented");
	}

	@Test
	void testInvestmentTxnIntInt() {
		InvestmentTxn tx = new InvestmentTxn(99, this.invest.acctid);
		Assert.assertNotNull(tx);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testInvestmentTxnInt() {
		InvestmentTxn tx = new InvestmentTxn(this.invest.acctid);
		Assert.assertNotNull(tx);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testInvestmentTxnIntInvestmentTxn() {
		InvestmentTxn txcopy = new InvestmentTxn(this.invest.acctid, this.tx);
		Assert.assertNotNull(txcopy);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testSetSecurity() {
		InvestmentTxn newtx = new InvestmentTxn(this.invest.acctid);
		Assert.assertNotNull(newtx);
		Assert.assertNull(newtx.getSecurity());

		newtx.setSecurity(this.stock);
		Assert.assertEquals(this.stock, newtx.getSecurity());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testSetSecurityTransferTxns() {
		List<InvestmentTxn> txns = this.tx.getSecurityTransferTxns();
		Assert.assertNotNull(txns);
		Assert.assertTrue(txns.isEmpty());

		txns.add(this.tx);
		this.tx.setSecurityTransferTxns(txns);

		txns = this.tx.getSecurityTransferTxns();
		Assert.assertNotNull(txns);
		Assert.assertFalse(txns.isEmpty());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testAddSecurityTransferTxn() {
		List<InvestmentTxn> txns = this.tx.getSecurityTransferTxns();
		Assert.assertNotNull(txns);
		Assert.assertTrue(txns.isEmpty());

		this.tx.addSecurityTransferTxn(this.tx);

		txns = this.tx.getSecurityTransferTxns();
		Assert.assertNotNull(txns);
		Assert.assertFalse(txns.isEmpty());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetSetQuantity() {
		Assert.assertFalse(Common.isEffectivelyEqual( //
				new BigDecimal("2.0"), this.tx.getQuantity()));

		this.tx.setQuantity(new BigDecimal("2.0"));
		Assert.assertTrue(Common.isEffectivelyEqual( //
				new BigDecimal("2.0"), this.tx.getQuantity()));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetShares() {
		this.tx.setQuantity(new BigDecimal("2.0"));

		BigDecimal q = this.tx.getShares();
		BigDecimal q2 = this.tx.getQuantity();
		Assert.assertTrue(Common.isEffectivelyEqual(q, q2));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testIsStockOptionTxn() {
		Assert.assertFalse(this.tx.isStockOptionTxn());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetShareAction() {
		ShareAction act = this.tx.getShareAction();
		Assert.assertEquals(ShareAction.NEW_SHARES, act);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetShareCost() {
		BigDecimal cost = this.tx.getShareCost();
		Assert.assertTrue(Common.isEffectivelyEqual(new BigDecimal("1.23"), cost));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetSplitRatio() {
		BigDecimal r = this.tx.getSplitRatio();
		Assert.assertNotNull(r);
		Assert.assertTrue(Common.isEffectivelyEqual(new BigDecimal("1.0"), r));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetLots() {
		List<Lot> lots = this.tx.getLots();
		Assert.assertNotNull(lots);
		Assert.assertTrue(lots.isEmpty());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetCreatedLots() {
		List<Lot> lots = this.tx.getCreatedLots();
		Assert.assertNotNull(lots);
		Assert.assertTrue(lots.isEmpty());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetDisposedLots() {
		List<Lot> lots = this.tx.getDisposedLots();
		Assert.assertNotNull(lots);
		Assert.assertTrue(lots.isEmpty());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testAddLot() {
		fail("Not yet implemented");
	}

	@Test
	void testAddCreatedLot() {
		fail("Not yet implemented");
	}

	@Test
	void testAddDisposedLot() {
		fail("Not yet implemented");
	}

	@Test
	void testLotListMatches() {
		fail("Not yet implemented");
	}

	@Test
	void testGetBuySellAmount() {
		BigDecimal amt = this.tx.getBuySellAmount();
		Assert.assertNotNull(amt);
		Assert.assertTrue(Common.isEffectivelyEqual(BigDecimal.ONE, amt));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testMatchesInvestmentTxn() {
		Assert.assertNull(this.tx.matches(this.tx));

		// TODO fail("Not yet implemented");
	}

}
