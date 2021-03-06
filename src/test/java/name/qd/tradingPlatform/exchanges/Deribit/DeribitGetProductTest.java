package name.qd.tradingPlatform.exchanges.Deribit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;
import name.qd.tradingPlatform.exchanges.Exchange;
import name.qd.tradingPlatform.exchanges.ExchangeManager;
import name.qd.tradingPlatform.exchanges.book.MarketBook;
import name.qd.tradingPlatform.strategies.Strategy;
import name.qd.tradingPlatform.utils.LazyLogSetting;
import name.qd.tradingPlatform.utils.StrategyUtils;

public class DeribitGetProductTest extends Strategy {
	private Logger log;
	
	public static void main(String[] s) {
		LazyLogSetting.setTestLog();
		
		Map<ExchangeName, Product[]> map = new HashMap<>();
		map.put(ExchangeName.Deribit, new Product[] {Product.BTC_PERPETUAL});
		Strategy strategy = new DeribitGetProductTest(map);
		StrategyUtils.start(strategy);
	}
	
	public DeribitGetProductTest(Map<ExchangeName, Product[]> map) {
		super(map);
		log = LoggerFactory.getLogger(DeribitWebsocketTest.class);
	
		Exchange exchange = ExchangeManager.getInstance().getExchange(ExchangeName.Deribit);
		List<String> lst = exchange.getProducts();
		for(String product : lst) {
			System.out.println(product);
		}
	}

	@Override
	public void onBook(ExchangeName exchangeName, Product product, double bidPrice, double bidQty, double askPrice,
			double askQty) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onBook(ExchangeName exchangeName, Product product, MarketBook marketBook) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTick() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOrderAck(int orderId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOrderRej(int orderId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCancelAck(int orderId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCancelRej(int orderId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onFill(int orderId, double price, double qty) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean stop() {
		// TODO Auto-generated method stub
		return false;
	}

}
