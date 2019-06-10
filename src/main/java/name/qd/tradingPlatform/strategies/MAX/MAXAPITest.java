package name.qd.tradingPlatform.strategies.MAX;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.tradingPlatform.exchanges.Exchange;
import name.qd.tradingPlatform.exchanges.ExchangeManager;
import name.qd.tradingPlatform.strategies.Strategy;
import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;
import name.qd.tradingPlatform.Constants.Side;

public class MAXAPITest extends Strategy {
	private static Logger log = LoggerFactory.getLogger(MAXAPITest.class);
	private boolean isOrderSend = false;
	private Exchange max;
	private int orderId = 0;

	public MAXAPITest(Map<ExchangeName, Product[]> map) {
		super(map);
		Product[] p = {Product.ETH_BTC};
		map.put(ExchangeName.MAX, p);
		
		max = ExchangeManager.getInstance().getExchange(ExchangeName.MAX);
		List<String> lstProducts = max.getProducts();
		for(String product : lstProducts) {
			System.out.println(product);
		}
		
		queryBalance();
	}
	
	private void queryBalance() {
		Map<String, Double> mapBalance = max.queryBalance();
		for(String currency : mapBalance.keySet()) {
			System.out.println(currency + " : " + mapBalance.get(currency));
		}
	}

	@Override
	public void onBook(ExchangeName exchangeName, Product product, double bidPrice, double bidQty, double askPrice, double askQty) {
//		if(!isOrderSend) {
//			isOrderSend = true;
//			max.sendOrder(orderId, product, Side.buy, 0.00002, 100, this);
//			
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			
//			max.sendCancel(orderId, this);
//		}
	}
	
	@Override
	public void onTick() {
	}
	@Override
	public void onOrderAck(int orderId) {
		System.out.println("AAAAAAAAAAAAAAAC");
	}
	@Override
	public void onOrderRej(int orderId) {
	}
	@Override
	public void onCancelAck(int orderId) {
		System.out.println("CANNNNNNNNNNNNNNNNN");
	}
	@Override
	public void onCancelRej(int orderId) {
	}
	@Override
	public void onFill(int orderId, double price, double qty) {
	}

	@Override
	public boolean stop() {
		return true;
	}
}
