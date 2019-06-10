package name.qd.arbitrage_digital_currencies.exchanges.Bitfinex;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;

import name.qd.arbitrage_digital_currencies.Constants.ExchangeName;
import name.qd.arbitrage_digital_currencies.Constants.Side;
import name.qd.arbitrage_digital_currencies.exchanges.ChannelMessageHandler;
import name.qd.arbitrage_digital_currencies.exchanges.book.MarketBook;
import name.qd.arbitrage_digital_currencies.product.ProductMapper;
import name.qd.arbitrage_digital_currencies.strategies.Strategy;

public class BitfinexChannelMessageHandler extends ChannelMessageHandler {
	private Map<String, List<Strategy>> mapStrategies = new HashMap<>();
	private final Map<String, MarketBook> mapBooks = new ConcurrentHashMap<>();
	private final ProductMapper productMapper;
	private final ExchangeName exchangeName;
	private Map<Integer, String> mapChannelIdToProduct = new HashMap<>();
	
	public BitfinexChannelMessageHandler(Map<String, List<Strategy>> mapStrategies, ProductMapper productMapper, ExchangeName exchangeName) {
		this.mapStrategies = mapStrategies;
		this.productMapper = productMapper;
		this.exchangeName = exchangeName;
	}
	
	@Override
	public void processMessage(JsonNode jsonNode) {
		if(jsonNode.has("event")) {
			if(jsonNode.has("chanId")) {
				int channelId = jsonNode.get("chanId").asInt();
				String product = jsonNode.get("pair").asText();
				mapChannelIdToProduct.put(channelId, product);
			}
		} else {
			processBook(jsonNode);
		}
	}
	
	private void processBook(JsonNode node) {
		int channelId = node.get(0).asInt();
		String product = mapChannelIdToProduct.get(channelId);
		
		if(!mapBooks.containsKey(product)) {
			mapBooks.put(product, new MarketBook());
		}
		MarketBook marketBook = mapBooks.get(product);
		if(!node.get(1).asText().equals("hb")) {
			updateBook(marketBook, node);
		}
		
		List<Strategy> lst = mapStrategies.get(product);
		for(Strategy strategy : lst) {
			strategy.onBook(exchangeName, productMapper.getProduct(product, exchangeName), marketBook.topPrice(Side.buy, 1)[0], marketBook.topQty(Side.buy, 1)[0], marketBook.topPrice(Side.sell, 1)[0], marketBook.topQty(Side.sell, 1)[0]);
		}
	}
	
	private void updateBook(MarketBook book, JsonNode jsonNode) {
		if(jsonNode.get(1).isArray()) {
			for(JsonNode node : jsonNode.get(1)) {
				updateSnapshot(book, node);
			}
		} else {
			updateNode(book, jsonNode);
		}
	}
	
	private void updateSnapshot(MarketBook book, JsonNode node) {
		double price = node.get(0).asDouble();
		int count = node.get(1).asInt();
		double qty = node.get(2).asDouble();
		
		if(count > 0) {
			if(qty > 0) {
				book.upsertQuote(Side.buy, price, qty);
			} else if(qty < 0) {
				book.upsertQuote(Side.sell, price, -qty);
			}
		} else if(count == 0) {
			if(qty == 1) {
				book.removeQuote(Side.buy, price);
			} else if(qty == -1) {
				book.removeQuote(Side.sell, price);
			}
		}
	}

	private void updateNode(MarketBook book, JsonNode node) {
		double price = node.get(1).asDouble();
		int count = node.get(2).asInt();
		double qty = node.get(3).asDouble();
		
		if(count > 0) {
			if(qty > 0) {
				book.upsertQuote(Side.buy, price, qty);
			} else if(qty < 0) {
				book.upsertQuote(Side.sell, price, -qty);
			}
		} else if(count == 0) {
			if(qty == 1) {
				book.removeQuote(Side.buy, price);
			} else if(qty == -1) {
				book.removeQuote(Side.sell, price);
			}
		}
	}

	@Override
	public void reconnect() {
	}
}
