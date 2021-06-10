package moneymgr.model.test;

import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import moneymgr.model.AccountCategory;
import moneymgr.model.AccountType;

public class AccountCategoryTests {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		System.out.println("Before class");
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		System.out.println("After class");
	}

	@BeforeEach
	void setUp() throws Exception {
		System.out.println("Before test");
	}

	@AfterEach
	void tearDown() throws Exception {
		System.out.println("Before test");
	}

	@Test
	void testGetAccountListOrder() {
		int ord = AccountCategory.ASSET.getAccountListOrder();
		Assert.assertTrue(ord >= 0);
	}

	@Test
	void testNumCategories() {
		int num = AccountCategory.numCategories();
		Assert.assertTrue(num > 0);
	}

	@Test
	void testForAccountType() {
		AccountCategory cat = AccountCategory.forAccountType(AccountType.Bank);
		Assert.assertNotNull(cat);

		System.out.println("cat is '" + cat + "'");

		cat = AccountCategory.forAccountType(AccountType.Asset);
		cat = AccountCategory.forAccountType(AccountType.CCard);
		cat = AccountCategory.forAccountType(AccountType.Invest);
		cat = AccountCategory.forAccountType(AccountType.Inv401k);
		cat = AccountCategory.forAccountType(AccountType.Liability);
		cat = AccountCategory.forAccountType(AccountType.Asset);
	}

	@Test
	void testToString() {
		String s = AccountCategory.ASSET.toString();

		System.out.println("acat is '" + s + "'");
	}

}
