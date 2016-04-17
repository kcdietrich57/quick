package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// This can be global information, or for a single account
class SecurityPortfolio {
	public List<SecurityPosition> positions;

	public SecurityPortfolio() {
		this.positions = new ArrayList<SecurityPosition>();
	}

	public SecurityPosition getPosition(Security sec) {
		for (final SecurityPosition pos : this.positions) {
			if (pos.security == sec) {
				return pos;
			}
		}

		final SecurityPosition newpos = new SecurityPosition(sec);
		this.positions.add(newpos);

		return newpos;
	}

	public BigDecimal getPortfolioValueForDate(Date d) {
		BigDecimal portValue = BigDecimal.ZERO;

		for (final SecurityPosition pos : this.positions) {
			portValue = portValue.add(pos.getSecurityPositionValueForDate(d));
		}

		return portValue;
	}

	public String toString() {
		String s = "Securities Held:\n";

		int nn = 0;
		for (final SecurityPosition p : this.positions) {
			s += "  " + ++nn + ": " + p.toString() + "\n";
		}

		return s;
	}
}

class SecurityPosition {
	public Security security;
	public BigDecimal shares;
	public List<InvestmentTxn> transactions;
	public List<BigDecimal> shrBalance;

	public SecurityPosition(Security sec, BigDecimal shares) {
		this.security = sec;
		this.shares = shares;
		this.transactions = new ArrayList<InvestmentTxn>();
		this.shrBalance = new ArrayList<BigDecimal>();
	}

	public SecurityPosition(Security sec) {
		this(sec, BigDecimal.ZERO);
	}

	public String toString() {
		final String s = String.format( //
				"Sec: %-20s  Bal: %10.3f  Ntran: %d", //
				this.security.name, //
				this.shrBalance.get(this.shrBalance.size() - 1), //
				this.transactions.size());
		return s;
	}

	public BigDecimal getSecurityPositionValueForDate(Date d) {
		final int idx = getTransactionIndexForDate(d);
		if (idx < 0) {
			return BigDecimal.ZERO;
		}

		final InvestmentTxn txn = this.transactions.get(idx);
		final BigDecimal tshrbal = this.shrBalance.get(idx);
		final BigDecimal price = txn.security.getPriceForDate(d).price;

		return price.multiply(tshrbal);
	}

	public BigDecimal reportSecurityPositionForDate(Date d) {
		final int idx = getTransactionIndexForDate(d);
		if (idx < 0) {
			return BigDecimal.ZERO;
		}

		final InvestmentTxn txn = this.transactions.get(idx);
		final BigDecimal tshrbal = this.shrBalance.get(idx);
		final BigDecimal price = txn.security.getPriceForDate(d).price;
		final BigDecimal value = price.multiply(tshrbal);

		System.out.println(String.format("    %-36s %10.3f %10.3f %10.3f", //
				txn.security.name, tshrbal, price, value));

		return value;
	}

	public int getTransactionIndexForDate(Date d) {
		final int idx = Common.findLastTransactionOnOrBeforeDate(this.transactions, d);
		if (idx < 0) {
			return -1;
		}

		final BigDecimal tshrbal = this.shrBalance.get(idx);
		if (tshrbal.compareTo(BigDecimal.ZERO) <= 0) {
			return -1;
		}

		return idx;
	}
}
