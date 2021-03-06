package name.qd.tradingPlatform.exchanges.Binance;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;
import name.qd.tradingPlatform.exchanges.ChannelMessageHandler;
import name.qd.tradingPlatform.product.FileProductMapperManager;
import name.qd.tradingPlatform.strategies.Strategy;

public class BinanceChannelMessageHandler extends ChannelMessageHandler {
	private Map<String, List<Strategy>> mapStrategies;
	private final FileProductMapperManager productMapperManager;
	private final ExchangeName exchangeName;
	
	public BinanceChannelMessageHandler(Map<String, List<Strategy>> mapStrategies, FileProductMapperManager productMapper, ExchangeName exchangeName) {
		this.mapStrategies = mapStrategies;
		this.productMapperManager = productMapper;
		this.exchangeName = exchangeName;
	}
	
	@Override
	public void processMessage(JsonNode jsonNode) {
		String productString = jsonNode.get("stream").asText().replace("@depth5", "").toUpperCase();
		Product product = productMapperManager.getProduct(productString, exchangeName);
		
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
