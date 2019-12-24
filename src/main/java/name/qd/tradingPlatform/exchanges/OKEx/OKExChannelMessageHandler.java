package name.qd.tradingPlatform.exchanges.OKEx;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import name.qd.tradingPlatform.exchanges.ChannelMessageHandler;
import name.qd.tradingPlatform.exchanges.book.MarketBook;
import name.qd.tradingPlatform.product.FileProductMapperManager;
import name.qd.tradingPlatform.product.ProductMapper;
import name.qd.tradingPlatform.strategies.Strategy;
import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Side;

public class OKExChannelMessageHandler extends ChannelMessageHandler {
	private static Logger log = LoggerFactory.getLogger(OKExChannelMessageHandler.class);
	private final OKExExchange exchange;
	private Map<String, List<Strategy>> mapStrategies;
	private final Map<String, MarketBook> mapBooks = new ConcurrentHashMap<>();
	private Map<Long, Integer> mapExOrderIdToOrderId;
	private final FileProductMapperManager productMapper;
	private final ExchangeName exchangeName = ExchangeName.OKEx;
	private Map<Long, Strategy> mapExOrderIdToStrategy;
	
	public OKExChannelMessageHandler(OKExExchange exchange, Map<String, List<Strategy>> mapStrategies, FileProductMapperManager productMapper, Map<Long, Integer> mapExOrderIdToOrderId, Map<Long, Strategy> mapIdToStrategy) {
		this.exchange = exchange;
		this.mapStrategies = mapStrategies;
		this.productMapper = productMapper;
		this.mapExOrderIdToOrderId = mapExOrderIdToOrderId;
		this.mapExOrderIdToStrategy = mapIdToStrategy;
	}
	
	public void processMessage(JsonNode jsonNode) {
		for(JsonNode node : jsonNode) {
			if(node.has("channel")) {
				String channel = node.get("channel").textValue();
				if(channel.endsWith("_depth")) {
					processBook(channel, node);
				} else if(channel.startsWith("ok_sub_spot_")) {
					processOrderEvent(node);
				} else if(channel.equals("login")) {
					processLogin(node);
				}
			}
		}
	}
	
	private void processBook(String channel, JsonNode jsonNode) {
		String product = StringUtils.replace(StringUtils.replace(channel, "ok_sub_spot_", ""), "_depth", "");
		if(!mapBooks.containsKey(product)) {
			mapBooks.put(product, new MarketBook());
		}
		MarketBook marketBook = mapBooks.get(product);
		updateBook(marketBook, jsonNode);
		
		List<Strategy> lst = mapStrategies.get(product);
		for(Strategy strategy : lst) {
			strategy.onBook(exchangeName, productMapper.getProduct(product, exchangeName), marketBook.topPrice(Side.buy, 1)[0], marketBook.topQty(Side.buy, 1)[0], marketBook.topPrice(Side.sell, 1)[0], marketBook.topQty(Side.sell, 1)[0]);
		}
	}
	
	private void updateBook(MarketBook book, JsonNode node) {
		JsonNode dataNode = node.get("data");
		
		JsonNode bidNodes = dataNode.get("bids");
		if(bidNodes != null) {
			for(JsonNode buyNode : bidNodes) {
				double price = buyNode.get(0).asDouble();
				double size = buyNode.get(1).asDouble();
				if(size == 0)
					book.removeQuote(Side.buy, price);
				else 
					book.upsertQuote(Side.buy, price, size);
			}
		}
		
		JsonNode askNodes = dataNode.get("asks");
		if(askNodes != null) {
			for(JsonNode askNode : askNodes) {
				double price = askNode.get(0).asDouble();
				double size = askNode.get(1).asDouble();
				if(size == 0)
					book.removeQuote(Side.sell, price);
				else 
					book.upsertQuote(Side.sell, price, size);
			}
		}
	}
	
	private void processOrderEvent(JsonNode node) {
		JsonNode dataNode = node.get("data");
		int status = dataNode.get("status").asInt();
		if(status == 2) {
			processFill(dataNode);
		} else if(status == 1) {
			processFill(dataNode);
		} else if(status == 0) {
		} else if(status == -1) {
		} else {
		}
	}
	
	private void processFill(JsonNode node) {
		long exOrderId = node.get("orderId").asLong();
		double qty = node.get("sigTradeAmount").asDouble();
		double price = node.get("sigTradePrice").asDouble();
		int orderId = mapExOrderIdToOrderId.get(exOrderId);
		Strategy strategy = mapExOrderIdToStrategy.get(exOrderId);
		strategy.onFill(orderId ,price, qty);
	}
	
	private void processLogin(JsonNode node) {
		JsonNode dataNode = node.get("data");
		boolean isLogin = dataNode.get("result").asBoolean();
		if(isLogin) {
			log.info("Login to OKEx websocket success.");
			exchange.subscribeTradeInfo();
		} else {
			String errorMsg = dataNode.get("error_msg").asText();
			String errorCode = dataNode.get("error_code").asText();
			log.info("Login to OKEx websocket failed. ErrorMsg:{}, ErrorCode:{}", errorMsg, errorCode);
		}
	}

	@Override
	public void reconnect() {
	}
}
