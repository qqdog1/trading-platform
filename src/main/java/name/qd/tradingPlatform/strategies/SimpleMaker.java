package name.qd.tradingPlatform.strategies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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
import name.qd.tradingPlatform.strategies.order.SidePrice;
import name.qd.tradingPlatform.strategies.order.TradeCycle;
import name.qd.tradingPlatform.strategies.order.TradingStatus;
import name.qd.tradingPlatform.strategies.utils.BalanceChecker;

public class SimpleMaker extends Strategy {
	private static Logger log = LoggerFactory.getLogger(SimpleMaker.class);
	private Map<Product, Book> mapBook = new HashMap<>();
	private Product firstProduct;
	private Side firstProductSide;
	private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> scheduledFuture;
	private FileProductMapperManager productMapper = ExchangeManager.getInstance().getFileProductMapperManager();
	private static int CHECK_INTERVAL = 1000;
	private TradingStatus tradingStatus;
	private BalanceChecker balanceChecker = new BalanceChecker();
	private Exchange max = ExchangeManager.getInstance().getExchange(ExchangeName.MAX);
	private static double PNL = 1.001d;
	private TradeCycle tradeCycle;
	private Map<Product, Double> mapTickSize = new HashMap<>();
	private int orderId = 1;
	private int activeOrderId = 0;
	private SidePrice activeOrder;
	private List<SidePrice> lstWantedOrder = new ArrayList<>();
	
	public SimpleMaker(Map<ExchangeName, Product[]> map, Product firstProduct, Side side) {
		super(map);
		
		this.firstProduct= firstProduct;
		this.firstProductSide = side;
		
		for(ExchangeName exchangeName : map.keySet()) {
			Product[] products = map.get(exchangeName);
			for(Product product : products) {
				mapBook.put(product, new Book());
			}
		}
		
		checkBalance();
		setTolerable();
		initTradeCycle();
		getTickSize();
		
		log.info("{}:{}, {}:{}, {}:{}", 
				tradeCycle.getAB().getSide(), tradeCycle.getAB().getProduct(), 
				tradeCycle.getAC().getSide(), tradeCycle.getAC().getProduct(), 
				tradeCycle.getBC().getSide(), tradeCycle.getBC().getProduct());
		
		setTradingStatus(TradingStatus.START);
		scheduledFuture = executorService.scheduleAtFixedRate(new Checker(), CHECK_INTERVAL, CHECK_INTERVAL, TimeUnit.MILLISECONDS);
	}
	
	private void getTickSize() {
		Map<String, Double> map = max.getTickSize();
		for(String key : map.keySet()) {
			Product product = productMapper.getProduct(key, ExchangeName.MAX);
			if(mapBook.containsKey(product)) {
				mapTickSize.put(product, map.get(key));
			}
		}
	}
	
	private void checkBalance() {
		setTradingStatus(TradingStatus.CHECKBALANCE);
		
		Map<String, Double> balance = max.queryBalance();
		if(balanceChecker.isBalanceDecrease(balance)) {
			setTradingStatus(TradingStatus.STOP);
			log.error("Balance decrease!! Emergency stop!");
			return;
		}
		
		StringBuilder sb = new StringBuilder();
		for(String key : balance.keySet()) {
			sb.append("[").append(key).append(":").append(balance.get(key)).append("],");
		}
		log.info("Balance:{}", sb.toString());
		
		setTradingStatus(TradingStatus.CLEAN);
	}
	
	private void setTolerable() {
		Map<String, Double> map = new HashMap<>();
		map.put("twd", 0.8d);
		map.put("twdt", 0.8d);
		map.put("usdt", 0.03d);
		map.put("eth", 0.0001d);
		map.put("btc", 0.000001d);
		balanceChecker.setTolerable(map);
	}
	
	private void initTradeCycle() {
		Set<Product> set = mapBook.keySet();
		Product[] products = new Product[set.size()];
		set.toArray(products);
		
		String[] p1 = products[0].name().split("_");
		String[] p2 = products[1].name().split("_");
		String[] p3 = products[2].name().split("_");
		if(p1[0].equals(p2[0])) {     // p1 AX p2 AX
			if(p1[1].equals(p3[0])) { // p1 AB p3 BC p2 AC
				initTradeCycle(products[0], products[1], products[2]);
			} else {                  // p1 AC p2 AB p3 BC
				initTradeCycle(products[1], products[0], products[2]);
			}
		} else if(p1[0].equals(p3[0])) {// p1 AX p3 AX p2 BC
			if(p1[1].equals(p2[0])) {
				initTradeCycle(products[0], products[2], products[1]);
			} else {
				initTradeCycle(products[2], products[0], products[1]);
			}
		} else if(p2[0].equals(p3[0])) { // p1 BC
			if(p1[0].equals(p2[1])) {
				initTradeCycle(products[1], products[2], products[0]);
			} else {
				initTradeCycle(products[2], products[1], products[0]);
			}
		}
	}
	
	private void initTradeCycle(Product ab, Product ac, Product bc) {
		SidePrice abSidePrice = new SidePrice();
		SidePrice acSidePrice = new SidePrice();
		SidePrice bcSidePrice = new SidePrice();
		abSidePrice.setProduct(ab);
		acSidePrice.setProduct(ac);
		bcSidePrice.setProduct(bc);
		if(firstProduct == ac) {
			acSidePrice.setSide(firstProductSide);
			abSidePrice.setSide(firstProductSide.switchSide());
			bcSidePrice.setSide(firstProductSide.switchSide());
		} else {
			abSidePrice.setSide(firstProductSide);
			acSidePrice.setSide(firstProductSide.switchSide());
			bcSidePrice.setSide(firstProductSide);
		}
		
		tradeCycle = new TradeCycle(acSidePrice, bcSidePrice, abSidePrice);
	}
	
	private double increaseTick(Product product, double price) {
		return price + mapTickSize.get(product);
	}
	
	private double decreaseTick(Product product, double price) {
		return price - mapTickSize.get(product);
	}
	
	private boolean isPriceDiff(Product product, double price1, double price2) {
		return Math.abs(price1-price2) > mapTickSize.get(product);
	}
	
	private void updatePrice() {
		updatePrice(tradeCycle.getAB());
		updatePrice(tradeCycle.getAC());
		updatePrice(tradeCycle.getBC());
	}
	
	private void updatePriceWithOrder() {
		updatePriceWithOrder(tradeCycle.getAB());
		updatePriceWithOrder(tradeCycle.getAC());
		updatePriceWithOrder(tradeCycle.getBC());
	}
	
	private void updatePriceWithOrder(SidePrice sidePrice) {
		Product product = sidePrice.getProduct();
		Book book = mapBook.get(product);
		switch(sidePrice.getSide()) {
		case buy:
			if(product == firstProduct) {
				sidePrice.setPrice(book.getbPrice());
			} else {
				sidePrice.setPrice(book.getsPrice());
				sidePrice.setQty(book.getsQty());
			}
			break;
		case sell:
			if(product == firstProduct) {
				sidePrice.setPrice(book.getsPrice());
			} else {
				sidePrice.setPrice(book.getbPrice());
				sidePrice.setQty(book.getbQty());
			}
			break;
		}
	}
	
	private void updatePrice(SidePrice sidePrice) {
		Product product = sidePrice.getProduct();
		Book book = mapBook.get(product);
		switch(sidePrice.getSide()) {
		case buy:
			if(product == firstProduct) {
				sidePrice.setPrice(increaseTick(firstProduct, book.getbPrice()));
			} else {
				sidePrice.setPrice(book.getsPrice());
				sidePrice.setQty(book.getsQty());
			}
			break;
		case sell:
			if(product == firstProduct) {
				sidePrice.setPrice(decreaseTick(firstProduct, book.getsPrice()));
			} else {
				sidePrice.setPrice(book.getbPrice());
				sidePrice.setQty(book.getbQty());
			}
			break;
		}
	}

	private void calcPnl() {
		Side abSide = tradeCycle.getAB().getSide();
		double rate = 0;
		double pnl = 0;
		switch(abSide) {
		case sell:
			rate = tradeCycle.getAC().getPrice() / tradeCycle.getBC().getPrice();
			pnl = tradeCycle.getAB().getPrice()/rate;
			break;
		case buy:
			rate = tradeCycle.getAC().getPrice() / tradeCycle.getBC().getPrice();
			pnl = rate/tradeCycle.getAB().getPrice();
			break;
		}
		tradeCycle.setPnl(pnl);
	}
	
	private void prepareOrder() {
		calcOrderQty();
	}
	
	private void calcOrderQty() {
		SidePrice ab = tradeCycle.getAB();
		SidePrice ac = tradeCycle.getAC();
		SidePrice bc = tradeCycle.getBC();
		
		double abVolume = ab.getPrice() * ab.getQty();
		double acVolume = ac.getPrice() * ac.getQty();
		double bcVolume = bc.getPrice() * bc.getQty();
		
		if(firstProduct == ab.getProduct()) {
			if(acVolume <= bcVolume) {
				ab.setQty(ac.getQty());
			} else {
				ab.setQty(bcVolume / ac.getPrice());
			}
		} else if(firstProduct == ac.getProduct()) {
			if(abVolume <= bc.getQty()) {
				ac.setQty(ab.getQty());
			} else {
				ac.setQty(bcVolume / ac.getPrice());
			}
		} else {
			if(ab.getQty() > ac.getQty()) {
				bc.setQty(acVolume / bc.getPrice());
			} else {
				bc.setQty(abVolume);
				
			}
		}
	}
	
	private void prepareNextOrder(double qty, double price, int count) {
		SidePrice sidePrice = lstWantedOrder.get(count);
		Product lastProduct = activeOrder.getProduct();
		Product thisProduct = sidePrice.getProduct();
		String[] p1 = lastProduct.name().split("_");
		String[] p2 = thisProduct.name().split("_");
		
		switch(activeOrder.getSide()) {
		case buy:
			switch(sidePrice.getSide()) {
			case buy:
				if(p1[0].equals(p2[1])) {
					// BC AB
					sidePrice.setQty(qty / sidePrice.getPrice());
				} else if(p1[1].equals(p2[0])) {
					// AB BC
					sidePrice.setQty(qty * price);
				} else {
					log.error("Unknown product pair. {}:{}, {}:{}", activeOrder.getProduct(), activeOrder.getSide(), sidePrice.getProduct(), sidePrice.getSide());
					setTradingStatus(TradingStatus.STOP);
					return;
				}
				break;
			case sell:
				if(p1[0].equals(p2[0])) {
					sidePrice.setQty(qty);
				} else if(p1[1].equals(p2[1])) {
					double cQty = price * qty;
					sidePrice.setQty(cQty / sidePrice.getPrice());
				} else {
					log.error("Unknown product pair. {}:{}, {}:{}", activeOrder.getProduct(), activeOrder.getSide(), sidePrice.getProduct(), sidePrice.getSide());
					setTradingStatus(TradingStatus.STOP);
					return;
				}
				break;
			}
			break;
		case sell:
			switch(sidePrice.getSide()) {
			case buy:
				if(p1[0].equals(p2[0])) {
					sidePrice.setQty(qty);
				} else if(p1[1].equals(p2[1])) {
					double cQty = price * qty;
					sidePrice.setQty(cQty / sidePrice.getPrice());
				} else {
					log.error("Unknown product pair. {}:{}, {}:{}", activeOrder.getProduct(), activeOrder.getSide(), sidePrice.getProduct(), sidePrice.getSide());
					setTradingStatus(TradingStatus.STOP);
					return;
				}
				break;
			case sell:
				if(p1[0].equals(p2[1])) {
					// BC AB
					sidePrice.setQty(qty / sidePrice.getPrice());
				} else if(p1[1].equals(p2[0])) {
					// AB BC
					sidePrice.setQty(qty * price);
				} else {
					log.error("Unknown product pair. {}:{}, {}:{}", activeOrder.getProduct(), activeOrder.getSide(), sidePrice.getProduct(), sidePrice.getSide());
					setTradingStatus(TradingStatus.STOP);
					return;
				}
				break;
			}
			break;
		}
		if(count == 1) {
			setTradingStatus(TradingStatus.PAIR2SEND);
		} else if(count == 2) {
			setTradingStatus(TradingStatus.PAIR3SEND);
		}
		sendOrder(sidePrice);
	}
	
	private boolean isNeedResend() {
		boolean isNeedResend = false;
		calcOrderQty();
		if(tradeCycle.getAB().getProduct() == firstProduct) {
			isNeedResend = comparePrice(tradeCycle.getAB()) ? true : compareQty(tradeCycle.getAB());
		} else if(tradeCycle.getAC().getProduct()== firstProduct) {
			isNeedResend = comparePrice(tradeCycle.getAC()) ? true : compareQty(tradeCycle.getAC());
		} else {
			isNeedResend = comparePrice(tradeCycle.getBC()) ? true : compareQty(tradeCycle.getBC());
		}
		return isNeedResend;
	}
	
	private boolean comparePrice(SidePrice sidePrice) {
		if(!isPriceDiff(sidePrice.getProduct(), sidePrice.getPrice(), activeOrder.getPrice())) {
			return false;
		} else if(sidePrice.getPrice() > activeOrder.getPrice()) {
			log.info("Price change.");
			return true;
		}
		log.error("Unknow price case ?!");
		return true;
	}
	
	private boolean compareQty(SidePrice sidePrice) {
		boolean qtyDiff = sidePrice.getQty() != activeOrder.getQty();
		if(qtyDiff) {
			log.info("Qty change.");
		}
		return qtyDiff;
	}
	
	private void sendFirstOrder() {
		setTradingStatus(TradingStatus.PAIR1SEND);
		if(firstProduct == tradeCycle.getAB().getProduct()) {
			switch(tradeCycle.getAB().getSide()) {
			case buy:
				lstWantedOrder.add(tradeCycle.getAB());
				lstWantedOrder.add(tradeCycle.getAC());
				lstWantedOrder.add(tradeCycle.getBC());
				break;
			case sell:
				lstWantedOrder.add(tradeCycle.getAB());
				lstWantedOrder.add(tradeCycle.getBC());
				lstWantedOrder.add(tradeCycle.getAC());
				break;
			}
			sendOrder(tradeCycle.getAB());
		} else if(firstProduct == tradeCycle.getAC().getProduct()) {
			switch(tradeCycle.getAC().getSide()) {
			case buy:
				lstWantedOrder.add(tradeCycle.getAC());
				lstWantedOrder.add(tradeCycle.getAB());
				lstWantedOrder.add(tradeCycle.getBC());
				break;
			case sell:
				lstWantedOrder.add(tradeCycle.getAC());
				lstWantedOrder.add(tradeCycle.getBC());
				lstWantedOrder.add(tradeCycle.getAB());
				break;
			}
			sendOrder(tradeCycle.getAC());
		} else {
			switch(tradeCycle.getBC().getSide()) {
			case buy:
				lstWantedOrder.add(tradeCycle.getBC());
				lstWantedOrder.add(tradeCycle.getAB());
				lstWantedOrder.add(tradeCycle.getAC());
				break;
			case sell:
				lstWantedOrder.add(tradeCycle.getBC());
				lstWantedOrder.add(tradeCycle.getAC());
				lstWantedOrder.add(tradeCycle.getAB());
				break;
			}
			sendOrder(tradeCycle.getBC());
		}
	}
	
	private void sendOrder(SidePrice sidePrice) {
		activeOrderId = orderId;
		activeOrder = new SidePrice();
		activeOrder.setPrice(sidePrice.getPrice());
		activeOrder.setProduct(sidePrice.getProduct());
		activeOrder.setSide(sidePrice.getSide());
		activeOrder.setQty(sidePrice.getQty());
		max.sendOrder(orderId, sidePrice.getProduct(), sidePrice.getSide(), sidePrice.getPrice(), sidePrice.getQty(), this);
		log.info("Send Order: [{}] {} {}:{}@{}", orderId, sidePrice.getProduct(), sidePrice.getSide(), sidePrice.getQty(), sidePrice.getPrice());
		orderId++;
	}
	
	private void sendCancel(int orderId) {
		max.sendCancel(orderId, this);
		log.info("Cancel order : [{}]", orderId);
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
		if(orderId == activeOrderId) {
			switch(getTradingStatus()) {
			case PAIR1SEND:
				setTradingStatus(TradingStatus.PAIR1ACK);
				break;
			case PAIR2SEND:
				setTradingStatus(TradingStatus.PAIR2ACK);
				break;
			case PAIR3SEND:
				setTradingStatus(TradingStatus.PAIR3ACK);
				break;
			default:
				log.error("Receive ack at not expected status. [{}] {}", orderId, getTradingStatus());
				break;
			}
		} else {
			log.error("Unknow order {}", orderId);
		}
	}
	@Override
	public void onOrderRej(int orderId) {
		log.error("!!!!!! order reject !!!!!! [{}]", orderId);
		setTradingStatus(TradingStatus.STOP);
	}

	@Override
	public void onCancelAck(int orderId) {
		log.info("Order cancel : [{}]", orderId);
	}

	@Override
	public void onCancelRej(int orderId) {
		log.error("Cancel rej ?!");
		setTradingStatus(TradingStatus.STOP);
	}

	@Override
	public void onFill(int orderId, double price, double qty) {
		// 處理 partial fill
		if(orderId != activeOrderId) {
			log.error("Unknow fill. [{}]", orderId);
			return;
		}
		
		switch(getTradingStatus()) {
		case PAIR1ACK:
			setTradingStatus(TradingStatus.PAIR1FILL);
			if(activeOrder.getQty() > qty) {
				sendCancel(orderId);
			}
			prepareNextOrder(qty, price, 1);
			break;
		case PAIR2ACK:
			setTradingStatus(TradingStatus.PAIR2FILL);
			prepareNextOrder(qty, price, 2);
			break;
		case PAIR3ACK:
			setTradingStatus(TradingStatus.PAIR3FILL);
			checkBalance();
			break;
		default:
			log.error("Receive fill at not expected status: {}, [{}], {}@{}", getTradingStatus(), orderId, price, qty);
			break;
		}
	}

	@Override
	public boolean stop() {
		scheduledFuture.cancel(true);
		return true;
	}
	
	private void setTradingStatus(TradingStatus tradingStatus) {
		this.tradingStatus = tradingStatus;
		log.info("Status: {}", tradingStatus);
	}
	
	private TradingStatus getTradingStatus() {
		return tradingStatus;
	}
	
	private class Checker implements Runnable {
		@Override
		public void run() {
			switch(getTradingStatus()) {
			case START:
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					log.error("", e);
				}
				setTradingStatus(TradingStatus.STANDBY);
				break;
			case STANDBY:
				updatePrice();
				calcPnl();
				if(tradeCycle.getPnl() > PNL) {
					log.info("Pnl: {}", tradeCycle.getPnl());
					prepareOrder();
					sendFirstOrder();
				}
				break;
			case PAIR1ACK:
				updatePriceWithOrder();
				// TODO 底下的人撤單  & PNL不夠了 都要Cancel
				// 底下的人撤單 BOOK要抓2檔
				if(isNeedResend()) {
					sendCancel(activeOrderId);
					setTradingStatus(TradingStatus.STANDBY);
				}
				break;
			case CLEAN:
				lstWantedOrder.clear();
				setTradingStatus(TradingStatus.STANDBY);
				break;
			case STOP:
				scheduledFuture.cancel(true);
				log.info("Strategy stop.");
				System.exit(0);
				break;
			default:
				break;
			}
		}
	}

	@Override
	public void onBook(ExchangeName exchangeName, Product product, MarketBook marketBook) {
		// TODO Auto-generated method stub
		
	}
}
