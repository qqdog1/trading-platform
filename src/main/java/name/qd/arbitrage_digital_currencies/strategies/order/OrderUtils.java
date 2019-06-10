package name.qd.arbitrage_digital_currencies.strategies.order;

import name.qd.arbitrage_digital_currencies.Constants.Side;

public class OrderUtils {

	public TradeCycle calcTradingQty(TradeCycle tradeCycle) {
		SidePrice sidePriceAB = new SidePrice();
		SidePrice sidePriceAC = new SidePrice();
		SidePrice sidePriceBC = new SidePrice();
		TradeCycle newTradeCycle = new TradeCycle(sidePriceAC, sidePriceBC, sidePriceAB);
		newTradeCycle.setPnl(tradeCycle.getPnl());
		// buy sell 本來是市場的 會被弄成要執行的行為
		double ABVolume = tradeCycle.getAB().getQty() * tradeCycle.getAB().getPrice();
		double BCVolume = tradeCycle.getBC().getQty() * tradeCycle.getBC().getPrice();
		double ACVolume = tradeCycle.getAC().getQty() * tradeCycle.getAC().getPrice();

		switch (tradeCycle.getAB().getSide()) {
		case buy:
			if (tradeCycle.getAB().getQty() > tradeCycle.getAC().getQty()) {
				if (ACVolume > BCVolume) {
					// BC
					sidePriceAC.setProduct(tradeCycle.getAC().getProduct());
					sidePriceAC.setSide(Side.buy);
					sidePriceAC.setPrice(tradeCycle.getAC().getPrice());
					sidePriceAC.setQty(BCVolume / tradeCycle.getAC().getPrice());
					sidePriceAB.setProduct(tradeCycle.getAB().getProduct());
					sidePriceAB.setSide(Side.sell);
					sidePriceAB.setPrice(tradeCycle.getAB().getPrice());
					sidePriceAB.setQty(BCVolume / tradeCycle.getAC().getPrice());
					sidePriceBC.setProduct(tradeCycle.getBC().getProduct());
					sidePriceBC.setSide(Side.sell);
					sidePriceBC.setPrice(tradeCycle.getBC().getPrice());
					sidePriceBC.setQty(tradeCycle.getBC().getQty());
				} else {
					// AC
					sidePriceAC.setProduct(tradeCycle.getAC().getProduct());
					sidePriceAC.setSide(Side.buy);
					sidePriceAC.setPrice(tradeCycle.getAC().getPrice());
					sidePriceAC.setQty(tradeCycle.getAC().getQty());
					sidePriceAB.setProduct(tradeCycle.getAB().getProduct());
					sidePriceAB.setSide(Side.sell);
					sidePriceAB.setPrice(tradeCycle.getAB().getPrice());
					sidePriceAB.setQty(tradeCycle.getAC().getQty());
					sidePriceBC.setProduct(tradeCycle.getBC().getProduct());
					sidePriceBC.setSide(Side.sell);
					sidePriceBC.setPrice(tradeCycle.getBC().getPrice());
					sidePriceBC.setQty(ACVolume/tradeCycle.getBC().getPrice());
				}
			} else {
				if (ABVolume > tradeCycle.getBC().getQty()) {
					// BC
					sidePriceAC.setProduct(tradeCycle.getAC().getProduct());
					sidePriceAC.setSide(Side.buy);
					sidePriceAC.setPrice(tradeCycle.getAC().getPrice());
					sidePriceAC.setQty(BCVolume / tradeCycle.getAC().getPrice());
					sidePriceAB.setProduct(tradeCycle.getAB().getProduct());
					sidePriceAB.setSide(Side.sell);
					sidePriceAB.setPrice(tradeCycle.getAB().getPrice());
					sidePriceAB.setQty(BCVolume / tradeCycle.getAC().getPrice());
					sidePriceBC.setProduct(tradeCycle.getBC().getProduct());
					sidePriceBC.setSide(Side.sell);
					sidePriceBC.setPrice(tradeCycle.getBC().getPrice());
					sidePriceBC.setQty(tradeCycle.getBC().getQty());
				} else {
					// AB
					sidePriceAC.setProduct(tradeCycle.getAC().getProduct());
					sidePriceAC.setSide(Side.buy);
					sidePriceAC.setPrice(tradeCycle.getAC().getPrice());
					sidePriceAC.setQty(tradeCycle.getAB().getQty());
					sidePriceAB.setProduct(tradeCycle.getAB().getProduct());
					sidePriceAB.setSide(Side.sell);
					sidePriceAB.setPrice(tradeCycle.getAB().getPrice());
					sidePriceAB.setQty(tradeCycle.getAB().getQty());
					sidePriceBC.setProduct(tradeCycle.getBC().getProduct());
					sidePriceBC.setSide(Side.sell);
					sidePriceBC.setPrice(tradeCycle.getBC().getPrice());
					sidePriceBC.setQty(tradeCycle.getAB().getQty() * tradeCycle.getAB().getPrice());
				}
			}
			break;
		case sell:
			if (tradeCycle.getAB().getQty() > tradeCycle.getAC().getQty()) {
				if (ACVolume > BCVolume) {
					// BC
					sidePriceAB.setProduct(tradeCycle.getAB().getProduct());
					sidePriceAB.setSide(Side.buy);
					sidePriceAB.setPrice(tradeCycle.getAB().getPrice());
					sidePriceAB.setQty(BCVolume / tradeCycle.getAC().getPrice());
					sidePriceAC.setProduct(tradeCycle.getAC().getProduct());
					sidePriceAC.setSide(Side.sell);
					sidePriceAC.setPrice(tradeCycle.getAC().getPrice());
					sidePriceAC.setQty(BCVolume / tradeCycle.getAC().getPrice());
					sidePriceBC.setProduct(tradeCycle.getBC().getProduct());
					sidePriceBC.setSide(Side.buy);
					sidePriceBC.setPrice(tradeCycle.getBC().getPrice());
					sidePriceBC.setQty(tradeCycle.getBC().getQty());
				} else {
					// AC
					sidePriceAB.setProduct(tradeCycle.getAB().getProduct());
					sidePriceAB.setSide(Side.buy);
					sidePriceAB.setPrice(tradeCycle.getAB().getPrice());
					sidePriceAB.setQty(tradeCycle.getAC().getQty());
					sidePriceAC.setProduct(tradeCycle.getAC().getProduct());
					sidePriceAC.setSide(Side.sell);
					sidePriceAC.setPrice(tradeCycle.getAC().getPrice());
					sidePriceAC.setQty(tradeCycle.getAC().getQty());
					sidePriceBC.setProduct(tradeCycle.getBC().getProduct());
					sidePriceBC.setSide(Side.buy);
					sidePriceBC.setPrice(tradeCycle.getBC().getPrice());
					sidePriceBC.setQty(tradeCycle.getAC().getQty() * tradeCycle.getAC().getPrice()/ tradeCycle.getBC().getPrice());
				}
			} else {
				if (ABVolume > tradeCycle.getBC().getQty()) {
					// BC
					sidePriceAB.setProduct(tradeCycle.getAB().getProduct());
					sidePriceAB.setSide(Side.buy);
					sidePriceAB.setPrice(tradeCycle.getAB().getPrice());
					sidePriceAB.setQty(BCVolume / tradeCycle.getAC().getPrice());
					sidePriceAC.setProduct(tradeCycle.getAC().getProduct());
					sidePriceAC.setSide(Side.sell);
					sidePriceAC.setPrice(tradeCycle.getAC().getPrice());
					sidePriceAC.setQty(BCVolume / tradeCycle.getAC().getPrice());
					sidePriceBC.setProduct(tradeCycle.getBC().getProduct());
					sidePriceBC.setSide(Side.buy);
					sidePriceBC.setPrice(tradeCycle.getBC().getPrice());
					sidePriceBC.setQty(tradeCycle.getBC().getQty());
				} else {
					// AB
					sidePriceAB.setProduct(tradeCycle.getAB().getProduct());
					sidePriceAB.setSide(Side.buy);
					sidePriceAB.setPrice(tradeCycle.getAB().getPrice());
					sidePriceAB.setQty(tradeCycle.getAB().getQty());
					sidePriceAC.setProduct(tradeCycle.getAC().getProduct());
					sidePriceAC.setSide(Side.sell);
					sidePriceAC.setPrice(tradeCycle.getAC().getPrice());
					sidePriceAC.setQty(tradeCycle.getAB().getQty());
					sidePriceBC.setProduct(tradeCycle.getBC().getProduct());
					sidePriceBC.setSide(Side.buy);
					sidePriceBC.setPrice(tradeCycle.getBC().getPrice());
					sidePriceBC.setQty(tradeCycle.getAB().getQty() * tradeCycle.getAB().getPrice());
				}
			}
			break;
		}
		return newTradeCycle;
	}
	
	public void calcPnl(TradeCycle tradeCycle) {
		Side abSide = tradeCycle.getAB().getSide();
		double rate = 0;
		double pnl = 0;
		switch(abSide) {
		case buy:
			rate = tradeCycle.getAC().getPrice() / tradeCycle.getBC().getPrice();
			pnl = tradeCycle.getAB().getPrice()/rate;
			break;
		case sell:
			rate = tradeCycle.getAC().getPrice() / tradeCycle.getBC().getPrice();
			pnl = rate/tradeCycle.getAB().getPrice();
			tradeCycle.setPnl(pnl);
			break;
		}
		tradeCycle.setPnl(pnl);
	}
}
