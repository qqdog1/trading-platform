package name.qd.tradingPlatform.strategies.MAX;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;
import name.qd.tradingPlatform.Constants.Side;
import name.qd.tradingPlatform.exchanges.Exchange;
import name.qd.tradingPlatform.exchanges.ExchangeManager;
import name.qd.tradingPlatform.exchanges.book.MarketBook;
import name.qd.tradingPlatform.product.FileProductMapperManager;
import name.qd.tradingPlatform.strategies.Book;
import name.qd.tradingPlatform.strategies.Strategy;
import name.qd.tradingPlatform.strategies.order.Order;
import name.qd.tradingPlatform.strategies.order.OrderUtils;
import name.qd.tradingPlatform.strategies.order.SidePrice;
import name.qd.tradingPlatform.strategies.order.TradeCycle;
import name.qd.tradingPlatform.strategies.order.TradingStatus;

public class MAXMarketMaker extends Strategy {
	private static Logger log = LoggerFactory.getLogger(MAXMarketMaker.class);
	private static final int CHECK_INTERVAL = 1000;
	private static final double PNL = 1.0d;
	private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
	private Product[] firstLegProducts = {Product.BAT_USDT};
	private Product[] products;
	private String[] excludeCurrency = {""};
	private FileProductMapperManager productMapper = ExchangeManager.getInstance().getFileProductMapperManager();
	private Exchange max = ExchangeManager.getInstance().getExchange(ExchangeName.MAX);
	private List<Product> lstProducts = productMapper.getProducts(ExchangeName.MAX);
	private Map<Product, Book> mapBook = new HashMap<>();
	private Map<Product, List<TradeCycle>> mapAllPair = new ConcurrentHashMap<>();
	private Map<Product, List<TradeCycle>> mapAvailable = new ConcurrentHashMap<>();
	private Map<Product, TradeCycle> mapTradingPair = new ConcurrentHashMap<>();
	private Map<Product, Order> mapActiveOrder = new HashMap<>();
	private Map<Product, Double> mapTickSize = new HashMap<>();
	private Map<Integer, Product> mapOrderIdToProduct = new HashMap<>();
	private OrderUtils orderUtils = new OrderUtils();
	private int orderId = 0;
	private Map<Product, TradingStatus> mapTradingStatus = new ConcurrentHashMap<>();
	private Map<Integer, Product> mapOrderIdToFirstPair = new ConcurrentHashMap<>();
	private Map<Integer, Order> mapOrderIdToOrder = new ConcurrentHashMap<>();
	
	public MAXMarketMaker(Map<ExchangeName, Product[]> map) {
		super(map);
		analysisProducts();
		setSubProducts();
		getTickSize();
		
		map.put(ExchangeName.MAX, products);
		for(Product product : mapAllPair.keySet()) {
			setTradingStatus(product, TradingStatus.STANDBY);
			log.info("------- {} ---------------------------", product);
			for(TradeCycle tradeCycle : mapAllPair.get(product)) {
				log.info("{} {}, {} {}, {} {}"
						, tradeCycle.getAB().getProduct(), tradeCycle.getAB().getSide()
						, tradeCycle.getAC().getProduct(), tradeCycle.getAC().getSide()
						, tradeCycle.getBC().getProduct(), tradeCycle.getBC().getSide());
			}
		}
		
		log.info("Strategy start.");
		executorService.scheduleAtFixedRate(new Checker(), CHECK_INTERVAL, CHECK_INTERVAL, TimeUnit.MILLISECONDS);
	}
	
	private void analysisProducts() {
		for(Product settingProduct : firstLegProducts) {
			List<TradeCycle> lstTradeCycle = new ArrayList<>();
			String[] p1 = settingProduct.name().split("_");
			for(int i = 0 ; i < lstProducts.size() ; i++) {
				String[] p2 = lstProducts.get(i).name().split("_");
				if(isExcludeCurrency(p2)) {
					continue;
				}
				if(p1[0].equals(p2[0]) && p1[1].equals(p2[1])) {
					continue;
				}
				
				if(p1[0].equals(p2[0]) || p1[0].equals(p2[1]) || p1[1].equals(p2[0]) || p1[1].equals(p2[1])) {
					for (int j = i + 1; j < lstProducts.size(); j++) {
						String[] p3 = lstProducts.get(j).name().split("_");
						if(p1[0].equals(p3[0]) && p1[1].equals(p3[1])) {
							continue;
						}
						if (p3[0].equals(p1[0])) {
							// AA
							if (p3[1].equals(p2[0])) {
								// BB
								Product[] p = new Product[3];
								p[0] = settingProduct;
								p[1] = lstProducts.get(i);
								p[2] = lstProducts.get(j);
								lstTradeCycle.addAll(getTradeCycle(p));
								break;
							} else if (p3[1].equals(p2[1])) {
								// CC
								Product[] p = new Product[3];
								p[0] = lstProducts.get(j);
								p[1] = lstProducts.get(i);
								p[2] = settingProduct;
								lstTradeCycle.addAll(getTradeCycle(p));
								break;
							}
						} else if (p3[0].equals(p1[1])) {
							// BB
							if (p3[1].equals(p2[1])) {
								// CC
								Product[] p = new Product[3];
								p[0] = lstProducts.get(i);
								p[1] = lstProducts.get(j);
								p[2] = settingProduct;
								lstTradeCycle.addAll(getTradeCycle(p));
								break;
							}
						} else if (p3[1].equals(p1[0])) {
							// BB
							if (p3[0].equals(p2[0])) {
								// AA
								Product[] p = new Product[3];
								p[0] = lstProducts.get(i);
								p[1] = settingProduct;
								p[2] = lstProducts.get(j);
								lstTradeCycle.addAll(getTradeCycle(p));
								break;
							}
						} else if (p3[1].equals(p1[1])) {
							// CC
							if (p3[0].equals(p2[0])) {
								// AA
								Product[] p = new Product[3];
								p[0] = lstProducts.get(j);
								p[1] = settingProduct;
								p[2] = lstProducts.get(i);
								lstTradeCycle.addAll(getTradeCycle(p));
								break;
							} else if (p3[0].equals(p2[1])) {
								// BB
								Product[] p = new Product[3];
								p[0] = settingProduct;
								p[1] = lstProducts.get(j);
								p[2] = lstProducts.get(i);
								lstTradeCycle.addAll(getTradeCycle(p));
								break;
							}
						}
					}
				}
			}
			mapAllPair.put(settingProduct, lstTradeCycle);
		}
	}
	
	private List<TradeCycle> getTradeCycle(Product[] products) {
		List<TradeCycle> lstTradeCycle = new ArrayList<>();
		SidePrice ac1 = new SidePrice();
		SidePrice bc1 = new SidePrice();
		SidePrice ab1 = new SidePrice();
		ac1.setProduct(products[0]);
		bc1.setProduct(products[1]);
		ab1.setProduct(products[2]);
		ac1.setSide(Side.buy);
		bc1.setSide(Side.sell);
		ab1.setSide(Side.sell);
		
		TradeCycle tradeCycle1 = new TradeCycle(ac1, bc1, ab1);
		
		SidePrice ac2 = new SidePrice();
		SidePrice bc2 = new SidePrice();
		SidePrice ab2 = new SidePrice();
		ac2.setProduct(products[0]);
		bc2.setProduct(products[1]);
		ab2.setProduct(products[2]);
		ac2.setSide(Side.sell);
		bc2.setSide(Side.buy);
		ab2.setSide(Side.buy);
		
		TradeCycle tradeCycle2 = new TradeCycle(ac2, bc2, ab2);
		
		lstTradeCycle.add(tradeCycle1);
		lstTradeCycle.add(tradeCycle2);
		return lstTradeCycle;
	}
	
	private synchronized TradingStatus getTradingStatus(Product product) {
		return mapTradingStatus.get(product);
	}
	
	private synchronized void setTradingStatus(Product product, TradingStatus tradingStatus) {
		mapTradingStatus.put(product, tradingStatus);
		log.info("{} : {}", product, tradingStatus);
	}
	
	private void setSubProducts() {
		Set<Product> set = new HashSet<>();
		for(Product product : mapAllPair.keySet()) {
			for(TradeCycle tradeCycle : mapAllPair.get(product)) {
				set.add(tradeCycle.getAB().getProduct());
				set.add(tradeCycle.getAC().getProduct());
				set.add(tradeCycle.getBC().getProduct());
			}
		}
		for(Product product : set) {
			Book book = new Book();
			mapBook.put(product, book);
		}
		products = new Product[set.size()];
		set.toArray(products);
	}
	
	private void getTickSize() {
		Map<String, Double> map = max.getTickSize();
		for(String key : map.keySet()) {
			Product product = productMapper.getProduct(key, ExchangeName.MAX);
			mapTickSize.put(product, map.get(key));
		}
	}
	
	private boolean isExcludeCurrency(String[] pair) {
		for(String currency : pair) {
			for(String exCurrency : excludeCurrency) {
				if(exCurrency.equals(currency)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void onBook(ExchangeName exchangeName, Product product, double bidPrice, double bidQty, double askPrice, double askQty) {
		Book book = mapBook.get(product);
		book.setbPrice(bidPrice);
		book.setbQty(bidQty);
		book.setsPrice(askPrice);
		book.setsQty(askQty);
	}

	@Override
	public void onTick() {
	}

	@Override
	public void onOrderAck(int orderId) {
		Product product = mapOrderIdToFirstPair.get(orderId);
		TradingStatus tradingStatus = getTradingStatus(product);
		switch(tradingStatus) {
		case PAIR1SEND:
			setTradingStatus(product, TradingStatus.PAIR1ACK);
			break;
		case PAIR2SEND:
			setTradingStatus(product, TradingStatus.PAIR2ACK);
			break;
		case PAIR3SEND:
			setTradingStatus(product, TradingStatus.PAIR3ACK);
			break;
		default:
			log.error("Receive order ack at wrong status. {}, {}", tradingStatus, orderId);
			break;
		}
	}

	@Override
	public void onOrderRej(int orderId) {
		Product product = mapOrderIdToProduct.get(orderId);
		setTradingStatus(product, TradingStatus.STOP);
		mapOrderIdToProduct.remove(orderId);
		mapOrderIdToFirstPair.remove(orderId);
		mapOrderIdToOrder.remove(orderId);
		mapTradingPair.remove(product);
	}

	@Override
	public void onCancelAck(int orderId) {
		log.info("Cancel Ack :{}", orderId);
		Product product = mapOrderIdToProduct.get(orderId);
		setTradingStatus(product, TradingStatus.STANDBY);
		mapOrderIdToProduct.remove(orderId);
		mapOrderIdToProduct.remove(orderId);
		mapOrderIdToFirstPair.remove(orderId);
		mapOrderIdToOrder.remove(orderId);
	}

	@Override
	public void onCancelRej(int orderId) {
		log.info("Cancel Rej :{}", orderId);
	}

	@Override
	public void onFill(int orderId, double price, double qty) {
		Product product = mapOrderIdToFirstPair.get(orderId);
		TradingStatus tradingStatus = getTradingStatus(product);
		Order order = mapOrderIdToOrder.get(orderId);
		switch(tradingStatus) {
		case PAIR1ACK:
			log.info("{} pair 1 filled. {} {}@{}", product, order.getProduct(), qty, price);
			setTradingStatus(product, TradingStatus.PAIR1FILL);
			sendNextOrder(product);
			break;
		case PAIR2ACK:
			log.info("{} pair 2 filled. {} {}@{}", product, order.getProduct(), qty, price);
			setTradingStatus(product, TradingStatus.PAIR2FILL);
			sendNextOrder(product);
			break;
		case PAIR3ACK:
			log.info("{} pair 3 filled. {} {}@{}", product, order.getProduct(), qty, price);
			setTradingStatus(product, TradingStatus.PAIR3FILL);
			
			break;
		default:
			log.error("Receive fill at wrong status. {}, {}, {}@{}", tradingStatus, orderId, qty, price);
			break;
		}
	}
	
	private void updatePrice(TradeCycle tradeCycle) {
		updatePrice(tradeCycle.getAB());
		updatePrice(tradeCycle.getAC());
		updatePrice(tradeCycle.getBC());
	}
	
	private void updatePrice(SidePrice sidePrice) {
		Product product = sidePrice.getProduct();
		Side side = sidePrice.getSide();
		Book book = mapBook.get(product);
		switch (side) {
		case buy:
			sidePrice.setPrice(book.getbPrice());
			sidePrice.setQty(book.getbQty());
			break;
		case sell:
			sidePrice.setPrice(book.getsPrice());
			sidePrice.setQty(book.getsQty());
			break;
		default:
			break;
		}
	}
	
	private boolean checkMinSize(Product product, TradeCycle tradeCycle) {
		if(tradeCycle.getAB().getProduct() == product && checkMinSize(tradeCycle.getAC()) && checkMinSize(tradeCycle.getBC())) {
			addToAvailable(product, tradeCycle);
			return true;
		} else if(tradeCycle.getAC().getProduct() == product && checkMinSize(tradeCycle.getAB()) && checkMinSize(tradeCycle.getBC())) {
			addToAvailable(product, tradeCycle);
			return true;
		} else if(tradeCycle.getBC().getProduct() == product && checkMinSize(tradeCycle.getAB()) && checkMinSize(tradeCycle.getAC())) {
			addToAvailable(product, tradeCycle);
			return true;
		}
		return false;
	}
	
	private void addToAvailable(Product product, TradeCycle tradeCycle) {
		if(!mapAvailable.containsKey(product)) {
			mapAvailable.put(product, new ArrayList<>());
		}
		mapAvailable.get(product).add(tradeCycle);
	}
	
	private boolean checkMinSize(SidePrice sidePrice) {
		String product = sidePrice.getProduct().name();
		String[] p = product.split("_");
		MaxQuote quote = MaxQuote.valueOf(p[1]);
		return sidePrice.getPrice() * sidePrice.getQty() > quote.getMinValue();
	}
	
	private void calcExpectedPnl(Product product, TradeCycle tradeCycle) {
		checkNearPrice(product, tradeCycle.getAB());
		checkNearPrice(product, tradeCycle.getAC());
		checkNearPrice(product, tradeCycle.getBC());
		
		Side abSide = tradeCycle.getAB().getSide();
		switch (abSide) {
		case buy:
			if (tradeCycle.getAB().getPrice() != 0 && tradeCycle.getAC().getPrice() != 0
					&& tradeCycle.getBC().getPrice() != 0) {
				double rate = tradeCycle.getAC().getPrice() / tradeCycle.getBC().getPrice();
				double pnl = tradeCycle.getAB().getPrice() / rate;
				tradeCycle.setPnl(pnl);
			}
			break;
		case sell:
			if (tradeCycle.getAB().getPrice() != 0 && tradeCycle.getAC().getPrice() != 0
					&& tradeCycle.getBC().getPrice() != 0) {
				double rate = tradeCycle.getAC().getPrice() / tradeCycle.getBC().getPrice();
				double pnl = rate / tradeCycle.getAB().getPrice();
				tradeCycle.setPnl(pnl);
			}
			break;
		}
	}
	
	private void checkNearPrice(Product product, SidePrice sidePrice) {
		if(product == sidePrice.getProduct()) {
			Book book = mapBook.get(product);
			double tickSize = mapTickSize.get(product);
			double newPrice = 0;
			
			if(mapActiveOrder.containsKey(product)) {
				Order order = mapActiveOrder.get(product);
				if(order.getPrice() == sidePrice.getPrice()) {
					return;
				}
			}
			
			switch(sidePrice.getSide()) {
			case sell:
				newPrice = BigDecimal.valueOf(book.getbPrice()).add(BigDecimal.valueOf(tickSize)).doubleValue();
				break;
			case buy:
				newPrice = BigDecimal.valueOf(book.getsPrice()).subtract(BigDecimal.valueOf(tickSize)).doubleValue();
				break;
			}
			sidePrice.setPrice(newPrice);
		}
	}
	
	private void getMaxPNLTradeCycle(Product product, List<TradeCycle> lstTradeCycle) {
		TradeCycle maxTradeCycle = null;
		for(TradeCycle tradeCycle : lstTradeCycle) {
			if(tradeCycle.getPnl() > PNL) {
				if(maxTradeCycle != null && tradeCycle.getPnl() > maxTradeCycle.getPnl()) {
					maxTradeCycle = tradeCycle;
				} else if(maxTradeCycle == null){
					maxTradeCycle = tradeCycle;
				}
			}
		}
		if(maxTradeCycle != null) {
			mapTradingPair.put(product, orderUtils.calcTradingQty(maxTradeCycle));
		} else {
			if(mapTradingPair.containsKey(product)) {
				mapTradingPair.remove(product);
				log.info("Chance gone. Remove trading pair. {}", product);
			}
		}
	}
	
	@SuppressWarnings("incomplete-switch")
	private void checkOrder() {
		for(Product product : mapTradingPair.keySet()) {
			TradingStatus tradingStatus = getTradingStatus(product);
			switch(tradingStatus) {
			case STANDBY:
				// place new order and cache
				if(!mapActiveOrder.containsKey(product)) {
					placeOrder(product);
				}
				break;
			case PAIR1ACK:
				// if price or qty change, cancel active order
				if(mapActiveOrder.containsKey(product)) {
					Order order = mapActiveOrder.get(product);
					if(isPriceQtyDiff(order, mapTradingPair.get(product))) {
						log.warn("New trading price or quantity diff. {}", order.getProduct());
						cancelOrder(order.getOrderId());
					}
				}
				break;
			}
		}
		
		for(Product product : mapActiveOrder.keySet()) {
			if(!mapTradingPair.containsKey(product)) {
				Order order = mapActiveOrder.get(product);
				cancelOrder(order.getOrderId());
			}
		}
	}
	
	private void sendNextOrder(Product product) {
		
	}
	
	private void placeOrder(Product product) {
		setTradingStatus(product, TradingStatus.PAIR1SEND);
		TradeCycle tradeCycle = mapTradingPair.get(product);
		if(tradeCycle.getAB().getProduct() == product) {
			placeOrder(tradeCycle.getAB(), product);
		} else if(tradeCycle.getAC().getProduct() == product) {
			placeOrder(tradeCycle.getAC(), product);
		} else if(tradeCycle.getBC().getProduct() == product) {
			placeOrder(tradeCycle.getBC(), product);
		}
	}
	
	private void placeOrder(SidePrice sidePrice, Product product) {
		log.info("[{}] {} Pair send order: {} {} , {}@{}", orderId, product, sidePrice.getProduct(), sidePrice.getSide(), sidePrice.getQty(), sidePrice.getPrice());
		mapOrderIdToFirstPair.put(orderId, product);
		mapOrderIdToProduct.put(orderId, sidePrice.getProduct());
		Order order = new Order(orderId, sidePrice);
		mapOrderIdToOrder.put(orderId, order);
		mapActiveOrder.put(product, order);
		max.sendOrder(orderId++, sidePrice.getProduct(), sidePrice.getSide(), sidePrice.getPrice(), sidePrice.getQty(), this);
	}
	
	private void cancelOrder(int orderId) {
		log.info("Cancel order :{}", orderId);
		Product product = mapOrderIdToProduct.get(orderId);
		mapActiveOrder.remove(product);
		max.sendCancel(orderId, this);
	}
	
	private boolean isPriceQtyDiff(Order order, TradeCycle tradeCycle) {
		return (isPriceQtyDiff(order, tradeCycle.getAB()) || 
			    isPriceQtyDiff(order, tradeCycle.getAC()) || 
			    isPriceQtyDiff(order, tradeCycle.getBC()));
	}
	
	private boolean isPriceQtyDiff(Order order, SidePrice sidePrice) {
		if(order.getProduct() == sidePrice.getProduct()) {
			if(order.getPrice() != sidePrice.getPrice() || order.getQty() != sidePrice.getQty()) {
				return true;
			}
		}
		return false;
	}
	
	@SuppressWarnings("incomplete-switch")
	private void searchPair() {
		for(Product product : mapAllPair.keySet()) {
			TradingStatus tradingStatus = getTradingStatus(product);
			switch(tradingStatus) {
			case STANDBY:
			case PAIR1ACK:
				for(TradeCycle tradeCycle : mapAllPair.get(product)) {
					updatePrice(tradeCycle);
					if(checkMinSize(product, tradeCycle)) {
						calcExpectedPnl(product, tradeCycle);
					}
				}
				
				getMaxPNLTradeCycle(product, mapAvailable.get(product));
				break;
			}
		}
		
		// Available 已經是可下 且量夠 量多少還沒算
		if(mapAvailable.size() > 0) {
			log.info("All Available pair:");
			for(Product product : mapAvailable.keySet()) {
				log.info("--- {} ----------", product);
				for(TradeCycle tradeCycle : mapAvailable.get(product)) {
					log.info("{} {} {} , {} {} {} , {} {} {} : {}", 
							tradeCycle.getAB().getProduct(), tradeCycle.getAB().getSide(), tradeCycle.getAB().getPrice(), 
							tradeCycle.getAC().getProduct(), tradeCycle.getAC().getSide(), tradeCycle.getAC().getPrice(), 
							tradeCycle.getBC().getProduct(), tradeCycle.getBC().getSide(), tradeCycle.getBC().getPrice(), 
							tradeCycle.getPnl());
				}
			}
			mapAvailable.clear();
		}
		
		if(mapTradingPair.size() > 0) {
			log.info("Trading Pair:");
			for(Product product : mapTradingPair.keySet()) {
				log.info("--- {} ------------", product);
				TradeCycle tradeCycle = mapTradingPair.get(product);
					log.info("{} {} {} , {} {} {} , {} {} {} : {}", 
							tradeCycle.getAB().getProduct(), tradeCycle.getAB().getSide(), tradeCycle.getAB().getPrice(), 
							tradeCycle.getAC().getProduct(), tradeCycle.getAC().getSide(), tradeCycle.getAC().getPrice(), 
							tradeCycle.getBC().getProduct(), tradeCycle.getBC().getSide(), tradeCycle.getBC().getPrice(), 
							tradeCycle.getPnl());
			}
		}
	}
	
	private class Checker implements Runnable {
		@Override
		public void run() {
			searchPair();
			checkOrder();
			
		}
	}

	@Override
	public boolean stop() {
		return true;
	}

	@Override
	public void onBook(ExchangeName exchangeName, Product product, MarketBook marketBook) {
		// TODO Auto-generated method stub
		
	}
}
