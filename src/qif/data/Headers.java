
package qif.data;

// The transaction type headers.
public interface Headers {
	// ---------------------------------------------------------
	// Section markers
	// ---------------------------------------------------------
	public static final String HdrAccount = "!Account";
	public static final String HdrStatement = "!Type:Statement";
	public static final String HdrBank = "!Type:Bank";
	public static final String HdrCash = "!Type:Cash";
	public static final String HdrCategory = "!Type:Cat";
	public static final String HdrCreditCard = "!Type:CCard";
	public static final String HdrInvestment = "!Type:Invst";
	public static final String HdrAsset = "!Type:Oth A";
	public static final String HdrLiability = "!Type:Oth L";

	// In document/code only
	public static final String HdrMemorizedTransaction = "!Type:Memorized";
	public static final String HdrClass = "!Type:Class";

	// In dietrich.qif only
	public static final String HdrPrices = "!Type:Prices";
	public static final String HdrSecurity = "!Type:Security";
	public static final String HdrTag = "!Type:Tag";

	// ---------------------------------------------------------
	// Field type markers
	// ---------------------------------------------------------
	public static final char SEC_NAME = 'N';
	public static final char SEC_SYMBOL = 'S';
	public static final char SEC_TYPE = 'T';
	public static final char SEC_GOAL = 'G';

	public static final char ACCT_NAME = 'N';
	public static final char ACCT_TYPE = 'T';
	public static final char ACCT_DESCRIPTION = 'D';
	public static final char ACCT_CREDITLIMIT = 'L';
	public static final char ACCT_STMTDATE = '/';
	public static final char ACCT_STMTBAL = '$';

	public static final char TXN_Date = 'D';
	public static final char TXN_Amount = 'T';
	public static final char TXN_Amount2 = 'U';
	public static final char TXN_ClearedStatus = 'C';
	public static final char TXN_Number = 'N';
	public static final char TXN_Payee = 'P';
	public static final char TXN_Memo = 'M';
	public static final char TXN_Address = 'A';
	public static final char TXN_Category = 'L';
	public static final char TXN_SplitCategory = 'S';
	public static final char TXN_SplitMemo = 'E';
	public static final char TXN_SplitAmount = '$';

	public static final char CAT_Name = 'N';
	public static final char CAT_Description = 'D';
	public static final char CAT_TaxRelated = 'T';
	public static final char CAT_IncomeCategory = 'I';
	public static final char CAT_ExpenseCategory = 'E';
	public static final char CAT_BudgetAmount = 'B';
	public static final char CAT_TaxSchedule = 'R';

	public static final char INV_Date = 'D';
	public static final char INV_Action = 'N';
	public static final char INV_Security = 'Y';
	public static final char INV_Price = 'I';
	public static final char INV_Quantity = 'Q';
	public static final char INV_TransactionAmount = 'T';
	public static final char INV_TransactionAmount2 = 'U';
	public static final char INV_ClearedStatus = 'C';
	public static final char INV_TextFirstLine = 'P';
	public static final char INV_Memo = 'M';
	public static final char INV_Commission = 'O';
	public static final char INV_AccountForTransfer = 'L';
	public static final char INV_AmountTransferred = '$';

	// public static final char MEMTXN_CheckTransaction = 'KC';
	// public static final char MEMTXN_DepositTransaction = 'KD';
	// public static final char MEMTXN_PaymentTransaction = 'KP';
	// public static final char MEMTXN_InvestmentTransaction = 'KI';
	// public static final char MEMTXN_ElectronicPayeeTransaction = 'KE';
	public static final char MEMTXN_Amount = 'T';
	public static final char MEMTXN_ClearedStatus = 'C';
	public static final char MEMTXN_Payee = 'P';
	public static final char MEMTXN_Memo = 'M';
	public static final char MEMTXN_Address = 'A';
	public static final char MEMTXN_Category = 'L';
	public static final char MEMTXN_SplitCategory = 'S';
	public static final char MEMTXN_SplitMemo = 'E';
	public static final char MEMTXN_SplitAmount = '$';
	public static final char MEMTXN_AmortizationFirstPaymentDate = '1';
	public static final char MEMTXN_AmortizationTotalYearsForLoan = '2';
	public static final char MEMTXN_AmortizationNumberOfPaymentsAlreadyMade = '3';
	public static final char MEMTXN_AmortizationNumberOfPeriodsPerYear = '4';
	public static final char MEMTXN_AmortizationInterestRate = '5';
	public static final char MEMTXN_AmortizationCurrentLoanBalance = '6';
	public static final char MEMTXN_AmortizationOriginalLoanAmount = '7';

	public static final char STMT_DATE = 'D';
	public static final char STMT_CR = 'C';
	public static final char STMT_DB = 'E';
	public static final char STMT_BAL = 'B';

	public static final char END = '^';
};
