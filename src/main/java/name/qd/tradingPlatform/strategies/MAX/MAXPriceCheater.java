package name.qd.tradingPlatform.strategies.MAX;

import java.util.Map;

import name.qd.tradingPlatform.strategies.Strategy;
import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;
import name.qd.tradingPlatform.exchanges.book.MarketBook;

public class MAXPriceCheater extends Strategy {

	public MAXPriceCheater(Map<ExchangeName, Product[]> map) {
		super(map);
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
		
	}

	@Override
	public boolean stop() {
		return false;
	}

	@Override
	public void onBook(ExchangeName exchangeName, Product product, MarketBook marketBook) {
		// TODO Auto-generated method stub
		
	}
}
