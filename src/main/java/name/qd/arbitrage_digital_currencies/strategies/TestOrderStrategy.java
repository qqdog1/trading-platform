package name.qd.arbitrage_digital_currencies.strategies;

import java.util.Map;

import name.qd.arbitrage_digital_currencies.Constants.ExchangeName;
import name.qd.arbitrage_digital_currencies.Constants.Product;
import name.qd.arbitrage_digital_currencies.Constants.Side;
import name.qd.arbitrage_digital_currencies.exchanges.Exchange;
import name.qd.arbitrage_digital_currencies.exchanges.ExchangeManager;

public class TestOrderStrategy extends Strategy {
	private Exchange exchange;
	private Product[] products;
	
	public TestOrderStrategy(Map<ExchangeName, Product[]> map) {
		super(map);
		for(ExchangeName exchangeName : map.keySet()) {
			exchange = ExchangeManager.getInstance().getExchange(exchangeName);
			products = map.get(exchangeName);
		}
	}
	
	@Override
	public void onBook(ExchangeName exchangeName, Product product, double bidPrice, double bidQty, double askPrice, double askQty) {
		exchange.sendOrder(1, products[0], Side.buy, 0.001, 1, this);
	}

	@Override
	public void onTick() {
	}
	@Override
	public void onOrderAck(int orderId) {
	}
	@Override
	public void onOrderRej(int orderId) {
	}
	@Override
	public void onCancelAck(int orderId) {
	}
	@Override
	public void onCancelRej(int orderId) {
	}
	@Override
	public void onFill(int orderId, double price, double qty) {
	}

	@Override
	public boolean stop() {
		return true;
	}
}
