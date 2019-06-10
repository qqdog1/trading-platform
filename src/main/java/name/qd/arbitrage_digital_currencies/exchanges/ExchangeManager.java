package name.qd.arbitrage_digital_currencies.exchanges;

import name.qd.arbitrage_digital_currencies.Constants.ExchangeName;
import name.qd.arbitrage_digital_currencies.exchanges.Binance.BinanceExchange;
import name.qd.arbitrage_digital_currencies.exchanges.Bitfinex.BitfinexExchange;
import name.qd.arbitrage_digital_currencies.exchanges.BitoPro.BitoProExchange;
import name.qd.arbitrage_digital_currencies.exchanges.Bitstamp.BitstampExchange;
import name.qd.arbitrage_digital_currencies.exchanges.Bittrex.BittrexExchange;
import name.qd.arbitrage_digital_currencies.exchanges.HitBTC.HitBTCExchange;
import name.qd.arbitrage_digital_currencies.exchanges.Huobi.HuobiExchange;
import name.qd.arbitrage_digital_currencies.exchanges.Kraken.KrakenExchange;
import name.qd.arbitrage_digital_currencies.exchanges.KuCoin.KuCoinExchange;
import name.qd.arbitrage_digital_currencies.exchanges.MAX.MAXExchange;
import name.qd.arbitrage_digital_currencies.exchanges.OKEx.OKExExchange;
import name.qd.arbitrage_digital_currencies.exchanges.Poloniex.PoloniexExchange;
import name.qd.arbitrage_digital_currencies.exchanges.ZB.ZBExchange;
import name.qd.arbitrage_digital_currencies.product.FileProductMapper;
import name.qd.arbitrage_digital_currencies.product.ProductMapper;

public class ExchangeManager {
	private static ExchangeManager instance = new ExchangeManager();
	private ProductMapper productMapper;
	private ExchangeConfigLoader exchangeConfigLoader;
	private Exchange okEx;
	private Exchange binance;
	private Exchange bitfinex;
	private Exchange kucoin;
	private Exchange hitBTC;
	private Exchange bittrex;
	private Exchange kraken;
	private Exchange zb;
	private Exchange poloniex;
	private Exchange bitstamp;
	private Exchange huobi;
	private Exchange max;
	private Exchange bitopro;

	public static ExchangeManager getInstance() {
		return instance;
	}
	
	private ExchangeManager() {
//		exchangeConfigLoader = new HardCodeExchangeConfigLoader();
		exchangeConfigLoader = new FileExchangeConfigLoader();
		productMapper = new FileProductMapper();
	}
	
	public ProductMapper getProductMapper() {
		return productMapper;
	}
	
	public Exchange getExchange(ExchangeName exchangeName) {
		switch(exchangeName) {
			case OKEx:
				if(okEx == null) {
					okEx = new OKExExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), productMapper);
				}
				return okEx;
			case Binance:
				if(binance == null) {
					binance = new BinanceExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), productMapper);
				}
				return binance;
			case Bitfinex:
				if(bitfinex == null) {
					bitfinex = new BitfinexExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), productMapper);
				}
				return bitfinex;
			case KuCoin:
				if(kucoin == null) {
					kucoin = new KuCoinExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), productMapper);
				}
				return kucoin;
			case HitBTC:
				if(hitBTC == null) {
					hitBTC = new HitBTCExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), productMapper);
				}
				return hitBTC;
			case Bittrex:
				if(bittrex == null) {
					bittrex = new BittrexExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), productMapper);
				}
				return bittrex;
			case Kraken:
				if(kraken == null) {
					kraken = new KrakenExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), productMapper);
				}
				return kraken;
			case ZB:
				if(zb == null) {
					zb = new ZBExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), productMapper);
				}
				return zb;
			case Poloniex:
				if(poloniex == null) {
					poloniex = new PoloniexExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), productMapper);
				}
				return poloniex;
			case Bitstamp:
				if(bitstamp == null) {
					bitstamp = new BitstampExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), productMapper);
				}
				return bitstamp;
			case Huobi:
				if(huobi == null) {
					huobi = new HuobiExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), productMapper);
				}
				return huobi;
			case MAX:
				if(max == null) {
					max = new MAXExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), productMapper);
				}
				return max;
			case BitoPro:
				if(bitopro == null) {
					bitopro = new BitoProExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), productMapper);
				}
				return bitopro;
		}
		return null;
	}
}
