package name.qd.tradingPlatform.strategies.order;

import name.qd.tradingPlatform.Constants.Product;
import name.qd.tradingPlatform.Constants.Side;

public class SidePrice {
	private Product product;
	private Side side;
	private double price;
	private double qty;
	public Product getProduct() {
		return product;
	}
	public void setProduct(Product product) {
		this.product = product;
	}
	public Side getSide() {
		return side;
	}
	public void setSide(Side side) {
		this.side = side;
	}
	public double getPrice() {
		return price;
	}
	public void setPrice(double price) {
		this.price = price;
	}
	public double getQty() {
		return qty;
	}
	public void setQty(double qty) {
		this.qty = qty;
	}
}
