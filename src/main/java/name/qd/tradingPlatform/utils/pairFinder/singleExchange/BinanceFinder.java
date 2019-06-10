package name.qd.tradingPlatform.utils.pairFinder.singleExchange;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.tradingPlatform.exchanges.Exchange;

public class BinanceFinder extends SingleExFinder {
	private Logger log = LoggerFactory.getLogger(BinanceFinder.class);
	private String[] BASE_CURRENCY = {"USDT", "ETH", "BTC", "BNB"};
	private Map<String, List<Double>> map;
	
	private double threshold = 1.01;
	
	public BinanceFinder(Exchange exchange) {
//		super.setThreshold(threshold);
		map = exchange.getInstantPrice();
		log.info("------>Binance<------");
		String[] products = new String[map.size()];
		map.keySet().toArray(products);
		if(map != null) findPair(products);
	}
	
	public String[] getPair(String product) {
		String[] ss = null;
		for(String s : BASE_CURRENCY) {
			if(product.endsWith(s)) {
				ss = new String[2];
				product = product.replace(s, "");
				ss[0] = product;
				ss[1] = s;
				break;
			}
		}
		return ss;
	}
	
	public List<Double> getPrice(String product) {
		return map.get(product);
	}
}
