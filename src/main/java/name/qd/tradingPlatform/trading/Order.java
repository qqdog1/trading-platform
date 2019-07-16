package name.qd.tradingPlatform.trading;

import name.qd.tradingPlatform.Constants.Side;
import name.qd.tradingPlatform.strategies.Strategy;

public class Order {
	public static final int ORDER = 1;
	public static final int CANCEL = 2;
	
	private int orderId;
	private int sequence;
	private int actionType;
	private String exOrderId;
	private String product;
	private Side side;
	private double price;
	private double qty;
	private Strategy strategy;
	
	public Order(int orderId, int sequence, String product, Side side, double price, double qty, Strategy strategy, int actionType) {
		this.orderId = orderId;
		this.sequence = sequence;
		this.side = side;
		this.product = product;
		this.price = price;
		this.qty = qty;
		this.strategy = strategy;
		this.actionType = actionType;
	}
	
	public void onOrderAck() {
		strategy.onOrderAck(orderId);
	}
	
	public void onOrderRej() {
		strategy.onOrderRej(orderId);
	}
	
	public void onCancelAck() {
		strategy.onCancelAck(orderId);
	}
	
	public void onCancelRej() {
		strategy.onCancelRej(orderId);
	}
	
	public String getProduct() {
		return product;
	}
	public void setProduct(String product) {
		this.product = product;
	}
	public int getOrderId() {
		return orderId;
	}
	public void setOrderId(int orderId) {
		this.orderId = orderId;
	}
	public int getSequence() {
		return sequence;
	}
	public void setSequence(int sequence) {
		this.sequence = sequence;
	}
	public int getActionType() {
		return actionType;
	}
	public void setActionType(int actionType) {
		this.actionType = actionType;
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
	public Strategy getStrategy() {
		return strategy;
	}
	public void setStrategy(Strategy strategy) {
		this.strategy = strategy;
	}
	public String getExOrderId() {
		return exOrderId;
	}
	public void setExOrderId(String exOrderId) {
		this.exOrderId = exOrderId;
	}
}
