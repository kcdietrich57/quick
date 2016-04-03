
package qif.data;

import java.math.BigDecimal;
import java.util.Date;

import qif.data.NonInvestmentTxn.TransactionType;

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
		final QFileReader.QLine qline = new QFileReader.QLine();

		final MemorizedTxn txn = new MemorizedTxn((short) 0, (short) -1);

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
				txn.AmortizationFirstPaymentDate = Common.parseDate(qline.value);
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
				txn.setAmount(Common.getDecimal(qline.value));
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

	public MemorizedTxn(short domid, short acctid) {
		super(domid, acctid);
	}
};
