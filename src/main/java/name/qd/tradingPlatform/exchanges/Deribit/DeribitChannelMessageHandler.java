package name.qd.tradingPlatform.exchanges.Deribit;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;
import name.qd.tradingPlatform.exchanges.ChannelMessageHandler;
import name.qd.tradingPlatform.exchanges.book.MarketBook;
import name.qd.tradingPlatform.product.FileProductMapperManager;
import name.qd.tradingPlatform.strategies.Strategy;
import name.qd.tradingPlatform.trading.Order;

public class DeribitChannelMessageHandler extends ChannelMessageHandler {
	private static Logger log = LoggerFactory.getLogger(DeribitChannelMessageHandler.class);
	private Map<String, List<Strategy>> mapStrategies;
	private Map<Integer, Order> mapSequenceToOrder;
	private Map<Integer, Order> mapOrderIdToOrder;
	private final FileProductMapperManager productMapper;
	private final ExchangeName exchangeName;
	
	public DeribitChannelMessageHandler(Map<String, List<Strategy>> mapStrategies, FileProductMapperManager productMapper, ExchangeName exchangeName, Map<Integer, Order> mapSequenceToOrder, Map<Integer, Order> mapOrderIdToOrder) {
		this.mapStrategies = mapStrategies;
		this.productMapper = productMapper;
		this.exchangeName = exchangeName;
		this.mapSequenceToOrder = mapSequenceToOrder;
		this.mapOrderIdToOrder = mapOrderIdToOrder;
	}
	
	@Override
	public void processMessage(JsonNode jsonNode) {
		log.debug("receive ws msg:[{}]", jsonNode.toString());

		if(jsonNode.has("params")) {
			JsonNode nodeParams = jsonNode.get("params");
			String channel = nodeParams.get("channel").asText();
			if(channel.startsWith("book")) {
				JsonNode nodeData = nodeParams.get("data");
				processBook(nodeData);
			} else if(channel.contains("orders")) {
				
			} else if(channel.contains("trades")) {
				
			} else {
				log.error("Unknow message: {}", jsonNode.toString());
			}
		} else if(jsonNode.has("id")) {
			int sequence = jsonNode.get("id").asInt();
			if(mapSequenceToOrder.containsKey(sequence)) {
				processOrder(sequence, jsonNode);
			}
		} else {
			log.error("Unknow message: {}", jsonNode.toString());
		}
	}
	
	private void processOrder(int sequence, JsonNode node) {
		Order order = mapSequenceToOrder.remove(sequence);
		if(node.has("result")) {
			if(order.getActionType() == Order.ORDER) {
				JsonNode nodeResult = node.get("result");
				JsonNode nodeOrder = nodeResult.get("order");
				String exOrderId = nodeOrder.get("order_id").asText();
				order.setExOrderId(exOrderId);
				order.onOrderAck();
			} else {
				order.onCancelAck();
			}
		} else if(node.has("error")) {
			if(order.getActionType() == Order.ORDER) {
				mapOrderIdToOrder.remove(order.getOrderId());
				order.onOrderRej();
			} else {
				order.onCancelRej();
			}
		} else {
			log.error("Unknow case for order, {}", node.toString());
		}
	}
	
	private void processBook(JsonNode node) {
		MarketBook book = new MarketBook();
		String productString = node.get("instrument_name").asText();
		Product product = productMapper.getProduct(productString, exchangeName);
		JsonNode nodesBid = node.get("bids");
		JsonNode nodesAsk = node.get("asks");
	
		for(JsonNode bid : nodesBid) {
			double price = bid.get(0).asDouble();
			double qty = bid.get(1).asDouble();
			book.addBidQuote(price, qty);
		}
		
		for(JsonNode ask : nodesAsk) {
			double price = ask.get(0).asDouble();
			double qty = ask.get(0).asDouble();
			book.addAskQuote(price, qty);
		}
		
		List<Strategy> lst = mapStrategies.get(productString);
		for(Strategy strategy : lst) {
			strategy.onBook(exchangeName, product, book);
		}
	}

	@Override
	public void reconnect() {
	}
}
