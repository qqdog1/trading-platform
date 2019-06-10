package name.qd.arbitrage_digital_currencies.strategies;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.arbitrage_digital_currencies.Constants.ExchangeName;
import name.qd.arbitrage_digital_currencies.Constants.Product;

public class MultiExchangeStrategy extends Strategy {
	private static final Logger log = LoggerFactory.getLogger(MultiExchangeStrategy.class);
	private ExchangeName[] exchanges = new ExchangeName[2];
	private double bPrice1;
	private double bQty1;
	private double aPrice1;
	private double aQty1;
	private double bPrice2;
	private double bQty2;
	private double aPrice2;
	private double aQty2;

	private double threshold = 1.01;

	public MultiExchangeStrategy(Map<ExchangeName, Product[]> map) {
		super(map);
		map.keySet().toArray(exchanges);
	}

	@Override
	public void onBook(ExchangeName exchangeName, Product product, double bidPrice, double bidQty, double askPrice,
			double askQty) {
		if (exchanges[0] == exchangeName) {
			bPrice1 = bidPrice;
			bQty1 = bidQty;
			aPrice1 = askPrice;
			aQty1 = askQty;
		} else if (exchanges[1] == exchangeName) {
			bPrice2 = bidPrice;
			bQty2 = bidQty;
			aPrice2 = askPrice;
			aQty2 = askQty;
		} else if (exchanges[0] == null) {
			exchanges[0] = exchangeName;
			bPrice1 = bidPrice;
			bQty1 = bidQty;
			aPrice1 = askPrice;
			aQty1 = askQty;
		} else if (exchanges[1] == null) {
			exchanges[1] = exchangeName;
			bPrice2 = bidPrice;
			bQty2 = bidQty;
			aPrice2 = askPrice;
			aQty2 = askQty;
		}

		calcCost();
	}

	private void calcCost() {
		double bCost = bPrice1;
		double aCost = aPrice2;
		double benefit = aCost / bCost;
		if (bCost < aCost && bCost != 0) {
			// if(benefit > threshold) {
			log.info("{}:{} {}:{} :{}", exchanges[0], bPrice1, exchanges[1], aPrice2, benefit);
			// }
		}

		bCost = bPrice2;
		aCost = aPrice1;
		benefit = aCost / bCost;
		if (bCost < aCost && bCost != 0) {
			// if(benefit > threshold) {
			log.info("{}:{} {}:{} :{}", exchanges[1], bPrice2, exchanges[0], aPrice1, benefit);
			// }
		}
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
		return true;
	}
}
