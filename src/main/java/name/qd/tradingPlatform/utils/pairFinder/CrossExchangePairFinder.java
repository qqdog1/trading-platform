package name.qd.tradingPlatform.utils.pairFinder;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.tradingPlatform.exchanges.Exchange;
import name.qd.tradingPlatform.utils.ProductUtils;
import name.qd.tradingPlatform.Constants.ExchangeName;

public class CrossExchangePairFinder {
	private static Logger log = LoggerFactory.getLogger(CrossExchangePairFinder.class);
	
	private double BENEFIT_RATE = 1.00d;
	private long timestamp;

	private CrossExchangePairFinder() {
		
		timestamp = System.currentTimeMillis();
		ExchangeName[] exchangeNames = ExchangeName.values();
		for (int i = 0; i < exchangeNames.length - 1; i++) {
			for (int j = i + 1; j < exchangeNames.length; j++) {
				Exchange exchange1 = PairFinderSupportExchange.getInstance().getExchange(exchangeNames[i]);
				Exchange exchange2 = PairFinderSupportExchange.getInstance().getExchange(exchangeNames[j]);

				if(exchange1 == null || exchange2 == null) continue;
				log.info("{} <-> {}", exchange1.getExchangeName(), exchange2.getExchangeName());
				findSameProduct(exchange1, exchange2);
			}
		}
		timestamp = System.currentTimeMillis() - timestamp;
		log.info("Done. {}", timestamp);
		System.exit(1);
	}

	private void findSameProduct(Exchange exchange1, Exchange exchange2) {
		List<String> lst1 = exchange1.getProducts();
		List<String> lst2 = exchange2.getProducts();

		if (lst1 == null || lst2 == null) return;
		
		for (String product1 : lst1) {
			for (String product2 : lst2) {
				String p1 = ProductUtils.getProductString(exchange1.getExchangeName(), product1);
				String p2 = ProductUtils.getProductString(exchange2.getExchangeName(), product2);
				if (p1.equals(p2)) {
					checkPrice(exchange1, product1, exchange2, product2);
				}
			}
		}
	}

	private void checkPrice(Exchange exchange1, String product1, Exchange exchange2, String product2) {
		List<Double> lstPrice1 = exchange1.getInstantPrice(product1);
		List<Double> lstPrice2 = exchange2.getInstantPrice(product2);

		if (lstPrice1.size() < 2 || lstPrice2.size() < 2)	return;

		double benefit = lstPrice1.get(0) / lstPrice2.get(1);
		if (benefit > BENEFIT_RATE) {
			log.info("{}:{}:{}:S , {}:{}:{}:B , {}", exchange1.getExchangeName(), product1, lstPrice1.get(0), exchange2.getExchangeName(), product2, lstPrice2.get(1), benefit);
		}
		benefit = lstPrice2.get(0) / lstPrice1.get(1);
		if (benefit > BENEFIT_RATE) {
			log.info("{}:{}:{}:B , {}:{}:{}:S , {}", exchange1.getExchangeName(), product1, lstPrice1.get(1), exchange2.getExchangeName(), product2, lstPrice2.get(0), benefit);
		}
	}

	public static void main(String[] s) {
		new CrossExchangePairFinder();
	}
}
