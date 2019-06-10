package name.qd.arbitrage_digital_currencies.strategies.order;

public class Order extends SidePrice {
	private int orderId;
	
	public Order() {
	}

	public Order(int orderId, SidePrice sidePrice) {
		this.orderId = orderId;
		setProduct(sidePrice.getProduct());
		setPrice(sidePrice.getPrice());
		setQty(sidePrice.getQty());
		setSide(sidePrice.getSide());
	}
	
	public int getOrderId() {
		return orderId;
	}
	
	public void setOrderId(int orderId) {
		this.orderId = orderId;
	}
}
