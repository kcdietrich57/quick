package moneymgr.model.test;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import moneymgr.model.Category;
import moneymgr.model.MoneyMgrModel;

class CategoryTest {

	@BeforeAll
	public static void beforeClass() throws Exception {
		System.out.println("Before class function");
	}

	@BeforeEach
	public void beforeTest() throws Exception {
		System.out.println("Before each test");

		new MoneyMgrModel("test_model");
		MoneyMgrModel.changeModel("test_model");
	}

	@Test
	void testCategoryIntStringStringBoolean() {
		Category cat = new Category(1, "cat-name", "cat-desc", true);

		Assert.assertNotNull(cat);
		Assert.assertEquals(cat.catid, 1);
		Assert.assertEquals(cat.name, "cat-name");
		Assert.assertEquals(cat.description, "cat-desc");
		Assert.assertTrue(cat.isExpense);
	}

	@Test
	void testCategoryStringStringBoolean() {
		int nextid = MoneyMgrModel.currModel.nextCategoryID();

		Category cat = new Category("cat-name", "cat-desc", true);

		Assert.assertNotNull(cat);
		Assert.assertEquals(cat.catid, nextid);
		Assert.assertEquals(cat.name, "cat-name");
		Assert.assertEquals(cat.description, "cat-desc");
		Assert.assertTrue(cat.isExpense);

		MoneyMgrModel.currModel.addCategory(cat);

		cat = new Category("cat-name2", "cat-desc2", false);

		Assert.assertNotNull(cat);
		Assert.assertEquals(cat.catid, nextid + 1);
		Assert.assertEquals(cat.name, "cat-name2");
		Assert.assertEquals(cat.description, "cat-desc2");
		Assert.assertFalse(cat.isExpense);

		MoneyMgrModel.currModel.addCategory(cat);
	}

	@Test
	void testToString() {
		Category cat = new Category("cat-name", "cat-desc", true);
		
		String s = cat.toString();
		
		System.out.println("Cat: '" + s + "'");
		
		Assert.assertTrue(s.contains("cat-name"));
		Assert.assertTrue(s.contains("cat-desc"));
		Assert.assertTrue(s.contains("=true"));
	}

	@Test
	void testMatches() {
		Category cat1 = new Category(1, "cat-name", "cat-desc", true);
		Category cat2 = new Category(1, "cat-name", "cat-desc", true);
		Category cat3 = new Category(2, "cat-name", "cat-desc", true);
		Category cat4 = new Category(1, "cat-name2", "cat-desc", true);
		Category cat5 = new Category(1, "cat-name", "cat-desc2", true);
		Category cat6 = new Category(1, "cat-name", "cat-desc", false);

		Assert.assertTrue(cat1.matches(cat2));
		Assert.assertFalse(cat1.matches(cat3));
		Assert.assertFalse(cat1.matches(cat4));
		Assert.assertFalse(cat1.matches(cat5));
		Assert.assertFalse(cat1.matches(cat6));
	}

}
