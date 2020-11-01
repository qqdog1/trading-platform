package name.qd.tradingPlatform.exchanges.BTSE;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.exchanges.ChannelMessageHandler;
import name.qd.tradingPlatform.product.FileProductMapperManager;
import name.qd.tradingPlatform.strategies.Strategy;

public class BTSEChannelMessageHandler extends ChannelMessageHandler {
	private static Logger log = LoggerFactory.getLogger(BTSEChannelMessageHandler.class);
	private BTSEExchange exchange;
	private Map<String, List<Strategy>> mapStrategies;
	private FileProductMapperManager productMapper;
	private final ExchangeName exchangeName = ExchangeName.BTSE;
	
	public BTSEChannelMessageHandler(BTSEExchange exchange, Map<String, List<Strategy>> mapStrategies, FileProductMapperManager productMapper) {
		this.exchange = exchange;
		this.mapStrategies = mapStrategies;
		this.productMapper = productMapper;
	}

	@Override
	public void processMessage(JsonNode jsonNode) {
		String topic = jsonNode.get("topic").asText();
		if(topic.contains("orderBook:")) {
			processBook(topic, jsonNode.get("data"));
		}
	}
	
	private void processBook(String topic, JsonNode node) {
		String product = topic.replace("orderBook:", "").replace("_0", "");
		JsonNode nodeBuyArray = node.get("buyQuote");
		JsonNode nodeSellArray = node.get("sellQuote");
		double buyQty = 0;
		double buyPrice = 0;
		double sellQty = 0;
		double sellPrice = 0;
		if(nodeBuyArray.size() > 0) {
			JsonNode nodeData = nodeBuyArray.get(0);
			buyQty = nodeData.get("size").asDouble();
			buyPrice = nodeData.get("price").asDouble();
		}
		if(nodeSellArray.size() > 0) {
			JsonNode nodeData = nodeSellArray.get(nodeSellArray.size() - 1);
			sellQty = nodeData.get("size").asDouble();
			sellPrice = nodeData.get("price").asDouble();
		}
		
		List<Strategy> lst = mapStrategies.get(product);
		for(Strategy strategy : lst) {
			strategy.onBook(exchangeName, productMapper.getProduct(product, exchangeName), buyPrice, buyQty, sellPrice, sellQty);
		}
	}

	@Override
	public void reconnect() {
		
	}
}
