package name.qd.arbitrage_digital_currencies.utils.pairFinder.singleExchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.arbitrage_digital_currencies.Constants.ExchangeName;
import name.qd.arbitrage_digital_currencies.utils.pairFinder.PairFinderSupportExchange;

public class SingleExchangePairFinder {
	private static Logger log = LoggerFactory.getLogger(SingleExchangePairFinder.class);
	
	private SingleExchangePairFinder() {
		log = LoggerFactory.getLogger(SingleExchangePairFinder.class);
		
		long timestamp = System.currentTimeMillis();
		for(ExchangeName exchangeName : ExchangeName.values()) {
			findPair(exchangeName);
		}
		
		timestamp = System.currentTimeMillis() - timestamp;
		log.info("FINISH {}", timestamp);
		System.exit(1);
	}
	
	private void findPair(ExchangeName exchangeName) {
		switch(exchangeName) {
		case Binance:
			new BinanceFinder(PairFinderSupportExchange.getInstance().getExchange(exchangeName));
			break;
		case HitBTC:
			new HitBTCFinder(PairFinderSupportExchange.getInstance().getExchange(exchangeName));
			break;
		case Poloniex:
			new PoloniexFinder(PairFinderSupportExchange.getInstance().getExchange(exchangeName));
			break;
		default:
			break;
		}
	}
	
	public static void main(String[] args) {
		new SingleExchangePairFinder();
	}
}
