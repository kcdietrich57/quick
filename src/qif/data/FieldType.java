package qif.data;

public enum FieldType {
	EndOfSection, //

	// Account
	AcctType, AcctCreditLimit, AcctDescription, AcctName, AcctStmtBal, AcctStmtDate,

	// Category
	CatName, CatDescription, CatTaxRelated, CatIncomeCategory, CatExpenseCategory, //
	CatBudgetAmount, CatTaxSchedule,

	// Generic Txn
	TxnDate, TxnAmount, TxnClearedStatus, TxnNumber, TxnPayee, TxnMemo, TxnCategory, //
	TxnAddress, TxnSplitCategory, TxnSplitMemo, TxnSplitAmount,

	// Generic Txn
	InvDate, InvAction, InvSecurity, InvPrice, InvQuantity, InvTransactionAmt, //
	InvClearedStatus, InvFirstLine, InvMemo, InvCommission, InvXferAcct, InvXferAmt,

	// Statement
	StmtDate, StmtCredits, StmtDebits, StmtBalance,

	// Statements
	StmtsAccount, StmtsMonthly,

	// Security
	SecName, SecSymbol, SecType, SecGoal,

	// Price
	PriceSymbol, PricePrice, PriceDate,

	// Memorized transaction
	MemtxnAddress, MemtxnAmortizationCurrentLoanBalance, //
	MemtxnAmortizationFirstPaymentDate, MemtxnAmortizationInterestRate, //
	MemtxnAmortizationNumberOfPaymentsAlreadyMade, //
	MemtxnAmortizationNumberOfPeriodsPerYear, MemtxnAmortizationOriginalLoanAmount, //
	MemtxnAmortizationTotalYearsForLoan, MemtxnAmount, MemtxnCategory, //
	MemtxnCheckTransaction, MemtxnClearedStatus, MemtxnDepositTransaction, //
	MemtxnElectronicPayeeTransaction, MemtxnInvestmentTransaction, MemtxnMemo, //
	MemtxnPayee, MemtxnPaymentTransaction, MemtxnSplitAmount, MemtxnSplitCategory, //
	MemtxnSplitMemo
};
