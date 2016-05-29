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

	// Investment Txn
	InvDate, InvAction, InvSecurity, InvPrice, InvQuantity, InvTransactionAmt, //
	InvClearedStatus, InvFirstLine, InvMemo, InvCommission, InvXferAcct, InvXferAmt,

	// TODO one kind of Statement
	StmtDate, StmtCredits, StmtDebits, StmtBalance,

	// Statements
	StmtsAccount, StmtsMonthly, StmtsSecurity,

	// Security
	SecName, SecSymbol, SecType, SecGoal,

	// Price
	PriceSymbol, PricePrice, PriceDate
};
