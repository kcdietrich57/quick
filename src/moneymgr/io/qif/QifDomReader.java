package moneymgr.io.qif;

import java.io.File;

import app.QifDom;
import moneymgr.io.OptionsProcessor;
import moneymgr.io.PortfolioProcessor;
import moneymgr.io.Reconciler;
import moneymgr.io.StatementDetails;
import moneymgr.io.qif.QFileReader.SectionType;
import moneymgr.model.MoneyMgrModel;
import moneymgr.model.Security;
import moneymgr.util.Common;

/** Class which loads quicken exported data (plus some additional info) */
public class QifDomReader {
	/** The main load method - processes all files in qifdir */
	public static void loadDom(MoneyMgrModel model, String[] qifFiles) {
		QifDom.qifDir = new File(qifFiles[0]).getParentFile();

		QifDomReader rdr = new QifDomReader(model, QifDom.qifDir);

		// Process all the QIF files
		for (String fn : qifFiles) {
			rdr.load(fn, true);
		}

		// Additional processing once the data is loaded (quotes, stmts, etc)
		if (rdr.model != null) {
			rdr.postLoad();
		}
	}

	public final MoneyMgrModel model;
	private final TransactionCleaner transactionCleaner;
	private final SecurityProcessor securityProcessor;
	private final StatementProcessor statementProcessor;
	private final OptionsProcessor optionsProcessor;
	private final PortfolioProcessor portfolioProcessor;
	private final Reconciler reconciler;

	/** Where data files live */
	private File qifDir;

	/** The current file being processed */
	private File curFile = null;

	/** Reader for data files */
	private QFileReader filerdr = null;

	public QifDomReader(MoneyMgrModel model, File qifDir) {
		this.model = model;
		this.qifDir = qifDir;

		this.transactionCleaner = new TransactionCleaner(this.model);
		this.securityProcessor = new SecurityProcessor(this);
		this.statementProcessor = new StatementProcessor(this);
		this.optionsProcessor = new OptionsProcessor(this.model);
		this.portfolioProcessor = new PortfolioProcessor(this.model);

		this.reconciler = new Reconciler(this.model);
	}

	public QFileReader getFileReader() {
		return this.filerdr;
	}

	public void load2(String fileName) {
		if (!new File(fileName).exists()) {
			if (!new File("c:" + fileName).exists()) {
				Common.reportError("Input file '" + fileName + "' does not exist");
				return;
			}

			fileName = "c:" + fileName;
		}

		init(fileName);

		processFile2();
	}

	private void processFile2() {
		Object fileContents = this.filerdr.ingestFile();
	}

	/** Load a single input file */
	public void load(String fileName, boolean doCleanup) {
		// Check windows and unix paths
		if (!new File(fileName).exists()) {
			if (!new File("c:" + fileName).exists()) {
				Common.reportError("Input file '" + fileName + "' does not exist");
				return;
			}

			fileName = "c:" + fileName;
		}

		init(fileName);

		processFile();

		if (doCleanup && this.model != null) {
			this.transactionCleaner.cleanUpTransactionsFromQIF();
		}
	}

	/** Process security info, statements, etc after all basic data is loaded */
	public void postLoad() {
		File d = new File(this.qifDir, "quotes");
		this.securityProcessor.loadSecurityPriceHistory(d);
		Security.fixSplits();

		// Create option objects
		this.optionsProcessor.loadStockOptions();

		// Add transactions to portfolios/positions
		this.securityProcessor.processSecurities();

		this.optionsProcessor.matchOptionsWithTransactions();

		// Load basic statement info
		File dd = new File(this.qifDir, "statements");
		this.statementProcessor.loadStatments(dd);

		// Process saved statement reconciliation information
		// Match statements with transactions
		this.reconciler.processStatementLog();

		// Update share balances in all positions
		this.portfolioProcessor.fixPortfolios();

		this.transactionCleaner.calculateRunningTotals();
		this.transactionCleaner.cleanStatementHoldings();

		// Update statement reconciliation file if format has changed
		if (QifDom.loadedStatementsVersion != StatementDetails.CURRENT_VERSION) {
			this.reconciler.rewriteStatementLogFile();
		}
	}

	private void init(String filename) {
		File f = new File(filename);
		if (!f.exists()) {
			Common.reportError("File '" + filename + "' does not exist");
		}

		this.curFile = f;
		this.filerdr = new QFileReader(this.model, f);
	}

	/** Process a file, loading the various sections via helper classes */
	private void processFile() {
		for (SectionType sectype = this.filerdr.findFirstSection(); //
				sectype != SectionType.EndOfFile; //
				sectype = this.filerdr.nextSection()) {
			switch (sectype) {
			case Tag:
			case Category:
				new CategoryProcessor(this).loadCategories();
				break;

			case Account:
				new AccountProcessor(this).loadAccounts();
				break;

			case Asset:
			case Liability:
			case Cash:
			case CreditCard:
			case Bank:
				new TransactionProcessor(this).loadNonInvestmentTransactions();
				break;

			case Investment:
				new TransactionProcessor(this).loadInvestmentTransactions();
				break;

			case Statements:
				this.statementProcessor.loadStatements(this.curFile);
				break;

			case Security:
				this.securityProcessor.loadSecurities();
				break;

			case Prices:
				this.securityProcessor.loadPrices();
				break;

			case QClass:
			case MemorizedTransaction:
				Common.reportError("TODO not implemented");
				break;

			default:
				break;
			}
		}
	}

	public String toString() {
		return "DomReader[" + this.curFile.getPath() + "]";
	}
}