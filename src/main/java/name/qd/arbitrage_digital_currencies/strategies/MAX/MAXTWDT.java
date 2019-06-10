package name.qd.arbitrage_digital_currencies.strategies.MAX;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.arbitrage_digital_currencies.Constants.ExchangeName;
import name.qd.arbitrage_digital_currencies.Constants.Product;
import name.qd.arbitrage_digital_currencies.exchanges.ExchangeManager;
import name.qd.arbitrage_digital_currencies.product.ProductMapper;
import name.qd.arbitrage_digital_currencies.strategies.Book;
import name.qd.arbitrage_digital_currencies.strategies.Strategy;

public class MAXTWDT extends Strategy {
	private static Logger log = LoggerFactory.getLogger(MAXTWDT.class);
	private List<Product[]> lst = new ArrayList<>();
	private static int CHECK_INTERVAL = 1000;
	private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
	private ProductMapper productMapper = ExchangeManager.getInstance().getProductMapper();
	private List<Product> lstProducts = productMapper.getProducts(ExchangeName.MAX);
	private Map<Product, Book> mapBook = new HashMap<>();
	private String[] baseCurrency = {"TWD", "TWDT"};
	
	public MAXTWDT(Map<ExchangeName, Product[]> map) {
		super(map);
		analysisProducts();
		for(Product[] pp : lst) {
			log.info("{}, {}, {}, {}", pp[0], pp[1], pp[2], pp[3]);
		}
		initBookAndMap(map);
		executorService.scheduleAtFixedRate(new Checker(), CHECK_INTERVAL, CHECK_INTERVAL, TimeUnit.MILLISECONDS);
	}

	private void analysisProducts() {
		for(int i = 0 ; i < lstProducts.size() ; i++) {
			String product1 = lstProducts.get(i).name();
			if(isBaseOnPair(product1)) {
				for(int j = i+1 ; j < lstProducts.size() ; j++) {
					String product2 = lstProducts.get(j).name();
					if(isBaseOnPair(product2)) {
						String[] p1 = product1.split("_");
						String[] p2 = product2.split("_");
						if(p1[1].equals(p2[1])) {
							// same quote, diff base AC BC
							for(int k = j+1 ; k < lstProducts.size() ; k++) {
								String product3 = lstProducts.get(k).name();
								if(isBaseOnPair(product3)) {
									String[] p3 = product3.split("_");
									if(p1[0].equals(p3[0])) { // AD
										for(int l = k+1 ; l < lstProducts.size() ; l++) {
											String product4 = lstProducts.get(l).name();
											if(isBaseOnPair(product4)) {
												String[] p4 = product4.split("_");
												if(p2[0].equals(p4[0])) {
													Product[] p = new Product[4];
													p[0] = lstProducts.get(i);
													p[1] = lstProducts.get(j);
													p[2] = lstProducts.get(k);
													p[3] = lstProducts.get(l);
													lst.add(p);
													break;
												}
											}
										}
									} else if(p2[0].equals(p3[0])) { // BD
										for(int l = k+1 ; l < lstProducts.size() ; l++) {
											String product4 = lstProducts.get(l).name();
											if(isBaseOnPair(product4)) {
												String[] p4 = product4.split("_");
												if(p1[0].equals(p4[0])) {
													Product[] p = new Product[4];
													p[0] = lstProducts.get(i);
													p[1] = lstProducts.get(j);
													p[2] = lstProducts.get(l);
													p[3] = lstProducts.get(k);
													lst.add(p);
													break;
												}
											}
										}
									}
								}
							}
						} else {
							// diff quote
							if(p1[0].equals(p2[0])) {
								// same base AC AD
								for(int k = j+1 ; k < lstProducts.size() ; k++) {
									String product3 = lstProducts.get(k).name();
									if(isBaseOnPair(product3)) {
										String[] p3 = product3.split("_");
										if(p1[1].equals(p3[1])) { // BC
											for(int l = k+1 ; l < lstProducts.size() ; l++) {
												String product4 = lstProducts.get(l).name();
												if(isBaseOnPair(product4)) {
													String[] p4 = product4.split("_");
													if(p4[0].equals(p3[0]) && p4[1].equals(p2[1])) {
														Product[] p = new Product[4];
														p[0] = lstProducts.get(i);
														p[1] = lstProducts.get(k);
														p[2] = lstProducts.get(j);
														p[3] = lstProducts.get(l);
														lst.add(p);
														break;
													}
												}
											}
										} else if(p2[1].equals(p3[1])) { // BD
											for(int l = k+1 ; l < lstProducts.size() ; l++) {
												String product4 = lstProducts.get(l).name();
												if(isBaseOnPair(product4)) {
													String[] p4 = product4.split("_");
													if(p4[0].equals(p3[0]) && p4[1].equals(p1[1])) {
														Product[] p = new Product[4];
														p[0] = lstProducts.get(i);
														p[1] = lstProducts.get(l);
														p[2] = lstProducts.get(j);
														p[3] = lstProducts.get(k);
														lst.add(p);
														break;
													}
												}
											}
										}
									}
								}
							} else {
								// AC BD
								for(int k = j+1 ; k < lstProducts.size() ; k++) {
									String product3 = lstProducts.get(k).name();
									if(isBaseOnPair(product3)) {
										String[] p3 = product3.split("_");
										if(p3[0].equals(p1[0]) && p3[1].equals(p2[1])) {
											// AD
											for(int l = k+1 ; l < lstProducts.size() ; l++) {
												String product4 = lstProducts.get(l).name();
												if(isBaseOnPair(product4)) {
													String[] p4 = product4.split("_");
													if(p4[0].equals(p2[0]) && p4[1].equals(p1[1])) {
														Product[] p = new Product[4];
														p[0] = lstProducts.get(i);
														p[1] = lstProducts.get(l);
														p[2] = lstProducts.get(k);
														p[3] = lstProducts.get(j);
														lst.add(p);
														break;
													}
												}
											}
										} else if(p3[0].equals(p2[0]) && p3[1].equals(p1[1])) {
											// BC
											for(int l = k+1 ; l < lstProducts.size() ; l++) {
												String product4 = lstProducts.get(l).name();
												if(isBaseOnPair(product4)) {
													String[] p4 = product4.split("_");
													if(p4[0].equals(p1[0]) && p4[1].equals(p2[1])) {
														Product[] p = new Product[4];
														p[0] = lstProducts.get(i);
														p[1] = lstProducts.get(k);
														p[2] = lstProducts.get(l);
														p[3] = lstProducts.get(j);
														lst.add(p);
														break;
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	private void initBookAndMap(Map<ExchangeName, Product[]> map) {
		List<Product> lstAll = new ArrayList<>();
		for(Product[] products : lst) {
			for(Product product : products) {
				if(!mapBook.containsKey(product)) {
					mapBook.put(product, new Book());
					lstAll.add(product);
				}
			}
		}
		Product[] pp = new Product[lstAll.size()];
		lstAll.toArray(pp);
		map.put(ExchangeName.MAX, pp);
	}
	
	private boolean isBaseOnPair(String product) {
		return product.endsWith(baseCurrency[0]) || product.endsWith(baseCurrency[1]);
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
			for(Product[] products : lst) {
				checkProduct(products);
			}
		}
		
		private void checkProduct(Product[] products) {
			Book ACBook = mapBook.get(products[0]);
			Book BCBook = mapBook.get(products[1]);
			Book ADBook = mapBook.get(products[2]);
			Book BDBook = mapBook.get(products[3]);
			
			if(ACBook.getsPrice() != 0 && BCBook.getbPrice() != 0 && ADBook.getbPrice() != 0 && BDBook.getsPrice() != 0) {
				double a = BCBook.getbPrice()/ACBook.getsPrice();
				double b = BDBook.getsPrice()/ADBook.getbPrice();
				if(a > b) {
					log.info("{} buy, {} sell, {} buy, {} sell, pnl:{}", products[0], products[2], products[3], products[1], a/b);
				}
			}
			
			if(ACBook.getbPrice() != 0 && BCBook.getsPrice() != 0 && ADBook.getsPrice() != 0 && BDBook.getbPrice() != 0) {
				double a = BCBook.getsPrice()/ACBook.getbPrice();
				double b = BDBook.getbPrice()/ADBook.getsPrice();
				if(a < b) {
					log.info("{} sell, {} buy, {} sell, {} buy, pnl:{}", products[0], products[2], products[3], products[1], b/a);
				}
			}
		}
	}

	@Override
	public boolean stop() {
		return true;
	}
}
