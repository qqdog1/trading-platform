package name.qd.tradingPlatform.exchanges.Binance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import name.qd.tradingPlatform.exchanges.ChannelMessageHandler;
import name.qd.tradingPlatform.product.ProductMapper;
import name.qd.tradingPlatform.strategies.Strategy;
import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;

public class BinanceChannelMessageHandler extends ChannelMessageHandler {
	private Map<String, List<Strategy>> mapStrategies = new HashMap<>();
	private final ProductMapper productMapper;
	private final ExchangeName exchangeName;
	
	public BinanceChannelMessageHandler(Map<String, List<Strategy>> mapStrategies, ProductMapper productMapper, ExchangeName exchangeName) {
		this.mapStrategies = mapStrategies;
		this.productMapper = productMapper;
		this.exchangeName = exchangeName;
	}
	
	@Override
	public void processMessage(JsonNode jsonNode) {
		String productString = jsonNode.get("stream").asText().replace("@depth5", "").toUpperCase();
		Product product = productMapper.getProduct(productString, exchangeName);
		
		JsonNode dataNode = jsonNode.get("data");
		JsonNode bDep1 = dataNode.get("bids").get(0);
		double bPrice = bDep1.get(0).asDouble();
		double bQty = bDep1.get(1).asDouble();
		JsonNode aDep1 = dataNode.get("asks").get(0);
		double aPrice = aDep1.get(0).asDouble();
		double aQty = aDep1.get(1).asDouble();
		
		List<Strategy> lst = mapStrategies.get(productString);
		for(Strategy strategy : lst) {
			strategy.onBook(exchangeName, product, bPrice, bQty, aPrice, aQty);
		}
	}
	
	@Override
	public void reconnect() {
	}
}
