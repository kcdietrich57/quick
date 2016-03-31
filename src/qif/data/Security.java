package qif.data;

import java.util.ArrayList;
import java.util.List;

public class Security {
	public short id;
	public String name;
	public String symbol;
	public String type;
	public String goal;

	List<Price> prices = new ArrayList<Price>();

	public Security() {
		this.id = 0;

		this.name = "";
		this.symbol = "";
		this.type = "";
		this.goal = "";
	}

	public Security(Security other) {
		this.id = other.id;
		this.name = other.name;
		this.symbol = other.symbol;
		this.type = other.type;
		this.goal = other.goal;
	}

	public void addPrice(Price price) {
		this.prices.add(price);
	}

	public String toString() {
		String s = "Security[" + this.id + "]: " + this.name //
				+ " sym=" + this.symbol //
				+ " type=" + this.type //
				+ " numprices=" + this.prices.size() //
				+ "\n";

		return s;
	}
};
