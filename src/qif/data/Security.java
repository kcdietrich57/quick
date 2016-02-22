package qif.data;

import java.util.ArrayList;
import java.util.List;

public class Security {
	public final short id;

	public String name;
	public String symbol;
	public String type;
	public String goal;

	List<Price> prices = new ArrayList<Price>();

	public Security(short id) {
		this.id = id;

		this.name = "";
		this.symbol = "";
		this.type = "";
		this.goal = "";
	}

	public void addPrice(Price price) {
		this.prices.add(price);
	}

	public String toString() {
		String s = "Security" + this.id + ": " + this.name //
				+ " sym=" + this.symbol //
				+ " type=" + this.type //
				+ " numprices=" + this.prices.size() //
				+ "\n";

		return s;
	}
};
