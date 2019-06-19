package moneymgr.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * This multi-part split handles a special case for transfers from another
 * account where the other account connects to several splits combined in the
 * other transaction.
 */
public class MultiSplitTxn extends SplitTxn {
	/** Group of splits in this txn that connect to the other txn */
	public List<SplitTxn> subsplits = new ArrayList<>();

	public MultiSplitTxn(SimpleTxn parent) {
		super(parent);
	}

	public boolean hasSplits() {
		return true;
	}

	public List<SplitTxn> getSplits() {
		return this.subsplits;
	}

	public BigDecimal getCashAmount() {
		BigDecimal total = BigDecimal.ZERO;

		for (SimpleTxn t : this.subsplits) {
			total = total.add(t.getAmount());
		}

		return total;
	}

	public String formatValue() {
		return super.formatValue();
	}
}