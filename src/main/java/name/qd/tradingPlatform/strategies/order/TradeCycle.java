package name.qd.tradingPlatform.strategies.order;

import name.qd.tradingPlatform.Constants.Product;
import name.qd.tradingPlatform.Constants.Side;

public class TradeCycle {
	private SidePrice ab;
	private SidePrice ac;
	private SidePrice bc;
	private double pnl;
	
	public TradeCycle(Product product1, Side side1, Product product2, Product product3) {
		String[] p1 = product1.name().split("_");
		String[] p2 = product2.name().split("_");
		String[] p3 = product3.name().split("_");
		
		ab = new SidePrice();
		ac = new SidePrice();
		bc = new SidePrice();
		
		if(p1[0].equals(p2[0])) {
			bc.setProduct(product3);
			if(p1[1].equals(p3[0])) {
				ab.setProduct(product1);
				ac.setProduct(product2);
				ab.setSide(side1);
				ac.setSide(side1.switchSide());
				bc.setSide(side1);
			} else {
				ac.setProduct(product1);
				ab.setProduct(product2);
				ab.setSide(side1.switchSide());
				ac.setSide(side1);
				bc.setSide(side1.switchSide());
			}
		} else if(p1[0].equals(p3[0])) {
			bc.setProduct(product2);
			if(p1[1].equals(p2[0])) {
				ab.setProduct(product1);
				ac.setProduct(product3);
				ab.setSide(side1);
				ac.setSide(side1.switchSide());
				bc.setSide(side1);
			} else {
				ac.setProduct(product1);
				ab.setProduct(product3);
				ab.setSide(side1.switchSide());
				ac.setSide(side1);
				bc.setSide(side1.switchSide());
			}
		} else if(p2[0].equals(p3[0])) {
			bc.setProduct(product1);
			if(p2[1].equals(p1[0])) {
				ab.setProduct(product2);
				ac.setProduct(product3);
			} else {
				ac.setProduct(product2);
				ab.setProduct(product3);
			}
			ab.setSide(side1);
			ac.setSide(side1.switchSide());
			bc.setSide(side1);
		}
	}
	
	public TradeCycle(SidePrice ac, SidePrice bc, SidePrice ab) {
		this.ab = ab;
		this.ac = ac;
		this.bc = bc;
	}
	
	public SidePrice getAB() {
		return ab;
	}
	
	public SidePrice getAC() {
		return ac;
	}
	
	public SidePrice getBC() {
		return bc;
	}
	
	public void setPnl(double pnl) {
		this.pnl = pnl;
	}
	
	public double getPnl() {
		return pnl;
	}
}
