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
import moneymgr.util.Common;
import moneymgr.util.QDate;

class SecurityTest {

	MoneyMgrModel model;
	Account invest;
	QDate today;
	Security foo;
	Security bar;
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

		this.invest = new Account("asset", AccountType.Invest);
		this.model.addAccount(this.invest);

		this.today = QDate.today();
		this.model.setCurrentDate(this.today);
		this.model.setAsOfDate(this.today);

		this.foo = new Security("FOO", "Foo, Inc");
		this.model.addSecurity(foo);
		this.bar = new Security("BAR", "Bar, Inc");
		this.model.addSecurity(bar);

		this.itx = new InvestmentTxn(this.invest.acctid);
		this.itx.setAction(TxAction.BUY);
		this.itx.setDate(today);
		this.itx.setSecurity(this.foo);
		this.itx.setQuantity(new BigDecimal("1.0"));
	}

	@AfterEach
	void tearDown() throws Exception {
		MoneyMgrModel.deleteModel("test-model");
	}

	@Test
	void testModelFunctions() {
		Security sec;

		sec = this.model.findSecurity("FOO");
		Assert.assertNotNull(sec);

		sec = this.model.findSecurityByName("Foo, Inc");
		Assert.assertNotNull(sec);

		sec = this.model.findSecurityBySymbol("FOO");
		Assert.assertNotNull(sec);

		sec = this.model.findSecurity("bogus");
		Assert.assertNull(sec);
	}

	@Test
	void testHashCode() {
		int hash = foo.hashCode();
		Assert.assertTrue(hash != 0);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testSecurityIntStringStringStringString() {
		Security s = new Security(1, "BAZ", "Baz, Inc", "stock", "growth");
		Assert.assertNotNull(s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testSecurityStringStringStringString() {
		Security s = new Security("BAZ", "Baz, Inc", "stock", "growth");
		Assert.assertNotNull(s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testSecurityStringString() {
		Security s = new Security("BAZ", "Baz, Inc");
		Assert.assertNotNull(s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetSymbol() {
		String s = this.foo.getSymbol();
		Assert.assertEquals("FOO", s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetName() {
		String s = this.foo.getName();
		Assert.assertEquals("Foo, Inc", s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetTransactions() {
		List<InvestmentTxn> txns = foo.getTransactions();
		Assert.assertNotNull(txns);
		Assert.assertTrue(txns.isEmpty());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetLots() {
		List<Lot> lots = foo.getLots();
		Assert.assertNotNull(lots);
		Assert.assertTrue(lots.isEmpty());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testSetLots() {
		fail("Not yet implemented");
	}

	@Test
	void testAddLot() {
		Assert.assertTrue(this.foo.getLots().isEmpty());

		Lot lot = new Lot( //
				null, this.invest.acctid, new BigDecimal("1.0"), this.itx);

		this.foo.addLot(lot);

		// fail("Not yet implemented");
	}

	@Test
	void testFixSplits() {
		Security.fixSplits();

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetLastTransaction() {
		InvestmentTxn tx = foo.getLastTransaction();
		Assert.assertNull(tx);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testAddTransaction() {
		Assert.assertTrue(this.foo.getTransactions().isEmpty());
		this.foo.addTransaction(this.itx);
		Assert.assertFalse(this.foo.getTransactions().isEmpty());

		// fail("Not yet implemented");
	}

	@Test
	void testAddPrice() {
		foo.addPrice(new QPrice(this.model, today, foo.secid, new BigDecimal("1.23")));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetPriceForDate() {
		QPrice p = foo.getPriceForDate(today);
		Assert.assertNotNull(p);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetSplitRatioForDate() {
		BigDecimal ratio = foo.getSplitRatioForDate(today);
		Assert.assertNotNull(ratio);
		Assert.assertTrue(Common.isEffectivelyEqual(BigDecimal.ONE, ratio));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testToString() {
		String s = foo.toString();
		Assert.assertNotNull(s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testEqualsObject() {
		Assert.assertFalse(foo.equals(null));
		Assert.assertFalse(foo.equals(invest));
		Assert.assertTrue(foo.equals(foo));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testMatches() {
		Assert.assertNull(foo.matches(foo));

		// TODO fail("Not yet implemented");
	}

}
