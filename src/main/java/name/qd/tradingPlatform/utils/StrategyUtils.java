package name.qd.tradingPlatform.utils;

import java.util.Map;

import name.qd.tradingPlatform.exchanges.Exchange;
import name.qd.tradingPlatform.exchanges.ExchangeManager;
import name.qd.tradingPlatform.strategies.Strategy;
import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;

public class StrategyUtils {
	public static void start(Strategy strategy) {
		Map<ExchangeName, Product[]> map = strategy.getProducts();
		
		for(ExchangeName exchangeName : map.keySet()) {
			Exchange exchange = ExchangeManager.getInstance().getExchange(exchangeName);
			Product[] products = map.get(exchangeName);
			exchange.subscribe(products, strategy);
		}
	}
	
	public static void stop(Strategy strategy) {
		Map<ExchangeName, Product[]> map = strategy.getProducts();
		
		for(ExchangeName exchangeName : map.keySet()) {
			Exchange exchange = ExchangeManager.getInstance().getExchange(exchangeName);
			exchange.unsubscribe(strategy);
		}
	}
}
