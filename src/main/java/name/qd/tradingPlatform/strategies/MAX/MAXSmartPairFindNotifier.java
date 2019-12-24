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

import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;
import name.qd.tradingPlatform.Constants.Side;
import name.qd.tradingPlatform.exchanges.ExchangeManager;
import name.qd.tradingPlatform.exchanges.book.MarketBook;
import name.qd.tradingPlatform.product.FileProductMapperManager;
import name.qd.tradingPlatform.strategies.Book;
import name.qd.tradingPlatform.strategies.Strategy;
import name.qd.tradingPlatform.strategies.frame.PriceNotifyFrame;
import name.qd.tradingPlatform.strategies.frame.SingleMessageFrame;
import name.qd.tradingPlatform.strategies.order.SidePrice;
import name.qd.tradingPlatform.strategies.order.TradeCycle;

public class MAXSmartPairFindNotifier extends Strategy {
	private static Logger log = LoggerFactory.getLogger(MAXSmartPairFindNotifier.class);
	private static int CHECK_INTERVAL = 1000;
	private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> scheduledFuture;
	private FileProductMapperManager productMapper = ExchangeManager.getInstance().getFileProductMapperManager();
	private List<Product> lstProducts = productMapper.getProducts(ExchangeName.MAX);
	private Map<Product, Book> mapBook = new HashMap<>();
	private List<Product[]> lst = new ArrayList<>();
	private List<TradeCycle> lstTradeCycle = new ArrayList<>();
	private PriceNotifyFrame frame;
	private SingleMessageFrame msgFrame;
	private static Map<ExchangeName, Product[]> map = new HashMap<>();
	
	public MAXSmartPairFindNotifier() {
		super(map);
		this.frame = new PriceNotifyFrame();
		this.msgFrame = new SingleMessageFrame();
		init(map);
		analysisProducts();
		setToFrame();
		for(Product[] pp : lst) {
			log.info("{}, {}, {}", pp[0], pp[1], pp[2]);
		}
		scheduledFuture = executorService.scheduleAtFixedRate(new Checker(), CHECK_INTERVAL, CHECK_INTERVAL, TimeUnit.MILLISECONDS);
	}

//	public MAXSmartPairFindNotifier(Map<ExchangeName, Product[]> map) {
//		super(map);
//		
//	}
	
	private void init(Map<ExchangeName, Product[]> map) {
		Product[] products = new Product[lstProducts.size()];
		lstProducts.toArray(products);
		map.put(ExchangeName.MAX, products);
		
		for(Product product : lstProducts) {
			mapBook.put(product, new Book());
		}
	}
	
	private void analysisProducts() {
		for(int i = 0 ; i < lstProducts.size() ; i++) {
			String product1 = lstProducts.get(i).name();
			for(int j = i+1 ; j < lstProducts.size() ; j++) {
				String product2 = lstProducts.get(j).name();
				String[] p1 = product1.split("_");
				String[] p2 = product2.split("_");
				if(p1[0].equals(p2[0]) || p1[0].equals(p2[1]) || p1[1].equals(p2[0]) || p1[1].equals(p2[1])) {
					for(int k = j+1 ; k < lstProducts.size() ; k++) {
						String product3 = lstProducts.get(k).name();
						String[] p3 = product3.split("_");
						if(p3[0].equals(p1[0])) {
							// AA
							if(p3[1].equals(p2[0])) {
								// BB
								Product[] p = new Product[3];
								p[0] = lstProducts.get(i);
								p[1] = lstProducts.get(j);
								p[2] = lstProducts.get(k);
								lst.add(p);
								break;
							} else if(p3[1].equals(p2[1])) {
								// CC
								Product[] p = new Product[3];
								p[0] = lstProducts.get(k);
								p[1] = lstProducts.get(j);
								p[2] = lstProducts.get(i);
								lst.add(p);
								break;
							}
						} else if(p3[0].equals(p1[1])) {
							// BB
							if(p3[1].equals(p2[1])) {
								// CC 
								Product[] p = new Product[3];
								p[0] = lstProducts.get(j);
								p[1] = lstProducts.get(k);
								p[2] = lstProducts.get(i);
								lst.add(p);
								break;
							}
						} else if(p3[1].equals(p1[0])) {
							// BB
							if(p3[0].equals(p2[0])) {
								// AA
								Product[] p = new Product[3];
								p[0] = lstProducts.get(j);
								p[1] = lstProducts.get(i);
								p[2] = lstProducts.get(k);
								lst.add(p);
								break;
							}
						} else if(p3[1].equals(p1[1])) {
							// CC
							if(p3[0].equals(p2[0])) {
								// AA
								Product[] p = new Product[3];
								p[0] = lstProducts.get(k);
								p[1] = lstProducts.get(i);
								p[2] = lstProducts.get(j);
								lst.add(p);
								break;
							} else if(p3[0].equals(p2[1])) {
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
	
	private void setToFrame() {
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
			frame.setTradeCycle(tradeCycle1);
			frame.setTradeCycle(tradeCycle2);
		}
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
	
	private class Checker implements Runnable {
		@Override
		public void run() {
			msgFrame.clear();
			for(TradeCycle tradeCycle : lstTradeCycle) {
				updatePrice(tradeCycle);
				checkPrice(tradeCycle);
			}
			frame.update();
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
		
		private void checkPrice(TradeCycle tradeCycle) {
			Side abSide = tradeCycle.getAB().getSide();
			switch(abSide) {
			case buy:
				if(tradeCycle.getAB().getPrice() != 0 && tradeCycle.getAC().getPrice() != 0 && tradeCycle.getBC().getPrice() != 0) {
					double rate = tradeCycle.getAC().getPrice() / tradeCycle.getBC().getPrice();
					double pnl = tradeCycle.getAB().getPrice()/rate;
					tradeCycle.setPnl(pnl);
					if(rate < tradeCycle.getAB().getPrice()) {
						StringBuilder sb = new StringBuilder();
						sb.append(tradeCycle.getAB().getProduct());
						sb.append(" sell it. ");
						sb.append(tradeCycle.getBC().getProduct());
						sb.append(" sell it. ");
						sb.append(tradeCycle.getAC().getProduct());
						sb.append(" buy it. PNL:");
						sb.append(pnl);
						log.info(sb.toString());
//						log.info("{} sell it. {} sell it. {} buy it. PNL:{}", tradeCycle.getAB().getProduct(), tradeCycle.getBC().getProduct(), tradeCycle.getAC().getProduct(), pnl);
						java.awt.Toolkit.getDefaultToolkit().beep();
						msgFrame.append(sb.toString());
						printVolume(tradeCycle);
					}
				}
				break;
			case sell:
				if(tradeCycle.getAB().getPrice() != 0 && tradeCycle.getAC().getPrice() != 0 && tradeCycle.getBC().getPrice() != 0) {
					double rate = tradeCycle.getAC().getPrice() / tradeCycle.getBC().getPrice();
					double pnl = rate/tradeCycle.getAB().getPrice();
					tradeCycle.setPnl(pnl);
					if(rate > tradeCycle.getAB().getPrice()) {
						StringBuilder sb = new StringBuilder();
						sb.append(tradeCycle.getAB().getProduct());
						sb.append(" buy it. ");
						sb.append(tradeCycle.getBC().getProduct());
						sb.append(" buy it. ");
						sb.append(tradeCycle.getAC().getProduct());
						sb.append(" sell it. PNL:");
						sb.append(pnl);
						log.info(sb.toString());
//						log.info("{} buy it. {} buy it. {} sell it. PNL:{}", tradeCycle.getAB().getProduct(), tradeCycle.getBC().getProduct(), tradeCycle.getAC().getProduct(), pnl);
						java.awt.Toolkit.getDefaultToolkit().beep();
						msgFrame.append(sb.toString());
						printVolume(tradeCycle);
					}
				}
				break;
			default:
				break;
			}
		}
		
		private void printVolume(TradeCycle tradeCycle) {
			Side abSide = tradeCycle.getAB().getSide();
			double ABVolume = tradeCycle.getAB().getQty() * tradeCycle.getAB().getPrice();
			double BCVolume = tradeCycle.getBC().getQty() * tradeCycle.getBC().getPrice();
			double ACVolume = tradeCycle.getAC().getQty() * tradeCycle.getAC().getPrice();
			
			switch(abSide) {
			case buy:
				if(tradeCycle.getAB().getQty() > tradeCycle.getAC().getQty()) {
					if(ACVolume > BCVolume) {
						// BC
						log.info("{} sell {}, {} buy {}, {} sell {}", 
								tradeCycle.getBC().getProduct(), tradeCycle.getBC().getQty(), 
								tradeCycle.getAC().getProduct(), BCVolume/tradeCycle.getAC().getPrice(), 
								tradeCycle.getAB().getProduct(), BCVolume/tradeCycle.getAC().getPrice());
						StringBuilder sb = new StringBuilder();
						sb.append(tradeCycle.getBC().getProduct()).append(" sell ").append(tradeCycle.getBC().getQty());
						sb.append(" ");
						sb.append(tradeCycle.getAC().getProduct()).append(" buy ").append(BCVolume/tradeCycle.getAC().getPrice());
						sb.append(" ");
						sb.append(tradeCycle.getAB().getProduct()).append(" sell ").append(BCVolume/tradeCycle.getAC().getPrice());
						msgFrame.append(sb.toString());
					} else {
						// AC
						log.info("{} buy {}, {} sell {}, {} sell {}", 
								tradeCycle.getAC().getProduct(), tradeCycle.getAC().getQty(), 
								tradeCycle.getAB().getProduct(), tradeCycle.getAC().getQty(), 
								tradeCycle.getBC().getProduct(), tradeCycle.getAC().getPrice() * tradeCycle.getAC().getQty());
						StringBuilder sb = new StringBuilder();
						sb.append(tradeCycle.getAC().getProduct()).append(" buy ").append(tradeCycle.getAC().getQty());
						sb.append(" ");
						sb.append(tradeCycle.getAB().getProduct()).append(" sell ").append(tradeCycle.getAC().getQty());
						sb.append(" ");
						sb.append(tradeCycle.getBC().getProduct()).append(" sell ").append(ACVolume/tradeCycle.getAC().getPrice());
						msgFrame.append(sb.toString());
					}
				} else {
					if(ABVolume > tradeCycle.getBC().getQty()) {
						// BC
						log.info("{} sell {}, {} buy {}, {} sell {}", 
								tradeCycle.getBC().getProduct(), tradeCycle.getBC().getQty(), 
								tradeCycle.getAC().getProduct(), BCVolume/tradeCycle.getAC().getPrice(), 
								tradeCycle.getAB().getProduct(), BCVolume/tradeCycle.getAC().getPrice());
						StringBuilder sb = new StringBuilder();
						sb.append(tradeCycle.getBC().getProduct()).append(" sell ").append(tradeCycle.getBC().getQty());
						sb.append(" ");
						sb.append(tradeCycle.getAC().getProduct()).append(" buy ").append(BCVolume/tradeCycle.getAC().getPrice());
						sb.append(" ");
						sb.append(tradeCycle.getAB().getProduct()).append(" sell ").append(BCVolume/tradeCycle.getAC().getPrice());
						msgFrame.append(sb.toString());
					} else {
						// AB
						log.info("{} sell {}, {} sell {}, {} buy {}", 
								tradeCycle.getAB().getProduct(), tradeCycle.getAB().getQty(), 
								tradeCycle.getBC().getProduct(), tradeCycle.getAB().getQty()*tradeCycle.getAB().getPrice(), 
								tradeCycle.getAC().getProduct(), tradeCycle.getAB().getQty());
						StringBuilder sb = new StringBuilder();
						sb.append(tradeCycle.getAB().getProduct()).append(" sell ").append(tradeCycle.getAB().getQty());
						sb.append(" ");
						sb.append(tradeCycle.getBC().getProduct()).append(" sell ").append(tradeCycle.getAB().getQty()*tradeCycle.getAB().getPrice());
						sb.append(" ");
						sb.append(tradeCycle.getAC().getProduct()).append(" buy ").append(tradeCycle.getAB().getQty());
						msgFrame.append(sb.toString());
					}
				}
				break;
			case sell:
				if(tradeCycle.getAB().getQty() > tradeCycle.getAC().getQty()) {
					if(ACVolume > BCVolume) {
						// BC
						log.info("{} buy {}, {} buy {}, {} sell {}", 
								tradeCycle.getBC().getProduct(), tradeCycle.getBC().getQty(), 
								tradeCycle.getAB().getProduct(), BCVolume/tradeCycle.getAC().getPrice(),
								tradeCycle.getAC().getProduct(), BCVolume/tradeCycle.getAC().getPrice());
						StringBuilder sb = new StringBuilder();
						sb.append(tradeCycle.getBC().getProduct()).append(" buy ").append(tradeCycle.getBC().getQty());
						sb.append(" ");
						sb.append(tradeCycle.getAB().getProduct()).append(" buy ").append(BCVolume/tradeCycle.getAC().getPrice());
						sb.append(" ");
						sb.append(tradeCycle.getAC().getProduct()).append(" sell ").append(BCVolume/tradeCycle.getAC().getPrice());
						msgFrame.append(sb.toString());
					} else {
						// AC
						log.info("{} sell {}, {} buy {}, {} buy {}", 
								tradeCycle.getAC().getProduct(), tradeCycle.getAC().getQty(), 
								tradeCycle.getBC().getProduct(), tradeCycle.getAC().getQty()*tradeCycle.getAC().getPrice()/tradeCycle.getBC().getPrice(),
								tradeCycle.getAB().getProduct(), tradeCycle.getAC().getQty());
						StringBuilder sb = new StringBuilder();
						sb.append(tradeCycle.getAC().getProduct()).append(" sell ").append(tradeCycle.getAC().getQty());
						sb.append(" ");
						sb.append(tradeCycle.getBC().getProduct()).append(" buy ").append(tradeCycle.getAC().getQty()*tradeCycle.getAC().getPrice()/tradeCycle.getBC().getPrice());
						sb.append(" ");
						sb.append(tradeCycle.getAB().getProduct()).append(" buy ").append(tradeCycle.getAC().getQty());
						msgFrame.append(sb.toString());
					}
				} else {
					if(ABVolume > tradeCycle.getBC().getQty()) {
						// BC
						log.info("{} buy {}, {} buy {}, {} sell {}", 
								tradeCycle.getBC().getProduct(), tradeCycle.getBC().getQty(), 
								tradeCycle.getAB().getProduct(), BCVolume/tradeCycle.getAC().getPrice(),
								tradeCycle.getAC().getProduct(), BCVolume/tradeCycle.getAC().getPrice());
						StringBuilder sb = new StringBuilder();
						sb.append(tradeCycle.getBC().getProduct()).append(" buy ").append(tradeCycle.getBC().getQty());
						sb.append(" ");
						sb.append(tradeCycle.getAB().getProduct()).append(" buy ").append(BCVolume/tradeCycle.getAC().getPrice());
						sb.append(" ");
						sb.append(tradeCycle.getAC().getProduct()).append(" sell ").append(BCVolume/tradeCycle.getAC().getPrice());
						msgFrame.append(sb.toString());
					} else {
						// AB
						log.info("{} buy {}, {} sell {}, {} buy {}", 
								tradeCycle.getAB().getProduct(), tradeCycle.getAB().getQty(), 
								tradeCycle.getAC().getProduct(), tradeCycle.getAB().getQty(),
								tradeCycle.getBC().getProduct(), tradeCycle.getAB().getQty()*tradeCycle.getAB().getPrice());
						StringBuilder sb = new StringBuilder();
						sb.append(tradeCycle.getAB().getProduct()).append(" buy ").append(tradeCycle.getAB().getQty());
						sb.append(" ");
						sb.append(tradeCycle.getAC().getProduct()).append(" sell ").append(tradeCycle.getAB().getQty());
						sb.append(" ");
						sb.append(tradeCycle.getBC().getProduct()).append(" buy ").append(tradeCycle.getAB().getQty()*tradeCycle.getAB().getPrice());
						msgFrame.append(sb.toString());
					}
				}
				break;
			}
		}
	}

	@Override
	public boolean stop() {
		scheduledFuture.cancel(true);
		return true;
	}

	@Override
	public void onBook(ExchangeName exchangeName, Product product, MarketBook marketBook) {
		// TODO Auto-generated method stub
		
	}
}
