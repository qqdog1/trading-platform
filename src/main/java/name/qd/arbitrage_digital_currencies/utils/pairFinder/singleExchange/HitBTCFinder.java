package name.qd.arbitrage_digital_currencies.utils.pairFinder.singleExchange;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.arbitrage_digital_currencies.exchanges.Exchange;

public class HitBTCFinder extends SingleExFinder {
	private Logger log = LoggerFactory.getLogger(HitBTCFinder.class);
	private String[] BASE_CURRENCY = {"USD", "ETH", "BTC"};
	private Exchange exchange;

	private double threshold = 1.01;
	
	public HitBTCFinder(Exchange exchange) {
		this.exchange = exchange;
		List<String> lst = exchange.getProducts();
		String[] products = new String[lst.size()];
		lst.toArray(products);
		log.info("------>HitBTC<------");
		findPair(products);
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
		return exchange.getInstantPrice(product);
	}
}
