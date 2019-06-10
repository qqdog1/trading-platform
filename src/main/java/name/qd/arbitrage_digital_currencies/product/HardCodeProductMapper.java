package name.qd.arbitrage_digital_currencies.product;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.qd.arbitrage_digital_currencies.Constants.ExchangeName;
import name.qd.arbitrage_digital_currencies.Constants.Product;

public class HardCodeProductMapper implements ProductMapper {
	private Map<String, Product> mapOKExProduct = new HashMap<>();
	private Map<Product, String> mapOKExString = new HashMap<>();
	
	private Map<String, Product> mapBinanceProduct = new HashMap<>();
	private Map<Product, String> mapBinanceString = new HashMap<>(); 
	
	private Map<String, Product> mapBitfinexProduct = new HashMap<>();
	private Map<Product, String> mapBitfinexString = new HashMap<>();  
	
	public HardCodeProductMapper() {
		mapOKExProduct.put("btc_usdt", Product.BTC_USD);
		mapOKExProduct.put("eth_usdt", Product.ETH_USD);
		mapOKExProduct.put("eth_btc", Product.ETH_BTC);
		mapOKExProduct.put("bch_usdt", Product.BCH_USD);
		mapOKExProduct.put("etc_usdt", Product.ETC_USD);
		mapOKExProduct.put("etc_bch", Product.ETC_BCH);
		
		mapOKExString.put(Product.BTC_USD, "btc_usdt");
		mapOKExString.put(Product.ETH_USD, "eth_usdt");
		mapOKExString.put(Product.ETH_BTC, "eth_btc");
		mapOKExString.put(Product.BCH_USD, "bch_usdt");
		mapOKExString.put(Product.ETC_USD, "etc_usdt");
		mapOKExString.put(Product.ETC_BCH, "etc_bch");
		
		mapBinanceProduct.put("btcusdt", Product.BTC_USD);
		mapBinanceProduct.put("ethusdt", Product.ETH_USD);
		mapBinanceProduct.put("ethbtc", Product.ETH_BTC);
		mapBinanceProduct.put("bnbusdt", Product.BNB_USD);
		mapBinanceProduct.put("neousdt", Product.NEO_USD);
		mapBinanceProduct.put("ltcusdt", Product.LTC_USD);
		mapBinanceProduct.put("bccusdt", Product.BCH_USD);
		mapBinanceProduct.put("bnbbtc", Product.BNB_BTC);
		mapBinanceProduct.put("bccbtc", Product.BCH_BTC);
		mapBinanceProduct.put("ltcbtc", Product.LTC_BTC);
		mapBinanceProduct.put("neobtc", Product.NEO_BTC);
		mapBinanceProduct.put("bnbeth", Product.BNB_ETH);
		mapBinanceProduct.put("bcceth", Product.BCH_ETH);
		mapBinanceProduct.put("ltceth", Product.LTC_ETH);
		mapBinanceProduct.put("neoeth", Product.NEO_ETH);
		mapBinanceProduct.put("xmreth", Product.XMR_ETH);
		mapBinanceProduct.put("xmrbtc", Product.XMR_BTC);
		
		mapBinanceString.put(Product.BTC_USD, "btcusdt");
		mapBinanceString.put(Product.ETH_USD, "ethusdt");
		mapBinanceString.put(Product.ETH_BTC, "ethbtc");
		mapBinanceString.put(Product.BNB_USD, "bnbusdt");
		mapBinanceString.put(Product.NEO_USD, "neousdt");
		mapBinanceString.put(Product.LTC_USD, "ltcusdt");
		mapBinanceString.put(Product.BCH_USD, "bccusdt");
		mapBinanceString.put(Product.BNB_BTC, "bnbbtc");
		mapBinanceString.put(Product.BCH_BTC, "bccbtc");
		mapBinanceString.put(Product.LTC_BTC, "ltcbtc");
		mapBinanceString.put(Product.NEO_BTC, "neobtc");
		mapBinanceString.put(Product.BNB_ETH, "bnbeth");
		mapBinanceString.put(Product.BCH_ETH, "bcceth");
		mapBinanceString.put(Product.LTC_ETH, "ltceth");
		mapBinanceString.put(Product.NEO_ETH, "neoeth");
		mapBinanceString.put(Product.XMR_ETH, "xmreth");
		mapBinanceString.put(Product.XMR_BTC, "xmrbtc");
		
		mapBitfinexProduct.put("ETHBTC", Product.ETH_BTC);
		
		mapBitfinexString.put(Product.ETH_BTC, "ETHBTC");
	}
	
	@Override
	public String getExchangeProductString(Product product, ExchangeName exchangeName) {
		switch(exchangeName) {
			case OKEx:
				return mapOKExString.get(product);
			case Binance:
				return mapBinanceString.get(product);
			case Bitfinex:
				return mapBitfinexString.get(product);
			default:
				break;
		}
		return null;
	}
	
	@Override
	public Product getProduct(String product, ExchangeName exchangeName) {
		switch(exchangeName) {
			case OKEx:
				return mapOKExProduct.get(product);
			case Binance:
				return mapBinanceProduct.get(product);
			case Bitfinex:
				return mapBitfinexProduct.get(product);
			default:
				break;
		}
		return null;
	}

	@Override
	public List<Product> getProducts(ExchangeName exchangeName) {
		return null;
	}
}
