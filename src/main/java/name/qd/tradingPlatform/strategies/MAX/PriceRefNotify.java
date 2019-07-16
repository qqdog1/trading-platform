package name.qd.tradingPlatform.strategies.MAX;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import name.qd.tradingPlatform.strategies.Strategy;
import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;
import name.qd.tradingPlatform.exchanges.book.MarketBook;

public class PriceRefNotify extends Strategy {
	private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
	private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
	private int CHECK_INTERVAL = 500;
	private double BEST_RATE = 29.3;
	private double WORST_RATE = 29.9;
	
	private double max_mith_buy;
	private double max_mith_sell;
	private double ok_mith_buy;
	private double ok_mith_sell;
	private double MITH_PRICE_GAP_TWD = 0.4;
	
	private double max_eth_buy;
	private double max_eth_sell;
	private double ok_eth_buy;
	private double ok_eth_sell;
	private double ETH_PRICE_GAP_TWD = 200;
	
	private double max_ltc_buy;
	private double max_ltc_sell;
	private double ok_ltc_buy;
	private double ok_ltc_sell;
	private double LTC_PRICE_GAP_TWD = 5;
	
	public PriceRefNotify(Map<ExchangeName, Product[]> map) {
		super(map);
		
		Product[] productMAX = {Product.MITH_TWD, Product.ETH_TWD/**, Product.BTC_TWD, Product.BCH_TWD, Product.LTC_TWD**/};
		map.put(ExchangeName.MAX, productMAX);
		Product[] productOK = {Product.MITH_USD, Product.ETH_USD/**, Product.BTC_USD, Product.BCH_USD, Product.LTC_USD**/};
		map.put(ExchangeName.OKEx, productOK);
		
		executorService.scheduleAtFixedRate(new Checker(), CHECK_INTERVAL, CHECK_INTERVAL, TimeUnit.MILLISECONDS);
	}

	@Override
	public void onBook(ExchangeName exchangeName, Product product, double bidPrice, double bidQty, double askPrice, double askQty) {
		switch(exchangeName) {
		case MAX:
			switch(product) {
			case MITH_TWD:
				max_mith_buy = bidPrice;
				max_mith_sell = askPrice;
				break;
			case ETH_TWD:
				max_eth_buy = bidPrice;
				max_eth_sell = askPrice;
				break;
			case LTC_TWD:
				max_ltc_buy = bidPrice;
				max_ltc_sell = askPrice;
				break;
			default:
				break;
			}
			break;
		case OKEx:
			switch(product) {
			case MITH_USD:
				ok_mith_buy = bidPrice;
				ok_mith_sell = askPrice;
				break;
			case ETH_USD:
				ok_eth_buy = bidPrice;
				ok_eth_sell = askPrice;
				break;
			case LTC_USD:
				ok_ltc_buy = bidPrice;
				ok_ltc_sell = askPrice;
				break;
			default:
				break;
			}
			break;
		default:
			break;
		}
	}
	
	private void checkMith() {
		// 突然大漲的話 OKEx 價格會高很多 取買價比較低較安全
		double buy = ok_mith_buy * BEST_RATE;
		// OKEx大漲 需要在MAX買 所以取賣價報價
		if(max_mith_sell < buy) {
			String msg = combineString("Mith MAX ", String.valueOf(max_mith_sell), "快買, MAX賣出價:", String.valueOf(buy),
					" MAX b/s:", String.valueOf(max_mith_buy), "/", String.valueOf(max_mith_sell),
					" OKEx b/s:", String.valueOf(ok_mith_buy), "/", String.valueOf(ok_mith_sell), "   -",getCurrentTime());
			System.out.println(msg);
			java.awt.Toolkit.getDefaultToolkit().beep();
		}
		
		// 突然大跌的話 OKEx 價格低 賣價高 取賣價安全
		double sell = ok_mith_sell * WORST_RATE;
		// OKEx先跌 MAX快賣掉 所以看買價 跌好在撿回來
		if(max_mith_buy > sell + 0.4) {
			String msg = combineString("Mith MAX ", String.valueOf(max_mith_buy), "快賣, MAX買回價:", String.valueOf(sell),
					" MAX b/s:", String.valueOf(max_mith_buy), "/", String.valueOf(max_mith_sell),
					" OKEx b/s:", String.valueOf(ok_mith_buy), "/", String.valueOf(ok_mith_sell), "   -",getCurrentTime());
			System.out.println(msg);
			java.awt.Toolkit.getDefaultToolkit().beep();
		}
	}
	
	private void checkETH() {
		double buy = ok_eth_buy * BEST_RATE;
		if(max_eth_sell < buy - ETH_PRICE_GAP_TWD) {
			String msg = combineString("ETH MAX ", String.valueOf(max_eth_sell), "快買, MAX賣出價:", String.valueOf(buy),
					" MAX b/s:", String.valueOf(max_eth_buy), "/", String.valueOf(max_eth_sell),
					" OKEx b/s:", String.valueOf(ok_eth_buy), "/", String.valueOf(ok_eth_sell), "   -",getCurrentTime());
			System.out.println(msg);
			java.awt.Toolkit.getDefaultToolkit().beep();
		}
		
		double sell = ok_eth_sell * WORST_RATE;
		if(max_eth_buy > sell + ETH_PRICE_GAP_TWD) {
			String msg = combineString("ETH MAX ", String.valueOf(max_eth_buy), "快賣, MAX買回價:", String.valueOf(sell),
					" MAX b/s:", String.valueOf(max_eth_buy), "/", String.valueOf(max_eth_sell),
					" OKEx b/s:", String.valueOf(ok_eth_buy), "/", String.valueOf(ok_eth_sell), "   -",getCurrentTime());
			System.out.println(msg);
			java.awt.Toolkit.getDefaultToolkit().beep();
		}
	}
	
	private void checkLTC() {
		
	}
	
	private void checkBTC() {
		
	}
	
	private void checkBCH() {
		
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
	
	private String getCurrentTime() {
		long timestamp = System.currentTimeMillis();
		Date date = new Date(timestamp);
		return sdf.format(date);
	}
	
	private String combineString(String ... strings) {
		StringBuilder sb = new StringBuilder();
		for(String s : strings) {
			sb.append(s);
		}
		return sb.toString();
	}
	
	private class Checker implements Runnable {
		@Override
		public void run() {
			checkMith();
			checkETH();
			checkLTC();
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
