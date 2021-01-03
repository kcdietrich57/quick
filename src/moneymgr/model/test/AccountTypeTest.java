package moneymgr.model.test;

import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import moneymgr.model.AccountType;
import moneymgr.model.MoneyMgrModel;

public class AccountTypeTest {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		new MoneyMgrModel("test-model");
		MoneyMgrModel.changeModel("test-model");
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void testParseAccountType() {
		AccountType atype;

		// TODO atype = AccountType.parseAccountType("BNK");
		// Assert.assertEquals(atype, AccountType.Bank);

		atype = AccountType.parseAccountType("Bank");
		Assert.assertEquals(atype, AccountType.Bank);

		atype = AccountType.parseAccountType("CCard");
		Assert.assertEquals(atype, AccountType.CCard);

		Assert.assertThrows(RuntimeException.class, //
				() -> AccountType.parseAccountType("bogus"));
	}

	@Test
	void testById() {
		AccountType atype = AccountType.byId(5);
		Assert.assertNotNull(atype);
		Assert.assertEquals(atype.id, 5);
	}

	@Test
	void testIsLiability() {
		// Bank, CCard, Cash, Asset, Liability, //
		// Invest, InvPort, Inv401k, InvMutual
		Assert.assertFalse(AccountType.Asset.isLiability());
		Assert.assertFalse(AccountType.Bank.isLiability());
		Assert.assertFalse(AccountType.Cash.isLiability());
		Assert.assertFalse(AccountType.Invest.isLiability());
		Assert.assertFalse(AccountType.InvPort.isLiability());
		Assert.assertFalse(AccountType.Inv401k.isLiability());
		Assert.assertFalse(AccountType.InvMutual.isLiability());

		Assert.assertTrue(AccountType.CCard.isLiability());
		Assert.assertTrue(AccountType.Liability.isLiability());
	}

	@Test
	void testIsAsset() {
		Assert.assertTrue(AccountType.Asset.isAsset());
		Assert.assertTrue(AccountType.Bank.isAsset());
		Assert.assertTrue(AccountType.Cash.isAsset());
		Assert.assertTrue(AccountType.Invest.isAsset());
		Assert.assertTrue(AccountType.InvPort.isAsset());
		Assert.assertTrue(AccountType.Inv401k.isAsset());
		Assert.assertTrue(AccountType.InvMutual.isAsset());

		Assert.assertFalse(AccountType.CCard.isAsset());
		Assert.assertFalse(AccountType.Liability.isAsset());
	}

	@Test
	void testIsCash() {
		Assert.assertFalse(AccountType.Asset.isCash());
		Assert.assertTrue(AccountType.Bank.isCash());
		Assert.assertTrue(AccountType.Cash.isCash());
		Assert.assertFalse(AccountType.Invest.isCash());
		Assert.assertFalse(AccountType.InvPort.isCash());
		Assert.assertFalse(AccountType.Inv401k.isCash());
		Assert.assertFalse(AccountType.InvMutual.isCash());
		Assert.assertFalse(AccountType.CCard.isCash());
		Assert.assertFalse(AccountType.Liability.isCash());
	}

	@Test
	void testIsInvestment() {
		Assert.assertFalse(AccountType.Asset.isInvestment());
		Assert.assertFalse(AccountType.Bank.isInvestment());
		Assert.assertFalse(AccountType.Cash.isInvestment());
		Assert.assertTrue(AccountType.Invest.isInvestment());
		Assert.assertTrue(AccountType.InvPort.isInvestment());
		Assert.assertTrue(AccountType.Inv401k.isInvestment());
		Assert.assertTrue(AccountType.InvMutual.isInvestment());
		Assert.assertFalse(AccountType.CCard.isInvestment());
		Assert.assertFalse(AccountType.Liability.isInvestment());
	}

	@Test
	void testIsNonInvestment() {
		Assert.assertTrue(AccountType.Asset.isNonInvestment());
		Assert.assertTrue(AccountType.Bank.isNonInvestment());
		Assert.assertTrue(AccountType.Cash.isNonInvestment());
		Assert.assertFalse(AccountType.Invest.isNonInvestment());
		Assert.assertFalse(AccountType.InvPort.isNonInvestment());
		Assert.assertFalse(AccountType.Inv401k.isNonInvestment());
		Assert.assertFalse(AccountType.InvMutual.isNonInvestment());
		Assert.assertTrue(AccountType.CCard.isNonInvestment());
		Assert.assertTrue(AccountType.Liability.isNonInvestment());
	}

	@Test
	void testToString() {
		for (AccountType atype : AccountType.getAccountTypes()) {
			Assert.assertEquals(atype.toString(), atype.name);
		}
	}

}
