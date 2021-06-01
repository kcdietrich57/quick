package moneymgr.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This multi-part split handles a special case for transfers from another
 * account where the single value in the other account connects to several
 * splits combined in this transaction.<br>
 * E.g. ThisTx [split1: 1.00, split2: 2.00] <--> OtherTx [split: 3.00]
 */
public class MultiSplitTxn extends SplitTxn {
	/** Group of splits in this txn that connect to the other txn */
	private final List<SplitTxn> subsplits = new ArrayList<>();

	public MultiSplitTxn(int txid, SimpleTxn parent) {
		super(txid, parent);

		setAmount(BigDecimal.ZERO);
	}

	public MultiSplitTxn(SimpleTxn parent) {
		this(0, parent);
	}

	public boolean hasSplits() {
		return true;
	}

	public void addSplit(SplitTxn txn) {
		if (this.subsplits.contains(txn)) {
			return;
		}

		this.subsplits.add(txn);
		txn.setParent(this);

		if (getAmount() != null && txn.getAmount() != null) {
			setAmount(getAmount().add(txn.getAmount()));
		} else {
			// TODO what?
		}
	}

	public List<SplitTxn> getSplits() {
		return Collections.unmodifiableList(this.subsplits);
	}

	public String formatValue() {
		return super.formatValue();
	}
}