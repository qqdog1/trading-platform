package name.qd.arbitrage_digital_currencies;

import java.util.HashMap;
import java.util.Map;

import name.qd.arbitrage_digital_currencies.Constants.ExchangeName;
import name.qd.arbitrage_digital_currencies.Constants.Product;
import name.qd.arbitrage_digital_currencies.Constants.Side;
import name.qd.arbitrage_digital_currencies.strategies.ControllPanel;
import name.qd.arbitrage_digital_currencies.strategies.SimpleMaker;
import name.qd.arbitrage_digital_currencies.strategies.Strategy;
import name.qd.arbitrage_digital_currencies.strategies.MAX.MAXAPITest;
import name.qd.arbitrage_digital_currencies.strategies.MAX.MAXAutoTrade;
import name.qd.arbitrage_digital_currencies.strategies.MAX.MAXMakers;
import name.qd.arbitrage_digital_currencies.strategies.MAX.MAXMarketMaker;
import name.qd.arbitrage_digital_currencies.strategies.MAX.MAXMarketMakerNew;
import name.qd.arbitrage_digital_currencies.strategies.MAX.MAXOrderTaker;
import name.qd.arbitrage_digital_currencies.strategies.MAX.MAXOrderTaker2;
import name.qd.arbitrage_digital_currencies.strategies.MAX.MAXSmartPairFindNotifier;
import name.qd.arbitrage_digital_currencies.strategies.MAX.MAXTWDT;
import name.qd.arbitrage_digital_currencies.utils.StrategyUtils;

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
		
		Map<ExchangeName, Product[]> map = new HashMap<>();
		Strategy strategy = new MAXSmartPairFindNotifier();
		StrategyUtils.start(strategy);
		
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
		
//		MAXMakers makers = new MAXMakers();
//		makers.go();
	}
	
	public static void main(String[] s) {
		new ArbitrageRunner();
	}
}
