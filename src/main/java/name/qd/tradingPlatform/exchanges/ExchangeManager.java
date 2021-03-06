package name.qd.tradingPlatform.exchanges;

import name.qd.tradingPlatform.exchanges.BTSE.BTSEExchange;
import name.qd.tradingPlatform.exchanges.Binance.BinanceExchange;
import name.qd.tradingPlatform.exchanges.Bitfinex.BitfinexExchange;
import name.qd.tradingPlatform.exchanges.BitoPro.BitoProExchange;
import name.qd.tradingPlatform.exchanges.Bitstamp.BitstampExchange;
import name.qd.tradingPlatform.exchanges.Bittrex.BittrexExchange;
import name.qd.tradingPlatform.exchanges.Deribit.DeribitExchange;
import name.qd.tradingPlatform.exchanges.HitBTC.HitBTCExchange;
import name.qd.tradingPlatform.exchanges.Huobi.HuobiExchange;
import name.qd.tradingPlatform.exchanges.Kraken.KrakenExchange;
import name.qd.tradingPlatform.exchanges.KuCoin.KuCoinExchange;
import name.qd.tradingPlatform.exchanges.MAX.MAXExchange;
import name.qd.tradingPlatform.exchanges.OKEx.OKExExchange;
import name.qd.tradingPlatform.exchanges.Poloniex.PoloniexExchange;
import name.qd.tradingPlatform.exchanges.ZB.ZBExchange;
import name.qd.tradingPlatform.product.FileProductMapper;
import name.qd.tradingPlatform.product.FileProductMapperManager;
import name.qd.tradingPlatform.product.ProductMapper;
import name.qd.tradingPlatform.Constants.ExchangeName;

public class ExchangeManager {
	private static ExchangeManager instance = new ExchangeManager();
	private FileProductMapperManager fileProductMapperManager;
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
	private Exchange deribit;
	private Exchange btse;

	public static ExchangeManager getInstance() {
		return instance;
	}
	
	private ExchangeManager() {
//		exchangeConfigLoader = new HardCodeExchangeConfigLoader();
		exchangeConfigLoader = new FileExchangeConfigLoader();
		fileProductMapperManager = new FileProductMapperManager();
	}
	
	public FileProductMapperManager getFileProductMapperManager() {
		return fileProductMapperManager;
	}
	
	public Exchange getExchange(ExchangeName exchangeName) {
		switch(exchangeName) {
			case OKEx:
				if(okEx == null) {
					okEx = new OKExExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), fileProductMapperManager);
				}
				return okEx;
			case Binance:
				if(binance == null) {
					binance = new BinanceExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), fileProductMapperManager);
				}
				return binance;
			case Bitfinex:
				if(bitfinex == null) {
					bitfinex = new BitfinexExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), fileProductMapperManager);
				}
				return bitfinex;
			case KuCoin:
				if(kucoin == null) {
					kucoin = new KuCoinExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), fileProductMapperManager);
				}
				return kucoin;
			case HitBTC:
				if(hitBTC == null) {
					hitBTC = new HitBTCExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), fileProductMapperManager);
				}
				return hitBTC;
			case Bittrex:
				if(bittrex == null) {
					bittrex = new BittrexExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), fileProductMapperManager);
				}
				return bittrex;
			case Kraken:
				if(kraken == null) {
					kraken = new KrakenExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), fileProductMapperManager);
				}
				return kraken;
			case ZB:
				if(zb == null) {
					zb = new ZBExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), fileProductMapperManager);
				}
				return zb;
			case Poloniex:
				if(poloniex == null) {
					poloniex = new PoloniexExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), fileProductMapperManager);
				}
				return poloniex;
			case Bitstamp:
				if(bitstamp == null) {
					bitstamp = new BitstampExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), fileProductMapperManager);
				}
				return bitstamp;
			case Huobi:
				if(huobi == null) {
					huobi = new HuobiExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), fileProductMapperManager);
				}
				return huobi;
			case MAX:
				if(max == null) {
					max = new MAXExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), fileProductMapperManager);
				}
				return max;
			case BitoPro:
				if(bitopro == null) {
					bitopro = new BitoProExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), fileProductMapperManager);
				}
				return bitopro;
			case Deribit:
				if(deribit == null) {
					deribit = new DeribitExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), fileProductMapperManager);
				}
				return deribit;
			case BTSE:
				if(btse == null) {
					btse = new BTSEExchange(exchangeConfigLoader.getExchangeConfig(exchangeName), fileProductMapperManager);
				}
				return btse;
		}
		return null;
	}
}
