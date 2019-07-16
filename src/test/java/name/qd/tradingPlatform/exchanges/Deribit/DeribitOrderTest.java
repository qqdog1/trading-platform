package name.qd.tradingPlatform.exchanges.Deribit;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;
import name.qd.tradingPlatform.Constants.Side;
import name.qd.tradingPlatform.exchanges.Exchange;
import name.qd.tradingPlatform.exchanges.ExchangeManager;
import name.qd.tradingPlatform.exchanges.book.MarketBook;
import name.qd.tradingPlatform.strategies.Strategy;
import name.qd.tradingPlatform.utils.LazyLogSetting;
import name.qd.tradingPlatform.utils.StrategyUtils;

public class DeribitOrderTest extends Strategy {
	private Logger log;
	
	public static void main(String[] s) {
		LazyLogSetting.setTestLog();
		
		Map<ExchangeName, Product[]> map = new HashMap<>();
		map.put(ExchangeName.Deribit, new Product[] {Product.BTC_PERPETUAL});
		Strategy strategy = new DeribitOrderTest(map);
		StrategyUtils.start(strategy);
	}
	
	public DeribitOrderTest(Map<ExchangeName, Product[]> map) {
		super(map);
		log = LoggerFactory.getLogger(DeribitWebsocketTest.class);
		
		Exchange exchange = ExchangeManager.getInstance().getExchange(ExchangeName.Deribit);
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		exchange.sendOrder(0, Product.BTC_PERPETUAL, Side.buy, 1, 10, this);
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		exchange.sendOrder(1, Product.BTC_PERPETUAL, Side.sell, 1, 1, this);
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		exchange.sendCancel(0, this);
	}

	@Override
	public void onBook(ExchangeName exchangeName, Product product, double bidPrice, double bidQty, double askPrice,
			double askQty) {
	}

	@Override
	public void onBook(ExchangeName exchangeName, Product product, MarketBook marketBook) {
	}

	@Override
	public void onTick() {
	}

	@Override
	public void onOrderAck(int orderId) {
		log.info("order acked {}", orderId);
	}

	@Override
	public void onOrderRej(int orderId) {
		log.info("order rejected {}", orderId);
	}

	@Override
	public void onCancelAck(int orderId) {
		
	}

	@Override
	public void onCancelRej(int orderId) {
		
	}

	@Override
	public void onFill(int orderId, double price, double qty) {
		
	}

	@Override
	public boolean stop() {
		return false;
	}
}
