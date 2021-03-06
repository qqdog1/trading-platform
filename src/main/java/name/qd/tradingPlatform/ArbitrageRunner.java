package name.qd.tradingPlatform;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;
import name.qd.tradingPlatform.strategies.Strategy;
import name.qd.tradingPlatform.strategies.TestBookStrategy;
import name.qd.tradingPlatform.strategies.BTSE.ConvertRateLogger;
import name.qd.tradingPlatform.utils.StrategyUtils;

public class ArbitrageRunner {
	
	private ArbitrageRunner() {
		Properties prop = System.getProperties();
		prop.setProperty("log4j.configurationFile", "./config/log4j2.xml");
		
//		ControllPanel controllPanel = new ControllPanel();
//		controllPanel.addStrategy(MAXAutoTrade.class);
//		controllPanel.addStrategy(MAXSmartPairFindNotifier.class);
		
//		Map<ExchangeName, Product[]> map = new HashMap<>();
//		Strategy strategy = new MAXAPITest(map);
//		StrategyUtils.start(strategy);
		
//		Map<ExchangeName, Product[]> map = new HashMap<>();
//		Strategy strategy = new MAXAutoTrade();
//		StrategyUtils.start(strategy);
		
//		Map<ExchangeName, Product[]> map = new HashMap<>();
//		Strategy strategy = new MAXSmartPairFindNotifier();
//		StrategyUtils.start(strategy);
		
//		Map<ExchangeName, Product[]> map2 = new HashMap<>();
//		Strategy strategy2 = new MAXTWDT(map2);
//		StrategyUtils.start(strategy2);
		
//		Map<ExchangeName, Product[]> map = new HashMap<>();
//		Strategy strategy = new MAXOrderTaker(map);
//		StrategyUtils.start(strategy);
//		
//		Map<ExchangeName, Product[]> map2 = new HashMap<>();
//		Strategy strategy2 = new MAXOrderTaker2(map2);
//		StrategyUtils.start(strategy2);
		
//		Map<ExchangeName, Product[]> mapBito = new HashMap<>();
//		Strategy strategyBito = new BitoSmartNotifier(mapBito);
//		StrategyUtils.start(strategyBito);
		
//		Map<ExchangeName, Product[]> map = new HashMap<>();
//		Strategy strategy = new MAXMarketMakerNew();
//		StrategyUtils.start(strategy);
		
		Map<ExchangeName, Product[]> map = new HashMap<>();
		Product[] p = new Product[] {Product.BTC_USD};
		map.put(ExchangeName.BTSE, p);
		Strategy strategy = new ConvertRateLogger(map);
		StrategyUtils.start(strategy);
		
//		MAXMakers makers = new MAXMakers();
//		makers.go();
	}
	
	public static void main(String[] s) {
		new ArbitrageRunner();
	}
}
