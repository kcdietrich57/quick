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
import moneymgr.model.NonInvestmentTxn;
import moneymgr.model.QPrice;
import moneymgr.model.Security;
import moneymgr.model.TxAction;
import moneymgr.model.InvestmentTxn.ShareAction;
import moneymgr.util.Common;
import moneymgr.util.QDate;

class ITxTest {

	MoneyMgrModel model;
	Account bank;
	Account invest;
	QDate today;
	InvestmentTxn itx;
	InvestmentTxn itx2;
	NonInvestmentTxn ntx;
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

		this.bank = new Account("bank", AccountType.Bank);
		this.model.addAccount(this.bank);

		this.invest = new Account("invest", AccountType.Invest);
		this.model.addAccount(this.invest);

		this.today = QDate.today();
		this.model.setCurrentDate(this.today);
		this.model.setAsOfDate(this.today);

		stock = new Security("FOO", "Foo, Inc");
		stock.addPrice(new QPrice(today, stock.secid, new BigDecimal("1.23")));

		this.ntx = new NonInvestmentTxn(this.bank.acctid);
		this.ntx.setDate(today);
		this.ntx.setAmount(new BigDecimal("1.23"));

		this.itx = new InvestmentTxn(this.invest.acctid);
		this.itx.setAction(TxAction.BUYX);
		this.itx.setDate(today);
		this.itx.setSecurity(stock);
		this.itx.setQuantity(new BigDecimal("1.0"));
		// TODO why do we need to do both of these? Setting tx should be sufficient
		this.itx.setCashTransferAcctid(this.bank.acctid);
		this.itx.cashTransferred = this.ntx.getAmount();
		this.itx.setCashTransferTxn(this.ntx);

		invest.addTransaction(this.itx);

		this.itx2 = new InvestmentTxn(this.invest.acctid);
		this.itx2.setAction(TxAction.BUY);
		this.itx2.setDate(today);
		this.itx2.setSecurity(stock);
		this.itx2.setQuantity(new BigDecimal("1.0"));

		invest.addTransaction(this.itx2);
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
		Assert.assertFalse(this.itx.removesShares());
		Assert.assertFalse(this.itx.addsShares());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetAction() {
		Assert.assertEquals(TxAction.BUYX, this.itx.getAction());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testSetAction() {
		Assert.assertEquals(TxAction.BUYX, this.itx.getAction());

		this.itx.setAction(TxAction.SELL);
		Assert.assertEquals(TxAction.SELL, this.itx.getAction());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetSecurityTransferTxns() {
		List<InvestmentTxn> txns = this.itx.getSecurityTransferTxns();
		Assert.assertNotNull(txns);
		Assert.assertTrue(txns.isEmpty());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetCashTransferAmount() {
		BigDecimal amt;

		amt = this.itx.getCashTransferAmount();
		Assert.assertNotNull(amt);
		Assert.assertTrue(Common.isEffectivelyEqual(this.ntx.getAmount(), amt));

		amt = this.itx2.getCashTransferAmount();
		Assert.assertNotNull(amt);
		Assert.assertTrue(Common.isEffectivelyZero(amt));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetCashAmount() {
		BigDecimal amt;

		amt = this.itx.getCashAmount();
		Assert.assertNotNull(amt);
		Assert.assertTrue(Common.isEffectivelyZero(amt));

		amt = this.itx2.getCashAmount();
		Assert.assertNotNull(amt);
		Assert.assertFalse(Common.isEffectivelyZero(amt));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetSecurity() {
		Security sec = this.itx.getSecurity();
		Assert.assertEquals(this.stock, sec);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testFormatValue() {
		this.itx.setCashTransferTxn(this.ntx);
		String s = this.itx.formatValue();
		Assert.assertNotNull(s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testToStringShort() {
		String s = this.itx.toStringShort(false);
		Assert.assertNotNull(s);

		s = this.itx.toStringShort(true);
		Assert.assertNotNull(s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testToStringLong() {
		String s = this.itx.toStringLong();
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
		InvestmentTxn txcopy = new InvestmentTxn(this.invest.acctid, this.itx);
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
		List<InvestmentTxn> txns = this.itx.getSecurityTransferTxns();
		Assert.assertNotNull(txns);
		Assert.assertTrue(txns.isEmpty());

		txns.add(this.itx);
		this.itx.setSecurityTransferTxns(txns);

		txns = this.itx.getSecurityTransferTxns();
		Assert.assertNotNull(txns);
		Assert.assertFalse(txns.isEmpty());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testAddSecurityTransferTxn() {
		List<InvestmentTxn> txns = this.itx.getSecurityTransferTxns();
		Assert.assertNotNull(txns);
		Assert.assertTrue(txns.isEmpty());

		this.itx.addSecurityTransferTxn(this.itx);

		txns = this.itx.getSecurityTransferTxns();
		Assert.assertNotNull(txns);
		Assert.assertFalse(txns.isEmpty());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetSetQuantity() {
		Assert.assertFalse(Common.isEffectivelyEqual( //
				new BigDecimal("2.0"), this.itx.getQuantity()));

		this.itx.setQuantity(new BigDecimal("2.0"));
		Assert.assertTrue(Common.isEffectivelyEqual( //
				new BigDecimal("2.0"), this.itx.getQuantity()));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetShares() {
		this.itx.setQuantity(new BigDecimal("2.0"));

		BigDecimal q = this.itx.getShares();
		BigDecimal q2 = this.itx.getQuantity();
		Assert.assertTrue(Common.isEffectivelyEqual(q, q2));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testIsStockOptionTxn() {
		Assert.assertFalse(this.itx.isStockOptionTxn());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetShareAction() {
		ShareAction act = this.itx.getShareAction();
		Assert.assertEquals(ShareAction.NEW_SHARES, act);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetShareCost() {
		BigDecimal cost = this.itx.getShareCost();
		Assert.assertTrue(Common.isEffectivelyEqual(new BigDecimal("1.23"), cost));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetSplitRatio() {
		BigDecimal r = this.itx.getSplitRatio();
		Assert.assertNotNull(r);
		Assert.assertTrue(Common.isEffectivelyEqual(new BigDecimal("1.0"), r));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetLots() {
		List<Lot> lots = this.itx.getLots();
		Assert.assertNotNull(lots);
		Assert.assertTrue(lots.isEmpty());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetCreatedLots() {
		List<Lot> lots = this.itx.getCreatedLots();
		Assert.assertNotNull(lots);
		Assert.assertTrue(lots.isEmpty());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetDisposedLots() {
		List<Lot> lots = this.itx.getDisposedLots();
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
		BigDecimal amt = this.itx.getBuySellAmount();
		Assert.assertNotNull(amt);
		Assert.assertTrue(Common.isEffectivelyEqual(BigDecimal.ONE, amt));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testMatchesInvestmentTxn() {
		Assert.assertNull(this.itx.matches(this.itx));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testShareAction() {
		ShareAction act = ShareAction.DISPOSE_SHARES;
		Assert.assertEquals("Dispose", act.toString());
	}
}
