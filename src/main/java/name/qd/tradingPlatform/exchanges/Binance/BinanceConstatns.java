package name.qd.tradingPlatform.exchanges.Binance;

import name.qd.tradingPlatform.Constants.Side;

public class BinanceConstatns {
	public enum BinanceSide {
		BUY, SELL;
		public static BinanceSide getSide(Side side) {
			switch(side) {
			case buy:
				return BUY;
			case sell:
				return SELL;
			}
			return null;
		}
	}
}
