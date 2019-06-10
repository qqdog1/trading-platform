package name.qd.arbitrage_digital_currencies.strategies;

import java.util.Map;

import name.qd.arbitrage_digital_currencies.Constants.ExchangeName;
import name.qd.arbitrage_digital_currencies.Constants.Product;

public abstract class Strategy {
	private Map<ExchangeName, Product[]> map;
	
	public Strategy(Map<ExchangeName, Product[]> map) {
		this.map = map;
	}
	
	public Map<ExchangeName, Product[]> getProducts() {
		return map;
	}
	
	public abstract void onBook(ExchangeName exchangeName, Product product, double bidPrice, double bidQty, double askPrice, double askQty);
	public abstract void onTick();
	public abstract void onOrderAck(int orderId);
	public abstract void onOrderRej(int orderId);
	public abstract void onCancelAck(int orderId);
	public abstract void onCancelRej(int orderId);
	public abstract void onFill(int orderId, double price, double qty);
	public abstract boolean stop();
}
