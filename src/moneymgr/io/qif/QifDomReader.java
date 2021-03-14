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
	public static void loadDom(String[] qifFiles) {
		QifDom.qifDir = new File(qifFiles[0]).getParentFile();

		QifDomReader rdr = new QifDomReader(QifDom.qifDir);
		//QifDomReader rdr2 = new QifDomReader(QifDom.qifDir);

		// Process all the QIF files
		for (String fn : qifFiles) {
			//rdr2.load2(fn);
			//System.exit(1);

			rdr.load(fn, true);
			
			// TODO compare models loaded by both methods
		}

		// Additional processing once the data is loaded (quotes, stmts, etc)
		if (MoneyMgrModel.currModel != null) {
			rdr.postLoad();
		}
	}

	/** Where data files live */
	private File qifDir;

	/** The current file being processed */
	private File curFile = null;

	/** Reader for data files */
	private QFileReader filerdr = null;

	public QifDomReader(File qifDir) {
		this.qifDir = qifDir;
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

		if (doCleanup && MoneyMgrModel.currModel != null) {
			TransactionCleaner.cleanUpTransactionsFromQIF();
		}
	}

	/** Process security info, statements, etc after all basic data is loaded */
	private void postLoad() {
		File d = new File(this.qifDir, "quotes");
		SecurityProcessor.loadSecurityPriceHistory(d);
		Security.fixSplits();

		// Create option objects
		OptionsProcessor.loadStockOptions();

		// Load basic statement info
		File dd = new File(this.qifDir, "statements");
		StatementProcessor.loadStatments(this, dd);

		// Add transactions to portfolios/positions
		SecurityProcessor.processSecurities();

		OptionsProcessor.matchOptionsWithTransactions();

		// Process saved statement reconciliation information
		// Match statements with transactions
		Reconciler.processStatementLog();

		// Update share balances in all positions
		PortfolioProcessor.fixPortfolios();

		TransactionCleaner.calculateRunningTotals();
		TransactionCleaner.cleanStatementHoldings();

		// Update statement reconciliation file if format has changed
		if (QifDom.loadedStatementsVersion != StatementDetails.CURRENT_VERSION) {
			Reconciler.rewriteStatementLogFile();
		}
	}

	private void init(String filename) {
		File f = new File(filename);
		if (!f.exists()) {
			Common.reportError("File '" + filename + "' does not exist");
		}

		this.curFile = f;
		this.filerdr = new QFileReader(f);
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
				StatementProcessor.loadStatements(this, this.curFile);
				break;

			case Security:
				SecurityProcessor.loadSecurities(this);
				break;

			case Prices:
				SecurityProcessor.loadPrices(this);
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