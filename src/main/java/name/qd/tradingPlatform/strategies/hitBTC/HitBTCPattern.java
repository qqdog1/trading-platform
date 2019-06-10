package name.qd.tradingPlatform.strategies.hitBTC;

import java.util.HashMap;
import java.util.Map;

import name.qd.tradingPlatform.strategies.SingleExchangeCycleStrategy;
import name.qd.tradingPlatform.strategies.Strategy;
import name.qd.tradingPlatform.utils.StrategyUtils;
import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;

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
