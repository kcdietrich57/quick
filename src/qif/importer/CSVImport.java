package qif.importer;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import qif.data.Account;
import qif.data.AccountType;
import qif.data.Category;
import qif.data.Common;
import qif.data.GenericTxn;
import qif.data.InvestmentTxn;
import qif.data.Lot;
import qif.data.NonInvestmentTxn;
import qif.data.QDate;
import qif.data.QifDom;
import qif.data.SimpleTxn;

public class CSVImport {
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

	private static int getFieldIndex(String fieldname, String[] fieldnames) {
		for (int idx = 0; idx < fieldnames.length; ++idx) {
			if (fieldnames[idx].equals(fieldname)) {
				return idx;
			}
		}

		return -1;
	}

	private static QDate dateFromTuple(String[] tuple) {
		return Common.parseQDate(tuple[DATE_IDX]);
	}

	private LineNumberReader rdr;

	public String[] fieldnames = null;
	public Map<String, List<String[]>> transactionsMap = new HashMap<String, List<String[]>>();

	public Map<SimpleTxn, GenericTxn> match = new HashMap<SimpleTxn, GenericTxn>();
	public Map<SimpleTxn, List<GenericTxn>> multimatch = new HashMap<SimpleTxn, List<GenericTxn>>();
	public List<SimpleTxn> nomatch = new ArrayList<SimpleTxn>();
	public List<SimpleTxn> zero = new ArrayList<SimpleTxn>();
	public int totaltx = 0;

	public CSVImport(String filename) {
		try {
			this.rdr = new LineNumberReader(new FileReader(filename));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void importFile() {
		ACCOUNT_IDX = -1;

		readFieldNames();
		if (this.fieldnames == null) {
			return;
		}

		importCSVRecords();
		processCSVRecords();

		try {
			this.rdr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void processCSVRecords() {
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

				QDate txdate = dateFromTuple(tuple);
				SimpleTxn txn = createTransaction(txdate, tuple);

				if (txn != null) {
					Account acct = Account.getAccountByID(txn.acctid);
					List<GenericTxn> txns = acct.findMatchingTransactions(txn, txdate);

					infoMessage(txn.toString());

					++totaltx;

					if (Common.isEffectivelyZero(txn.getAmount())) {
						zero.add(txn);
					} else if (txns.size() == 1) {
						// System.out.println("size=" + txns.size());
						match.put(txn, txns.get(0));
					} else if (txns.isEmpty()) {
						// System.out.println("NO MATCH");
						nomatch.add(txn);
					} else {
						// System.out.println("" + txns.size() + " MATCHES");
						while (!txns.isEmpty() && match.containsValue(txns.get(0))) {
							txns.remove(0);
						}

						if (txns.isEmpty()) {
							nomatch.add(txn);
						} else {
							match.put(txn, txns.get(0));
						}

//						multimatch.put(txn, txns);
					}
				}
			}
		}
	}

	private SimpleTxn createTransaction(QDate txdate, String[] tuple) {
		String acctname = tuple[ACCOUNT_IDX];

		Account acct = Account.findAccount(acctname);
		if (acct == null) {
			acct = new Account();
			acct.setName(acctname);
			acct.type = AccountType.Bank;
			Account.addAccount(acct);
		}

		String payee = tuple[PAYEE_IDX];
		BigDecimal amount = Common.getDecimal(tuple[AMOUNT_IDX]);

		String split = tuple[SPLIT_IDX];
		String type = tuple[TYPE_IDX];
		String memo = tuple[MEMO_IDX];
		String cknum = tuple[CHECKNUM_IDX];
		String sec = tuple[SECURITY_IDX];
		String cat = tuple[CATEGORY_IDX];
		String fees = tuple[FEES_IDX];
		String shares = tuple[SHARES_IDX];

		Account xferAcct = null;
		Category c = null;
		int catid;

		if (cat.startsWith("Transfer:[")) {
			cat = cat.substring(10, cat.length() - 1);
			xferAcct = Account.findAccount(cat);
			catid = (xferAcct != null) ? -xferAcct.acctid : 0;
		} else {
			c = (!cat.isEmpty()) ? Category.findCategory(cat) : null;
			catid = (c != null) ? c.catid : 0;
		}

		SimpleTxn txn = null;

		if (split.equals("S")) {
			// TODO assemble splits into transaction
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

		txn.setDate(txdate);
		txn.setAmount(amount);
		txn.setMemo(memo);
		txn.setXtxn(null);
		txn.setCatid(catid);

		if (txn instanceof GenericTxn) {
			GenericTxn gtxn = (GenericTxn) txn;
			gtxn.setPayee(payee);
		}

		if (txn instanceof NonInvestmentTxn) {
			NonInvestmentTxn nitxn = (NonInvestmentTxn) txn;
			nitxn.chkNumber = cknum;
			nitxn.split = new ArrayList<SimpleTxn>();
		}

		if (txn instanceof InvestmentTxn) {
			InvestmentTxn itxn = (InvestmentTxn) txn;
			itxn.accountForTransfer = null;
			itxn.amountTransferred = BigDecimal.ZERO;
			itxn.setCatid(0);
			itxn.commission = BigDecimal.ZERO;
			itxn.lotsDisposed = new ArrayList<Lot>();
			itxn.price = BigDecimal.ZERO;
			itxn.security = null;
			itxn.lotsCreated = new ArrayList<Lot>();
			//itxn.textFirstLine = null;
			itxn.xferTxns = null;
		}

//		public int xacctid;
//		public int catid; // >0: CategoryID; <0 AccountID
//		public SimpleTxn xtxn;

		return txn;
	}

	private void importCSVRecords() {
		for (;;) {
			List<String> record = readRecord();
			if (record == null) {
				break;
			}

			String f0 = (!record.isEmpty()) ? record.get(0) : "";

			if (f0.trim().isEmpty()) {
				String acctname = record.get(ACCOUNT_IDX);

				List<String[]> accttxns = transactionsMap.get(acctname);

				if (accttxns == null) {
					accttxns = new ArrayList<String[]>();
					transactionsMap.put(acctname, accttxns);
				}

				while (record.size() < fieldnames.length) {
					record.add("");
				}

				String[] tuple = record.toArray(new String[0]);
				accttxns.add(tuple);
			}
		}
	}

	public List<String> readRecord() {
		List<String> fields = new ArrayList<String>();

		try {
			String line = rdr.readLine();
			if (line == null) {
				return null;
			}

			int startidx = 0;
			int endidx = line.length();

			// process each field
			while (startidx < endidx) {
				int ch = line.charAt(startidx);
				if (ch == ',') {
					fields.add("");
					++startidx;

					continue;
				}

				boolean inquote = false;

				if (ch == '"') {
					inquote = true;
					++startidx;
				}

				int tokenstart = startidx;
				int tokenend = -1;

				while ((startidx < endidx) && (tokenend < 0)) {
					ch = line.charAt(startidx);

					if (inquote && (ch == '"')) {
						if (startidx < endidx - 1) {
							if (line.charAt(startidx + 1) == '"') {
								startidx += 2;
								continue;
							}
						}

						inquote = false;
						tokenend = startidx;
						++startidx;
					} else if (!inquote && (ch == ',')) {
						tokenend = startidx;
					} else {
						++startidx;
					}
				}

				if (inquote) {
					Common.reportError("Unmatched quote in CSV: " + line);
				}

				if (tokenend < 0) {
					tokenend = endidx;
				}

				String field = line.substring(tokenstart, tokenend);
				fields.add(field);

				if ((startidx < endidx) && (line.charAt(startidx) != ',')) {
					Common.reportError("Missing comma at " + startidx + " in CSV: " + line);
				}

				++startidx;
			}
		} catch (Exception e) {
			e.printStackTrace();

			return null;
		}

		String f0 = (!fields.isEmpty()) ? fields.get(0) : "";
		if (f0.length() == 1 && f0.charAt(0) == 65279) {
			fields.remove(0);
			fields.add(0, "");
		}

		return fields;
	}

	private void setFieldIndexes(String[] fieldnames) {
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

	public void readFieldNames() {
		for (;;) {
			List<String> record = readRecord();
			if (record == null) {
				return;
			}

			if (record.contains("Account") && record.contains("Date")) {
				this.fieldnames = record.toArray(new String[0]);
				List<String[]> fnlist = new ArrayList<String[]>();
				fnlist.add(this.fieldnames);
				transactionsMap.put("FieldNames", fnlist);

				setFieldIndexes(this.fieldnames);

				break;
			}
		}

		List<String[]> fnlist = new ArrayList<String[]>();
		fnlist.add(this.fieldnames);
		transactionsMap.put("FieldNames", fnlist);
		setFieldIndexes(this.fieldnames);
	}
}
