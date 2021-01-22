package moneymgr.model.test;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

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
import moneymgr.util.Common;
import moneymgr.util.QDate;

class LotTest {

	MoneyMgrModel model;
	Account invest;
	Account invest2;
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
		this.invest2 = new Account("invest2", AccountType.Invest);
		this.model.addAccount(this.invest2);

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
	void testLotConstructors() {
		QPrice price = this.stock.getPriceForDate(this.today);

		// New lot for deposit
		// ========================================================

		QDate date1 = this.today.addDays(-10);
		InvestmentTxn srctx1 = new InvestmentTxn(this.invest.acctid);
		srctx1.setDate(date1);

		BigDecimal shares1 = new BigDecimal("20.0");
		BigDecimal cost1 = price.getPrice().multiply(shares1);

		Lot lot1 = new Lot(this.invest.acctid, date1, this.stock.secid, //
				shares1, price.getPrice(), srctx1);

		assertNotNull(lot1);
		assertEquals(lot1.acctid, this.invest.acctid);
		assertEquals(0, lot1.getAcquisitionDate().compareTo(date1));
		assertEquals(lot1.shares, shares1);
		assertEquals(lot1.getCostBasis(), cost1);
		assertTrue(lot1.getChildLots().isEmpty());
		assertNull(lot1.getDisposingTransaction());
		assertNull(lot1.getSourceLot());
		assertTrue(lot1.isOpen());
		assertNotNull(lot1.matches(null));
		assertNull(lot1.matches(lot1));

		// Split a lot - partial sale/transfer
		// ========================================================

		QDate date2 = date1.addDays(1);
		InvestmentTxn srctx2 = new InvestmentTxn(this.invest.acctid);
		srctx2.setDate(date2);

		BigDecimal shares2 = new BigDecimal("14.0");
		BigDecimal cost2 = price.getPrice().multiply(shares2);

		Lot lot2 = new Lot(lot1, this.invest.acctid, shares2, srctx2);

		assertNotNull(lot2);
		assertEquals(lot2.acctid, this.invest.acctid);
		assertEquals(0, lot2.getAcquisitionDate().compareTo(date1));
		assertEquals(lot2.shares, shares2);
		assertEquals(lot2.getCostBasis(), cost2);
		assertTrue(lot2.getChildLots().isEmpty());
		assertNull(lot2.getDisposingTransaction());
		assertEquals(lot1, lot2.getSourceLot());
		assertTrue(lot2.isOpen());
		assertNotNull(lot2.matches(lot1));

		assertFalse(lot1.getChildLots().isEmpty());
		assertTrue(lot1.getChildLots().contains(lot2));

		// Split lot - partial sale/transfer
		// ========================================================

		int lotid = 99;
		InvestmentTxn disptx3 = null;

		QDate date3 = date2.addDays(1);
		InvestmentTxn srctx3 = new InvestmentTxn(this.invest.acctid);
		srctx3.setDate(date3);

		BigDecimal shares3 = new BigDecimal("12.5");
		BigDecimal cost3 = price.getPrice().multiply(shares3);

		Lot lot3 = new Lot(lotid, date3, //
				this.invest.acctid, this.stock.secid, //
				shares3, price.getPrice(), srctx3, disptx3, lot2);

		assertNotNull(lot3);
		assertEquals(lot3.acctid, this.invest.acctid);
		assertEquals(0, lot3.getAcquisitionDate().compareTo(date1));
		assertEquals(lot3.shares, shares3);
		assertEquals(lot3.getCostBasis(), cost3);
		assertTrue(lot3.getChildLots().isEmpty());
		assertNull(lot3.getDisposingTransaction());
		assertEquals(lot2, lot3.getSourceLot());
		assertTrue(lot3.isOpen());

		assertFalse(lot1.getChildLots().isEmpty());
		assertTrue(lot1.getChildLots().contains(lot2));
		assertFalse(lot1.getChildLots().contains(lot3));

		assertFalse(lot2.getChildLots().isEmpty());
		assertTrue(lot2.getChildLots().contains(lot3));

		// Disposed lot constructor
		// ========================================================

		QDate date5 = date3.addDays(1);
		InvestmentTxn srctx5 = new InvestmentTxn(this.invest.acctid);
		srctx5.setDate(date5);
		InvestmentTxn dsttx5 = new InvestmentTxn(this.invest2.acctid);
		dsttx5.setDate(date5);
		BigDecimal shares5 = new BigDecimal("9.0");
		BigDecimal cost5 = price.getPrice().multiply(shares5);

		Lot lot5 = new Lot(lotid, shares5, lot3, this.invest.acctid, srctx5, dsttx5);

		assertNotNull(lot5);
		assertEquals(lot5.acctid, this.invest2.acctid);
		assertEquals(0, lot5.getAcquisitionDate().compareTo(date1));
		assertTrue(Common.isEffectivelyEqual(shares5, lot5.shares));
		assertEquals(cost5, lot5.getCostBasis());
		assertTrue(lot5.getChildLots().isEmpty());
		assertNull(lot5.getDisposingTransaction());
		assertEquals(lot3, lot5.getSourceLot());
		assertTrue(lot5.isOpen());

		assertFalse(lot3.getChildLots().isEmpty());
		assertTrue(lot3.getChildLots().contains(lot5));
		assertNull(lot3.getDisposingTransaction());
		assertTrue(lot3.isOpen());

		// Disposed lot constructor
		// ========================================================

		QDate date4 = date5.addDays(1);
		InvestmentTxn srctx4 = new InvestmentTxn(this.invest.acctid);
		srctx4.setDate(date4);
		InvestmentTxn dsttx4 = new InvestmentTxn(this.invest2.acctid);
		dsttx4.setDate(date4);

		Lot lot4 = new Lot(lot3, this.invest.acctid, srctx4, dsttx4);

		assertNotNull(lot4);
		assertEquals(lot4.acctid, this.invest2.acctid);
		assertEquals(0, lot4.getAcquisitionDate().compareTo(date1));
		assertEquals(lot3.shares, lot4.shares);
		assertEquals(lot3.getCostBasis(), lot4.getCostBasis());
		assertTrue(lot4.getChildLots().isEmpty());
		assertNull(lot4.getDisposingTransaction());
		assertEquals(lot3, lot4.getSourceLot());
		assertTrue(lot4.isOpen());

		assertFalse(lot3.getChildLots().isEmpty());
		assertTrue(lot3.getChildLots().contains(lot4));
		assertNotNull(lot3.getDisposingTransaction());
		assertEquals(srctx3, lot3.getDisposingTransaction());
		assertFalse(lot3.isOpen());

		// fail("Not yet implemented");
	}

	@Test
	void testGetSourceLot() {
		fail("Not yet implemented");
	}

	@Test
	void testGetChildLots() {
		fail("Not yet implemented");
	}

	@Test
	void testGetDisposingTransaction() {
		fail("Not yet implemented");
	}

	@Test
	void testSetDisposingTransaction() {
		fail("Not yet implemented");
	}

	@Test
	void testIsDerivedFrom() {
		fail("Not yet implemented");
	}

	@Test
	void testIsOpen() {
		fail("Not yet implemented");
	}

	@Test
	void testGetAcquisitionDate() {
		fail("Not yet implemented");
	}

	@Test
	void testGetPriceBasis() {
		fail("Not yet implemented");
	}

	@Test
	void testGetCostBasis() {
		fail("Not yet implemented");
	}

	@Test
	void testSplit() {
		fail("Not yet implemented");
	}

	@Test
	void testToString() {
		fail("Not yet implemented");
	}

	@Test
	void testMatches() {
		fail("Not yet implemented");
	}

}
