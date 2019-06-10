package name.qd.tradingPlatform.exchanges.BitoPro;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import name.qd.tradingPlatform.exchanges.ChannelMessageHandler;
import name.qd.tradingPlatform.strategies.Strategy;
import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;

public class BitoChannelMessageHandler extends ChannelMessageHandler {
	private ExchangeName exchangeName = ExchangeName.BitoPro;
	private final List<Strategy> lstStrategies;
	private double POWER = 100000000;
	private Product product;

	public BitoChannelMessageHandler(List<Strategy> lstStrategies, Product product) {
		this.lstStrategies = lstStrategies;
		this.product = product;
	}
	
	@Override
	public void processMessage(JsonNode jsonNode) {
		int type = jsonNode.get(0).asInt();
		if(type == 51) {
			JsonNode dataNode = jsonNode.get(3);
			JsonNode bidNode = dataNode.get(0);
			JsonNode askNode = dataNode.get(1);
			double bidPrice = bidNode.get(0).get(0).asDouble();
			bidPrice /= POWER;
			double bidQty = bidNode.get(0).get(1).asDouble();
			bidQty /= POWER;
			double askPrice = askNode.get(0).get(0).asDouble();
			askPrice /= POWER;
			double askQty = askNode.get(0).get(1).asDouble();
			askQty /= POWER;
			for(Strategy strategy : lstStrategies) {
				strategy.onBook(exchangeName, product, bidPrice, bidQty, askPrice, askQty);
			}
		}
	}

	@Override
	public void reconnect() {
		
	}
}
