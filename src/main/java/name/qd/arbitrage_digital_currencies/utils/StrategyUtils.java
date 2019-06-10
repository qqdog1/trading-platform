package name.qd.arbitrage_digital_currencies.utils;

import java.util.Map;

import name.qd.arbitrage_digital_currencies.Constants.ExchangeName;
import name.qd.arbitrage_digital_currencies.Constants.Product;
import name.qd.arbitrage_digital_currencies.exchanges.Exchange;
import name.qd.arbitrage_digital_currencies.exchanges.ExchangeManager;
import name.qd.arbitrage_digital_currencies.strategies.Strategy;

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
