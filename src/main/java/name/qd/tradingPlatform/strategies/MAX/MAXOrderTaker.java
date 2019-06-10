package name.qd.tradingPlatform.strategies.MAX;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import name.qd.tradingPlatform.exchanges.Exchange;
import name.qd.tradingPlatform.exchanges.ExchangeManager;
import name.qd.tradingPlatform.strategies.Strategy;
import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;
import name.qd.tradingPlatform.Constants.Side;

public class MAXOrderTaker extends Strategy {
	private Product[] products = { Product.ETH_TWD };
	
	private double price = 0.6892;
	private double qty = 400;
	private Side side = Side.buy;
	private Exchange exchange = ExchangeManager.getInstance().getExchange(ExchangeName.MAX);
	private int orderId = 0;
	private int lastOrderId;
	private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

	public MAXOrderTaker(Map<ExchangeName, Product[]> map) {
		super(map);
		map.put(ExchangeName.MAX, products);
//		try {
//			Thread.sleep(1000);
//			sendOrder();
//			Thread.sleep(200);
//			sendCancel();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		System.exit(0);
//		executorService.scheduleAtFixedRate(new Checker(), 500, 500, TimeUnit.MILLISECONDS);
	}

	private class Checker implements Runnable {
		@Override
		public void run() {
			try {
				sendOrder();
//				Thread.sleep(150);
				sendCancel();
				Thread.sleep(1200);
			} catch (InterruptedException e) {
			}
		}
	}
	
	private void sendOrder() {
		lastOrderId = orderId;
		exchange.sendOrder(orderId++, products[0], side, price, qty, this);
	}
	
	private void sendCancel() {
		exchange.sendCancel(lastOrderId, this);
	}

	@Override
	public void onBook(ExchangeName exchangeName, Product product, double bidPrice, double bidQty, double askPrice, double askQty) {
		System.out.println(bidPrice);
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
		System.exit(0);
	}

	@Override
	public boolean stop() {
		return true;
	}
}
