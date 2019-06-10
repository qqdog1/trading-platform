package name.qd.tradingPlatform.strategies;

import java.util.Map;

import name.qd.tradingPlatform.exchanges.Exchange;
import name.qd.tradingPlatform.exchanges.ExchangeManager;
import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;

public class QueryBalance extends Strategy {
	
	public QueryBalance(Map<ExchangeName, Product[]> map) {
		super(map);
		Exchange exchange = null;
		for(ExchangeName exchangeName : map.keySet()) {
			exchange = ExchangeManager.getInstance().getExchange(exchangeName);
		}
		 Map<String, Double> mapBalance = exchange.queryBalance();
		 for(String key : mapBalance.keySet()) {
			 System.out.println(key + ":" + mapBalance.get(key));
		 }
	}

	@Override
	public void onBook(ExchangeName exchangeName, Product product, double bidPrice, double bidQty, double askPrice, double askQty) {
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
