package name.qd.arbitrage_digital_currencies.strategies.MAX;

public enum MaxQuote {
	TWD(10), USDT(0.5), BTC(0.00005), ETH(0.0005), TWDT(10);
	private double minValue;
	MaxQuote(double min) {
		this.minValue = min;
	}
	public double getMinValue() {
		return minValue;
	}
}
