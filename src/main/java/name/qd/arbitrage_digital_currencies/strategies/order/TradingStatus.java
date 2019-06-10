package name.qd.arbitrage_digital_currencies.strategies.order;

public enum TradingStatus {
	START, STANDBY, TRADING, 
	PAIR1SEND, PAIR1ACK, PAIR1FILL, PAIR2SEND, PAIR2ACK, PAIR2FILL, PAIR3SEND, PAIR3ACK, PAIR3FILL, 
	CHECKBALANCE, CLEAN, STOP,
	RESET;
}
