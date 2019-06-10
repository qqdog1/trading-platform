package name.qd.tradingPlatform.utils.pairFinder;

import java.util.ArrayList;
import java.util.List;

import name.qd.tradingPlatform.exchanges.Exchange;
import name.qd.tradingPlatform.exchanges.ExchangeManager;
import name.qd.tradingPlatform.Constants.ExchangeName;

public class PairFinderSupportExchange {
	private static PairFinderSupportExchange instance = new PairFinderSupportExchange();
	
	private ExchangeManager exchangeManager = ExchangeManager.getInstance();
	
	private List<ExchangeName> lst = new ArrayList<>();
	
	public static PairFinderSupportExchange getInstance() {
		return instance;
	}
	
	private PairFinderSupportExchange() {
		lst.add(ExchangeName.Binance);
		lst.add(ExchangeName.Bitfinex);
		lst.add(ExchangeName.HitBTC);
//		lst.add(ExchangeName.Huobi);
		lst.add(ExchangeName.KuCoin);
		lst.add(ExchangeName.ZB);
		lst.add(ExchangeName.Kraken);
		lst.add(ExchangeName.OKEx);
		lst.add(ExchangeName.Poloniex);
	}
	
	public Exchange getExchange(ExchangeName exchangeName) {
		if(!lst.contains(exchangeName)) return null;
		return exchangeManager.getExchange(exchangeName);
	}
}
