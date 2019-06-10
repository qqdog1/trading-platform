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

public class MAXOrderTaker2 extends Strategy {
	private Product[] products = { Product.CCCX_ETH };
	
	private double price = 0.00001071;
	private double qty = 15000;
	private Side side = Side.buy;
	private Exchange exchange = ExchangeManager.getInstance().getExchange(ExchangeName.MAX);
	private int orderId = 20000;
	private int lastOrderId;
	private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

	public MAXOrderTaker2(Map<ExchangeName, Product[]> map) {
		super(map);
		map.put(ExchangeName.MAX, products);

		executorService.scheduleAtFixedRate(new Checker(), 500, 500, TimeUnit.MILLISECONDS);
	}

	private class Checker implements Runnable {
		@Override
		public void run() {
			try {
//				placeWall();
//				Thread.sleep(3000);
				sendOrder();
				Thread.sleep(250);
				sendCancel();
//				removeWall();
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
