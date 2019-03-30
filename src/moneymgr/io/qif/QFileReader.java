package moneymgr.io.qif;

import static moneymgr.io.qif.Headers.ACCT_CLOSEDATE;
import static moneymgr.io.qif.Headers.ACCT_CREDITLIMIT;
import static moneymgr.io.qif.Headers.ACCT_DESCRIPTION;
import static moneymgr.io.qif.Headers.ACCT_NAME;
import static moneymgr.io.qif.Headers.ACCT_STMTBAL;
import static moneymgr.io.qif.Headers.ACCT_STMTDATE;
import static moneymgr.io.qif.Headers.ACCT_STMTDAY;
import static moneymgr.io.qif.Headers.ACCT_STMTFREQ;
import static moneymgr.io.qif.Headers.ACCT_TYPE;
import static moneymgr.io.qif.Headers.CAT_BudgetAmount;
import static moneymgr.io.qif.Headers.CAT_Description;
import static moneymgr.io.qif.Headers.CAT_ExpenseCategory;
import static moneymgr.io.qif.Headers.CAT_IncomeCategory;
import static moneymgr.io.qif.Headers.CAT_Name;
import static moneymgr.io.qif.Headers.CAT_TaxRelated;
import static moneymgr.io.qif.Headers.CAT_TaxSchedule;
import static moneymgr.io.qif.Headers.END;
import static moneymgr.io.qif.Headers.HdrAccount;
import static moneymgr.io.qif.Headers.HdrAsset;
import static moneymgr.io.qif.Headers.HdrBank;
import static moneymgr.io.qif.Headers.HdrCash;
import static moneymgr.io.qif.Headers.HdrCategory;
import static moneymgr.io.qif.Headers.HdrClass;
import static moneymgr.io.qif.Headers.HdrCreditCard;
import static moneymgr.io.qif.Headers.HdrInvestment;
import static moneymgr.io.qif.Headers.HdrLiability;
import static moneymgr.io.qif.Headers.HdrMemorizedTransaction;
import static moneymgr.io.qif.Headers.HdrPrices;
import static moneymgr.io.qif.Headers.HdrSecurity;
import static moneymgr.io.qif.Headers.HdrStatements;
import static moneymgr.io.qif.Headers.HdrTag;
import static moneymgr.io.qif.Headers.INV_AccountForTransfer;
import static moneymgr.io.qif.Headers.INV_Action;
import static moneymgr.io.qif.Headers.INV_AmountTransferred;
import static moneymgr.io.qif.Headers.INV_ClearedStatus;
import static moneymgr.io.qif.Headers.INV_Commission;
import static moneymgr.io.qif.Headers.INV_Date;
import static moneymgr.io.qif.Headers.INV_Memo;
import static moneymgr.io.qif.Headers.INV_Price;
import static moneymgr.io.qif.Headers.INV_Quantity;
import static moneymgr.io.qif.Headers.INV_Security;
import static moneymgr.io.qif.Headers.INV_TextFirstLine;
import static moneymgr.io.qif.Headers.INV_TransactionAmount;
import static moneymgr.io.qif.Headers.INV_TransactionAmount2;
import static moneymgr.io.qif.Headers.SEC_GOAL;
import static moneymgr.io.qif.Headers.SEC_NAME;
import static moneymgr.io.qif.Headers.SEC_SYMBOL;
import static moneymgr.io.qif.Headers.SEC_TYPE;
import static moneymgr.io.qif.Headers.STMTS_ACCOUNT;
import static moneymgr.io.qif.Headers.STMTS_CASH;
import static moneymgr.io.qif.Headers.STMTS_MONTHLY;
import static moneymgr.io.qif.Headers.STMTS_SECURITY;
import static moneymgr.io.qif.Headers.TXN_Address;
import static moneymgr.io.qif.Headers.TXN_Amount;
import static moneymgr.io.qif.Headers.TXN_Amount2;
import static moneymgr.io.qif.Headers.TXN_Category;
import static moneymgr.io.qif.Headers.TXN_ClearedStatus;
import static moneymgr.io.qif.Headers.TXN_Date;
import static moneymgr.io.qif.Headers.TXN_Memo;
import static moneymgr.io.qif.Headers.TXN_Number;
import static moneymgr.io.qif.Headers.TXN_Payee;
import static moneymgr.io.qif.Headers.TXN_SplitAmount;
import static moneymgr.io.qif.Headers.TXN_SplitCategory;
import static moneymgr.io.qif.Headers.TXN_SplitMemo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.LineNumberReader;

import moneymgr.util.Common;

/** Reader to process input QIF files */
public class QFileReader {
	/** Various sections that occur in QIF files */
	public static enum SectionType {
		EndOfFile, Account, Statement, Statements, //
		Bank, Cash, Category, CreditCard, Investment, //
		Asset, Liability, MemorizedTransaction, QClass, Prices, Security, Tag
	};

	/** Contains decomposed line from QIF file */
	public static class QLine {
		/** The type of field for the line */
		public FieldType type;
		/** Type marker character */
		public char typechar;
		/** Value portion of the line for the field */
		public String value;

		public String toString() {
			return this.type + ": " + this.value;
		}
	}

	private LineNumberReader rdr;
	private String lookaheadLine = null;

	public QFileReader(File file) {
		try {
			this.rdr = new LineNumberReader(new FileReader(file));
		} catch (final FileNotFoundException e) {
			this.rdr = null;
		}
	}

	public String readLine() {
		try {
			if (this.lookaheadLine != null) {
				String ret = this.lookaheadLine;
				this.lookaheadLine = null;

				return ret;
			}

			return this.rdr.readLine();
		} catch (final Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public String peekLine() {
		String line = readLine();
		unreadLine(line);
		return line;
	}

	private void unreadLine(String line) {
		if (this.lookaheadLine != null) {
			Common.reportError("Attempted to push back two lines:\n" //
					+ "[1]: " + this.lookaheadLine + "\n" //
					+ "[2]: " + line + "\n");
		}

		this.lookaheadLine = line;
	}

	public SectionType findFirstSection() {
		return nextSection();
	}

	public SectionType nextSection() {
		try {
			for (;;) {
				String line = readLine();
				if (line == null) {
					return SectionType.EndOfFile;
				}

				if (line.startsWith("!") //
						&& !line.startsWith("!Option") //
						&& !line.startsWith("!Clear")) {
					return parseSectionType(line.trim());
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}

		return SectionType.EndOfFile;
	}

	private SectionType parseSectionType(String line) {
		if (line.equalsIgnoreCase(HdrAccount)) {
			return SectionType.Account;
		}
		if (line.equalsIgnoreCase(HdrBank)) {
			return SectionType.Bank;
		}
		if (line.equalsIgnoreCase(HdrStatements)) {
			return SectionType.Statements;
		}
		if (line.equalsIgnoreCase(HdrCash)) {
			return SectionType.Cash;
		}
		if (line.equalsIgnoreCase(HdrCategory)) {
			return SectionType.Category;
		}
		if (line.equalsIgnoreCase(HdrCreditCard)) {
			return SectionType.CreditCard;
		}
		if (line.equalsIgnoreCase(HdrInvestment)) {
			return SectionType.Investment;
		}
		if (line.equalsIgnoreCase(HdrAsset)) {
			return SectionType.Asset;
		}
		if (line.equalsIgnoreCase(HdrLiability)) {
			return SectionType.Liability;
		}
		if (line.equalsIgnoreCase(HdrMemorizedTransaction)) {
			return SectionType.MemorizedTransaction;
		}
		if (line.equalsIgnoreCase(HdrClass)) {
			return SectionType.QClass;
		}
		if (line.equalsIgnoreCase(HdrPrices)) {
			return SectionType.Prices;
		}
		if (line.equalsIgnoreCase(HdrSecurity)) {
			return SectionType.Security;
		}
		if (line.equalsIgnoreCase(HdrTag)) {
			return SectionType.Tag;
		}

		Common.reportError("Syntax Error: Section header: " + line);
		return SectionType.Bank;
	}

	public void nextAccountLine(QLine line) {
		try {
			if (nextLine(line)) {
				line.type = accountFieldType(line.typechar);
				return;
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}

		line.type = FieldType.EndOfSection;
	}

	public void nextStatementsLine(QLine line) {
		try {
			if (nextLine(line)) {
				line.type = statementsFieldType(line.typechar);
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		line.type = FieldType.EndOfSection;
	}

	public void nextSecurityLine(QLine line) {
		try {
			if (nextLine(line)) {
				line.type = securityFieldType(line.typechar);
				return;
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}

		line.type = FieldType.EndOfSection;
	}

	public void nextPriceLine(QLine line) {
		try {
			if (nextLine(line)) {
				line.type = priceFieldType(line.typechar);
				return;
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}

		line.type = FieldType.EndOfSection;
	}

	public void nextCategoryLine(QLine line) {
		try {
			if (nextLine(line)) {
				line.type = categoryFieldType(line.typechar);
				return;
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}

		line.type = FieldType.EndOfSection;
	}

	public void nextTxnLine(QLine line) {
		try {
			if (nextLine(line)) {
				line.type = txnFieldType(line.typechar);
				return;
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}

		line.type = FieldType.EndOfSection;
	}

	public void nextInvLine(QLine line) {
		try {
			if (nextLine(line)) {
				line.type = invFieldType(line.typechar);
				return;
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}

		line.type = FieldType.EndOfSection;
	}

	/**
	 * Get the next QIF file line
	 * 
	 * @param line Structure to fill with line info
	 * @return True if a line is found; false if EOF
	 * @throws Exception
	 */
	private boolean nextLine(QLine line) throws Exception {
		String s = readLine();

		if (s == null) {
			line.type = FieldType.EndOfSection;
			return false;
		}

		if (s.length() < 1) {
			Common.reportError("Syntax error: field: " + s);
		}

		line.value = s.substring(1);
		line.typechar = s.charAt(0);

		return true;
	}

	private static FieldType securityFieldType(char key) {
		switch (key) {
		case END:
			return FieldType.EndOfSection;
		case SEC_SYMBOL:
			return FieldType.SecSymbol;
		case SEC_NAME:
			return FieldType.SecName;
		case SEC_TYPE:
			return FieldType.SecType;
		case SEC_GOAL:
			return FieldType.SecGoal;

		default:
			Common.reportError("Bad field type for security: " + key);
			return FieldType.EndOfSection;
		}
	}

	private static FieldType priceFieldType(char key) {
		switch (key) {
		case END:
			return FieldType.EndOfSection;

		default:
			Common.reportError("Bad field type for price: " + key);
			return FieldType.EndOfSection;
		}
	}

	private static FieldType accountFieldType(char key) {
		switch (key) {
		case END:
			return FieldType.EndOfSection;
		case ACCT_NAME:
			return FieldType.AcctName;
		case ACCT_TYPE:
			return FieldType.AcctType;
		case ACCT_DESCRIPTION:
			return FieldType.AcctDescription;
		case ACCT_CREDITLIMIT:
			return FieldType.AcctCreditLimit;
		case ACCT_STMTDATE:
			return FieldType.AcctStmtDate;
		case ACCT_STMTBAL:
			return FieldType.AcctStmtBal;
		case ACCT_CLOSEDATE:
			return FieldType.AcctCloseDate;
		case ACCT_STMTFREQ:
			return FieldType.AcctStmtFrequency;
		case ACCT_STMTDAY:
			return FieldType.AcctStmtDay;

		default:
			Common.reportError("Bad field type for account: " + key);
			return FieldType.EndOfSection;
		}
	}

	private static FieldType statementsFieldType(char key) {
		switch (key) {
		case END:
			return FieldType.EndOfSection;

		case STMTS_ACCOUNT:
			return FieldType.StmtsAccount;

		case STMTS_MONTHLY:
			return FieldType.StmtsMonthly;

		case STMTS_SECURITY:
			return FieldType.StmtsSecurity;

		case STMTS_CASH:
			return FieldType.StmtsCash;

		default:
			Common.reportError("Bad field type for statements: " + key);
			return FieldType.EndOfSection;
		}
	}

	private static FieldType categoryFieldType(char key) {
		switch (key) {
		case END:
			return FieldType.EndOfSection;

		case CAT_Name:
			return FieldType.CatName;
		case CAT_Description:
			return FieldType.CatDescription;
		case CAT_TaxRelated:
			return FieldType.CatTaxRelated;
		case CAT_IncomeCategory:
			return FieldType.CatIncomeCategory;
		case CAT_ExpenseCategory:
			return FieldType.CatExpenseCategory;
		case CAT_BudgetAmount:
			return FieldType.CatBudgetAmount;
		case CAT_TaxSchedule:
			return FieldType.CatTaxSchedule;

		default:
			Common.reportError("Bad field type for account: " + key);
			return FieldType.EndOfSection;
		}
	}

	private static FieldType txnFieldType(char key) {
		switch (key) {
		case END:
			return FieldType.EndOfSection;

		case TXN_Date:
			return FieldType.TxnDate;
		case TXN_Amount:
		case TXN_Amount2:
			return FieldType.TxnAmount;
		case TXN_ClearedStatus:
			return FieldType.TxnClearedStatus;
		case TXN_Number:
			return FieldType.TxnNumber;
		case TXN_Payee:
			return FieldType.TxnPayee;
		case TXN_Memo:
			return FieldType.TxnMemo;
		case TXN_Address:
			return FieldType.TxnAddress;
		case TXN_Category:
			return FieldType.TxnCategory;
		case TXN_SplitCategory:
			return FieldType.TxnSplitCategory;
		case TXN_SplitMemo:
			return FieldType.TxnSplitMemo;
		case TXN_SplitAmount:
			return FieldType.TxnSplitAmount;

		default:
			Common.reportError("Bad field type for account: " + key);
			return FieldType.EndOfSection;
		}
	}

	private static FieldType invFieldType(char key) {
		switch (key) {
		case END:
			return FieldType.EndOfSection;

		case INV_Date:
			return FieldType.InvDate;
		case INV_Action:
			return FieldType.InvAction;
		case INV_Security:
			return FieldType.InvSecurity;
		case INV_Price:
			return FieldType.InvPrice;
		case INV_Quantity:
			return FieldType.InvQuantity;
		case INV_TransactionAmount:
		case INV_TransactionAmount2:
			return FieldType.InvTransactionAmt;
		case INV_ClearedStatus:
			return FieldType.InvClearedStatus;
		case INV_TextFirstLine:
			return FieldType.InvFirstLine;
		case INV_Memo:
			return FieldType.InvMemo;
		case INV_Commission:
			return FieldType.InvCommission;
		case INV_AccountForTransfer:
			return FieldType.InvXferAcct;
		case INV_AmountTransferred:
			return FieldType.InvXferAmt;

		default:
			Common.reportError("Bad field type for account: " + key);
			return FieldType.EndOfSection;
		}
	}
}
