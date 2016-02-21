
package qif.data;

import java.math.BigDecimal;
import java.util.Date;

// !Type:Memorized
class MemorizedTxn extends GenericTxn {
	public TransactionType Type;
	public BigDecimal AmortizationCurrentLoanBalance;
	public Date AmortizationFirstPaymentDate;
	public BigDecimal AmortizationInterestRate;
	public BigDecimal AmortizationNumberOfPaymentsAlreadyMade;
	public BigDecimal AmortizationNumberOfPeriodsPerYear;
	public BigDecimal AmortizationOriginalLoanAmount;
	public BigDecimal AmortizationTotalYearsForLoan;

	public static MemorizedTxn load(QFileReader qfr) {
		QFileReader.QLine qline = new QFileReader.QLine();

		MemorizedTxn txn = new MemorizedTxn((short) -1);

		for (;;) {
			qfr.nextSecurityLine(qline);

			switch (qline.type) {
			case MemtxnAddress:
				// txn.address.Add(mtlt.Address.Count, qline.value);
				break;
			case MemtxnAmortizationCurrentLoanBalance:
				txn.AmortizationCurrentLoanBalance = Common.getDecimal(qline.value);
				break;
			case MemtxnAmortizationFirstPaymentDate:
				txn.AmortizationFirstPaymentDate = Common.GetDate(qline.value);
				break;
			case MemtxnAmortizationInterestRate:
				txn.AmortizationInterestRate = Common.getDecimal(qline.value);
				break;
			case MemtxnAmortizationNumberOfPaymentsAlreadyMade:
				txn.AmortizationNumberOfPaymentsAlreadyMade = Common.getDecimal(qline.value);
				break;
			case MemtxnAmortizationNumberOfPeriodsPerYear:
				txn.AmortizationNumberOfPeriodsPerYear = Common.getDecimal(qline.value);
				break;
			case MemtxnAmortizationOriginalLoanAmount:
				txn.AmortizationOriginalLoanAmount = Common.getDecimal(qline.value);
				break;
			case MemtxnAmortizationTotalYearsForLoan:
				txn.AmortizationTotalYearsForLoan = Common.getDecimal(qline.value);
				break;
			case MemtxnAmount:
				txn.amount = Common.getDecimal(qline.value);
				break;
			case MemtxnCategory:
				// txn.catid = qline.value;
				break;
			case MemtxnCheckTransaction:
				txn.Type = TransactionType.Check;
				break;
			case MemtxnClearedStatus:
				txn.clearedStatus = qline.value;
				break;
			case MemtxnDepositTransaction:
				txn.Type = TransactionType.Deposit;
				break;
			case MemtxnElectronicPayeeTransaction:
				txn.Type = TransactionType.ElectronicPayee;
				break;
			case MemtxnInvestmentTransaction:
				txn.Type = TransactionType.Investment;
				break;
			case MemtxnMemo:
				txn.memo = qline.value;
				break;
			case MemtxnPayee:
				// txn.payee = qline.value;
				break;
			case MemtxnPaymentTransaction:
				txn.Type = TransactionType.Payment;
				break;
			case MemtxnSplitAmount:
				// txn.split.Add(mtlt.SplitAmounts.Count,
				// Common.GetDecimal(qline.value));
				break;
			case MemtxnSplitCategory:
				// txn.split.Add(mtlt.SplitCategories.Count,
				// qline.value);
				break;
			case MemtxnSplitMemo:
				// NOTE: Using split amount count because memo is optional
				// txn.split.Add(mtlt.SplitAmounts.Count, qline.value);
				break;

			case EndOfSection:
				return txn;

			default:
				Common.reportError("Invalid syntax");
				return null;
			}
		}
	}

	public MemorizedTxn(short acctid) {
		super(acctid);
	}

	// static void Export(StreamWriter writer, List<MemorizedTxn> list) {
	// if ((list != null) && (list.Count > 0)) {
	// return;
	// }
	//
	// writer.WriteLine(Headers.MemorizedTransactionList);
	//
	// foreach (MemorizedTxn item in list) {
	// writeIfSet(Address, item.Address[i]);
	// writer.WriteLine(AmortizationCurrentLoanBalance
	// +
	// item.AmortizationCurrentLoanBalance.ToString(CultureInfo.CurrentCulture));
	// writer.WriteLine(AmortizationFirstPaymentDate
	// + item.AmortizationFirstPaymentDate.ToShortDateString());
	// writer.WriteLine(AmortizationInterestRate
	// + item.AmortizationInterestRate.ToString(CultureInfo.CurrentCulture));
	// writer.WriteLine(AmortizationNumberOfPaymentsAlreadyMade
	// +
	// item.AmortizationNumberOfPaymentsAlreadyMade.ToString(CultureInfo.CurrentCulture));
	// writer.WriteLine(AmortizationNumberOfPeriodsPerYear
	// +
	// item.AmortizationNumberOfPeriodsPerYear.ToString(CultureInfo.CurrentCulture));
	// writer.WriteLine(AmortizationOriginalLoanAmount
	// +
	// item.AmortizationOriginalLoanAmount.ToString(CultureInfo.CurrentCulture));
	// writer.WriteLine(AmortizationTotalYearsForLoan
	// +
	// item.AmortizationTotalYearsForLoan.ToString(CultureInfo.CurrentCulture));
	// writer.WriteLine(Amount
	// + item.Amount.ToString(CultureInfo.CurrentCulture));
	// writeIfSet(Category, item.Category);
	// writeIfSet(ClearedStatus, item.ClearedStatus);
	// writeIfSet(Memo, item.Memo);
	// writeIfSet(Payee, item.Payee);
	//
	// foreach (int i in item.SplitCategories.Keys) {
	// writer.WriteLine(SplitCategory + item.SplitCategories[i]);
	// writer.WriteLine(SplitAmount + item.SplitAmounts[i]);
	// writeIfSet(SplitMemo, item.SplitMemos[i]);
	// }
	//
	// switch (item.Type) {
	// case TransactionType.Check:
	// writer.WriteLine(CheckTransaction);
	// break;
	// case TransactionType.Deposit:
	// writer.WriteLine(DepositTransaction);
	// break;
	// case TransactionType.ElectronicPayee:
	// writer.WriteLine(ElectronicPayeeTransaction);
	// break;
	// case TransactionType.Investment:
	// writer.WriteLine(InvestmentTransaction);
	// break;
	// case TransactionType.Payment:
	// writer.WriteLine(PaymentTransaction);
	// break;
	// }
	// }
	// }
};
