package qif.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// This handles the case where we have multiple splits that involve
// transferring from another account. The other account may have a single
// entry that corresponds to more than one split in the other account.
// N.B. Alternatively, we could merge the splits into one.
class MultiSplitTxn extends SimpleTxn {
	public List<SimpleTxn> subsplits = new ArrayList<SimpleTxn>();

	public MultiSplitTxn(int domid, int acctid) {
		super(domid, acctid);
	}

	public MultiSplitTxn(int domid, MultiSplitTxn other) {
		super(domid, other);

		for (final SimpleTxn st : other.subsplits) {
			this.subsplits.add(new SimpleTxn(domid, st));
		}
	}

	public BigDecimal getCashAmount() {
		BigDecimal total = BigDecimal.ZERO;

		for (final SimpleTxn t : this.subsplits) {
			total = total.add(t.getAmount());
		}

		return total;
	}
}