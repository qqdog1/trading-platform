package name.qd.arbitrage_digital_currencies.exchanges;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class ExchangeWebSocketListener extends WebSocketListener {
	private static Logger log = LoggerFactory.getLogger(ExchangeWebSocketListener.class);
	private ChannelMessageHandler channelMessageHandler;
	
	public ExchangeWebSocketListener(ChannelMessageHandler channelMessageHandler) {
		this.channelMessageHandler = channelMessageHandler;
	}
	
	@Override
	public void onOpen(WebSocket socket, Response response) {
		log.info("OPEN websocket");
	}
	
	@Override
	public void onMessage(WebSocket webSocket, String text) {
		channelMessageHandler.onMessage(text);
	}
	
	@Override
	public void onClosed(WebSocket webSocket, int code, String reason) {
		log.info("CLOSE websocket, {}", reason);
	}
	
	@Override
	public void onFailure(WebSocket webSocket, Throwable t, Response response) {
		log.error("websocket connect failed. {}", t);
		channelMessageHandler.reconnect();
	}
}
