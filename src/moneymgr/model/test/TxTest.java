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
import moneymgr.model.Category;
import moneymgr.model.InvestmentTxn;
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.NonInvestmentTxn;
import moneymgr.model.Security;
import moneymgr.model.SimpleTxn;
import moneymgr.model.SplitTxn;
import moneymgr.model.Statement;
import moneymgr.model.TxAction;
import moneymgr.util.Common;
import moneymgr.util.QDate;

class TxTest {

	MoneyMgrModel model;
	Account bank;
	QDate today;
	NonInvestmentTxn tx;

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

		this.today = QDate.today();
		this.model.setCurrentDate(this.today);
		this.model.setAsOfDate(this.today);

		this.tx = new NonInvestmentTxn(this.bank.acctid);
		this.tx.setDate(today);
		this.tx.setAmount(new BigDecimal("1.23"));

		bank.addTransaction(this.tx);
	}

	@AfterEach
	void tearDown() throws Exception {
		MoneyMgrModel.deleteModel("test-model");
	}

	@Test
	void testFormatValue() {
		String s = this.tx.formatValue();
		Assert.assertNotNull(s);
		// Assert.assertEquals("1.23", s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testToStringShort() {
		String s = this.tx.toStringShort(false);
		Assert.assertNotNull(s);
		// Assert.assertEquals("1.23", s);

		s = this.tx.toStringShort(true);
		Assert.assertNotNull(s);
		// Assert.assertEquals("1.23", s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testToStringLong() {
		String s = this.tx.toStringLong();
		Assert.assertNotNull(s);
		// Assert.assertEquals("1.23", s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testNonInvestmentTxnIntInt() {
		NonInvestmentTxn txx = new NonInvestmentTxn(1, this.bank.acctid);
		Assert.assertNotNull(txx);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testNonInvestmentTxnInt() {
		NonInvestmentTxn txx = new NonInvestmentTxn(this.bank.acctid);
		Assert.assertNotNull(txx);

		// TODO fail("Not yet implemented");
	}

//	@Test
//	void testGenericTxnIntInt() {
//		fail("Not yet implemented");
//	}

//	@Test
//	void testGenericTxnInt() {
//		fail("Not yet implemented");
//	}

//	@Test
//	void testSimpleTxnIntInt() {
//		fail("Not yet implemented");
//	}

//	@Test
//	void testSimpleTxnInt() {
//		fail("Not yet implemented");
//	}

	@Test
	void testGetStatementDate() {
		QDate d = this.tx.getStatementDate();
		Assert.assertNull(d);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testSetStatementDate() {
		QDate d = this.tx.getStatementDate();
		Assert.assertNull(d);

		this.tx.setStatementDate(this.today);

		d = this.tx.getStatementDate();
		Assert.assertEquals(this.today, d);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testHasSplits() {
		Assert.assertFalse(this.tx.hasSplits());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetSplits() {
		List<SplitTxn> splits = this.tx.getSplits();
		Assert.assertNotNull(splits);
		Assert.assertTrue(splits.isEmpty());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testAddSplit() {
		fail("Not yet implemented");
	}

	@Test
	void testGetPayee() {
		String s = this.tx.getPayee();
		Assert.assertNotNull(s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testSetPayee() {
		String s = this.tx.getPayee();
		Assert.assertNotNull(s);
		Assert.assertNotEquals("new payee", s);

		this.tx.setPayee("new payee");

		s = this.tx.getPayee();
		Assert.assertNotNull(s);
		Assert.assertEquals("new payee", s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetCheckNumberString() {
		String s = this.tx.getCheckNumberString();
		Assert.assertNotNull(s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetCheckNumber() {
		int ck = this.tx.getCheckNumber();
		Assert.assertEquals(0, ck);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testSetCheckNumber() {
		this.tx.setCheckNumber("1234");

		int ck = this.tx.getCheckNumber();
		Assert.assertEquals(1234, ck);

		String s = this.tx.getCheckNumberString();
		Assert.assertEquals("1234", s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetRunningTotal() {
		BigDecimal v = this.tx.getRunningTotal();
		Assert.assertNull(v);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testSetRunningTotal() {
		BigDecimal val = new BigDecimal("1.23");
		this.tx.setRunningTotal(val);
		BigDecimal v = this.tx.getRunningTotal();
		Assert.assertNotNull(v);
		Assert.assertEquals(val, v);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testVerifySplit() {
		this.tx.verifySplit();

		// TODO fail("Not yet implemented");
	}

	@Test
	void testRepair() {
		// NonInvestmentTxn.repair(txinfo);

		fail("Not yet implemented");
	}

	@Test
	void testClear() {
		Statement s = new Statement(bank.acctid, today, null);

		this.tx.clear(s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetDate() {
		QDate d = this.tx.getDate();
		Assert.assertNotNull(d);
		Assert.assertEquals(0, this.today.compareTo(d));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testSetDate() {
		QDate d = this.today.addDays(1);
		this.tx.setDate(d);
		Assert.assertEquals(0, d.compareTo(this.tx.getDate()));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testCompareTo() {
		Assert.assertTrue(this.tx.compareTo(this.tx) == 0);

		// TRUE fail("Not yet implemented");
	}

	@Test
	void testMatchesSimpleTxn() {
		Assert.assertNull(this.tx.matches(this.tx));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testMatchesGenericTxn() {
		Assert.assertNull(this.tx.matches(this.tx));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetTxid() {
		int txid = this.tx.getTxid();
		Assert.assertTrue(txid > 0);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetAccountID() {
		int acctid = this.tx.getAccountID();
		Assert.assertTrue(acctid > 0);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testCompareWith() {
		// TODO this.tx.compareWith(txinfo, othertx);

		fail("Not yet implemented");
	}

	@Test
	void testGetAccount() {
		Account a = this.tx.getAccount();
		Assert.assertEquals(this.bank, a);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testIsCredit() {
		Assert.assertTrue(this.tx.isCredit());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testAddsShares() {
		Assert.assertFalse(this.tx.addsShares());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testRemovesShares() {
		Assert.assertFalse(this.tx.removesShares());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetAction() {
		TxAction action = this.tx.getAction();
		Assert.assertNotNull(action);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testSetAction() {
		this.tx.setAction(TxAction.CASH);
		Assert.assertEquals(this.tx.getAction(), TxAction.CASH);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testIsCleared() {
		Assert.assertFalse(this.tx.isCleared());

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
	void testGetCashTransferTxn() {
		SimpleTxn xtx = this.tx.getCashTransferTxn();
		Assert.assertNull(xtx);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testSetCashTransferTxn() {
		fail("Not yet implemented");
	}

	@Test
	void testGetCategory() {
		String s = this.tx.getCategory();
		Assert.assertNotNull(s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetCatid() {
		int catid = this.tx.getCatid();
		Assert.assertEquals(0, catid);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testSetCatid() {
		Category cat = new Category("new-cat", "new-cat-desc", true);
		Assert.assertNotNull(cat);
		MoneyMgrModel.currModel.addCategory(cat);

		this.tx.setCatid(cat.catid);
		Assert.assertEquals(cat.catid, this.tx.getCatid());
		Assert.assertEquals("new-cat", this.tx.getCategory());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetSetAmount() {
		BigDecimal txamt = this.tx.getAmount();
		Assert.assertEquals(0, txamt.compareTo(new BigDecimal("1.23")));

		BigDecimal amt = new BigDecimal("1.23");
		this.tx.setAmount(amt);
		txamt = tx.getAmount();
		Assert.assertNotNull(txamt);
		Assert.assertEquals(0, txamt.compareTo(amt));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetCashTransferAmount() {
		BigDecimal amt = this.tx.getCashTransferAmount();
		Assert.assertTrue(Common.isEffectivelyZero(amt));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetCashAmount() {
		BigDecimal amt = this.tx.getCashAmount();
		Assert.assertNotNull(amt);
		Assert.assertTrue(Common.isEffectivelyEqual(new BigDecimal("1.23"), amt));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetGain() {
		BigDecimal amt = this.tx.getGain();
		Assert.assertTrue(Common.isEffectivelyZero(amt));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetSetMemo() {
		String s = this.tx.getMemo();
		Assert.assertEquals("", s);

		this.tx.setMemo("new-memo");
		s = this.tx.getMemo();
		Assert.assertEquals("new-memo", this.tx.getMemo());

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetSecurity() {
		Security sec = this.tx.getSecurity();
		Assert.assertNull(sec);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetSecurityId() {
		int id = this.tx.getSecurityId();
		Assert.assertEquals(0, id);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetSecuritySymbol() {
		String s = this.tx.getSecuritySymbol();
		Assert.assertEquals("", s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testGetSecurityName() {
		String s = this.tx.getSecurityName();
		Assert.assertEquals("", s);

		// TODO fail("Not yet implemented");
	}

	@Test
	void testAmountIsEqual() {
		Assert.assertTrue(this.tx.amountIsEqual(this.tx, true));

		// TODO fail("Not yet implemented");
	}

	@Test
	void testToString() {
		String s = this.tx.toString();
		Assert.assertNotNull(s);

		// TODO fail("Not yet implemented");
	}
}
