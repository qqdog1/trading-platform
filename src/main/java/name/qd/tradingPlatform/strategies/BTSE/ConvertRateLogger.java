package name.qd.tradingPlatform.strategies.BTSE;

import java.text.DecimalFormat;
import java.util.Map;
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
	
	private DecimalFormat df = new DecimalFormat("########.##########");
	private double rate1;
	private double rate2;
	private double bid;
	private double ask;

	public ConvertRateLogger(Map<ExchangeName, Product[]> map) {
		super(map);
		
		Exchange exchange = ExchangeManager.getInstance().getExchange(ExchangeName.BTSE);
		
		Runnable task1 = () -> {
			rate1 = (Double) exchange.customizeAction(BTSECustomizeAction.QUERY_CONVERT_RATE.name(), "BTC", "USD");
			log.info("rate: {}", df.format(rate1));
        };
        
        Runnable task2 = () -> {
			rate2 = (Double) exchange.customizeAction(BTSECustomizeAction.QUERY_CONVERT_RATE.name(), "USD", "BTC");
			log.info("rate: {}", df.format(rate2));
        };

        scheduledExecutorService.scheduleAtFixedRate(task1, 0, 1, TimeUnit.SECONDS);
        scheduledExecutorService.scheduleAtFixedRate(task2, 0, 1, TimeUnit.SECONDS);
	}

	@Override
	public void onBook(ExchangeName exchangeName, Product product, double bidPrice, double bidQty, double askPrice, double askQty) {
		bid = bidPrice;
		ask = askPrice;
		
		log.info("price update, bid:{}. ask:{}", bid, ask);
		
		// 1 BTC -> * bidPrice = USD, 
		// USD * rate2 = BTC
		if((bid * rate2) > 1d) {
			log.info("bidPrice:{}, convert rate:{}", bid, rate2);
		} else {
			log.info("bid * rate2 = {}", bid*rate2);
		}
		
		// 100USD -> / askPrice = BTC
		// BTC * rate1 = USD
		if(1/ask * rate1 > 1d) {
			log.info("askPrice:{}, convert rate:{}", ask, rate1);
		} else {
			log.info("1/ask*rate1 = {}", 1/ask * rate1);
		}
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
