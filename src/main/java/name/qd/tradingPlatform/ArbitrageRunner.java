package name.qd.tradingPlatform;

import java.util.HashMap;
import java.util.Map;

import name.qd.tradingPlatform.strategies.ControllPanel;
import name.qd.tradingPlatform.strategies.SimpleMaker;
import name.qd.tradingPlatform.strategies.Strategy;
import name.qd.tradingPlatform.strategies.TestBookStrategy;
import name.qd.tradingPlatform.strategies.MAX.MAXAPITest;
import name.qd.tradingPlatform.strategies.MAX.MAXAutoTrade;
import name.qd.tradingPlatform.strategies.MAX.MAXMakers;
import name.qd.tradingPlatform.strategies.MAX.MAXMarketMaker;
import name.qd.tradingPlatform.strategies.MAX.MAXMarketMakerNew;
import name.qd.tradingPlatform.strategies.MAX.MAXOrderTaker;
import name.qd.tradingPlatform.strategies.MAX.MAXOrderTaker2;
import name.qd.tradingPlatform.strategies.MAX.MAXSmartPairFindNotifier;
import name.qd.tradingPlatform.strategies.MAX.MAXTWDT;
import name.qd.tradingPlatform.utils.StrategyUtils;
import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;
import name.qd.tradingPlatform.Constants.Side;

public class ArbitrageRunner {
	
	
	private ArbitrageRunner() {
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
		Product[] p = new Product[] {Product.BTC_USD, Product.ETH_USD};
		map.put(ExchangeName.BTSE, p);
		Strategy strategy = new TestBookStrategy(map);
		StrategyUtils.start(strategy);
		
//		MAXMakers makers = new MAXMakers();
//		makers.go();
	}
	
	public static void main(String[] s) {
		new ArbitrageRunner();
	}
}
