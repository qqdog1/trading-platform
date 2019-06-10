package name.qd.arbitrage_digital_currencies.exchanges;

import java.util.List;
import java.util.Map;

import name.qd.arbitrage_digital_currencies.Constants.ExchangeName;
import name.qd.arbitrage_digital_currencies.Constants.Product;
import name.qd.arbitrage_digital_currencies.Constants.Side;
import name.qd.arbitrage_digital_currencies.strategies.Strategy;

public interface Exchange {
	public ExchangeName getExchangeName();
	public List<String> getProducts();
	public Map<String, Double> getTickSize();
	public List<Double> getInstantPrice(String product);
	public Map<String, List<Double>> getInstantPrice();
	public void subscribe(Product[] products, Strategy strategy);
	public void unsubscribe(Strategy strategy);
	public void sendOrder(int orderId, Product product, Side side, double price, double qty, Strategy strategy);
	public void sendCancel(int orderId, Strategy strategy);
	public Map<String, Double> queryBalance();
}
