package qif.importer;

import java.io.File;

import qif.data.Account;
import qif.data.Common;
import qif.data.QifDom;
import qif.importer.QFileReader.SectionType;
import qif.persistence.Reconciler;

public class QifDomReader {
	private File curFile = null;
	private QFileReader filerdr = null;
	private File qifDir;

	public static void loadDom(String[] qifFiles) {
		QifDom.qifDir = new File(qifFiles[0]).getParentFile();

		final QifDomReader rdr = new QifDomReader(QifDom.qifDir);

		// Process all the QIF files
		for (final String fn : qifFiles) {
			rdr.load(fn, true);
		}

		// Additional processing once the data is loaded (quotes, stmts, etc)
		rdr.postLoad();
	}

	public QifDomReader(File qifDir) {
		this.qifDir = qifDir;
	}

	public QFileReader getFileReader() {
		return this.filerdr;
	}

	public void load(String fileName, boolean doCleanup) {
		if (!new File(fileName).exists()) {
			if (new File("c:" + fileName).exists()) {
				fileName = "c:" + fileName;
			} else {
				Common.reportError("Input file '" + fileName + "' does not exist");
				return;
			}
		}

		init(fileName);

		processFile();

		if (doCleanup) {
			new TransactionCleaner().cleanUpTransactions();
		}
	}

	private void postLoad() {
		final File d = new File(this.qifDir, "quotes");
		new SecurityProcessor(this).loadSecurityPriceHistory(d);

		new OptionsProcessor().processStockOptions();
		new SecurityProcessor(this).processSecurities();
		new OptionsProcessor().processOptions();
		new PortfolioProcessor(this).fixPortfolios();

		// TODO need to match up lots with transactions
		for (Account acct : Account.accounts) {
			if (!acct.isInvestmentAccount() || acct.securities.isEmpty()) {
				continue;
			}

		}

		final File dd = new File(this.qifDir, "statements");
		new StatementProcessor(this).processStatementFiles(dd);

		// Process saved statement reconciliation information
		Reconciler.processStatementLog();

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

	private void processFile() {
		this.filerdr.reset();

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
				new StatementProcessor(this).loadStatements(this.curFile);
				break;

			case Security:
				new SecurityProcessor(this).loadSecurities();
				break;

			case Prices:
				new SecurityProcessor(this).loadPrices();
				break;

			case QClass:
				Common.reportError("TODO not implemented");
				break;

			case MemorizedTransaction:
				Common.reportError("TODO not implemented");
				break;

			default:
				break;
			}
		}
	}
}