package name.qd.tradingPlatform.strategies.MAX;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
import name.qd.tradingPlatform.strategies.utils.BalanceChecker;
import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;
import name.qd.tradingPlatform.Constants.Side;

public class MAXAutoTrade extends Strategy {
	private static Logger log = LoggerFactory.getLogger(MAXAutoTrade.class);
	private static int CHECK_INTERVAL = 1000;
	private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> scheduledFuture;
	private ProductMapper productMapper = ExchangeManager.getInstance().getProductMapper();
	private List<Product> lstProducts = productMapper.getProducts(ExchangeName.MAX);
	private Map<Product, Book> mapBook = new HashMap<>();
	private List<Product[]> lst = new ArrayList<>();
	private List<TradeCycle> lstTradeCycle = new ArrayList<>();
	private Exchange max = ExchangeManager.getInstance().getExchange(ExchangeName.MAX);
	private TradingStatus tradingStatus = TradingStatus.STANDBY;
	private TradeCycle nullTradeCycle = new TradeCycle(null, null, null);
	private TradeCycle maxPnlTradeCycle = nullTradeCycle;
	private List<TradeCycle> lstAvaliable = new ArrayList<>();
	private List<SidePrice> lstWantedOrder = new ArrayList<>();
	private OrderUtils orderUtils = new OrderUtils();
	private BalanceChecker balanceChecker = new BalanceChecker();
	private int orderId = 0;
	private long orderTime = 0;
	private long CHECK_FILL = 3000;
	private String[] excludeCurrency = {"TWDT", "CCCX", "EOS"};
	private static Map<ExchangeName, Product[]> map = new HashMap<>();
	private static int BALANCE_CHECK_TIME = 3;
	private int balance_check_time = 0;

	private static double PNL = 1.005d;
	
	public MAXAutoTrade() {
		super(map);
		initAll();
	}

//	public MAXAutoTrade(Map<ExchangeName, Product[]> map) {
//		super(map);
//		initAll();
//	}
	
	private void initAll() {
		init(map);
		setTolerable();
		analysisProducts();
		setTradeCycle();
		for(String currency : excludeCurrency) {
			log.info("Exclusive currency: {}", currency);
		}
		for (Product[] pp : lst) {
			log.info("{}, {}, {}", pp[0], pp[1], pp[2]);
		}
		scheduledFuture = executorService.scheduleAtFixedRate(new Checker(), CHECK_INTERVAL, CHECK_INTERVAL, TimeUnit.MILLISECONDS);

		tradingStatus = TradingStatus.CHECKBALANCE;
		log.info("Start Auto Trade.");
	}
	
	private void setTolerable() {
		Map<String, Double> map = new HashMap<>();
		map.put("twd", 0.8d);
		map.put("twdt", 0.8d);
		map.put("usdt", 0.03d);
		map.put("eth", 0.0001d);
		map.put("btc", 0.00001d);
		map.put("mith", 1d);
		map.put("trx", 2d);
		balanceChecker.setTolerable(map);
	}

	private void init(Map<ExchangeName, Product[]> map) {
		List<Product> lst = new ArrayList<>();
		for(Product product : lstProducts) {
			boolean isExclusive = false;
			String[] currencies = product.name().split("_");
			for(String currency : excludeCurrency) {
				if(currencies[0].equals(currency) || currencies[1].equals(currency)) {
					isExclusive = true;
				}
			}
			if(!isExclusive) {
				lst.add(product);
			}
		}
		
		Product[] products = new Product[lst.size()];
		
		lst.toArray(products);
		map.put(ExchangeName.MAX, products);

		for (Product product : lst) {
			mapBook.put(product, new Book());
		}
		
		lstProducts = lst;
	}

	private void analysisProducts() {
		for (int i = 0; i < lstProducts.size(); i++) {
			String product1 = lstProducts.get(i).name();
			for (int j = i + 1; j < lstProducts.size(); j++) {
				String product2 = lstProducts.get(j).name();
				String[] p1 = product1.split("_");
				String[] p2 = product2.split("_");
				if (p1[0].equals(p2[0]) || p1[0].equals(p2[1]) || p1[1].equals(p2[0]) || p1[1].equals(p2[1])) {
					for (int k = j + 1; k < lstProducts.size(); k++) {
						String product3 = lstProducts.get(k).name();
						String[] p3 = product3.split("_");
						if (p3[0].equals(p1[0])) {
							// AA
							if (p3[1].equals(p2[0])) {
								// BB
								Product[] p = new Product[3];
								p[0] = lstProducts.get(i);
								p[1] = lstProducts.get(j);
								p[2] = lstProducts.get(k);
								lst.add(p);
								break;
							} else if (p3[1].equals(p2[1])) {
								// CC
								Product[] p = new Product[3];
								p[0] = lstProducts.get(k);
								p[1] = lstProducts.get(j);
								p[2] = lstProducts.get(i);
								lst.add(p);
								break;
							}
						} else if (p3[0].equals(p1[1])) {
							// BB
							if (p3[1].equals(p2[1])) {
								// CC
								Product[] p = new Product[3];
								p[0] = lstProducts.get(j);
								p[1] = lstProducts.get(k);
								p[2] = lstProducts.get(i);
								lst.add(p);
								break;
							}
						} else if (p3[1].equals(p1[0])) {
							// BB
							if (p3[0].equals(p2[0])) {
								// AA
								Product[] p = new Product[3];
								p[0] = lstProducts.get(j);
								p[1] = lstProducts.get(i);
								p[2] = lstProducts.get(k);
								lst.add(p);
								break;
							}
						} else if (p3[1].equals(p1[1])) {
							// CC
							if (p3[0].equals(p2[0])) {
								// AA
								Product[] p = new Product[3];
								p[0] = lstProducts.get(k);
								p[1] = lstProducts.get(i);
								p[2] = lstProducts.get(j);
								lst.add(p);
								break;
							} else if (p3[0].equals(p2[1])) {
								// BB
								Product[] p = new Product[3];
								p[0] = lstProducts.get(i);
								p[1] = lstProducts.get(k);
								p[2] = lstProducts.get(j);
								lst.add(p);
								break;
							}
						}
					}
				}
			}
		}
	}
	
	private void setTradeCycle() {
		for(Product[] products : lst) {
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
		}
	}

	@Override
	public void onBook(ExchangeName exchangeName, Product product, double bidPrice, double bidQty, double askPrice,
			double askQty) {
		Book book = mapBook.get(product);
		book.setbPrice(bidPrice);
		book.setbQty(bidQty);
		book.setsPrice(askPrice);
		book.setsQty(askQty);
	}

	private void checkBalance() {
		Map<String, Double> balance = max.queryBalance();
		if(balanceChecker.isBalanceDecrease(balance)) {
			balance_check_time++;
			if(balance_check_time == BALANCE_CHECK_TIME) {
				tradingStatus = TradingStatus.STOP;
				log.error("Balance decrease!! Emergency stop!");
			}
			return;
		}
		
		balanceChecker.setNewBalance(balance);
		
		StringBuilder sb = new StringBuilder();
		for(String key : balance.keySet()) {
			sb.append("[").append(key).append(":").append(balance.get(key)).append("],");
		}
		log.info("Balance:{}", sb.toString());
		
		tradingStatus = TradingStatus.CLEAN;
	}

	@Override
	public void onTick() {
	}

	@Override
	public void onOrderAck(int orderId) {
		switch(tradingStatus) {
		case PAIR1SEND:
			if(this.orderId == orderId) {
				tradingStatus = TradingStatus.PAIR1ACK;
				log.info("Pair 1 acked.");
			} else {
				log.error("!!!!!! unknow ack ! [{}]", orderId);
			}
			break;
		case PAIR2SEND:
			if(this.orderId == orderId) {
				tradingStatus = TradingStatus.PAIR2ACK;
				log.info("Pair 2 acked.");
			} else {
					log.error("!!!!!! unknow ack ! [{}]", orderId);
				}
			break;
		case PAIR3SEND:
			if(this.orderId == orderId) {
				tradingStatus = TradingStatus.PAIR3ACK;
				log.info("Pair 3 acked.");
			} else {
				log.error("!!!!!! unknow ack ! [{}]", orderId);
			}
			break;
		default:
			log.error("!!!!!! unknow ack !!!!!! [{}]", orderId);
			break;
		}
	}

	@Override
	public void onOrderRej(int orderId) {
		log.error("!!!!!! order reject !!!!!! [{}]", orderId);
		tradingStatus = TradingStatus.CHECKBALANCE;
	}

	@Override
	public void onCancelAck(int orderId) {
		log.info("order canceled. [{}]", orderId);
		tradingStatus = TradingStatus.CHECKBALANCE;
	}

	@Override
	public void onCancelRej(int orderId) {
		log.error("!!!!!! cancel reject !!!!!! [{}]", orderId);
		tradingStatus = TradingStatus.CHECKBALANCE;
	}

	@Override
	public void onFill(int orderId, double price, double qty) {
		switch(tradingStatus) {
		case PAIR1ACK:
			if(this.orderId == orderId) {
				tradingStatus = TradingStatus.PAIR1FILL;
				log.info("Pair 1 filled.");
				sendOrder(2);
			} else {
				log.error("!!!!!! unknow fill !!!!!! [{}] {}@{}", orderId, qty, price);
			}
			break;
		case PAIR2ACK:
			if(this.orderId == orderId) {
				tradingStatus = TradingStatus.PAIR2FILL;
				log.info("Pair 2 filled.");
				sendOrder(3);
			} else {
				log.error("!!!!!! unknow fill !!!!!! [{}] {}@{}", orderId, qty, price);
			}
			break;
		case PAIR3ACK:
			if(this.orderId == orderId) {
				tradingStatus = TradingStatus.PAIR3FILL;
				log.info("Pair 3 filled.");
				tradingStatus = TradingStatus.CHECKBALANCE;
			} else {
				log.error("!!!!!! unknow fill !!!!!! [{}] {}@{}", orderId, qty, price);
			}
			break;
		default:
			log.error("!!!!!! unknow fill !!!!!! [{}] {}@{}", orderId, qty, price);
			break;
		}
	}

	private class Checker implements Runnable {
		@Override
		public void run() {
			switch (tradingStatus) {
			case STANDBY:
				for (TradeCycle tradeCycle : lstTradeCycle) {
					updatePrice(tradeCycle);
					updateTradeCycle(tradeCycle);
					checkMinSize(tradeCycle);
				}
				for (TradeCycle tradeCycle : lstAvaliable) {
					getMaxPNL(tradeCycle);
				}
				if(maxPnlTradeCycle.getAB() != null) {
					prepareOrder();
				}
				break;
			case PAIR1ACK:
			case PAIR2ACK:
			case PAIR3ACK:
				if(System.currentTimeMillis() > orderTime + CHECK_FILL) {
					log.error("No fill come. Cancel order! [{}]", orderId);
					sendCancel();
				}
				break;
			case CLEAN:
				maxPnlTradeCycle = nullTradeCycle;
				lstAvaliable.clear();
				lstWantedOrder.clear();
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				tradingStatus = TradingStatus.STANDBY;
				break;
			case STOP:
				log.error("Stop strategy.");
				scheduledFuture.cancel(true);
				System.exit(0);
				break;
			case CHECKBALANCE:
				checkBalance();
				break;
			default:
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

		private void updateTradeCycle(TradeCycle tradeCycle) {
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
	}

	private void checkMinSize(TradeCycle tradeCycle) {
		if (checkPNL(tradeCycle)) {
			if (checkMinSize(tradeCycle.getAB()) && checkMinSize(tradeCycle.getAC()) && checkMinSize(tradeCycle.getBC())) {
				lstAvaliable.add(tradeCycle);
			} else {
				checkNextDepth(tradeCycle);
			}
		}
	}
	
	private void checkNextDepth(TradeCycle tradeCycle) {
		
	}

	private boolean checkPNL(TradeCycle tradeCycle) {
		return tradeCycle.getPnl() > PNL;
	}

	private boolean checkMinSize(SidePrice sidePrice) {
		String product = sidePrice.getProduct().name();
		String[] p = product.split("_");
		MaxQuote quote = MaxQuote.valueOf(p[1]);
		return sidePrice.getPrice() * sidePrice.getQty() > quote.getMinValue();
	}

	private void getMaxPNL(TradeCycle tradeCycle) {
		if (tradeCycle.getPnl() > maxPnlTradeCycle.getPnl()) {
			maxPnlTradeCycle = tradeCycle;
		}
	}

	private void prepareOrder() {
		tradingStatus = TradingStatus.TRADING;
		log.info("Find trading pair : {}:{}:{}, PNL:{}", maxPnlTradeCycle.getAB().getProduct(), maxPnlTradeCycle.getAC().getProduct(), maxPnlTradeCycle.getBC().getProduct(), maxPnlTradeCycle.getPnl());
		
		maxPnlTradeCycle = orderUtils.calcTradingQty(maxPnlTradeCycle);
		if(maxPnlTradeCycle.getAB().getSide() == Side.buy) {
			setOrder(maxPnlTradeCycle.getAB(), maxPnlTradeCycle.getAC(), maxPnlTradeCycle.getBC());
		} else {
			setOrder(maxPnlTradeCycle.getAC(), maxPnlTradeCycle.getAB(), maxPnlTradeCycle.getBC());
		}
		
		sendOrder(1);
	}
	
	private void setOrder(SidePrice order1, SidePrice order2, SidePrice order3) {
		lstWantedOrder.add(order1);
		lstWantedOrder.add(order2);
		lstWantedOrder.add(order3);
	}
	
	private void sendOrder(int time) {
		SidePrice sidePrice = lstWantedOrder.get(time-1);
		switch(time) {
		case 1:
			tradingStatus = TradingStatus.PAIR1SEND;
			break;
		case 2:
			tradingStatus = TradingStatus.PAIR2SEND;
			break;
		case 3:
			tradingStatus = TradingStatus.PAIR3SEND;
			break;
		}
		orderId++;
		log.info("Send Order: [{}] {} {}:{}@{}", orderId, sidePrice.getProduct(), sidePrice.getSide(), sidePrice.getQty(), sidePrice.getPrice());
		orderTime = System.currentTimeMillis();
		max.sendOrder(orderId, sidePrice.getProduct(), sidePrice.getSide(), sidePrice.getPrice(), sidePrice.getQty(), this);
	}
	
	private void sendCancel() {
		max.sendCancel(orderId, this);
	}

	@Override
	public boolean stop() {
		tradingStatus = TradingStatus.STOP;
		return true;
	}

	@Override
	public void onBook(ExchangeName exchangeName, Product product, MarketBook marketBook) {
		// TODO Auto-generated method stub
		
	}
}