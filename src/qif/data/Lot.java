package qif.data;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Lot {
	public int secid;
	public BigInteger originalShares;
	public BigInteger remainingShares;
	public Date purchaseDate;
	public List<InvestmentTxn> relatedTransactions = new ArrayList<InvestmentTxn>();
}