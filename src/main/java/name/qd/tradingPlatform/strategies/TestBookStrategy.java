package name.qd.tradingPlatform.strategies;

import java.util.Map;

import name.qd.tradingPlatform.exchanges.Exchange;
import name.qd.tradingPlatform.exchanges.ExchangeManager;
import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;
import name.qd.tradingPlatform.Constants.Side;

public class TestBookStrategy extends Strategy {
	private Exchange exchange;
	private int orderId = 0;
	
	public TestBookStrategy(Map<ExchangeName, Product[]> map) {
		super(map);
		for(ExchangeName exchangeName : map.keySet()) {
			exchange = ExchangeManager.getInstance().getExchange(exchangeName);
		}
		exchange.queryBalance();
	}

	@Override
	public void onBook(ExchangeName exchangeName, Product product, double bidPrice, double bidQty, double askPrice,	double askQty) {
		System.out.println("onBook");
		System.out.println(product + " " + bidPrice + " " + bidQty + " " + askPrice + " " + askQty);
		if(orderId == 0) {
			System.out.println("send order");
			exchange.sendOrder(++orderId, product, Side.sell, bidPrice, 0.01, this);
		}
	}

	@Override
	public void onTick() {
	}
	@Override
	public void onOrderAck(int orderId) {
		System.out.println("Order ack");
	}
	@Override
	public void onOrderRej(int orderId) {
		System.out.println("Order rej");
	}
	@Override
	public void onCancelAck(int orderId) {
		System.out.println("on Cancel");
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
