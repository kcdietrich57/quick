package moneymgr.model.test;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.runner.RunWith;

// NB Suite must be run with JUnit4 runner (actual tests are JUnit5)

// SelectPackages("com.howtodoinjava.junit5.examples.packageA") 
@RunWith(JUnitPlatform.class)
@SelectClasses({
	AccountCategoryTests.class,
	AccountTests.class,
	AccountTypeTest.class,
	CategoryTest.class,
	ITxTest.class,
	LotTest.class,
	SecurityTest.class,
	SecurityPositionTest.class,
	SecurityPortfolioTest.class,
	StatementTest.class,
	StockOptionTest.class,
	TxTest.class
	})
public class ModelTests {
	// code relevant to test suite goes here
}
