package name.qd.tradingPlatform.exchanges.BTSE;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.exchanges.Exchange;
import name.qd.tradingPlatform.exchanges.ExchangeManager;

public class BTSEExchangeTest {
	private Exchange btseExchange;

	@Before
	public void init() {
		btseExchange = ExchangeManager.getInstance().getExchange(ExchangeName.BTSE);
	}
	
	@Test
	public void getProducts() {
		List<String> lst = btseExchange.getProducts();
		for(String product : lst) {
			System.out.println(product);
		}
	}
	
	@Test
	public void getTickSize() {
		Map<String, Double> map = btseExchange.getTickSize();
		for(String product : map.keySet()) {
			System.out.println(product + ":" + map.get(product));
		}
	}
}
