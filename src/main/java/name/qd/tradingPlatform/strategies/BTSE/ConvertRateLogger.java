package name.qd.tradingPlatform.strategies.BTSE;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;
import name.qd.tradingPlatform.exchanges.Exchange;
import name.qd.tradingPlatform.exchanges.ExchangeManager;
import name.qd.tradingPlatform.exchanges.BTSE.BTSECustomizeAction;
import name.qd.tradingPlatform.exchanges.book.MarketBook;
import name.qd.tradingPlatform.strategies.Strategy;

public class ConvertRateLogger extends Strategy {
	private static Logger log = LoggerFactory.getLogger(ConvertRateLogger.class);
	private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

	public ConvertRateLogger(Map<ExchangeName, Product[]> map) {
		super(map);
		
		Exchange exchange = ExchangeManager.getInstance().getExchange(ExchangeName.BTSE);
		
		Runnable task1 = () -> {
			Object rate = exchange.customizeAction(BTSECustomizeAction.QUERY_CONVERT_RATE.name(), "BTC", "ETH");
			log.info("rate: {}", (Double) rate);
        };

        scheduledExecutorService.scheduleAtFixedRate(task1, 0, 10, TimeUnit.MINUTES);
	}

	@Override
	public void onBook(ExchangeName exchangeName, Product product, double bidPrice, double bidQty, double askPrice, double askQty) {
		System.out.println(product + ", Buy: " + bidQty + "@" + bidPrice + ", Sell: " + askQty + "@" + askPrice);
	}

	@Override
	public void onBook(ExchangeName exchangeName, Product product, MarketBook marketBook) {
		
	}

	@Override
	public void onTick() {
		
	}

	@Override
	public void onOrderAck(int orderId) {
		
	}

	@Override
	public void onOrderRej(int orderId) {
		
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
