package name.qd.tradingPlatform;

public class Constants {
	public enum Side {
		buy, sell;
		public Side switchSide() {
			switch(this) {
			case buy:
				return sell;
			case sell:
				return buy;
			}
			return null;
		}
	}
	
	public enum ExchangeName {
		Binance, 
		Bitfinex, 
		HitBTC, 
		Poloniex, 
		Bittrex, 
		Huobi, 
		Kraken,
		Bitstamp, 
		KuCoin,
		ZB,
		OKEx,
		MAX,
		BitoPro,
		;
	}
	
	public enum Product {
		BTC_USD,
		ETH_USD,
		BCH_USD,
		ETC_USD,
		BNB_USD,
		LTC_USD,
		NEO_USD,
		MITH_USD,
		TRX_USD,
		CCCX_USD,
		BAT_USD,
		ZRX_USD,
		GNT_USD,
		OMG_USD,
		KNC_USD,
		EOS_USD,
		
		BTC_USDT,
		ETH_USDT,
		BCH_USDT,
		ETC_USDT,
		BNB_USDT,
		LTC_USDT,
		NEO_USDT,
		MITH_USDT,
		TRX_USDT,
		CCCX_USDT,
		BAT_USDT,
		ZRX_USDT,
		GNT_USDT,
		OMG_USDT,
		KNC_USDT,
		EOS_USDT,
		XRP_USDT,
		MAX_USDT,
		BTG_USDT,
		BITO_USDT,
		
		BTC_TWD,
		ETH_TWD,
		LTC_TWD,
		BCH_TWD,
		MITH_TWD,
		USDT_TWD,
		TRX_TWD,
		CCCX_TWD,
		BTG_TWD,
		BCD_TWD,
		BITO_TWD,
		CGP_TWD,
		SDA_TWD,
		PAL_TWD,
		BAT_TWD,
		ZRX_TWD,
		GNT_TWD,
		OMG_TWD,
		KNC_TWD,
		EOS_TWD,
		XRP_TWD,
		PANDA_TWD,
		
		BTC_TWDT,
		ETH_TWDT,
		LTC_TWDT,
		BCH_TWDT,
		MITH_TWDT,
		USDT_TWDT,
		TRX_TWDT,
		CCCX_TWDT,
		PAL_TWDT,
		BAT_TWDT,
		ZRX_TWDT,
		GNT_TWDT,
		OMG_TWDT,
		KNC_TWDT,
		EOS_TWDT,
		MAX_TWDT,
		
		ETH_BTC,
		BNB_BTC,
		LTC_BTC,
		NEO_BTC,
		BCH_BTC,
		XMR_BTC,
		TRX_BTC,
		MITH_BTC,
		CCCX_BTC,
		PAL_BTC,
		BAT_BTC,
		ZRX_BTC,
		GNT_BTC,
		OMG_BTC,
		KNC_BTC,
		EOS_BTC,
		MAX_BTC,
		
		BNB_ETH,
		LTC_ETH,
		NEO_ETH,
		BCH_ETH,
		XMR_ETH,
		MITH_ETH,
		TRX_ETH,
		CCCX_ETH,
		BITO_ETH,
		PAL_ETH,
		BAT_ETH,
		ZRX_ETH,
		GNT_ETH,
		OMG_ETH,
		KNC_ETH,
		EOS_ETH,
		MAX_ETH,
		PANDA_ETH,
		QNTU_ETH,
		NPXS_ETH,
		BTG_ETH,
		TWDT_ETH,
		
		LTC_MAX,
		BCH_MAX,
		MITH_MAX,
		TRX_MAX,
		CCCX_MAX,
		PAL_MAX,
		EOS_MAX,
		BAT_MAX,
		ZRX_MAX,
		GNT_MAX,
		OMG_MAX,
		KNC_MAX,
		XRP_MAX,
		FMF_MAX,
		
		ETC_BCH,
		;
	}
	
	public enum FiatMoney {
		USD, TWD,
		;
	}
}
