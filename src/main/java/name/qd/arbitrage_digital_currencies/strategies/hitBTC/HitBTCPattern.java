package name.qd.arbitrage_digital_currencies.strategies.hitBTC;

import java.util.HashMap;
import java.util.Map;

import name.qd.arbitrage_digital_currencies.Constants.ExchangeName;
import name.qd.arbitrage_digital_currencies.Constants.Product;
import name.qd.arbitrage_digital_currencies.strategies.SingleExchangeCycleStrategy;
import name.qd.arbitrage_digital_currencies.strategies.Strategy;
import name.qd.arbitrage_digital_currencies.utils.StrategyUtils;

public class HitBTCPattern {
	private Product[][] products = {{Product.BCH_ETH, Product.ETH_BTC, Product.BCH_BTC}};
	
	public HitBTCPattern() {
		cyclePattern();
	}

	private void cyclePattern() {
		for(Product[] p : products) {
			Map<ExchangeName, Product[]> map = new HashMap<>();
			map.put(ExchangeName.HitBTC, p);
			Strategy s = new SingleExchangeCycleStrategy(map);
			StrategyUtils.start(s);
		}
	}
}
