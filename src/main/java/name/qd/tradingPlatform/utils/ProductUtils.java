package name.qd.tradingPlatform.utils;

import name.qd.tradingPlatform.Constants.ExchangeName;

public class ProductUtils {
	
	public static String getProductFilePath(ExchangeName exchangeName) {
		return "./config/" + exchangeName.name() + "Products.txt";
	}
	
	public static String getProductString(ExchangeName exchangeName, String value) {
		if(exchangeName == ExchangeName.Bittrex) {
			value = value.replace("BCC", "BCH");
			value.replace("-", "");
		}
		if(exchangeName == ExchangeName.Binance) {
			value = value.replace("BCC", "BCH");
		}
		if(exchangeName == ExchangeName.Bitfinex) {
			value = value.replace("dsh", "dash");
			value = value.replace("bcc", "bch");
		}
		if(exchangeName == ExchangeName.Poloniex) {
			value.replace("_", "");
		}
		if(exchangeName == ExchangeName.ZB) {
			value = value.replace("bcc", "bch");
		}
		if(exchangeName == ExchangeName.Kraken) {
			value = value.replace("XBT", "BTC");
		}
		return value.toLowerCase().replace("/", "").replace("_", "").replace("-", "").replace("usdt", "usd");
	}
}
