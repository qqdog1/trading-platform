package name.qd.tradingPlatform.exchanges.Deribit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;
import name.qd.tradingPlatform.Constants.Side;
import name.qd.tradingPlatform.exchanges.ChannelMessageHandler;
import name.qd.tradingPlatform.exchanges.Exchange;
import name.qd.tradingPlatform.exchanges.ExchangeConfig;
import name.qd.tradingPlatform.exchanges.ExchangeWebSocketListener;
import name.qd.tradingPlatform.product.FileProductMapperManager;
import name.qd.tradingPlatform.strategies.Strategy;
import name.qd.tradingPlatform.trading.Order;
import name.qd.tradingPlatform.utils.JsonUtils;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class DeribitExchange extends Exchange {
	private Logger log = LoggerFactory.getLogger(DeribitExchange.class);
	private ObjectMapper objectMapper = JsonUtils.getObjectMapper();
	private final ExchangeConfig exchangeConfig;
	private Map<String, List<Strategy>> mapStrategies = new HashMap<>();
	private final OkHttpClient okHttpClient = new OkHttpClient.Builder().pingInterval(10, TimeUnit.SECONDS).build();
	private final FileProductMapperManager productMapper;
	private ChannelMessageHandler channelMessageHandler;
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private HttpUrl httpUrl;
	private int sequence;
	private WebSocket websocket;
	private Map<Integer, Order> mapOrderIdToOrder = new HashMap<>();
	private Map<Integer, Order> mapSequenceToOrder = new HashMap<>();

	public DeribitExchange(ExchangeConfig exchangeConfig, FileProductMapperManager productMapper) {
		this.exchangeConfig = exchangeConfig;
		this.productMapper = productMapper;
		httpUrl = HttpUrl.parse(exchangeConfig.getRESTAddr());
		channelMessageHandler = new DeribitChannelMessageHandler(mapStrategies, productMapper, getExchangeName(), mapSequenceToOrder, mapOrderIdToOrder);
		executor.execute(channelMessageHandler);
		connectWebsocket();
		getWebsocketToken();
		subscribeAckAndFill();
	}

	@Override
	public ExchangeName getExchangeName() {
		return ExchangeName.Deribit;
	}

	@Override
	public List<String> getProducts() {
		List<String> lst = new ArrayList<>();
		lst.addAll(getProducts("BTC"));
		lst.addAll(getProducts("ETH"));
		return lst;
	}

	private List<String> getProducts(String currency) {
		List<String> lst = new ArrayList<>();
		try {
			HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
			urlBuilder.addPathSegments("api/v2/public/get_instruments");
			urlBuilder.addEncodedQueryParameter("currency", currency);
			String result = sendSyncHttpGet(urlBuilder.build().url().toString());
			JsonNode node = objectMapper.readTree(result);
			JsonNode nodeResult = node.get("result");
			for (JsonNode nodeProduct : nodeResult) {
				lst.add(nodeProduct.get("instrument_name").asText());
			}
		} catch (IOException e) {
			log.error("get products failed.", e);
		}
		return lst;
	}

	@Override
	public Map<String, Double> getTickSize() {
		Map<String, Double> map = new HashMap<>();
		map.putAll(getTickSize("BTC"));
		map.putAll(getTickSize("ETH"));
		return map;
	}

	private Map<String, Double> getTickSize(String currency) {
		Map<String, Double> map = new HashMap<>();
		try {
			HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
			urlBuilder.addPathSegments("api/v2/public/get_instruments");
			urlBuilder.addEncodedQueryParameter("currency", currency);
			String result = sendSyncHttpGet(urlBuilder.build().url().toString());
			JsonNode node = objectMapper.readTree(result);
			JsonNode nodeResult = node.get("result");
			for (JsonNode nodeProduct : nodeResult) {
				String product = nodeProduct.get("instrument_name").asText();
				double tickSize = nodeProduct.get("tick_size").asDouble();
				map.put(product, tickSize);
			}
		} catch (IOException e) {
			log.error("get ticksize failed.", e);
		}
		return map;
	}

	@Override
	public List<Double> getInstantPrice(String product) {
		List<Double> lst = new ArrayList<>();
		try {
			HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
			urlBuilder.addPathSegments("api/v2/public/get_book_summary_by_instrument");
			urlBuilder.addEncodedQueryParameter("instrument_name", product);
			String result = sendSyncHttpGet(urlBuilder.build().url().toString());
			JsonNode node = objectMapper.readTree(result);
			JsonNode nodeResult = node.get("result");
			JsonNode nodePrice = nodeResult.get(0);
			lst.add(nodePrice.get("bid_price").asDouble());
			lst.add(nodePrice.get("ask_price").asDouble());
		} catch (IOException e) {
			log.error("get all product instant price failed.", e);
		}
		return lst;
	}

	@Override
	public Map<String, List<Double>> getInstantPrice() {
		return null;
	}

	@Override
	public void subscribe(Product[] products, Strategy strategy) {
		ObjectNode node = objectMapper.createObjectNode();
		node.put("jsonrpc", "2.0");
		node.put("id", sequence++);
		node.put("method", "public/subscribe");
		ObjectNode nodeParams = node.putObject("params");
		ArrayNode nodeChannel = nodeParams.putArray("channels");
		for (Product product : products) {
			StringBuilder sb = new StringBuilder();
			String productString = productMapper.getExchangeProductString(product, getExchangeName());
			sb.append("book.").append(productString).append(".none.20.100ms");
			nodeChannel.add(sb.toString());

			if (!mapStrategies.containsKey(productString)) {
				mapStrategies.put(productString, new ArrayList<>());
			}
			List<Strategy> lst = mapStrategies.get(productString);
			lst.add(strategy);
		}
		websocket.send(node.toString());
	}

	@Override
	public void unsubscribe(Strategy strategy) {

	}

	@Override
	public void sendOrder(int orderId, Product product, Side side, double price, double qty, Strategy strategy) {
		String productString = productMapper.getExchangeProductString(product, getExchangeName());
		int seqNo = sequence++;
		Order order = new Order(orderId, seqNo, productString, side, price, qty, strategy, Order.ORDER);
		mapOrderIdToOrder.put(orderId, order);
		mapSequenceToOrder.put(seqNo, order);
		ObjectNode node = objectMapper.createObjectNode();
		node.put("jsonrpc", "2.0");
		node.put("id", seqNo);
		
		switch(side) {
		case buy:
			node.put("method", "private/buy");
			break;
		case sell:
			node.put("method", "private/sell");
			break;
		}
		ObjectNode nodeParams = node.putObject("params");
		nodeParams.put("instrument_name", productString);
		nodeParams.put("amount", qty);
		nodeParams.put("price", price);
		
		websocket.send(node.toString());
	}
	
	@Override
	public void sendCancel(int orderId, Strategy strategy) {
		Order order = mapOrderIdToOrder.get(orderId);
		if(order == null) {
			log.error("Order not exist, can't cancel this order. OrderId:{}", orderId);
			strategy.onCancelRej(orderId);
		}
		
		int seqNo = sequence++;
		Order cancelOrder = makeACancleOrder(order, seqNo);
		mapSequenceToOrder.put(sequence, cancelOrder);
		
		ObjectNode node = objectMapper.createObjectNode();
		node.put("jsonrpc", "2.0");
		node.put("id", seqNo);
		node.put("method", "private/cancel");
		ObjectNode nodeParams = node.putObject("params");
		nodeParams.put("order_id", order.getExOrderId());
		websocket.send(node.toString());
	}
	
	private Order makeACancleOrder(Order order, int sequence) {
		Order orderCancel = new Order(order.getOrderId(), sequence, order.getProduct(), order.getSide(), order.getPrice(), order.getQty(), order.getStrategy(), Order.CANCEL);
		return orderCancel;
	}

	@Override
	public Map<String, Double> queryBalance() {
		Map<String, Double> map = new HashMap<>();
		try {
			map.put("BTC", queryBalance("BTC"));
			map.put("ETH", queryBalance("ETH"));
		} catch (IOException e) {
			log.error("Failed to query balance from Deribit.");
		}
		return map;
	}
	
	private double queryBalance(String currency) throws IOException {
		HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
		urlBuilder.addPathSegments("api/v1/private/account");
		urlBuilder.addEncodedQueryParameter("currency", currency);
		HttpUrl httpUrl = urlBuilder.build();
		String path = httpUrl.encodedPath();
		String timestamp = String.valueOf(System.currentTimeMillis());
		StringBuilder sb = new StringBuilder();
		sb.append("_=").append(timestamp);
		sb.append("&_ackey=").append(exchangeConfig.getApiKey());
		sb.append("&_acsec=").append(exchangeConfig.getSecret());
		sb.append("&_action=").append(path);
		sb.append("&currency=").append(currency);
		
		String signature = getSignature(sb.toString());
		StringBuilder sb2 = new StringBuilder();
		sb2.append(exchangeConfig.getApiKey()).append(".").append(timestamp).append(".").append(signature);
		Request.Builder builder = new Request.Builder().url(httpUrl.url().toString());
		builder.addHeader("X-Deribit-Sig", sb2.toString());
		String result = okHttpClient.newCall(builder.build()).execute().body().string();
		
		JsonNode node = objectMapper.readTree(result);
		JsonNode nodeResult = node.get("result");
		return nodeResult.get("equity").asDouble();
	}

	private void getWebsocketToken() {
		ObjectNode node = objectMapper.createObjectNode();
		node.put("jsonrpc", "2.0");
		node.put("id", sequence++);
		node.put("method", "public/auth");
		ObjectNode nodeParams = node.putObject("params");
		nodeParams.put("grant_type", "client_credentials");
		nodeParams.put("client_id", exchangeConfig.getApiKey());
		nodeParams.put("client_secret", exchangeConfig.getSecret());
		websocket.send(node.toString());
	}

	private String sendSyncHttpGet(String url) throws IOException {
		return okHttpClient.newCall(new Request.Builder().url(url).build()).execute().body().string();
	}

	private void connectWebsocket() {
		StringBuilder sb = new StringBuilder();
		sb.append(exchangeConfig.getWebSocketAddr()).append("api/v2");
		websocket = createWebSocket(sb.toString(), new ExchangeWebSocketListener(channelMessageHandler));
		log.info("Trying to connect to Deribit websocket.");
	}

	private WebSocket createWebSocket(String url, WebSocketListener listener) {
		Request request = new Request.Builder().url(url).build();
		return okHttpClient.newWebSocket(request, listener);
	}
	
	private void subscribeAckAndFill() {
		ObjectNode node = objectMapper.createObjectNode();
		node.put("jsonrpc", "2.0");
		node.put("id", sequence++);
		node.put("method", "private/subscribe");
		ObjectNode nodeParams = node.putObject("params");
		ArrayNode nodeChannel = nodeParams.putArray("channels");
		nodeChannel.add("user.orders.any.BTC.100ms");
		nodeChannel.add("user.orders.any.ETH.100ms");
		nodeChannel.add("user.trades.any.BTC.100ms");
		nodeChannel.add("user.trades.any.ETH.100ms");
		websocket.send(node.toString());
	}
	
	private String getSignature(String signString) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(signString.getBytes(StandardCharsets.UTF_8));
			return new String(Base64.encodeBase64(hash));
		} catch (NoSuchAlgorithmException e) {
			log.error("Failed to get signature, {}", signString);
		}
		return null;
	}
}
