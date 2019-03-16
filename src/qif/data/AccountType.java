package qif.data;

public enum AccountType { //
	Bank, CCard, Cash, Asset, Liability, Invest, InvPort, Inv401k, InvMutual;

	public static AccountType parseAccountType(String s) {
		switch (s.charAt(0)) {
		case 'B':
			if (s.equals("Bank")) {
				return AccountType.Bank;
			}
			break;
		case 'C':
			if (s.equals("CCard")) {
				return AccountType.CCard;
			}
			if (s.equals("Cash")) {
				return AccountType.Cash;
			}
			break;
		case 'I':
			if (s.equals("Invst")) {
				return AccountType.Invest;
			}
			break;
		case 'M':
			if (s.equals("Mutual")) {
				return AccountType.InvMutual;
			}
			break;
		case 'O':
			if (s.equals("Oth A")) {
				return AccountType.Asset;
			}
			if (s.equals("Oth L")) {
				return AccountType.Liability;
			}
			break;
		case 'P':
			if (s.equals("Port")) {
				return AccountType.InvPort;
			}
			break;
		case '4':
			if (s.equals("401(k)/403(b)")) {
				return AccountType.Inv401k;
			}
			break;
		}

		Common.reportError("Unknown account type: " + s);
		return AccountType.Bank;
	}

	public boolean isLiability() {
		return !isAsset();
	}

	public boolean isAsset() {
		switch (this) {
		case Bank:
		case Cash:
		case Asset:
		case InvMutual:
		case InvPort:
		case Invest:
		case Inv401k:
			return true;

		case CCard:
		case Liability:
			return false;
		}

		return false;
	}

	public String toString() {
		switch (this) {
		case Cash:
			return "CSH";
		case Bank:
			return "BNK";
		case Asset:
			return "AST";
		case Invest:
		case InvPort:
		case InvMutual:
			return "INV";
		case Inv401k:
			return "RET";
		case CCard:
			return "CCD";
		case Liability:
			return "LIA";
		default:
			return "---";
		}
	}
}