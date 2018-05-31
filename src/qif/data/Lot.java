package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Lot {
	public int secid;
	public BigDecimal originalShares;
	public BigDecimal remainingShares;
	public Date purchaseDate;
	public List<InvestmentTxn> relatedTransactions = new ArrayList<InvestmentTxn>();
}