package name.qd.tradingPlatform.strategies.binance;

import java.util.HashMap;
import java.util.Map;

import name.qd.tradingPlatform.exchanges.Exchange;
import name.qd.tradingPlatform.exchanges.ExchangeManager;
import name.qd.tradingPlatform.strategies.SingleExchangeCycleStrategy;
import name.qd.tradingPlatform.strategies.Strategy;
import name.qd.tradingPlatform.utils.StrategyUtils;
import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;

public class BinancePattern {
//	private Product[][] products = {
//									{Product.ETH_BTC, Product.BTC_USD, Product.ETH_USD},
//            					    {Product.BNB_ETH, Product.ETH_USD, Product.BNB_USD},
//            					    {Product.BCH_ETH, Product.ETH_USD, Product.BCH_USD},
//            					    {Product.NEO_ETH, Product.ETH_USD, Product.NEO_USD},
//            					    {Product.LTC_ETH, Product.ETH_USD, Product.LTC_USD},
//            					    {Product.BNB_BTC, Product.BTC_USD, Product.BNB_USD},
//            					    {Product.BCH_BTC, Product.BTC_USD, Product.BCH_USD},
//            					    {Product.NEO_BTC, Product.BTC_USD, Product.NEO_USD},
//            					    {Product.LTC_BTC, Product.BTC_USD, Product.LTC_USD},
//		   						   };
	private Product[][] products = {{Product.BNB_ETH, Product.ETH_USD, Product.BNB_USD}};
	
	public BinancePattern() {
		cyclePattern();
	}

	private void cyclePattern() {
		for(Product[] p : products) {
			Map<ExchangeName, Product[]> map = new HashMap<>();
			map.put(ExchangeName.Binance, p);
			Strategy s = new SingleExchangeCycleStrategy(map);
			StrategyUtils.start(s);
		}
	}
}
