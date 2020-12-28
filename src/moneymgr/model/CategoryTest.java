package moneymgr.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategoryTest {

	@BeforeAll
	public static void beforeClass() throws Exception {
		System.out.println("Before class function");
	}

	@BeforeEach
	public void beforeTest() throws Exception {
		System.out.println("Before each test");
	}

	@Test
	void testCategoryIntStringStringBoolean() {
		Category cat = new Category(1, "cat-name", "cat-desc", true);

		Assert.assertNotNull(cat);
		Assert.assertEquals(cat.catid, 1);
		Assert.assertEquals(cat.name, "cat-name");
		Assert.assertEquals(cat.description, "cat-desc");
		Assert.assertTrue(cat.isExpense);

		// fail("Not yet implemented");
	}

	@Test
	void testCategoryStringStringBoolean() {
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
