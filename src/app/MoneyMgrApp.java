package app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import qif.data.Account;
import qif.data.Common;
import qif.data.InvestmentTxn;
import qif.data.NonInvestmentTxn;
import qif.data.QifDom;
import qif.data.SimpleTxn;
import qif.importer.CSVImport;
import qif.importer.QifDomReader;
import qif.ui.MainFrame;

public class MoneyMgrApp {
	public static Scanner scn;

	private static void infoMessage(String msg) {
		if (QifDom.verbose) {
			infoMessageNoln(msg + "\n");
		}
	}

	private static void infoMessageNoln(String msg) {
		if (QifDom.verbose) {
			System.out.print(msg);
		}
	}

	public static void importCSV() {
		String[] fieldnames = null;
		Map<String, List<String[]>> transactionsMap = new HashMap<String, List<String[]>>();

		CSVImport csvimp = new CSVImport();
		for (;;) {
			List<String> record = csvimp.readRecord();
			if (record == null) {
				break;
			}

			// System.out.println(record.toString());

			if ((fieldnames == null) //
					&& record.contains("Account") && record.contains("Date")) {
				fieldnames = record.toArray(new String[0]);
				List<String[]> fnlist = new ArrayList<String[]>();
				fnlist.add(fieldnames);
				transactionsMap.put("FieldNames", fnlist);

				setFieldIndexes(fieldnames);
			} else if ((fieldnames != null) && record.get(0).isEmpty()) {
				String acctname = record.get(ACCOUNT_IDX);

				List<String[]> accttxns = transactionsMap.get(acctname);

				if (accttxns == null) {
					accttxns = new ArrayList<String[]>();
					transactionsMap.put(acctname, accttxns);
				}

				while (record.size() < fieldnames.length) {
					record.add("");
				}

				accttxns.add(record.toArray(new String[0]));
			}
		}

		for (Map.Entry<String, List<String[]>> entry : transactionsMap.entrySet()) {
			if (entry.getKey().equals("FieldNames")) {
				continue;
			}

			infoMessage(entry.getKey());

			for (String[] tuple : entry.getValue()) {
				infoMessage("  [");
				for (String field : tuple) {
					infoMessageNoln("'" + field + "', ");
				}
				infoMessage("]");

				SimpleTxn txn = createTransaction(fieldnames, tuple);
				if (txn != null) {
					Account acct = Account.getAccountByID(txn.acctid);
					List<SimpleTxn> txns = acct.findMatchingTransactions(txn);

					infoMessage(txn.toString());
				}
			}
		}
	}

	// 'Amount', '', ]

	private static int SPLIT_IDX = -1;
	private static int ACCOUNT_IDX = -1;
	private static int DATE_IDX = -1;
	private static int TYPE_IDX = -1;
	private static int CHECKNUM_IDX = -1;
	private static int SECURITY_IDX = -1;
	private static int PAYEE_IDX = -1;
	private static int CATEGORY_IDX; // >0: CategoryID; <0 AccountID
	private static int FEES_IDX = -1;
	private static int SHARES_IDX = -1;
	private static int AMOUNT_IDX;
	private static int MEMO_IDX;
	private static int XACCOUNT_IDX;
	// private static int xtxn;

	private static int getFieldIndex(String fieldname, String[] fieldnames) {
		for (int idx = 0; idx < fieldnames.length; ++idx) {
			if (fieldnames[idx].equals(fieldname)) {
				return idx;
			}
		}

		return -1;
	}

	private static void setFieldIndexes(String[] fieldnames) {
		if (ACCOUNT_IDX >= 0) {
			return;
		}

		SPLIT_IDX = getFieldIndex("Split", fieldnames);
		ACCOUNT_IDX = getFieldIndex("Account", fieldnames);
		DATE_IDX = getFieldIndex("Date", fieldnames);
		AMOUNT_IDX = getFieldIndex("Amount", fieldnames);
		TYPE_IDX = getFieldIndex("Type", fieldnames);
		CHECKNUM_IDX = getFieldIndex("Check #", fieldnames);
		SECURITY_IDX = getFieldIndex("Security", fieldnames);
		PAYEE_IDX = getFieldIndex("Payee", fieldnames);
		CATEGORY_IDX = getFieldIndex("Category", fieldnames);
		FEES_IDX = getFieldIndex("Comm/Fee", fieldnames);
		DATE_IDX = getFieldIndex("Date", fieldnames);
		SHARES_IDX = getFieldIndex("Shares", fieldnames);
		MEMO_IDX = getFieldIndex("Memo/Notes", fieldnames);
	}

	private static SimpleTxn createTransaction(String[] fieldnames, String[] tuple) {
		String acctname = tuple[ACCOUNT_IDX];
		Account acct = Account.findAccount(acctname);
		if (acct == null) {
			acct = new Account();
			acct.setName(acctname);
			Account.addAccount(acct);
		}

		String split = tuple[SPLIT_IDX];
		String type = tuple[TYPE_IDX];

		SimpleTxn txn = null;

		if (split.equals("S")) {
			txn = new SimpleTxn(acct.acctid);
		} else {
			switch (acct.type) {
			case Bank:
			case Asset:
			case Cash:
			case CCard:
			case Liability:
				txn = new NonInvestmentTxn(acct.acctid);
				break;
			case Inv401k:
			case Invest:
			case InvMutual:
			case InvPort:
				txn = new InvestmentTxn(acct.acctid);
				break;
			}
		}

		txn.setDate(Common.parseQDate(tuple[DATE_IDX]));
		txn.setAmount(Common.getDecimal(tuple[AMOUNT_IDX]));
		txn.memo = tuple[MEMO_IDX];

//		public int xacctid;
//		public int catid; // >0: CategoryID; <0 AccountID
//		public SimpleTxn xtxn;

		return txn;
	}

	public static void main(String[] args) {
		MoneyMgrApp.scn = new Scanner(System.in);
		QifDomReader.loadDom(new String[] { "qif/DIETRICH.QIF" });

		MainFrame.createUI();

		importCSV();
	}
}
