package moneymgr.io.cvs;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.QifDom;
import moneymgr.model.Account;
import moneymgr.model.AccountType;
import moneymgr.model.Category;
import moneymgr.model.GenericTxn;
import moneymgr.model.InvestmentTxn;
import moneymgr.model.NonInvestmentTxn;
import moneymgr.model.SimpleTxn;
import moneymgr.model.SplitTxn;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/** Import data from CSV file exported from MacOS Quicken */
public class CSVImport {
	// "Split",""Account",Date","Check #","Payee","Category","Amount","Memo/Notes",
	// "Description/Category","Type","Security","Comm/Fee","Shares",
	// "Modified","Action","Reference","Transfer",
	// "Shares Out","Shares In","Outflow","Clr","Inflow"
	private static int SPLIT_IDX = -1;
	private static int ACCOUNT_IDX = -1;
	private static int DATE_IDX = -1;
	private static int CHECKNUM_IDX = -1;
	private static int PAYEE_IDX = -1;
	private static int CATEGORY_IDX; // >0: CategoryID; <0 AccountID
	private static int AMOUNT_IDX;
	private static int MEMO_IDX;
	private static int DESCRIPTION_IDX = -1;
	private static int TYPE_IDX = -1;
	private static int SECURITY_IDX = -1;
	private static int FEES_IDX = -1;
	private static int SHARES_IDX = -1;
	// private static int XACCOUNT_IDX;

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

	/** List of field names for the input CSV file */
	public String[] fieldnames = null;

	/** Map account name to transaction tuples */
	public Map<String, List<String[]>> transactionsMap = new HashMap<>();

	/** When assembling splits, this holds the current main transaction */
	public GenericTxn lasttxn = null;

	/**
	 * Matches come in several forms<br>
	 * Simple - no splits on either side Split - tx exists as split in both versions
	 * PseudoSplit - non-split matches split in other version
	 */
	public static class MatchInfo {
		public boolean isPseudoSplit = false;
		public boolean ismatched = false;
		public List<SimpleTxn> macTxn = new ArrayList<>();
		public List<SimpleTxn> winTxn = new ArrayList<>();

		public List<SimpleTxn> winTxnPotential = null;
		public List<SimpleTxn> winTxnMatches = null;
	}

	public List<MatchInfo> matches = new ArrayList<>();
	public Map<SimpleTxn, MatchInfo> matchInfoForWinTxn = new HashMap<>();
	public Map<GenericTxn, MatchInfo> partialMatches = new HashMap<>();

	public Set<SimpleTxn> matchedTransactions = new HashSet<>();

	/** Map MAC tx to WIN tx */
	public Map<SimpleTxn, SimpleTxn> match = new HashMap<>();
	public Map<SimpleTxn, List<GenericTxn>> multimatch = new HashMap<>();
	public List<SimpleTxn> nomatch = new ArrayList<>();
	public List<SimpleTxn> allzero = new ArrayList<>();
	public List<SimpleTxn> nomatchZero = new ArrayList<>();
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
		for (Map.Entry<String, List<String[]>> entry : this.transactionsMap.entrySet()) {
			if (entry.getKey().equals("FieldNames")) {
				continue;
			}

			infoMessage(entry.getKey());

			for (String[] tuple : entry.getValue()) {
				processTuple(tuple);
			}
		}

		for (MatchInfo mi : this.matches) {
			for (SimpleTxn mactx : mi.macTxn) {
				matchTransaction(mi, mactx);
			}
		}
	}

	private void processTuple(String[] tuple) {
		infoMessage("  [");
		for (String field : tuple) {
			infoMessageNoln("'" + field + "', ");
		}
		infoMessage("]");

		QDate txdate = dateFromTuple(tuple);
		SimpleTxn txn = createTransaction(txdate, tuple);
		if (txn != null) {
			++this.totaltx;

			MatchInfo mi = new MatchInfo();
			mi.macTxn.add(txn);

			this.matches.add(mi);
		}
	}

	/**
	 * @param mi     Matchinfo for the mac txn
	 * @param mactxn The mac txn to match with windows txn(s)
	 */
	public void matchTransaction(MatchInfo mi, SimpleTxn mactxn) {
		infoMessage(mactxn.toString());

		Account acct = Account.getAccountByID(mactxn.getAccountID());
		List<SimpleTxn> txns = acct.findMatchingTransactions(mactxn);
		List<SimpleTxn> potentialtxns = acct.findPotentialMatchingTransactions(mactxn);

//		if (txns.isEmpty()) {
//			acct.findMatchingTransactions(mactxn);
//		}
//
//		while (!txns.isEmpty() //
//				&& this.matchedTransactions.contains(txns.get(0))) {
//			SimpleTxn t = txns.get(0);
//			if (mactxn.hasSplits() == t.hasSplits()) {
//				txns.remove(0);
//			}
//		}

		if (txns.isEmpty()) {
			acct.findMatchingTransactions(mactxn);
		}

		boolean iszero = Common.isEffectivelyZero(mactxn.getAmount());
		if (iszero) {
			this.allzero.add(mactxn);
		}

		if (!txns.isEmpty()) {
			if (txns.size() > 1) {
				mi.winTxnMatches = txns;
				System.out.println("Multiple wintxn matches for mactxn:\n   " + mactxn.toString());
			}

			SimpleTxn wintxn = txns.get(0);
			mi.winTxn.add(wintxn);
			this.matchInfoForWinTxn.put(wintxn, mi);

//			mi = null;
//
//			if (mactxn.hasSplits() != wintxn.hasSplits()) {
//				mi = this.partialMatches.get(wintxn);
//				if (mi == null) {
//					mi = new MatchInfo();
//					mi.isPseudoSplit = true;
//					mi.macTxn.add((GenericTxn) mactxn);
//					mi.winTxn.add(wintxn);
//				}
//			}
//			
//			this.match.put(mactxn, txns.get(0));
//			this.matchedTransactions.add(txns.get(0));
		} else {
			mi.winTxnPotential = potentialtxns;
			if (iszero) {
				this.nomatchZero.add(mactxn);
			} else {
				this.nomatch.add(mactxn);
			}
		}
	}

	private SimpleTxn createTransaction(QDate txdate, String[] tuple) {
		SimpleTxn txn = null;

		try {
			String acctname = tuple[ACCOUNT_IDX];

			// TODO account names different in mac file
			if (acctname.contentEquals("Tesla Model 3")) {
				acctname = "Tesla";
			} else if (acctname.contentEquals("Tesla Loan")) {
				acctname = "TeslaLoan";
			}

			Account acct = Account.findAccount(acctname);
			if (acct == null) {
				acct = new Account(acctname, AccountType.Bank);

				Account.addAccount(acct);
			}

			String payee = tuple[PAYEE_IDX];
			BigDecimal amount = Common.getDecimal(tuple[AMOUNT_IDX]);

			String split = tuple[SPLIT_IDX];
			String memo = tuple[MEMO_IDX];
			String cknum = tuple[CHECKNUM_IDX];
			String cat = tuple[CATEGORY_IDX];
			String desc = (DESCRIPTION_IDX >= 0) ? tuple[DESCRIPTION_IDX] : "";
			String type = tuple[TYPE_IDX];
			String sec = tuple[SECURITY_IDX];
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

			if (split.equals("S")) {
				if (this.lasttxn != null) {
					if (!this.lasttxn.getDate().equals(txdate)) {
						// TODO at some point, validate splits
						this.lasttxn = null;
					}
				}

				if (this.lasttxn == null) {
					this.lasttxn = (acct.isInvestmentAccount()) //
							? new InvestmentTxn(acct.acctid) //
							: new NonInvestmentTxn(acct.acctid);
				}

				txn = new SplitTxn(this.lasttxn);
			} else if (acct.isNonInvestmentAccount()) {
				txn = new NonInvestmentTxn(acct.acctid);
			} else if (acct.isInvestmentAccount()) {
				txn = new InvestmentTxn(acct.acctid);
			}

			if (!split.equals("S")) {
				txn.setDate(txdate);
			} else if (this.lasttxn.getDate() == null) {
				this.lasttxn.setDate(txdate);
			}

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
				nitxn.splits = new ArrayList<>();
			}
// TODO payee, acctForTransfer, amountTransferred, 
// TODO lots
			if (txn instanceof InvestmentTxn) {
				InvestmentTxn itxn = (InvestmentTxn) txn;
				itxn.accountForTransfer = null;
				itxn.amountTransferred = BigDecimal.ZERO;
				//itxn.setCatid(0);
				itxn.commission = BigDecimal.ZERO;
				itxn.price = BigDecimal.ZERO;
				itxn.security = null;
				itxn.xferTxns = null;
				// itxn.textFirstLine = null;

				// 0.42 shares @ 1.00
				if (!desc.isEmpty()) {
					int sharesidx = desc.indexOf(' ');
					int priceidx = desc.indexOf('@');
					if (priceidx >= 0) {
						try {
							String pricestr = desc.substring(priceidx + 1).trim();
							String quantitystr = desc.substring(0, sharesidx).trim();

							itxn.setQuantity(Common.getDecimal(quantitystr));
							itxn.price = Common.getDecimal(pricestr);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
				
			List<SimpleTxn> matches = acct.findPotentialMatchingTransactions(txn);
			matches.add(txn);
			System.out.println("" + matches.size() + " potential matches found");

//		public int xacctid;
//		public int catid; // >0: CategoryID; <0 AccountID
//		public SimpleTxn xtxn;
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (txn instanceof GenericTxn) {
			this.lasttxn = (GenericTxn) txn;
		}

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

				List<String[]> accttxns = this.transactionsMap.get(acctname);

				if (accttxns == null) {
					accttxns = new ArrayList<>();
					this.transactionsMap.put(acctname, accttxns);
				}

				while (record.size() < this.fieldnames.length) {
					record.add("");
				}

				String[] tuple = record.toArray(new String[0]);
				accttxns.add(tuple);
			}
		}
	}

	public List<String> readRecord() {
		List<String> fields = new ArrayList<>();

		try {
			String line = this.rdr.readLine();
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
						if (startidx < (endidx - 1)) {
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
		if ((f0.length() == 1) && (f0.charAt(0) == 65279)) {
			fields.remove(0);
			fields.add(0, "");
		}

		return fields;
	}

	private void setFieldIndexes(String[] fieldnames) {
		if (ACCOUNT_IDX >= 0) {
			return;
		}

		// "Split","Date","Payee","Category","Amount","Account"
		SPLIT_IDX = getFieldIndex("Split", fieldnames);
		DATE_IDX = getFieldIndex("Date", fieldnames);
		PAYEE_IDX = getFieldIndex("Payee", fieldnames);
		CATEGORY_IDX = getFieldIndex("Category", fieldnames);
		AMOUNT_IDX = getFieldIndex("Amount", fieldnames);
		ACCOUNT_IDX = getFieldIndex("Account", fieldnames);
		CHECKNUM_IDX = getFieldIndex("Check #", fieldnames);
		MEMO_IDX = getFieldIndex("Memo/Notes", fieldnames);
		DESCRIPTION_IDX = getFieldIndex("Description/Category", fieldnames);
		TYPE_IDX = getFieldIndex("Type", fieldnames);
		SECURITY_IDX = getFieldIndex("Security", fieldnames);
		FEES_IDX = getFieldIndex("Comm/Fee", fieldnames);
		SHARES_IDX = getFieldIndex("Shares", fieldnames);
	}

	public void readFieldNames() {
		for (;;) {
			List<String> record = readRecord();
			if (record == null) {
				return;
			}

			if (record.contains("Account") && record.contains("Date")) {
				this.fieldnames = record.toArray(new String[0]);

				List<String[]> fnlist = new ArrayList<>();
				fnlist.add(this.fieldnames);

				this.transactionsMap.put("FieldNames", fnlist);
				setFieldIndexes(this.fieldnames);

				break;
			}
		}

//		List<String[]> fnlist = new ArrayList<>();
//		fnlist.add(this.fieldnames);
//		this.transactionsMap.put("FieldNames", fnlist);
//		setFieldIndexes(this.fieldnames);
	}
}
