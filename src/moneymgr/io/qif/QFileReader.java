package moneymgr.io.qif;

import static moneymgr.io.qif.Headers.ACCT_CLOSEDATE;
import static moneymgr.io.qif.Headers.ACCT_CREDITLIMIT;
import static moneymgr.io.qif.Headers.ACCT_DESCRIPTION;
import static moneymgr.io.qif.Headers.ACCT_NAME;
import static moneymgr.io.qif.Headers.ACCT_STMTBAL;
import static moneymgr.io.qif.Headers.ACCT_STMTDATE;
import static moneymgr.io.qif.Headers.ACCT_STMTDAY;
import static moneymgr.io.qif.Headers.ACCT_STMTFREQ;
import static moneymgr.io.qif.Headers.ACCT_TYPE;
import static moneymgr.io.qif.Headers.CAT_BudgetAmount;
import static moneymgr.io.qif.Headers.CAT_Description;
import static moneymgr.io.qif.Headers.CAT_ExpenseCategory;
import static moneymgr.io.qif.Headers.CAT_IncomeCategory;
import static moneymgr.io.qif.Headers.CAT_Name;
import static moneymgr.io.qif.Headers.CAT_TaxRelated;
import static moneymgr.io.qif.Headers.CAT_TaxSchedule;
import static moneymgr.io.qif.Headers.END;
import static moneymgr.io.qif.Headers.HdrAccount;
import static moneymgr.io.qif.Headers.HdrAsset;
import static moneymgr.io.qif.Headers.HdrBank;
import static moneymgr.io.qif.Headers.HdrCash;
import static moneymgr.io.qif.Headers.HdrCategory;
import static moneymgr.io.qif.Headers.HdrClass;
import static moneymgr.io.qif.Headers.HdrCreditCard;
import static moneymgr.io.qif.Headers.HdrInvestment;
import static moneymgr.io.qif.Headers.HdrLiability;
import static moneymgr.io.qif.Headers.HdrMemorizedTransaction;
import static moneymgr.io.qif.Headers.HdrPrices;
import static moneymgr.io.qif.Headers.HdrSecurity;
import static moneymgr.io.qif.Headers.HdrStatements;
import static moneymgr.io.qif.Headers.HdrTag;
import static moneymgr.io.qif.Headers.INV_AccountForTransfer;
import static moneymgr.io.qif.Headers.INV_Action;
import static moneymgr.io.qif.Headers.INV_Payee;
import static moneymgr.io.qif.Headers.INV_AmountTransferred;
import static moneymgr.io.qif.Headers.INV_ClearedStatus;
import static moneymgr.io.qif.Headers.INV_Commission;
import static moneymgr.io.qif.Headers.INV_Date;
import static moneymgr.io.qif.Headers.INV_Memo;
import static moneymgr.io.qif.Headers.INV_Price;
import static moneymgr.io.qif.Headers.INV_Quantity;
import static moneymgr.io.qif.Headers.INV_Security;
import static moneymgr.io.qif.Headers.INV_TextFirstLine;
import static moneymgr.io.qif.Headers.INV_TransactionAmount;
import static moneymgr.io.qif.Headers.INV_TransactionAmount2;
import static moneymgr.io.qif.Headers.SEC_GOAL;
import static moneymgr.io.qif.Headers.SEC_NAME;
import static moneymgr.io.qif.Headers.SEC_SYMBOL;
import static moneymgr.io.qif.Headers.SEC_TYPE;
import static moneymgr.io.qif.Headers.STMTS_ACCOUNT;
import static moneymgr.io.qif.Headers.STMTS_CASH;
import static moneymgr.io.qif.Headers.STMTS_MONTHLY;
import static moneymgr.io.qif.Headers.STMTS_SECURITY;
import static moneymgr.io.qif.Headers.TXN_Address;
import static moneymgr.io.qif.Headers.TXN_Amount;
import static moneymgr.io.qif.Headers.TXN_Amount2;
import static moneymgr.io.qif.Headers.TXN_Category;
import static moneymgr.io.qif.Headers.TXN_ClearedStatus;
import static moneymgr.io.qif.Headers.TXN_Date;
import static moneymgr.io.qif.Headers.TXN_Memo;
import static moneymgr.io.qif.Headers.TXN_Number;
import static moneymgr.io.qif.Headers.TXN_Payee;
import static moneymgr.io.qif.Headers.TXN_SplitAmount;
import static moneymgr.io.qif.Headers.TXN_SplitCategory;
import static moneymgr.io.qif.Headers.TXN_SplitMemo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import moneymgr.model.MoneyMgrModel;
import moneymgr.util.Common;

class RawObjectInfo {
	static Map<String, String> typeMap = new HashMap<>();
	static Map<Character, String> fieldMap = new HashMap<>();
	static Map<Character, String> txFieldMap = new HashMap<>();
	static Map<Character, String> catFieldMap = new HashMap<>();
	static Map<Character, String> assFieldMap = new HashMap<>();

	static Map<String, List<RawObjectInfo>> objectMap = new HashMap<>();

	// There is a hierarchy of types:
	// Tag
	// Category
	// Security
	// - Prices
	// Account
	// - Transaction
	// - - Split

	static List<RawObjectInfo> tags = new ArrayList<>();
	static List<RawObjectInfo> categories = new ArrayList<>();
	static List<RawObjectInfo> securities = new ArrayList<>();
	static List<RawObjectInfo> accounts = new ArrayList<>();
	static Map<RawObjectInfo, List<RawObjectInfo>> accountTransactions = new HashMap<>();
	static Map<RawObjectInfo, List<RawObjectInfo>> securityPrices = new HashMap<>();

	static {
		typeMap.put("Cat", "Category");
		typeMap.put("Tag", "Tag");
		typeMap.put("Security", "Security");
		typeMap.put("Prices", "Prices");
		typeMap.put("Account", "Account");

		typeMap.put("Bank", "Transaction");
		typeMap.put("Cash", "Transaction");
		typeMap.put("CCard", "Transaction");
		typeMap.put("Invst", "Transaction");
		typeMap.put("Oth A", "Transaction");
		typeMap.put("Oth L", "Transaction");

		txFieldMap.put('D', "Date"); // Transaction
		txFieldMap.put('L', "Category/transfer"); // Transaction
		txFieldMap.put('S', "Split category/transfer"); // Transaction
		txFieldMap.put('T', "Amount"); // Transaction

		assFieldMap.put('L', "Category/transfer"); // Transaction
		assFieldMap.put('S', "Split category/transfer"); // Transaction
		assFieldMap.put('T', "Amount"); // Transaction

		catFieldMap.put('E', "E??"); // Category
		catFieldMap.put('I', "I??"); // Category
		catFieldMap.put('T', "T??"); // Category

		fieldMap.put('B', "Budget");
		fieldMap.put('C', "Cleared");
		fieldMap.put('D', "Description");
		fieldMap.put('E', "Split memo");
		fieldMap.put('G', "Goal");
		fieldMap.put('I', "Price"); // InvTransaction(security)
		fieldMap.put('L', "Initial balance"); // Account
		fieldMap.put('M', "Memo");
		fieldMap.put('N', "Name");
		fieldMap.put('O', "Commission");
		fieldMap.put('P', "Payee");
		fieldMap.put('Q', "Quantity");
		fieldMap.put('R', "Tax Code");
		fieldMap.put('S', "Symbol"); // Security
		fieldMap.put('T', "Type"); // Account/Security
		fieldMap.put('U', "Amount");
		fieldMap.put('Y', "Security");
		fieldMap.put('$', "Split amount");

		// Prices ??????
		fieldMap.put('"', "???????");
	}

	static void add(RawObjectInfo object) {
		List<RawObjectInfo> objs = objectMap.getOrDefault(object.objType, null);

		if (objs == null) {
			objs = new ArrayList<>();
			objectMap.put(object.objType, objs);
		}

		objs.add(object);
	}

	static void processObjects() {
		tags.addAll(objectMap.get("Tag"));
		categories.addAll(objectMap.get("Category"));
		securities.addAll(objectMap.get("Security"));
		accounts.addAll(objectMap.get("Account"));

		// TODO Prices

		for (RawObjectInfo tx : objectMap.get("Transaction")) {
			List<RawObjectInfo> txs = accountTransactions.getOrDefault(tx.account, null);
			if (txs == null) {
				txs = new ArrayList<>();
				accountTransactions.put(tx.account, txs);
			}

			txs.add(tx);
		}
	}

	static void reportObjects(PrintStream ps) {
		ps.println("\nObject Report:");

		ps.printf("  There are %s tags\n", tags.size());
		ps.printf("  There are %s categories\n", categories.size());
		ps.printf("  There are %s securities\n", securities.size());
		ps.printf("  There are %s accounts\n", accounts.size());

		for (RawObjectInfo acct : accounts) {
			List<RawObjectInfo> txs = accountTransactions.getOrDefault( //
					acct, new ArrayList<>());

			ps.printf("  %s has %s transactions\n", //
					acct.getValue("Name"), txs.size());
		}
	}

	static String mapType(String objType) {
		objType = objType.trim();

		return typeMap.getOrDefault(objType, objType);
	}

	String mapFieldType(Character typeChar) {
		if (this.objType == "Transaction") {
			String txFieldType = txFieldMap.getOrDefault(typeChar, null);

			if (txFieldType != null) {
				return txFieldType;
			}
		} else if (this.objType.equals("Asset") || this.objType.equals("Liability")) {
			String assFieldType = assFieldMap.getOrDefault(typeChar, null);

			if (assFieldType != null) {
				return assFieldType;
			}
		} else if (this.objType.equals("Category")) {
			String catFieldType = catFieldMap.getOrDefault(typeChar, null);

			if (catFieldType != null) {
				return catFieldType;
			}
		}

		return fieldMap.getOrDefault(typeChar, null);
	}

	final int lineNum;
	final String objType;
	final String acctType;
	final RawObjectInfo account;
	final Map<String, Object> valueMap;

	@SuppressWarnings("unchecked")
	RawObjectInfo(String objType, int lineNum, List<String> lines, RawObjectInfo acct) {
		this.objType = mapType(objType);
		this.acctType = null; // (this.objType == "Account") ? objType : null;
		this.lineNum = lineNum;
		this.account = (this.objType.equals("Transaction")) ? acct : null;
		this.valueMap = new HashMap<>();

		if (objType.equals("Prices")) {
			this.valueMap.put("SymPriceDate", lines.get(0));

			return;
		}

		String[] currentSplit = null;

		for (String line : lines) {
			String valType = mapFieldType(line.charAt(0));
			String value = line.substring(1);

			if (valType.startsWith("Split")) {
				if (valType.startsWith("Split category")) {
					List<String[]> splitList = (List<String[]>) this.valueMap.getOrDefault("Splits", null);

					if (splitList == null) {
						splitList = new ArrayList<>();
						this.valueMap.put("Splits", splitList);
					}

					currentSplit = new String[3];
					splitList.add(currentSplit);

					currentSplit[0] = value;
				} else if (valType.equals("Split memo")) {
					currentSplit[1] = value;
				} else {
					if (currentSplit != null) {
						currentSplit[2] = value;
						currentSplit = null;
					} else {
						// TODO special case for investment accounts
						this.valueMap.put("Transfer Amount", line.substring(1));
					}
				}
			} else if (this.valueMap.containsKey(valType)) {
				List<String> values;

				if (this.valueMap.get(valType) instanceof String) {
					values = new ArrayList<>();
					values.add((String) this.valueMap.get(valType));
					this.valueMap.put(valType, values);
				} else {
					values = (List<String>) this.valueMap.get(valType);
				}

				values.add(line.substring(1));
			} else {
				this.valueMap.put(valType, line.substring(1));
			}
		}
	}

	String getName() {
		if (this.valueMap.containsKey("Name")) {
			return (String) this.valueMap.get("Name");
		}

		return "n/a";
	}

	void addValue(String c, String value) {
		valueMap.put(c, value);
	}

	String getValue(String valType) {
		Object valObj = this.valueMap.get(valType);

		if (valObj instanceof String) {
			return (String) valObj;
		}

		return null;
	}

	@Override
	public String toString() {
		String atype = (this.acctType != null) //
				? String.format("(%s)", this.acctType) //
				: "";
		String ret = String.format("  %s%s[%d]", //
				this.objType, atype, this.lineNum);

		if (this.account != null) {
			ret += String.format("\n  (A): %s", this.account.getName());
		}

		ret += "\n";

		for (Entry<String, Object> o : this.valueMap.entrySet()) {
			String valstr = "";

			Object v = o.getValue();
			if (o.getKey().equals("Splits")) {
				String sep = "\n    ";

				valstr = "[";

				ArrayList<String[]> vlist = (ArrayList<String[]>) v;
				for (String[] ss : vlist) {
					valstr += sep;
					sep = ",\n    ";

					String sep2 = "[";

					for (String s : ss) {
						valstr += sep2 + s;
						sep2 = ",";
					}

					valstr += ']';
				}

				valstr += ']';
			} else if (v instanceof String) {
				valstr = (String) v;
			} else if (v != null) {
				valstr += v.toString();
			}

			ret += String.format("  (%s): %s\n", //
					o.getKey(), valstr);
		}

		return ret;
	}
}

/** Reader to process input QIF files */
public class QFileReader {
	/** Various sections that occur in QIF files */
	public static enum SectionType {
		EndOfFile, Account, Statement, Statements, //
		Bank, Cash, Category, CreditCard, Investment, //
		Asset, Liability, MemorizedTransaction, QClass, Prices, Security, Tag
	};

	/** Contains decomposed line from QIF file */
	public static class QLine {
		/** The type of field for the line */
		public FieldType type;
		/** Type marker character */
		public char typechar;
		/** Value portion of the line for the field */
		public String value;

		public String toString() {
			return this.type + ": " + this.value;
		}
	}

	public final MoneyMgrModel model;
	private LineNumberReader rdr;
	private String lookaheadLine = null;

	public QFileReader(MoneyMgrModel model, File file) {
		this.model = model;

		try {
			this.rdr = new LineNumberReader(new FileReader(file));
		} catch (final FileNotFoundException e) {
			this.rdr = null;
		}
	}

	public String readLine() {
		try {
			if (this.lookaheadLine != null) {
				String ret = this.lookaheadLine;
				this.lookaheadLine = null;

				return ret;
			}

			return this.rdr.readLine();
		} catch (final Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public String peekLine() {
		String line = readLine();
		unreadLine(line);
		return line;
	}

	private void unreadLine(String line) {
		if (this.lookaheadLine != null) {
			Common.reportError("Attempted to push back two lines:\n" //
					+ "[1]: " + this.lookaheadLine + "\n" //
					+ "[2]: " + line + "\n");
		}

		this.lookaheadLine = line;
	}

	private List<String> ingestLines() {
		List<String> lines = new ArrayList<>();

		for (;;) {
			String line = readLine();
			if (line == null) {
				break;
			}

			lines.add(line);
		}

		return lines;
	}

	public Object ingestFile() {
		List<String> lines = ingestLines();

		PrintStream ps = System.out;

		try {
			ps = new PrintStream("/tmp/ingest.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		String currType = "";
		// List<RawObjectInfo> currObjects = null;
		int objectCount = 0;
		int objectLineNum = 0;
		List<String> objectLines = new ArrayList<>();
		RawObjectInfo currAccount = null;

		for (int lineNum = 0; lineNum < lines.size(); ++lineNum) {
			String line = lines.get(lineNum);

			if (line.endsWith("AutoSwitch")) {
				continue;
			}

			if (line.startsWith("!Account")) {
				line = "!Type:Account";
			}

			if (line.startsWith("!Type:")) {
				currType = RawObjectInfo.mapType(line.substring(6));

//				if (!RawObjectInfo.objectMap.containsKey(currType)) {
//					RawObjectInfo.objectMap.put(currType, new ArrayList<>());
//				}
//
//				currObjects = RawObjectInfo.objectMap.get(currType);
			} else {
				if (line.equals("^")) {
					if (!objectLines.isEmpty()) {
						++objectCount;

						RawObjectInfo raw = new RawObjectInfo(currType, objectLineNum, objectLines, currAccount);
						RawObjectInfo.add(raw);

						// System.out.println("\nObject of type " + currType + ":");
						ps.println(raw.toString());

						objectLines.clear();
						objectLineNum = lineNum + 1;

						if (currType.equals("Account")) {
							RawObjectInfo existingAccount = findObject( //
									RawObjectInfo.objectMap, //
									"Account", "Name", raw.getValue("Name"));

							if (existingAccount != null) {
								// TODO validate accounts match
								currAccount = existingAccount;
							} else {
								currAccount = raw;
							}
						} else if (!raw.objType.equals("Transaction")) {
							currAccount = null;
						}
					}
				} else {
					objectLines.add(line);
				}
			}
		}

		ps.println("Loaded " + objectCount + " objects");

		RawObjectInfo.processObjects();
		RawObjectInfo.reportObjects(ps);

		return lines;
	}

	RawObjectInfo findObject(Map<String, List<RawObjectInfo>> objMap, //
			String objType, String valType, String value) {
		List<RawObjectInfo> objects = objMap.get(objType);

		for (RawObjectInfo raw : objects) {
			String v = raw.getValue(valType);

			if (v != null && v.equals(value)) {
				return raw;
			}
		}

		return null;
	}

	public SectionType findFirstSection() {
		return nextSection();
	}

	public SectionType nextSection() {
		try {
			for (;;) {
				String line = readLine();
				if (line == null) {
					return SectionType.EndOfFile;
				}

				if (line.startsWith("!") //
						&& !line.startsWith("!Option") //
						&& !line.startsWith("!Clear")) {
					return parseSectionType(line.trim());
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}

		return SectionType.EndOfFile;
	}

	private SectionType parseSectionType(String line) {
		if (line.equalsIgnoreCase(HdrAccount)) {
			return SectionType.Account;
		}
		if (line.equalsIgnoreCase(HdrBank)) {
			return SectionType.Bank;
		}
		if (line.equalsIgnoreCase(HdrStatements)) {
			return SectionType.Statements;
		}
		if (line.equalsIgnoreCase(HdrCash)) {
			return SectionType.Cash;
		}
		if (line.equalsIgnoreCase(HdrCategory)) {
			return SectionType.Category;
		}
		if (line.equalsIgnoreCase(HdrCreditCard)) {
			return SectionType.CreditCard;
		}
		if (line.equalsIgnoreCase(HdrInvestment)) {
			return SectionType.Investment;
		}
		if (line.equalsIgnoreCase(HdrAsset)) {
			return SectionType.Asset;
		}
		if (line.equalsIgnoreCase(HdrLiability)) {
			return SectionType.Liability;
		}
		if (line.equalsIgnoreCase(HdrMemorizedTransaction)) {
			return SectionType.MemorizedTransaction;
		}
		if (line.equalsIgnoreCase(HdrClass)) {
			return SectionType.QClass;
		}
		if (line.equalsIgnoreCase(HdrPrices)) {
			return SectionType.Prices;
		}
		if (line.equalsIgnoreCase(HdrSecurity)) {
			return SectionType.Security;
		}
		if (line.equalsIgnoreCase(HdrTag)) {
			return SectionType.Tag;
		}

		Common.reportError("Syntax Error: Section header: " + line);
		return SectionType.Bank;
	}

	public void nextAccountLine(QLine line) {
		try {
			if (nextLine(line)) {
				line.type = accountFieldType(line.typechar);
				return;
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}

		line.type = FieldType.EndOfSection;
	}

	public void nextStatementsLine(QLine line) {
		try {
			if (nextLine(line)) {
				line.type = statementsFieldType(line.typechar);
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		line.type = FieldType.EndOfSection;
	}

	public void nextSecurityLine(QLine line) {
		try {
			if (nextLine(line)) {
				line.type = securityFieldType(line.typechar);
				return;
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}

		line.type = FieldType.EndOfSection;
	}

	public void nextPriceLine(QLine line) {
		try {
			if (nextLine(line)) {
				line.type = priceFieldType(line.typechar);
				return;
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}

		line.type = FieldType.EndOfSection;
	}

	public void nextCategoryLine(QLine line) {
		try {
			if (nextLine(line)) {
				line.type = categoryFieldType(line.typechar);
				return;
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}

		line.type = FieldType.EndOfSection;
	}

	public void nextTxnLine(QLine line) {
		try {
			if (nextLine(line)) {
				line.type = txnFieldType(line.typechar);
				return;
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}

		line.type = FieldType.EndOfSection;
	}

	public void nextInvLine(QLine line) {
		try {
			if (nextLine(line)) {
				line.type = invFieldType(line.typechar);
				return;
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}

		line.type = FieldType.EndOfSection;
	}

	/**
	 * Get the next QIF file line
	 * 
	 * @param line Structure to fill with line info
	 * @return True if a line is found; false if EOF
	 * @throws Exception
	 */
	private boolean nextLine(QLine line) throws Exception {
		String s = readLine();

		if (s == null) {
			line.type = FieldType.EndOfSection;
			return false;
		}

		if (s.length() < 1) {
			Common.reportError("Syntax error: field: " + s);
		}

		line.value = s.substring(1);
		line.typechar = s.charAt(0);

		return true;
	}

	private static FieldType securityFieldType(char key) {
		switch (key) {
		case END:
			return FieldType.EndOfSection;
		case SEC_SYMBOL:
			return FieldType.SecSymbol;
		case SEC_NAME:
			return FieldType.SecName;
		case SEC_TYPE:
			return FieldType.SecType;
		case SEC_GOAL:
			return FieldType.SecGoal;

		default:
			Common.reportError("Bad field type for security: " + key);
			return FieldType.EndOfSection;
		}
	}

	private static FieldType priceFieldType(char key) {
		switch (key) {
		case END:
			return FieldType.EndOfSection;

		default:
			Common.reportError("Bad field type for price: " + key);
			return FieldType.EndOfSection;
		}
	}

	private static FieldType accountFieldType(char key) {
		switch (key) {
		case END:
			return FieldType.EndOfSection;
		case ACCT_NAME:
			return FieldType.AcctName;
		case ACCT_TYPE:
			return FieldType.AcctType;
		case ACCT_DESCRIPTION:
			return FieldType.AcctDescription;
		case ACCT_CREDITLIMIT:
			return FieldType.AcctCreditLimit;
		case ACCT_STMTDATE:
			return FieldType.AcctStmtDate;
		case ACCT_STMTBAL:
			return FieldType.AcctStmtBal;
		case ACCT_CLOSEDATE:
			return FieldType.AcctCloseDate;
		case ACCT_STMTFREQ:
			return FieldType.AcctStmtFrequency;
		case ACCT_STMTDAY:
			return FieldType.AcctStmtDay;

		default:
			Common.reportError("Bad field type for account: " + key);
			return FieldType.EndOfSection;
		}
	}

	private static FieldType statementsFieldType(char key) {
		switch (key) {
		case END:
			return FieldType.EndOfSection;

		case STMTS_ACCOUNT:
			return FieldType.StmtsAccount;

		case STMTS_MONTHLY:
			return FieldType.StmtsMonthly;

		case STMTS_SECURITY:
			return FieldType.StmtsSecurity;

		case STMTS_CASH:
			return FieldType.StmtsCash;

		default:
			Common.reportError("Bad field type for statements: " + key);
			return FieldType.EndOfSection;
		}
	}

	private static FieldType categoryFieldType(char key) {
		switch (key) {
		case END:
			return FieldType.EndOfSection;

		case CAT_Name:
			return FieldType.CatName;
		case CAT_Description:
			return FieldType.CatDescription;
		case CAT_TaxRelated:
			return FieldType.CatTaxRelated;
		case CAT_IncomeCategory:
			return FieldType.CatIncomeCategory;
		case CAT_ExpenseCategory:
			return FieldType.CatExpenseCategory;
		case CAT_BudgetAmount:
			return FieldType.CatBudgetAmount;
		case CAT_TaxSchedule:
			return FieldType.CatTaxSchedule;

		default:
			Common.reportError("Bad field type for account: " + key);
			return FieldType.EndOfSection;
		}
	}

	private static FieldType txnFieldType(char key) {
		switch (key) {
		case END:
			return FieldType.EndOfSection;

		case TXN_Date:
			return FieldType.TxnDate;
		case TXN_Amount:
		case TXN_Amount2:
			return FieldType.TxnAmount;
		case TXN_ClearedStatus:
			return FieldType.TxnClearedStatus;
		case TXN_Number:
			return FieldType.TxnNumber;
		case TXN_Payee:
			return FieldType.TxnPayee;
		case TXN_Memo:
			return FieldType.TxnMemo;
		case TXN_Address:
			return FieldType.TxnAddress;
		case TXN_Category:
			return FieldType.TxnCategory;
		case TXN_SplitCategory:
			return FieldType.TxnSplitCategory;
		case TXN_SplitMemo:
			return FieldType.TxnSplitMemo;
		case TXN_SplitAmount:
			return FieldType.TxnSplitAmount;

		default:
			Common.reportError("Bad field type for account: " + key);
			return FieldType.EndOfSection;
		}
	}

	private static FieldType invFieldType(char key) {
		switch (key) {
		case END:
			return FieldType.EndOfSection;

		case INV_Date:
			return FieldType.InvDate;
		case INV_Action:
			return FieldType.InvAction;
		case INV_Payee:
			return FieldType.InvPayee;
		case INV_Security:
			return FieldType.InvSecurity;
		case INV_Price:
			return FieldType.InvPrice;
		case INV_Quantity:
			return FieldType.InvQuantity;
		case INV_TransactionAmount:
		case INV_TransactionAmount2:
			return FieldType.InvTransactionAmt;
		case INV_ClearedStatus:
			return FieldType.InvClearedStatus;
		case INV_TextFirstLine:
			return FieldType.InvFirstLine;
		case INV_Memo:
			return FieldType.InvMemo;
		case INV_Commission:
			return FieldType.InvCommission;
		case INV_AccountForTransfer:
			return FieldType.InvXferAcct;
		case INV_AmountTransferred:
			return FieldType.InvXferAmt;

		default:
			Common.reportError("Bad field type for account: " + key);
			return FieldType.EndOfSection;
		}
	}
}
