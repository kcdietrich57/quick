package moneymgr.io.cvs;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.QifDom;
import moneymgr.model.Account;
import moneymgr.model.AccountType;
import moneymgr.model.Category;
import moneymgr.model.GenericTxn;
import moneymgr.model.InvestmentTxn;
import moneymgr.model.NonInvestmentTxn;
import moneymgr.model.Security;
import moneymgr.model.SimpleTxn;
import moneymgr.model.SplitTxn;
import moneymgr.model.TxAction;
import moneymgr.util.Common;
import moneymgr.util.QDate;

/** Import data from CSV file exported from MacOS Quicken */
public class CSVImport {

	public static class TupleInfo {
		// "Split",""Account",Date","Check #","Payee","Category","Amount","Memo/Notes",
		// "Description/Category","Type","Security","Comm/Fee","Shares",
		// "Action","Shares In","Shares Out","Inflow","Outflow",
		//
		// "Modified","Reference","Transfer","Clr",
		public static int SPLIT_IDX = -1;
		public static int ACCOUNT_IDX = -1;
		public static int DATE_IDX = -1;
		public static int CHECKNUM_IDX = -1;
		public static int PAYEE_IDX = -1;
		public static int CATEGORY_IDX; // >0: CategoryID; <0 AccountID
		public static int AMOUNT_IDX;
		public static int MEMO_IDX;
		public static int DESCRIPTION_IDX = -1;
		public static int TYPE_IDX = -1;
		public static int SECURITY_IDX = -1;
		public static int FEES_IDX = -1;
		public static int SHARES_IDX = -1;

		public static int ACTION_IDX = -1;
		public static int SHARESIN_IDX = -1;
		public static int SHARESOUT_IDX = -1;
		public static int INFLOW_IDX = -1;
		public static int OUTFLOW_IDX = -1;
		// private static int XACCOUNT_IDX;

		/** List of field names for the input CSV file */
		public static String[] fieldnames = null;

		private static int getFieldIndex(String fieldname) {
			for (int idx = 0; idx < fieldnames.length; ++idx) {
				if (fieldnames[idx].equals(fieldname)) {
					return idx;
				}
			}

			return -1;
		}

		public static void setFieldNames(String[] fieldnames) {
			TupleInfo.fieldnames = fieldnames;

			// "Split","Date","Payee","Category","Amount","Account"
			SPLIT_IDX = getFieldIndex("Split");
			DATE_IDX = getFieldIndex("Date");
			PAYEE_IDX = getFieldIndex("Payee");
			CATEGORY_IDX = getFieldIndex("Category");
			AMOUNT_IDX = getFieldIndex("Amount");
			ACCOUNT_IDX = getFieldIndex("Account");
			CHECKNUM_IDX = getFieldIndex("Check #");
			MEMO_IDX = getFieldIndex("Memo/Notes");
			DESCRIPTION_IDX = getFieldIndex("Description/Category");
			TYPE_IDX = getFieldIndex("Type");
			SECURITY_IDX = getFieldIndex("Security");
			FEES_IDX = getFieldIndex("Comm/Fee");
			SHARES_IDX = getFieldIndex("Shares");
			ACTION_IDX = getFieldIndex("Action");
			SHARESIN_IDX = getFieldIndex("Shares In");
			SHARESOUT_IDX = getFieldIndex("Shares Out");
			INFLOW_IDX = getFieldIndex("Inflow");
			OUTFLOW_IDX = getFieldIndex("Outflow");
		}

		public String[] values;
		public QDate date = null;
		public SimpleTxn macTxn = null;
		public SimpleTxn winTxn = null;
		public final List<SimpleTxn> winTxnMatches = new ArrayList<SimpleTxn>();
		public String messages = "";
		public boolean datemismatch = false;
		public boolean fixaction = false;

		public TupleInfo(List<String> values) {
			while (values.size() < TupleInfo.fieldnames.length) {
				values.add("");
			}

			this.values = values.toArray(new String[0]);
		}

		public String value(int idx) {
			return ((idx >= 0) && (idx < this.values.length)) ? this.values[idx] : "";
		}

		public QDate getDate() {
			if (this.date == null) {
				try {
					this.date = Common.parseQDate(value(DATE_IDX));
				} catch (Exception e) {
					this.date = QDate.today();
				}
			}

			return this.date;
		}

		public void addMessage(String msg) {
			this.messages += msg + "\n";
		}

		public String toString() {
			return "" //
					+ value(DATE_IDX) //
					+ "\n   acct:" + value(ACCOUNT_IDX) + ":" //
					+ "\n   ty:" + value(TYPE_IDX) + ":" //
					+ "\n   actn:" + value(ACTION_IDX) + ":" //
					+ "\n   pay:" + value(PAYEE_IDX) + ":" //
					+ "\n   amt:" + value(AMOUNT_IDX) + ":" //
					+ "\n   spl:" + value(SPLIT_IDX) + ":" //
					+ "\n   cat:" + value(CATEGORY_IDX) + ":" //
					+ "\n   fee:" + value(FEES_IDX) + ":" //
					+ "\n   ck:" + value(CHECKNUM_IDX) + ":" //
					+ "\n   mem:" + value(MEMO_IDX) + ":" //
					+ "\n   dsc:" + value(DESCRIPTION_IDX) + ":" //
					+ "\n   infl:" + value(INFLOW_IDX) + ":" //
					+ "\n   outfl:" + value(OUTFLOW_IDX) + ":" //
					+ "\n   sec:" + value(SECURITY_IDX) + ":" //
					+ "\n   shr:" + value(SHARES_IDX) + ":" //
					+ "\n   shin:" + value(SHARESIN_IDX) + ":" //
					+ "\n   shout:" + value(SHARESOUT_IDX) + ":" //
			;
		}
	}

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

	private LineNumberReader rdr;

	/** Map account name to transaction tuples */
	public Map<String, List<TupleInfo>> transactionsMap = new HashMap<>();

	/** When assembling splits, this holds the current main transaction */
	public GenericTxn lasttxn = null;

	/**
	 * Matches come in several forms<br>
	 * Simple - no splits on either side Split - tx exists as split in both versions
	 * PseudoSplit - non-split matches split in other version
	 */
//	public static class MatchInfo {
//		public List<SimpleTxn> macTxn = new ArrayList<>();
//		public List<SimpleTxn> winTxn = new ArrayList<>();
//	}

	// create mactx from tuple
	// tuple<->mactx
	// tuple->[wintx]
	// wintx->[tuple]

//	public List<MatchInfo> matches = new ArrayList<>();
//	public Map<SimpleTxn, MatchInfo> matchInfoForWinTxn = new HashMap<>();
//	public Map<GenericTxn, MatchInfo> partialMatches = new HashMap<>();

//	public Set<SimpleTxn> matchedTransactions = new HashSet<>();

	/** Map MAC tx to WIN tx */
//	public Map<SimpleTxn, SimpleTxn> match = new HashMap<>();
//	public Map<SimpleTxn, List<GenericTxn>> multimatch = new HashMap<>();
	public List<TupleInfo> nomatch = new ArrayList<>();
	public List<TupleInfo> allzero = new ArrayList<>();
	public List<TupleInfo> nomatchZero = new ArrayList<>();
	public int totaltx = 0;

	public CSVImport(String filename) {
		try {
			this.rdr = new LineNumberReader(new FileReader(filename));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void importFile() {
		if (!readFieldNames()) {
			return;
		}

		importCSVRecords();
		// processCSVRecords();

		try {
			this.rdr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			PrintStream out = new PrintStream("/Users/greg/qif/tupleinfo.out");

			int cleantuples = 0;
			int dirtytuples = 0;
			int datefixed = 0;
			int actionfixed = 0;

			for (Account acct : Account.getAccounts()) {
				List<TupleInfo> tuples = this.transactionsMap.get(acct.name);

				if (tuples != null) {
					for (TupleInfo tuple : tuples) {
						if (tuple.messages.isEmpty()) {
							++cleantuples;
							if (tuple.datemismatch) {
								++datefixed;
							}
							if (tuple.fixaction) {
								++actionfixed;
							}
						} else {
							++dirtytuples;
							out.println("\nMessages for tuple " + tuple.macTxn.txid //
									+ " dateMismatch:" + tuple.datemismatch //
									+ " fixaction: " + tuple.fixaction);
							out.print(tuple.messages);
						}
					}
				}
			}

			out.println("\nClean tuples: " + cleantuples);
			out.println("\nClean but fixed date: " + datefixed + " action: " + actionfixed);
			out.println("\nDirty tuples: " + dirtytuples);

			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void importCSVRecords() {
		for (;;) {
			TupleInfo tuple = readRecord();
			if (tuple == null) {
				break;
			}

			String f0 = tuple.value(0);

			if (f0.trim().isEmpty()) {
				String acctname = tuple.value(TupleInfo.ACCOUNT_IDX);

				List<TupleInfo> accttxns = this.transactionsMap.get(acctname);

				if (accttxns == null) {
					accttxns = new ArrayList<>();
					this.transactionsMap.put(acctname, accttxns);
				}

				accttxns.add(tuple);

				processTuple(tuple);
			}
		}
	}

	private void processTuple(TupleInfo tuple) {
		SimpleTxn txn = createTransaction(tuple);
		if (txn != null) {
			++this.totaltx;
			tuple.macTxn = txn;

			matchTransaction(tuple);
		}
	}

	/**
	 * @param tuple Tupleinfo for the txn
	 */
	public void matchTransaction(TupleInfo tuple) {
		SimpleTxn mactxn = tuple.macTxn;

		infoMessage(mactxn.toString());

		Account acct = Account.getAccountByID(mactxn.getAccountID());
		List<SimpleTxn> txns = acct.findMatchingTransactions(mactxn);
//		List<SimpleTxn> potentialtxns = acct.findPotentialMatchingTransactions(mactxn);

//		if (txns.isEmpty()) {
//			acct.findMatchingTransactions(mi.macTxn);
//		}
//
//		while (!txns.isEmpty() //
//				&& this.matchedTransactions.contains(txns.get(0))) {
//			SimpleTxn t = txns.get(0);
//			if (mi.macTxn.hasSplits() == t.hasSplits()) {
//				txns.remove(0);
//			}
//		}

		if (txns.isEmpty()) {
			acct.findMatchingTransactions(mactxn);
		}

		boolean iszero = Common.isEffectivelyZero(mactxn.getAmount());
		if (iszero) {
			this.allzero.add(tuple);
		}

		if (!txns.isEmpty()) {
			if (txns.size() > 1) {
				tuple.winTxnMatches.addAll(txns);
				tuple.addMessage( //
						"Multiple(" + txns.size() + ") wintxn matches found for:\n   " //
								+ mactxn.toString());
			}

			SimpleTxn wintxn = null;
			int lastdiff = 0;

			// TODO this matching sucks
			for (SimpleTxn tx : txns) {
				int diff = mactxn.compareToXX(tuple, tx);

				if (wintxn == null || lastdiff > diff) {
					wintxn = tx;
					lastdiff = diff;
				}
			}

			if (lastdiff != 0) {
				tuple.addMessage("Inexact match:\n" //
						+ wintxn.toString() + "\n" //
						+ mactxn.toString());
			}

			tuple.winTxn = wintxn;
//			this.matchInfoForWinTxn.put(wintxn, mi);

//			mi = null;
//
//			if (mi.macTxn.hasSplits() != wintxn.hasSplits()) {
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
			if (iszero) {
				this.nomatchZero.add(tuple);
			} else {
				this.nomatch.add(tuple);
			}
		}
	}

	private SimpleTxn createTransaction(TupleInfo tuple) {
		SimpleTxn txn = null;

		try {
			// TODO account names different in mac file
			String acctname = tuple.value(TupleInfo.ACCOUNT_IDX);
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

			String payee = tuple.value(TupleInfo.PAYEE_IDX);
			BigDecimal amount = Common.getDecimal(tuple.value(TupleInfo.AMOUNT_IDX));

			String split = tuple.value(TupleInfo.SPLIT_IDX);
			String memo = tuple.value(TupleInfo.MEMO_IDX);
			String cknum = tuple.value(TupleInfo.CHECKNUM_IDX);
			String cat = tuple.value(TupleInfo.CATEGORY_IDX);
			String desc = tuple.value(TupleInfo.DESCRIPTION_IDX);
			String type = tuple.value(TupleInfo.TYPE_IDX);
			String sec = tuple.value(TupleInfo.SECURITY_IDX);
			String fees = tuple.value(TupleInfo.FEES_IDX);
			String shares = tuple.value(TupleInfo.SHARES_IDX);
			String action = tuple.value(TupleInfo.ACTION_IDX);
			String sharesIn = tuple.value(TupleInfo.SHARESIN_IDX);
			String sharesOut = tuple.value(TupleInfo.SHARESOUT_IDX);
			String inflow = tuple.value(TupleInfo.INFLOW_IDX);
			String outflow = tuple.value(TupleInfo.OUTFLOW_IDX);

			Account xferAcct = null;
			Category c = null;
			int catid;

			if (cat.startsWith("Transfer:[")) {
				// TODO account names different in mac file
				cat = cat.substring(10, cat.length() - 1);
				if (cat.contentEquals("Tesla Model 3")) {
					cat = "Tesla";
				} else if (cat.contentEquals("Tesla Loan")) {
					cat = "TeslaLoan";
				}
				xferAcct = Account.findAccount(cat);
				catid = (xferAcct != null) ? -xferAcct.acctid : 0;
			} else {
				c = (!cat.isEmpty()) ? Category.findCategory(cat) : null;
				catid = (c != null) ? c.catid : 0;
			}

			if (split.equals("S")) {
				if (this.lasttxn != null) {
					if (!this.lasttxn.getDate().equals(tuple.getDate())) {
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
				txn.setDate(tuple.getDate());
			} else if (this.lasttxn.getDate() == null) {
				this.lasttxn.setDate(tuple.getDate());
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
			}
// TODO payee, acctForTransfer, amountTransferred, 
// TODO lots
			if (txn instanceof InvestmentTxn) {
				InvestmentTxn itxn = (InvestmentTxn) txn;
// TODO action for non-transfers (payment, income, buy/sell, buyx/sellx, etc)
				if (itxn.getCatid() < 0) {
					itxn.setXferAcctid(-itxn.getCatid());
				}

				if (itxn.getXferAcctid() > 0) {
					itxn.accountForTransfer = Account.getAccountByID(itxn.getXferAcctid()).name;
					itxn.setAction((inflow.isEmpty()) ? TxAction.XOUT : TxAction.XIN);
					itxn.amountTransferred = amount;
				} else {
					itxn.setAction(TxAction.OTHER);
					itxn.amountTransferred = BigDecimal.ZERO;
					itxn.accountForTransfer = null;
				}
				itxn.commission = (fees.isEmpty()) //
						? BigDecimal.ZERO //
						: Common.getDecimal(fees);
				itxn.price = BigDecimal.ZERO;
				if (!sec.isEmpty()) {
					itxn.security = Security.findSecurity(sec);
				}

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
//			System.out.println("" + matches.size() + " potential matches found");

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

	public List<String> readRawRecord() {
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

	public TupleInfo readRecord() {
		List<String> fields = readRawRecord();
		return (fields != null) ? new TupleInfo(fields) : null;
	}

	public boolean readFieldNames() {
		for (;;) {
			List<String> record = readRawRecord();
			if (record == null) {
				break;
			}

			if (record.contains("Account") && record.contains("Date")) {
				TupleInfo.setFieldNames(record.toArray(new String[0]));

//				List<String[]> fnlist = new ArrayList<>();
//				fnlist.add(TupleInfo.fieldnames);
//				this.transactionsMap.put("FieldNames", fnlist);
//				TupleInfo.setFieldIndexes();

				return true;
			}
		}

//		List<String[]> fnlist = new ArrayList<>();
//		fnlist.add(this.fieldnames);
//		this.transactionsMap.put("FieldNames", fnlist);
//		setFieldIndexes(this.fieldnames);

		return false;
	}

//	private void processCSVRecords() {
//		for (Map.Entry<String, List<TupleInfo>> entry : this.transactionsMap.entrySet()) {
//			String accountName = entry.getKey();
//			List<TupleInfo> accountTuples = entry.getValue();
//
//			if (!accountName.equals("FieldNames")) {
//				infoMessage(accountName);
//
//				for (TupleInfo tuple : accountTuples) {
//					processTuple(tuple);
//				}
//			}
//		}
//
//		for (MatchInfo mi : this.matches) {
//			for (SimpleTxn mactx : mi.macTxn) {
//				//matchTransaction(mi, mactx);
//			}
//		}
//	}

//	private void processTuple(TupleInfo tuple) {
////		infoMessage("  [");
////		for (String field : tuple.values) {
////			infoMessageNoln("'" + field + "', ");
////		}
////		infoMessage("]");
//
//		SimpleTxn txn = createTransaction(tuple);
//		if (txn != null) {
//			++this.totaltx;
//			tuple.macTxn = txn;
//			matchTransaction(tuple);
////			MatchInfo mi = new MatchInfo();
////			tuple.macTxn.add(txn);
//
////			this.matches.add(mi);
//		}
//	}
}
