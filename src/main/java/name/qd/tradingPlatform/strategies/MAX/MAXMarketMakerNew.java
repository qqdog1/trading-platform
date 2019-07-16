package name.qd.tradingPlatform.strategies.MAX;

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
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.tradingPlatform.exchanges.Exchange;
import name.qd.tradingPlatform.exchanges.ExchangeManager;
import name.qd.tradingPlatform.exchanges.book.MarketBook;
import name.qd.tradingPlatform.product.ProductMapper;
import name.qd.tradingPlatform.strategies.Book;
import name.qd.tradingPlatform.strategies.Strategy;
import name.qd.tradingPlatform.strategies.order.OrderUtils;
import name.qd.tradingPlatform.strategies.order.SidePrice;
import name.qd.tradingPlatform.strategies.order.TradeCycle;
import name.qd.tradingPlatform.strategies.order.TradingStatus;
import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;
import name.qd.tradingPlatform.Constants.Side;

public class MAXMarketMakerNew extends Strategy {
	private static Logger log = LoggerFactory.getLogger(MAXMarketMakerNew.class);
	private Product[] firstLegProducts = {Product.ETH_BTC};
	private String[] excludeCurrency = {"CCCX"};
	private static final int CHECK_INTERVAL = 1000;
	private static final double PNL = 1.00d;
	
	private ProductMapper productMapper = ExchangeManager.getInstance().getProductMapper();
	private List<Product> lstProducts = productMapper.getProducts(ExchangeName.MAX);
	private Map<Product, Book> mapBook = new HashMap<>();
	private Product[] allProductsForSub;
	private Exchange max = ExchangeManager.getInstance().getExchange(ExchangeName.MAX);
	private Map<Product, Double> mapTickSize = new HashMap<>();
	
	private Map<Product, List<TradeCycle>> mapAllPair = new ConcurrentHashMap<>();
	private Map<Product, TradingStatus> mapTradingStatus = new ConcurrentHashMap<>();
	
	private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
	private OrderUtils orderUtils = new OrderUtils();
	
	private Map<Product, SidePrice> mapActiveOrder = new ConcurrentHashMap<>();
	private Map<Integer, Product> mapOrderIdToProduct = new ConcurrentHashMap<>();
	private Map<Product, Integer> mapProductToOrderId = new ConcurrentHashMap<>();
	private Map<Product, List<SidePrice>> mapWantedOrder = new ConcurrentHashMap<>();
	
	private AtomicInteger orderId = new AtomicInteger(1);
	private static Map<ExchangeName, Product[]> map = new HashMap<>();
	
	public MAXMarketMakerNew() {
		super(map);
		analysisProducts();
		setSubProducts();
		getTickSize();
		
		map.put(ExchangeName.MAX, allProductsForSub);
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
	
	private synchronized void setTradingStatus(Product product, TradingStatus tradingStatus) {
		mapTradingStatus.put(product, tradingStatus);
		log.info("{} : {}", product, tradingStatus);
	}
	
	private synchronized TradingStatus getTradingStatus(Product product) {
		return mapTradingStatus.get(product);
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
		Product product = mapOrderIdToProduct.get(orderId);
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
			log.error("Wrong status when receiving ack ! [{}], {} {}", orderId, product, tradingStatus);
			break;
		}
	}

	@Override
	public void onOrderRej(int orderId) {
		
	}

	@Override
	public void onCancelAck(int orderId) {
		log.info("Cancel order : [{}]", orderId);
		Product product = mapOrderIdToProduct.get(orderId);
		TradingStatus tradingStatus = getTradingStatus(product);
		
		switch(tradingStatus) {
		case RESET:
			mapOrderIdToProduct.remove(orderId);
			mapProductToOrderId.remove(product);
			mapActiveOrder.remove(product);
			mapWantedOrder.remove(product);
			setTradingStatus(product, TradingStatus.STANDBY);
			break;
		default:
			log.error("Receive cancel ack at not expected status. {}", tradingStatus);
			break;
		}
	}

	@Override
	public void onCancelRej(int orderId) {
		
	}

	@Override
	public void onFill(int orderId, double price, double qty) {
		
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
		allProductsForSub = new Product[set.size()];
		set.toArray(allProductsForSub);
	}
	
	private void getTickSize() {
		Map<String, Double> map = max.getTickSize();
		for(String key : map.keySet()) {
			Product product = productMapper.getProduct(key, ExchangeName.MAX);
			mapTickSize.put(product, map.get(key));
		}
	}
	
	private double increaseTick(Product product, double price) {
		return price + mapTickSize.get(product);
	}
	
	private double decreaseTick(Product product, double price) {
		return price - mapTickSize.get(product);
	}
	
	private void updatePrice(Product product) {
		List<TradeCycle> lst = mapAllPair.get(product);
		for(TradeCycle tradeCycle : lst) {
			updatePrice(tradeCycle.getAB());
			updatePrice(tradeCycle.getAC());
			updatePrice(tradeCycle.getBC());
		}
	}
	
	private void updatePrice(SidePrice sidePrice) {
		Book book = mapBook.get(sidePrice.getProduct());
		switch(sidePrice.getSide()) {
		case buy:
			sidePrice.setPrice(book.getbPrice());
			sidePrice.setQty(book.getbQty());
			break;
		case sell:
			sidePrice.setPrice(book.getsPrice());
			sidePrice.setQty(book.getsQty());
			break;
		}
	}
	
	private TradeCycle findMaxPnl(Product product) {
		List<TradeCycle> lst = mapAllPair.get(product);
		List<TradeCycle> lstPnl = new ArrayList<>();
		for(TradeCycle tradeCycle : lst) {
			if(checkTradeVolume(product, tradeCycle)) {
				lstPnl.add(tradeCycle);
			}
		}
		
		setFirstLeg(product, lstPnl);
		
		for(TradeCycle tradeCycle : lstPnl) {
			orderUtils.calcPnl(tradeCycle);
			log.info("{}:{}:{}, {}", tradeCycle.getAB().getProduct(), tradeCycle.getAC().getProduct(), tradeCycle.getBC().getProduct(), tradeCycle.getPnl());
		}
		
		return getMaxPnl(lstPnl);
	}
	
	private boolean checkTradeVolume(Product product, TradeCycle tradeCycle) {
		if(tradeCycle.getAB().getProduct() == product) {
			return checkMinSize(tradeCycle.getAC()) && checkMinSize(tradeCycle.getBC());
		} else if(tradeCycle.getAC().getProduct() == product) {
			return checkMinSize(tradeCycle.getAB()) && checkMinSize(tradeCycle.getBC());
		} else {
			return checkMinSize(tradeCycle.getAB()) && checkMinSize(tradeCycle.getAC());
		}
	}
	
	private void setFirstLeg(Product product, List<TradeCycle> lst) {
		// 如果是我要買 本來是看賣價第一檔 這時side會是sell
		// 所以要把sell price 變成買價第一檔 + 1 tick
		for(TradeCycle tradeCycle : lst) {
			if(tradeCycle.getAB().getProduct() == product) {
				setFirstLeg(tradeCycle.getAB());
			} else if(tradeCycle.getAC().getProduct() == product) {
				setFirstLeg(tradeCycle.getAC());
			} else {
				setFirstLeg(tradeCycle.getBC());
			}
		}
	}
	
	private void setFirstLeg(SidePrice sidePrice) {
		Book book = mapBook.get(sidePrice.getProduct());
		double price = 0;
		switch(sidePrice.getSide()) {
		case buy:
			price = decreaseTick(sidePrice.getProduct(), book.getsPrice());
			break;
		case sell:
			price = increaseTick(sidePrice.getProduct(), book.getbPrice());
			break;
		}
		sidePrice.setPrice(price);
	}
	
	private boolean checkMinSize(SidePrice sidePrice) {
		String product = sidePrice.getProduct().name();
		String[] p = product.split("_");
		MaxQuote quote = MaxQuote.valueOf(p[1]);
		return sidePrice.getPrice() * sidePrice.getQty() > quote.getMinValue();
	}
	
	private TradeCycle getMaxPnl(List<TradeCycle> lst) {
		TradeCycle maxTradeCycle = new TradeCycle(null, null, null);
		for(TradeCycle tradeCycle : lst) {
			if(tradeCycle.getPnl() > maxTradeCycle.getPnl()) {
				maxTradeCycle = tradeCycle;
			}
		}
		return maxTradeCycle;
	}
	
	private void prepareOrders(Product product, TradeCycle tradeCycle) {
		TradeCycle orderTradeCycle = orderUtils.calcTradingQty(tradeCycle);
		setWantedOrder(product, orderTradeCycle);
	}
	
	private void setWantedOrder(Product product, TradeCycle tradeCycle) {
		List<SidePrice> lst = new ArrayList<>();
		if(tradeCycle.getAB().getProduct() == product) {
			lst.add(tradeCycle.getAB());
			if(tradeCycle.getAB().getSide() == Side.buy) {
				lst.add(tradeCycle.getAC());
				lst.add(tradeCycle.getBC());
			} else {
				lst.add(tradeCycle.getBC());
				lst.add(tradeCycle.getAC());
			}
		} else if(tradeCycle.getAC().getProduct() == product) {
			lst.add(tradeCycle.getAC());
			if(tradeCycle.getAC().getSide() == Side.buy) {
				lst.add(tradeCycle.getAB());
				lst.add(tradeCycle.getBC());
			} else {
				lst.add(tradeCycle.getAB());
				lst.add(tradeCycle.getBC());
			}
		} else {
			lst.add(tradeCycle.getBC());
			if(tradeCycle.getBC().getSide() == Side.buy) {
				lst.add(tradeCycle.getAB());
				lst.add(tradeCycle.getAC());
			} else {
				lst.add(tradeCycle.getAC());
				lst.add(tradeCycle.getAB());
			}
		}
		mapWantedOrder.put(product, lst);
	}
	
	private void sendOrder(Product product, int index) {
		List<SidePrice> lst = mapWantedOrder.get(product);
		SidePrice sidePrice = lst.get(index-1);
		if(sidePrice.getPrice() == 0 || sidePrice.getQty() == 0) {
			log.error("Price || Qty == 0, {}", product);
			return;
		}
		int thisOrder = orderId.getAndIncrement();
		log.info("Send order: [{}], {}:{} {} {}@{}", thisOrder, product, sidePrice.getProduct(), sidePrice.getSide(), sidePrice.getQty(), sidePrice.getPrice());
		mapOrderIdToProduct.put(thisOrder, product);
		mapProductToOrderId.put(product, thisOrder);
		mapActiveOrder.put(product, sidePrice);
		setTradingStatus(product, TradingStatus.PAIR1SEND);
		max.sendOrder(thisOrder, sidePrice.getProduct(), sidePrice.getSide(), sidePrice.getPrice(), sidePrice.getQty(), this);
	}
	
	private boolean isNeedResend(Product product, TradeCycle tradeCycle) {
		boolean isNeedResend = false;
		SidePrice activeOrder = mapActiveOrder.get(product);
		SidePrice sidePrice;
		if(tradeCycle.getAB().getProduct() == product) {
			sidePrice = tradeCycle.getAB();
		} else if(tradeCycle.getAC().getProduct() == product) {
			sidePrice = tradeCycle.getAC();
		} else {
			sidePrice = tradeCycle.getBC();
		}
		
		if(activeOrder.getSide() == sidePrice.getSide()) {
			log.info("First leg side change. {}->{}", activeOrder.getSide(), sidePrice.getSide());
			isNeedResend = true;
		} else if(activeOrder.getQty() != sidePrice.getQty()) {
			log.info("First leg qty change. {}->{}", activeOrder.getQty(), sidePrice.getQty());
			isNeedResend = true;
		} else {
			double price;
			switch(sidePrice.getSide()) {
			case buy:
				price = decreaseTick(sidePrice.getProduct(), sidePrice.getPrice());
				if(activeOrder.getPrice() != price) {
					isNeedResend = true;
				}
				break;
			case sell:
				price = increaseTick(sidePrice.getProduct(), sidePrice.getPrice());
				if(activeOrder.getPrice() != price) {
					isNeedResend = true;
				}
				break;
			}
		}
		
		return isNeedResend;
	}
	
	private void sendCancel(Product product) {
		sendCancel(mapProductToOrderId.get(product));
	}
	
	private void sendCancel(int orderId) {
		max.sendCancel(orderId, this);
	}
	
	private class Checker implements Runnable {
		@Override
		public void run() {
			for(Product product : mapAllPair.keySet()) {
				TradingStatus tradingStatus = getTradingStatus(product);
				TradeCycle tradeCycle;
				switch(tradingStatus) {
				case STANDBY:
					updatePrice(product);
					tradeCycle = findMaxPnl(product);
					log.info("MAX Pnl: {}:{}:{}, {}", tradeCycle.getAB().getProduct(), tradeCycle.getAC().getProduct(), tradeCycle.getBC().getProduct(), tradeCycle.getPnl());
					if(tradeCycle.getPnl() < PNL) {
						break;
					}
					prepareOrders(product, tradeCycle);
					sendOrder(product, 1);
					break;
				case PAIR1ACK:
					updatePrice(product);
					tradeCycle = findMaxPnl(product);
					log.info("MAX Pnl: {}:{}:{}, {}", tradeCycle.getAB().getProduct(), tradeCycle.getAC().getProduct(), tradeCycle.getBC().getProduct(), tradeCycle.getPnl());
					if(isNeedResend(product, tradeCycle)) {
						log.info("Price change, cancel order.");
						setTradingStatus(product, TradingStatus.RESET);
						sendCancel(product);
					}
					break;
				case PAIR2ACK:
					break;
				case PAIR3ACK:
					break;
					
				case CHECKBALANCE:
					break;
				
				case STOP:
					break;
				default:
					break;
				}
			}
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
