package name.qd.tradingPlatform.strategies;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.tradingPlatform.exchanges.Exchange;
import name.qd.tradingPlatform.exchanges.ExchangeManager;
import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;
import name.qd.tradingPlatform.Constants.Side;

public class SingleExchangeCycleStrategy extends Strategy {
	private static final Logger log = LoggerFactory.getLogger(SingleExchangeCycleStrategy.class);
	private Exchange exchange;
//	private Product[] products = new Product[]{Product.BCH_ETH, Product.ETH_BTC, Product.BCH_BTC};
	
	private Product abProduct;
	private Product bcProduct;
	private Product acProduct;
	private double abBPrice = 0;
	private double abAPrice = 0;
	private double abBQty = 0;
	private double abAQty = 0;
	private double bcBPrice = 0;
	private double bcAPrice = 0;
	private double bcBQty = 0;
	private double bcAQty = 0;
	private double acBPrice = 0;
	private double acAPrice = 0;
	private double acBQty = 0;
	private double acAQty = 0;
	
	private final int STATUS_INIT = 0;
	private final int STATUS_STANDBY = 1;
	private final int STATUS_TRADING = 2;
	private final int STATUS_STOP = 3;
	private AtomicInteger status = new AtomicInteger(STATUS_INIT);
	
	private Map<Integer, Order> mapOrderIdOrder = new HashMap<>();
	private Order abOrder;
	private Order bcOrder;
	private Order acOrder;
	private DecimalFormat df = new DecimalFormat("#.####");
	
//	private double cQty = 50;
	private double aQty = 2;
	private double marketVolume = 0.7;
	
	private int orderId = 0;
	
	private Map<String, Double> mapBalance;
	private double threshold = 1.012;
	
	public SingleExchangeCycleStrategy(Map<ExchangeName, Product[]> map) {
		super(map);
		for(ExchangeName exchangeName : map.keySet()) {
			exchange = ExchangeManager.getInstance().getExchange(exchangeName);
//			products = map.get(exchangeName);
		}
		mapBalance = exchange.queryBalance();
		printBalance();
		
		status.set(STATUS_STANDBY);
	}
	
	@Override
	public void onBook(ExchangeName exchangeName, Product product, double bidPrice, double bidQty, double askPrice, double askQty) {
		if(product == abProduct) {
			abBPrice = bidPrice;
			abAPrice = askPrice;
			abBQty = bidQty;
			abAQty = askQty;
		} else if(product == bcProduct) {
			bcBPrice = bidPrice;
			bcAPrice = askPrice;
			bcBQty = bidQty;
			bcAQty = askQty;
		} else if(product == acProduct) {
			acBPrice = bidPrice;
			acAPrice = askPrice;
			acBQty = bidQty;
			acAQty = askQty;
		}
		
		if(status.get() == STATUS_STANDBY) {
			calcCost();
		}
	}
	
	private void calcCost() {
		double ratio = (acAPrice / bcBPrice) * 1.001 * 1.001 * 1.001;
		double ab = abBPrice;
		if(ab > ratio) {
			double benefit = ab / ratio;
			if(benefit > threshold && benefit != Double.POSITIVE_INFINITY) {
				startTrade(true);
				log.info("{} {} {}, {} {} {} :{}", abProduct, bcProduct, acProduct, abBPrice, bcBPrice, acAPrice, benefit);
			}
		}
		
		ratio = acBPrice / bcAPrice;
		double rate = abAPrice * 1.001 * 1.001 * 1.001;
		if(ratio > rate) {
			double benefit = ratio / abAPrice;
			if(benefit > threshold && benefit != Double.POSITIVE_INFINITY) {
				startTrade(false);
				log.info("{} {} {}, {} {} {} :{}", abProduct, bcProduct, acProduct, abAPrice, bcAPrice, acBPrice, benefit);
			}
		}
	}
	
	private void startTrade(boolean clockwise) {
		if(status.get() != STATUS_TRADING) {
			status.set(STATUS_TRADING);
		} else {
			return;
		}
		
		if(clockwise) {
			double bQty = aQty * abBPrice;
			bQty = Double.valueOf(df.format(bQty));
			
			if(acAQty * marketVolume < aQty || abBQty * marketVolume < aQty || bcBQty * marketVolume < bQty) {
				status.set(STATUS_STANDBY);
				log.info("market volume not enough. {} {} {}", abBQty, bcBQty, acAQty);
				return;
			}
			
			sendOrder(acProduct, Side.buy, acAPrice, aQty, acOrder);
			sendOrder(abProduct, Side.sell, abBPrice, aQty, abOrder);
			sendOrder(bcProduct, Side.sell, bcBPrice, bQty, bcOrder);
		} else {
			double bQty = aQty * abAPrice;
			bQty = Double.valueOf(df.format(bQty));
			
			if(acBQty * marketVolume < aQty || abAQty * marketVolume < aQty || bcAQty * marketVolume < bQty) {
				status.set(STATUS_STANDBY);
				log.info("market volume not enough. {} {} {}", abAQty, bcAQty, acBQty);
				return;
			}
			
			sendOrder(bcProduct, Side.buy, bcAPrice, bQty, bcOrder);
			sendOrder(abProduct, Side.buy, abAPrice, aQty, abOrder);
			sendOrder(acProduct, Side.sell, acBPrice, aQty, acOrder);
		}
	}
	
	private void sendOrder(Product product, Side side, double price, double qty, Order order) {
		exchange.sendOrder(++orderId, product, side, price, qty, this);
		mapOrderIdOrder.put(orderId, order);
		order.setQty(qty);
		order.setSide(side);
		order.setPrice(price);
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
		log.warn("on Cancel order {}", orderId);
		Order order = mapOrderIdOrder.remove(orderId);
		
		Product product = order.getProduct();
		double price = 0;
		if(product == abProduct) {
			switch(order.getSide()) {
			case buy:
				price = abAPrice;
				break;
			case sell:
				price = abBPrice;
				break;
			}
		} else if(product == bcProduct) {
			switch(order.getSide()) {
			case buy:
				price = bcAPrice;
				break;
			case sell:
				price = bcBPrice;
				break;
			}
		} else if(product == acProduct) {
			switch(order.getSide()) {
			case buy:
				price = acAPrice;
				break;
			case sell:
				price = acBPrice;
				break;
			}
		}
		
		sendOrder(product, order.getSide(), price, order.getQty(), order);
	}
	
	@Override
	public void onCancelRej(int orderId) {
		
	}

	@Override
	public void onFill(int orderId, double price, double qty) {
		Order order = mapOrderIdOrder.remove(orderId);
		
		log.info("onFill: {} {} {} {} {}", orderId, order.getProduct(), order.getSide(), price, qty);
		
		if(mapOrderIdOrder.size() == 0) {
			status.set(STATUS_STOP);
			mapBalance = exchange.queryBalance();
			printBalance();
		}
	}
	
	private void printBalance() {
		StringBuilder sb = new StringBuilder();
		for(String key : mapBalance.keySet()) {
			sb.append(key).append(":").append(mapBalance.get(key)).append(" , ");
		}
		log.info("Balance: [{}]", sb.toString());
	}
	
	private class Order {
		private Product product;
		private Side side;
		private double price;
		private double qty;
		
		public Order(Product product) {
			this.product = product;
		}
		
		public Product getProduct() {
			return product;
		}
		
		public void setSide(Side side) {
			this.side = side;
		}
		
		public Side getSide() {
			return side;
		}
		
		public void setPrice(double price) {
			this.price = price;
		}
		
		public double getPrice() {
			return price;
		}
		
		public void setQty(double qty) {
			this.qty = qty;
		}
		
		public double getQty() {
			return qty;
		}
	}

	@Override
	public boolean stop() {
		return true;
	}
}
