package moneymgr.io.mm;

import java.io.FileNotFoundException;
import java.io.LineNumberReader;
import java.io.PrintStream;

import moneymgr.model.Account;
import moneymgr.model.AccountCategory;
import moneymgr.model.AccountType;
import moneymgr.model.Category;
import moneymgr.model.QPrice;
import moneymgr.model.Security;
import moneymgr.model.Security.SplitInfo;
import moneymgr.util.Common;

/** Read/write data in native format */
public class Persistence {
	private String filename;
	private LineNumberReader rdr;
	private PrintStream wtr;

	public Persistence(String filename) {
		this.filename = filename;
	}

	public void save() {
		try {
			this.wtr = new PrintStream(this.filename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		wtr.println("{");
		saveCategories();
		saveAccounts();
		saveSecurities();
		saveTransactions();
		saveStatements();
		wtr.println("}");
	}

	private void saveCategories() {
		wtr.println("Categories:[");

		for (int catid = 1; catid < Category.getNextCategoryID(); ++catid) {
			Category cat = Category.getCategory(catid);

			if (cat != null) {
				String line = String.format("%d,%s,%s,%s;", //
						cat.catid, //
						cat.name, //
						cat.description, //
						Boolean.toString(cat.isExpense));
				wtr.println(line);
			}
		}

		wtr.println("],");
	}

	private void saveAccounts() {
		wtr.println("AccountTypes:[");
		for (AccountType at : AccountType.values()) {
			String line = String.format("%d,%s,%s,%s,%s;", //
					at.id, //
					at.name, //
					at.isAsset, //
					at.isInvestment, //
					at.isCash);
			wtr.println(line);
		}
		wtr.println("],");

		wtr.println("AccountCategories:[");
		for (AccountCategory ac : AccountCategory.values()) {
			String line = String.format("%s,%s,[", //
					ac.label, //
					ac.isAsset);

			String sep = "";
			for (AccountType at : ac.accountTypes) {
				line += String.format("%s%s", sep, at.id);
				sep = ",";
			}
			line += "]";

			wtr.println(line);
		}
		wtr.println("],");

		wtr.println("Accounts:[");
		for (Account ac : Account.getAccounts()) {
			String line = String.format("%s,%d,%s,%s,%d,%d,%s,%s;", //
					ac.name, //
					ac.type.id, //
					// ac.acctCategory.label, //
					ac.description, //
					ac.closeDate, //
					ac.statementFrequency, //
					ac.statementDayOfMonth, //
					Common.formatAmount(ac.balance).trim(), //
					Common.formatAmount(ac.clearedBalance).trim());

			// ac.transactions, //
			// ac.statements, //
			// ac.securities //

			wtr.println(line);
		}
		wtr.println("],");
	}

	void saveSecurities() {
		wtr.println("Securities:[");
		for (Security sec : Security.getSecurities()) {
			String secNames = "[";
			String sep = "";
			for (String name : sec.names) {
				secNames += sep + name;
				sep = ",";
			}
			secNames += "]";

			String line = String.format("%d,%s,%s,%s,", //
					sec.secid, //
					sec.symbol, //
					secNames, //
					sec.type);
			wtr.println();
			wtr.println(line);

			wtr.print("[");
			for (SplitInfo split : sec.splits) {
				line = String.format("%s,%f;", //
						split.splitDate, //
						split.splitRatio);
				wtr.print(line);
			}
			
			wtr.println("],[");

			int count = 0;
			for (QPrice price : sec.prices) {
				line = String.format("%s,%s;", //
						price.date, //
						Common.formatAmount3(price).trim());

				wtr.print(line);
				if (++count == 8) {
					count = 0;
					wtr.println();
				}
			}

			if (count > 0) {
				wtr.println();
			}

			wtr.println("],");
		}
		wtr.println("],");
	}

	void saveTransactions() {

	}

	void saveStatements() {

	}
}
