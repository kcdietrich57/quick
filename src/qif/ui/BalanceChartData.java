package qif.ui;

import java.util.ArrayList;
import java.util.List;

import qif.data.Account;
import qif.data.AccountCategory;
import qif.data.QDate;
import qif.report.StatusForDateModel;
import qif.report.StatusForDateModel.Section;
import qif.ui.MainWindow.IntervalUnit;

/** This data class is used by various charts */
public class BalanceChartData {
	public QDate[] dates;
	public String[] accountCategoryNames;
	public double[][] accountCategoryValues;
	public double[][] netWorthValues;
	// TODO per-account values
	// TODO break out by money source

	public BalanceChartData(QDate start, QDate end) {
		this(start, end, MainWindow.instance.reportUnit);
	}

	public BalanceChartData(QDate start, QDate end, IntervalUnit units) {
		List<StatusForDateModel> balances = getNetWorthData(start, end, units);

		getData(balances);
	}

	private void getData(List<StatusForDateModel> balances) {
		this.dates = new QDate[balances.size()];
		this.accountCategoryNames = AccountCategory.accountCategoryNames;
		this.accountCategoryValues = new double[AccountCategory.numCategories()][balances.size()];
		this.netWorthValues = new double[1][balances.size()];

		for (int dateIndex = 0; dateIndex < balances.size(); ++dateIndex) {
			Section[] sections = balances.get(dateIndex).sections;

			this.dates[dateIndex] = balances.get(dateIndex).date;
			this.netWorthValues[0][dateIndex] = 0.0;

			double[] massagedValues = new double[sections.length];
			for (int ii = 0; ii < massagedValues.length; ++ii) {
				massagedValues[ii] = Math.floor(sections[ii].subtotal.floatValue() / 1000);
			}

			int aidx = AccountCategory.ASSET.index;
			int lidx = AccountCategory.LOAN.index;

			double assets = massagedValues[aidx];
			double loans = massagedValues[lidx];

			boolean separateAssetsAndLoans = true;

			if (!separateAssetsAndLoans) {
				if (assets > Math.abs(loans)) {
					massagedValues[aidx] = assets + loans;
					massagedValues[lidx] = 0.0;
				} else {
					massagedValues[lidx] = loans + assets;
					massagedValues[aidx] = 0.0;
				}
			}

			for (int sectionNum = 0; sectionNum < sections.length; ++sectionNum) {
				double val = (separateAssetsAndLoans) //
						? Math.floor(sections[sectionNum].subtotal.floatValue() / 1000) //
						: massagedValues[sectionNum];

				this.accountCategoryValues[sectionNum][dateIndex] = val;
				this.netWorthValues[0][dateIndex] += val;
			}
		}
	}

	/** Construct a skeleton list of values to be filled in later */
	public static List<StatusForDateModel> getNetWorthData() {
		return getNetWorthData(null, MainWindow.instance.asOfDate, //
				MainWindow.instance.reportUnit);
	}

	/** Construct a skeleton list of values to be filled in later */
	public static List<StatusForDateModel> getNetWorthData(QDate start, QDate end) {
		return getNetWorthData(start, end, MainWindow.instance.reportUnit);
	}

	/** Construct a skeleton list of values to be filled in later */
	public static List<StatusForDateModel> getNetWorthData( //
			QDate start, QDate end, MainWindow.IntervalUnit unit) {
		List<StatusForDateModel> balances = new ArrayList<StatusForDateModel>();

		QDate d = (start != null) ? start : getFirstTransactionDate();
		QDate lastTxDate = (end != null) ? end : QDate.today(); // getLastTransactionDate();

		int year = d.getYear();
		int month = d.getMonth();
		d = QDate.getDateForEndOfMonth(year, month);

		do {
			StatusForDateModel b = new StatusForDateModel(d);

			balances.add(b);

			d = unit.nextDate(d);
		} while (d.compareTo(lastTxDate) <= 0);

		return balances;
	}

	private static QDate getFirstTransactionDate() {
		QDate retdate = null;

		for (final Account a : Account.getAccounts()) {
			final QDate d = a.getFirstTransactionDate();

			if ((d != null) && ((retdate == null) || d.compareTo(retdate) < 0)) {
				retdate = d;
			}
		}

		return retdate;
	}
}