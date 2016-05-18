package qif.data;

import static qif.data.Headers.ACCT_CREDITLIMIT;
import static qif.data.Headers.ACCT_DESCRIPTION;
import static qif.data.Headers.ACCT_NAME;
import static qif.data.Headers.ACCT_STMTBAL;
import static qif.data.Headers.ACCT_STMTDATE;
import static qif.data.Headers.ACCT_TYPE;
import static qif.data.Headers.CAT_BudgetAmount;
import static qif.data.Headers.CAT_Description;
import static qif.data.Headers.CAT_ExpenseCategory;
import static qif.data.Headers.CAT_IncomeCategory;
import static qif.data.Headers.CAT_Name;
import static qif.data.Headers.CAT_TaxRelated;
import static qif.data.Headers.CAT_TaxSchedule;
import static qif.data.Headers.END;
import static qif.data.Headers.HdrAccount;
import static qif.data.Headers.HdrAsset;
import static qif.data.Headers.HdrBank;
import static qif.data.Headers.HdrCash;
import static qif.data.Headers.HdrCategory;
import static qif.data.Headers.HdrClass;
import static qif.data.Headers.HdrCreditCard;
import static qif.data.Headers.HdrInvestment;
import static qif.data.Headers.HdrLiability;
import static qif.data.Headers.HdrMemorizedTransaction;
import static qif.data.Headers.HdrPrices;
import static qif.data.Headers.HdrSecurity;
import static qif.data.Headers.HdrStatements;
import static qif.data.Headers.HdrTag;
import static qif.data.Headers.INV_AccountForTransfer;
import static qif.data.Headers.INV_Action;
import static qif.data.Headers.INV_AmountTransferred;
import static qif.data.Headers.INV_ClearedStatus;
import static qif.data.Headers.INV_Commission;
import static qif.data.Headers.INV_Date;
import static qif.data.Headers.INV_Memo;
import static qif.data.Headers.INV_Price;
import static qif.data.Headers.INV_Quantity;
import static qif.data.Headers.INV_Security;
import static qif.data.Headers.INV_TextFirstLine;
import static qif.data.Headers.INV_TransactionAmount;
import static qif.data.Headers.INV_TransactionAmount2;
import static qif.data.Headers.SEC_GOAL;
import static qif.data.Headers.SEC_NAME;
import static qif.data.Headers.SEC_SYMBOL;
import static qif.data.Headers.SEC_TYPE;
import static qif.data.Headers.STMTS_ACCOUNT;
import static qif.data.Headers.STMTS_MONTHLY;
import static qif.data.Headers.STMT_BAL;
import static qif.data.Headers.STMT_CR;
import static qif.data.Headers.STMT_DATE;
import static qif.data.Headers.STMT_DB;
import static qif.data.Headers.TXN_Address;
import static qif.data.Headers.TXN_Amount;
import static qif.data.Headers.TXN_Amount2;
import static qif.data.Headers.TXN_Category;
import static qif.data.Headers.TXN_ClearedStatus;
import static qif.data.Headers.TXN_Date;
import static qif.data.Headers.TXN_Memo;
import static qif.data.Headers.TXN_Number;
import static qif.data.Headers.TXN_Payee;
import static qif.data.Headers.TXN_SplitAmount;
import static qif.data.Headers.TXN_SplitCategory;
import static qif.data.Headers.TXN_SplitMemo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.LineNumberReader;

public class QFileReader {
	SectionType currSectionType;
	LineNumberReader rdr;
	String nextline = null;

	enum SectionType {
		EndOfFile, Account, Statement, Statements, //
		Bank, Cash, Category, CreditCard, Investment, //
		Asset, Liability, MemorizedTransaction, QClass, Prices, Security, Tag
	};

	public static class QLine {
		public FieldType type;
		public char typechar;
		public String value;

		public String toString() {
			return this.type + ": " + this.value;
		}
	}

	public QFileReader(File file) {
		try {
			this.rdr = new LineNumberReader(new FileReader(file));
		} catch (final FileNotFoundException e) {
			this.rdr = null;
		}
	}

	public void reset() {
		if (this.rdr == null) {
			this.currSectionType = SectionType.EndOfFile;
			return;
		}
	}

	public String readLine() {
		try {
			if (this.nextline != null) {
				final String ret = this.nextline;
				this.nextline = null;

				return ret;
			}

			return this.rdr.readLine();
		} catch (final Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public String peekLine() {
		final String line = readLine();
		unreadLine(line);
		return line;
	}

	public void unreadLine(String line) {
		assert this.nextline == null;
		this.nextline = line;
	}

	public SectionType findFirstSection() {
		reset();

		return nextSection();
	}

	public SectionType nextSection() {
		try {
			for (;;) {
				String line = readLine();

				if (line == null) {
					return SectionType.EndOfFile;
				}

				if (line.startsWith("!") && !line.startsWith("!Option") && !line.startsWith("!Clear")) {
					line = line.trim();
					final SectionType st = parseSectionType(line);
					return st;
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

	public void nextStatementLine(QLine line) {
		try {
			if (nextLine(line)) {
				line.type = statementFieldType(line.typechar);
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
		} catch (final Exception e) {
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

	private boolean nextLine(QLine line) throws Exception {
		final String s = readLine();

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

	FieldType securityFieldType(char key) {
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

	FieldType priceFieldType(char key) {
		switch (key) {
		case END:
			return FieldType.EndOfSection;

		default:
			Common.reportError("Bad field type for price: " + key);
			return FieldType.EndOfSection;
		}
	}

	FieldType accountFieldType(char key) {
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

		default:
			Common.reportError("Bad field type for account: " + key);
			return FieldType.EndOfSection;
		}
	}

	FieldType statementFieldType(char key) {
		switch (key) {
		case END:
			return FieldType.EndOfSection;

		case STMT_DATE:
			return FieldType.StmtDate;
		case STMT_DB:
			return FieldType.StmtDebits;
		case STMT_CR:
			return FieldType.StmtCredits;
		case STMT_BAL:
			return FieldType.StmtBalance;

		default:
			Common.reportError("Bad field type for statement: " + key);
			return FieldType.EndOfSection;
		}
	}

	FieldType statementsFieldType(char key) {
		switch (key) {
		case END:
			return FieldType.EndOfSection;

		case STMTS_ACCOUNT:
			return FieldType.StmtsAccount;

		case STMTS_MONTHLY:
			return FieldType.StmtsMonthly;

		default:
			Common.reportError("Bad field type for statements: " + key);
			return FieldType.EndOfSection;
		}
	}

	FieldType categoryFieldType(char key) {
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

	FieldType txnFieldType(char key) {
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

	FieldType stmtFieldType(char key) {
		switch (key) {
		case END:
			return FieldType.EndOfSection;

		case STMT_DATE:
			return FieldType.InvDate;
		case STMT_CR:
			return FieldType.InvDate;
		case STMT_DB:
			return FieldType.InvDate;
		case STMT_BAL:
			return FieldType.InvDate;

		default:
			Common.reportError("Bad field type for statement: " + key);
			return FieldType.EndOfSection;
		}
	}

	FieldType invFieldType(char key) {
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
