package name.qd.tradingPlatform.strategies.MAX;

import java.util.HashMap;
import java.util.Map;

import name.qd.tradingPlatform.strategies.SimpleMaker;
import name.qd.tradingPlatform.strategies.Strategy;
import name.qd.tradingPlatform.utils.StrategyUtils;
import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;
import name.qd.tradingPlatform.Constants.Side;

public class MAXMakers {
	private Product[][] p = {
							 {Product.ETH_TWD, Product.ETH_USDT, Product.USDT_TWD},
//							 {Product.BTC_TWD, Product.BTC_USDT, Product.USDT_TWD},
//							 {Product.MITH_TWD, Product.MITH_USDT, Product.USDT_TWD},
//							 {Product.EOS_TWD, Product.EOS_USDT, Product.USDT_TWD},
//							 {Product.EOS_ETH, Product.EOS_USDT, Product.ETH_USDT},
//							 {Product.EOS_ETH, Product.EOS_TWD, Product.ETH_TWD},
							};
	
	
	public MAXMakers() {
	}
	
	public void go() {
		for(Product[] products : p) {
			Map<ExchangeName, Product[]> map = new HashMap<>();
			map.put(ExchangeName.MAX, products);
			Strategy strategy1 = new SimpleMaker(map, products[0], Side.buy);
			StrategyUtils.start(strategy1);
			Strategy strategy2 = new SimpleMaker(map, products[0], Side.sell);
			StrategyUtils.start(strategy2);
		}
	}
}
