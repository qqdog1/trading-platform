package name.qd.tradingPlatform.exchanges.HitBTC;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Side;
import name.qd.tradingPlatform.exchanges.ChannelMessageHandler;
import name.qd.tradingPlatform.exchanges.book.MarketBook;
import name.qd.tradingPlatform.product.FileProductMapperManager;
import name.qd.tradingPlatform.strategies.Strategy;

public class HitBTCChannelMessageHandler extends ChannelMessageHandler {
	private static Logger log = LoggerFactory.getLogger(HitBTCChannelMessageHandler.class);
	private HitBTCExchange exchange;
	private Map<String, List<Strategy>> mapStrategies;
	private final Map<String, MarketBook> mapBooks = new ConcurrentHashMap<>();
	private ExchangeName exchangeName;
	private FileProductMapperManager productMapper;
	private Map<Integer, Strategy> mapOrderIdToStrategy;
	
	public HitBTCChannelMessageHandler(HitBTCExchange exchange, Map<String, List<Strategy>> mapStrategies, ExchangeName exchangeName, FileProductMapperManager productMapper, Map<Integer, Strategy> mapOrderIdToStrategy) {
		this.exchange = exchange;
		this.mapStrategies = mapStrategies;
		this.exchangeName = exchangeName;
		this.productMapper = productMapper;
		this.mapOrderIdToStrategy = mapOrderIdToStrategy;
	}
	
	@Override
	public void processMessage(JsonNode jsonNode) {
		if(jsonNode.has("method")) {
			String method = jsonNode.get("method").asText();
			switch(method) {
			case "report":
				processReport(jsonNode);
				break;
			case "updateOrderbook":
				processBookUpdate(jsonNode);
				break;
			case "snapshotOrderbook":
				processBookSnapshot(jsonNode);
				break;
			}
		} else if(jsonNode.has("id")) {
			int channelId = jsonNode.get("id").asInt();
			switch(channelId) {
			case HitBTCConstants.CHANNEL_ID_ORDER:
				processAck(jsonNode);
				break;
			case HitBTCConstants.CHANNEL_ID_BALANCE:
				exchange.setBalance(jsonNode);
				break;
			case HitBTCConstants.CHANNEL_ID_LOGIN:
				exchange.afterLogin();
				break;
			}
		}
	}
	
	private void processReport(JsonNode node) {
		JsonNode resultNode = node.get("result");
		int orderId = resultNode.get("clientOrderId").asInt();
		String status = resultNode.get("status").asText();
		switch(status) {
		case "filled":
		case "partiallyFilled":
			double price = resultNode.get("tradePrice").asDouble();
			double qty = resultNode.get("tradeQuantity").asDouble();
			mapOrderIdToStrategy.get(orderId).onFill(orderId, price, qty);
			break;
		case "expired":
			mapOrderIdToStrategy.get(orderId).onCancelAck(orderId);
			break;
		case "new":
			mapOrderIdToStrategy.get(orderId).onOrderAck(orderId);
			break;
		default:
			log.error("unknow message {}", node.toString());
			break;
		}
	}
	
	private void processAck(JsonNode node) {
		if(node.has("result")) {
			JsonNode resultNode = node.get("result");
			int orderId = resultNode.get("clientOrderId").asInt();
			mapOrderIdToStrategy.get(orderId).onOrderAck(orderId);
		} else {
			// order rej
			log.error("order failed. {}", node.toString());
		}
	}
	
	private void processBookUpdate(JsonNode node) {
		JsonNode dataNode = node.get("params");
		String productString = dataNode.get("symbol").asText();
		MarketBook marketBook = mapBooks.get(productString);
		updateBook(marketBook, dataNode);
		List<Strategy> lstStrategys = mapStrategies.get(productString);
		for(Strategy strategy : lstStrategys) {
			strategy.onBook(exchangeName, productMapper.getProduct(productString, exchangeName), marketBook.topPrice(Side.buy, 1)[0], marketBook.topQty(Side.buy, 1)[0], marketBook.topPrice(Side.sell, 1)[0], marketBook.topQty(Side.sell, 1)[0]);
		}
	}
	
	private void processBookSnapshot(JsonNode node) {
		JsonNode dataNode = node.get("params");
		String productString = dataNode.get("symbol").asText();
		MarketBook marketBook = new MarketBook();
		mapBooks.put(productString, marketBook);
		updateBook(marketBook, dataNode);
	}
	
	private void updateBook(MarketBook book, JsonNode node) {
		JsonNode bidNodes = node.get("bid");
		if(bidNodes != null) {
			for(JsonNode buyNode : bidNodes) {
				double price = buyNode.get("price").asDouble();
				double size = buyNode.get("size").asDouble();
				if(size == 0)
					book.removeQuote(Side.buy, price);
				else 
					book.upsertQuote(Side.buy, price, size);
			}
		}
		
		JsonNode askNodes = node.get("ask");
		if(askNodes != null) {
			for(JsonNode askNode : askNodes) {
				double price = askNode.get("price").asDouble();
				double size = askNode.get("size").asDouble();
				if(size == 0)
					book.removeQuote(Side.sell, price);
				else 
					book.upsertQuote(Side.sell, price, size);
			}
		}
	}

	@Override
	public void reconnect() {
	}
}
