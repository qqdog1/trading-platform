package name.qd.tradingPlatform.exchanges.Kraken;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.tradingPlatform.product.FileProductMapperManager;
import name.qd.tradingPlatform.strategies.Strategy;

public class KrakenMarketGrabber implements Runnable {
	private static Logger log = LoggerFactory.getLogger(KrakenMarketGrabber.class);
	private Set<String> subscribeProducts;
	private Map<String, List<Strategy>> mapStrategies;
	private KrakenExchange exchange;
	private FileProductMapperManager productMapper;

	public KrakenMarketGrabber(KrakenExchange exchange, Set<String> subscribeProducts, Map<String, List<Strategy>> mapStrategies, FileProductMapperManager productMapper) {
		this.exchange = exchange;
		this.subscribeProducts = subscribeProducts;
		this.mapStrategies = mapStrategies;
		this.productMapper = productMapper;
	}
	
	@Override
	public void run() {
		while(!Thread.currentThread().isInterrupted()) {
			for(String product : subscribeProducts) {
				List<Double> lst = exchange.getMarketData(product);
				List<Strategy> lstStrategies = mapStrategies.get(product);
				for(Strategy strategy : lstStrategies) {
					strategy.onBook(exchange.getExchangeName(), productMapper.getProduct(product, exchange.getExchangeName()), lst.get(0), lst.get(1), lst.get(2), lst.get(3));
				}
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				log.error("", e);
			}
		}
	}
	
}
