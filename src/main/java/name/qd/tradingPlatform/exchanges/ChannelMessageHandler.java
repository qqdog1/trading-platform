package name.qd.tradingPlatform.exchanges;

import java.util.concurrent.ConcurrentLinkedDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import name.qd.tradingPlatform.utils.JsonUtils;

public abstract class ChannelMessageHandler implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(ChannelMessageHandler.class);
	private final ConcurrentLinkedDeque<String> queue = new ConcurrentLinkedDeque<>();
	private ObjectMapper objectMapper = JsonUtils.getObjectMapper();
	
	public void onMessage(String text) {
		queue.offer(text);
	}
	
	@Override
	public void run() {
		String message;
		while (!Thread.currentThread().isInterrupted()) {
			if((message = queue.poll())!=null) {
				try {
					processMessage(objectMapper.readTree(message));
				}catch(Exception e) {
					if(!"UNLOGIN_USER connect success".contentEquals(message))
						log.error("failed to process message",e);
				}
			}
		}	
	}
	
	public abstract void processMessage(JsonNode jsonNode);
	public abstract void reconnect();
}
