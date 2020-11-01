package name.qd.tradingPlatform.exchanges;

import java.util.List;
import java.util.Map;

import name.qd.tradingPlatform.strategies.Strategy;
import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;
import name.qd.tradingPlatform.Constants.Side;

public abstract class Exchange {
	public abstract ExchangeName getExchangeName();
	public abstract List<String> getProducts();
	public abstract Map<String, Double> getTickSize();
	public abstract List<Double> getInstantPrice(String product);
	public abstract void sendOrder(int orderId, Product product, Side side, double price, double qty, Strategy strategy);
	public abstract void sendCancel(int orderId, Strategy strategy);
	public abstract Map<String, Double> queryBalance();
	
	public void subscribe(Product[] products, Strategy strategy) {
	}
	
	public void unsubscribe(Strategy strategy) {
	}
	
	public Map<String, List<Double>> getInstantPrice() {
		return null;
	}
	
	public Object customizeAction(Object ... objects) {
		return null;
	}
}
