package qif.importer;

public enum FieldType {
	EndOfSection, //

	// Account
	AcctType, AcctCreditLimit, AcctDescription, AcctName, AcctStmtBal, //
	AcctStmtDate, AcctCloseDate,

	// Category
	CatName, CatDescription, CatTaxRelated, CatIncomeCategory, CatExpenseCategory, //
	CatBudgetAmount, CatTaxSchedule,

	// Generic Txn
	TxnDate, TxnAmount, TxnClearedStatus, TxnNumber, TxnPayee, TxnMemo, TxnCategory, //
	TxnAddress, TxnSplitCategory, TxnSplitMemo, TxnSplitAmount,

	// Investment Txn
	InvDate, InvAction, InvSecurity, InvPrice, InvQuantity, InvTransactionAmt, //
	InvClearedStatus, InvFirstLine, InvMemo, InvCommission, InvXferAcct, InvXferAmt,

	// Statements
	StmtsAccount, StmtsMonthly, StmtsSecurity, StmtsCash,

	// Security
	SecName, SecSymbol, SecType, SecGoal,

	// Price
	PriceSymbol, PricePrice, PriceDate
};