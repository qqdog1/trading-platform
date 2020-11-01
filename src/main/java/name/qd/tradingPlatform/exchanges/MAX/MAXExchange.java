package name.qd.tradingPlatform.exchanges.MAX;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;
import name.qd.tradingPlatform.Constants.Side;
import name.qd.tradingPlatform.exchanges.ChannelMessageHandler;
import name.qd.tradingPlatform.exchanges.Exchange;
import name.qd.tradingPlatform.exchanges.ExchangeConfig;
import name.qd.tradingPlatform.exchanges.ExchangeWebSocketListener;
import name.qd.tradingPlatform.exchanges.book.MarketBook;
import name.qd.tradingPlatform.product.FileProductMapperManager;
import name.qd.tradingPlatform.strategies.Strategy;
import name.qd.tradingPlatform.utils.JsonUtils;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class MAXExchange extends Exchange {
	private Logger log = LoggerFactory.getLogger(MAXExchange.class);
	private final ExchangeConfig exchangeConfig;
	private final FileProductMapperManager productMapper;
	private WebSocket webSocket;
	private HttpUrl httpUrl;
	private final OkHttpClient okHttpClient = new OkHttpClient.Builder().pingInterval(10, TimeUnit.SECONDS).build();
	private ObjectMapper objectMapper = JsonUtils.getObjectMapper();
	private MAXChannelMessageHandler channelMessageHandler;
	private WebSocketListener websocketListener;
	private Map<String, List<Strategy>> mapStrategies = new HashMap<>();
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private long timeDiff;
	private Map<Integer, Long> mapClientIdToMAXId = new ConcurrentHashMap<>();
	private Map<Long, Integer> mapMAXIdToClientId = new ConcurrentHashMap<>();
	private Map<Integer, Strategy> mapClientIdToStrategy = new ConcurrentHashMap<>();
	private static final BigDecimal PRECISION = BigDecimal.valueOf(0.1);
	private Map<String, MarketBook> mapBooks = new ConcurrentHashMap<>();
	private Set<String> setAfterSnapshot = new HashSet<>();
	private Map<String, List<JsonNode>> mapCacheBook = new HashMap<>();
	
	public MAXExchange(ExchangeConfig exchangeConfig, FileProductMapperManager productMapper) {
		this.exchangeConfig = exchangeConfig;
		this.productMapper = productMapper;
		httpUrl = HttpUrl.parse(exchangeConfig.getRESTAddr());
		channelMessageHandler = new MAXChannelMessageHandler();
		websocketListener = new ExchangeWebSocketListener(channelMessageHandler);
		webSocket = createWebSocket(exchangeConfig.getWebSocketAddr(), websocketListener);
		executor.execute(channelMessageHandler);
		syncTime();
	}
	
	private void subPrivateMsg(String challenge) {
		try {
			String payload = exchangeConfig.getApiKey() + challenge;
			byte[] hash = HmacUtils.getInitializedMac(HmacAlgorithms.HMAC_SHA_256, exchangeConfig.getSecret().getBytes("UTF-8")).doFinal(payload.getBytes());
			String signature = Hex.encodeHexString(hash);
			String msg = getSubPrivateString(signature);
			webSocket.send(msg);
		} catch (Exception e) {
			log.error("Subscribe private message failed.", e);
		}
	}
	
	private String getSubPrivateString(String signature) {
		ObjectNode node = objectMapper.createObjectNode();
		node.put("cmd", "auth");
		node.put("access_key", exchangeConfig.getApiKey());
		node.put("answer", signature);
		return node.toString();
	}
	
	private void syncTime() {
		try {
			HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
			urlBuilder.addPathSegments("timestamp");
			String result = sendSyncHttpGet(urlBuilder.build().url().toString());
			long serverTime = Long.parseLong(result)*1000;
			long currentTime = System.currentTimeMillis();
			timeDiff = serverTime - currentTime;
		} catch (IOException e) {
			log.error("Sync time with server error.", e);
		}
	}
	
	@Override
	public ExchangeName getExchangeName() {
		return ExchangeName.MAX;
	}

	@Override
	public List<String> getProducts() {
		List<String> lst = new ArrayList<>();
		try {
			HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
			urlBuilder.addPathSegments("markets");
			String result = sendSyncHttpGet(urlBuilder.build().url().toString());
			JsonNode node = objectMapper.readTree(result);
			for(JsonNode data : node) {
				lst.add(data.get("id").asText());
			}
		} catch (IOException e) {
			log.error("get products failed.", e);
		}	
		return lst;
	}
	
	@Override
	public Map<String, Double> getTickSize() {
		Map<String, Double> map = new HashMap<>();
		try {
			HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
			urlBuilder.addPathSegments("markets");
			String result = sendSyncHttpGet(urlBuilder.build().url().toString());
			JsonNode node = objectMapper.readTree(result);
			for(JsonNode data : node) {
				String product = data.get("id").asText();
				int precision = data.get("quote_unit_precision").asInt();
				BigDecimal tickSize = BigDecimal.ONE;
				for(int i = 0 ; i < precision; i++) {
					tickSize = tickSize.multiply(PRECISION);
				}
				map.put(product, tickSize.doubleValue());
			}
		} catch (IOException e) {
			log.error("get tick size failed.", e);
		}	
		return map;
	}

	@Override
	public List<Double> getInstantPrice(String product) {
		return null;
	}

	@Override
	public Map<String, List<Double>> getInstantPrice() {
		return null;
	}

	@Override
	public void subscribe(Product[] products, Strategy strategy) {
		for(Product product : products) {
			String productString = productMapper.getExchangeProductString(product, getExchangeName());
			if(!mapStrategies.containsKey(productString)) {
				mapStrategies.put(productString, new ArrayList<Strategy>());
				subscribe(productString);
			}
			mapStrategies.get(productString).add(strategy);
		}
	}
	
	private void subscribe(String productString) {
		subscribeSocket(productString);
	}
	
	private void afterSubscribeSocket(String productString) {
		getSnapshot(productString);
		processCache(productString);
		setAfterSnapshot.add(productString);
	}
	
	private void processCache(String productString) {
		if(mapCacheBook.containsKey(productString)) {
			List<JsonNode> lst = mapCacheBook.remove(productString);
			for(JsonNode node : lst) {
				processBook(node);
			}
		}
	}
	
	private void getSnapshot(String productString) {
		HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
		urlBuilder.addPathSegment("depth");
		urlBuilder.addEncodedQueryParameter("market", productString);
		urlBuilder.addEncodedQueryParameter("limit", "200");
		Request request = new Request.Builder().url(urlBuilder.build().uri().toString()).build();
		try {
			String response = okHttpClient.newCall(request).execute().body().string();
			JsonNode node = objectMapper.readTree(response);
			JsonNode askNode = node.get("asks");
			JsonNode bidNode = node.get("bids");
			MarketBook marketBook = new MarketBook();
			if(bidNode.size() > 0) {
				for(JsonNode bid : bidNode) {
					marketBook.addBidQuote(bid.get(0).asDouble(), bid.get(1).asDouble());
				}
			}
			if(askNode.size() > 0) {
				for(JsonNode ask : askNode) {
					marketBook.addAskQuote(ask.get(0).asDouble(), ask.get(1).asDouble());
				}
			}
			mapBooks.put(productString, marketBook);
		} catch (IOException e) {
			log.error("Get market data failed.", e);
		}
	}
	
	private void subscribeSocket(String productString) {
		webSocket.send(getSubscirbeMsg(productString));
	}
	
	private String getSubscirbeMsg(String productString) {
		ObjectNode node = objectMapper.createObjectNode();
		node.put("cmd", "subscribe");
		node.put("channel", "orderbook");
		ObjectNode paramsNode = node.putObject("params");
		paramsNode.put("market", productString);
		return node.toString();
	}

	@Override
	public void unsubscribe(Strategy strategy) {
		for(String product : mapStrategies.keySet()) {
			List<Strategy> lst = mapStrategies.get(product);
			if(lst.contains(strategy)) {
				lst.remove(strategy);
			}
		}
	}
	
	@Override
	public void sendOrder(int orderId, Product product, Side side, double price, double qty, Strategy strategy) {
		log.info("Send order : {}, {}, {}, {}, {}", orderId, product, side, price, qty);
		String productString = productMapper.getExchangeProductString(product, getExchangeName());
		try {
			String payload = getOrderPayload(productString, side, price, qty);
			byte[] base64 = Base64.encodeBase64(payload.getBytes());
			String base64String = Base64.encodeBase64String(payload.getBytes());
			byte[] hash = HmacUtils.getInitializedMac(HmacAlgorithms.HMAC_SHA_256, exchangeConfig.getSecret().getBytes("UTF-8")).doFinal(base64);
			String signature = Hex.encodeHexString(hash);
			FormBody formBody = getOrderFormBody(productString, side, price, qty);
			
			HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
			urlBuilder.addPathSegments("orders");
			Request.Builder orderRequestBuilder = new Request.Builder().url(urlBuilder.build().url().toString());
			orderRequestBuilder.addHeader("X-MAX-ACCESSKEY", exchangeConfig.getApiKey());
			orderRequestBuilder.addHeader("X-MAX-PAYLOAD", base64String);
			orderRequestBuilder.addHeader("X-MAX-SIGNATURE", signature);
			Request request = orderRequestBuilder.post(formBody).build();
			Response response = okHttpClient.newCall(request).execute();
			JsonNode node = objectMapper.readTree(response.body().string());
			if(node.has("id")) {
				long maxId = node.get("id").asLong();
				log.info("Order Id:{}", maxId);
				mapClientIdToMAXId.put(orderId, maxId);
				mapMAXIdToClientId.put(maxId, orderId);
				mapClientIdToStrategy.put(orderId, strategy);
				strategy.onOrderAck(orderId);
			} else if(node.has("error")) {
				JsonNode errorNode = node.get("error");
				log.error("Order reject, {}", errorNode.get("message").asText());
				strategy.onOrderRej(orderId);
			} else {
				log.error("Unknow status when send order. {}", node.toString());
				strategy.onOrderRej(orderId);
			}
		} catch (Exception e) {
			log.error("Send order failed.", e);
		}
	}
	
	private String getOrderPayload(String productString, Side side, double price, double qty) {
		ObjectNode node = objectMapper.createObjectNode();
		long nonce = getCurrentMillis();
		node.put("path", "/api/v2/orders");
		node.put("nonce", nonce);
		node.put("market", productString);
		node.put("side", side.name());
		node.put("volume", qty);
		node.put("price", price);
		node.put("ord_type", "limit");
		return node.toString();
	}
	
	private FormBody getOrderFormBody(String productString, Side side, double price, double qty) {
		FormBody.Builder builder = new FormBody.Builder();
		builder.addEncoded("market", productString);
		builder.addEncoded("side", side.name());
		builder.addEncoded("volume", String.valueOf(qty));
		builder.addEncoded("price", String.valueOf(price));
		builder.addEncoded("ord_type", "limit");
		return builder.build();
	}

	@Override
	public void sendCancel(int orderId, Strategy strategy) {
		log.info("Cancel order : {}", orderId);
		try {
			String payload = getCancelPayload(orderId);
			byte[] base64 = Base64.encodeBase64(payload.getBytes());
			String base64String = Base64.encodeBase64String(payload.getBytes());
			byte[] hash = HmacUtils.getInitializedMac(HmacAlgorithms.HMAC_SHA_256, exchangeConfig.getSecret().getBytes("UTF-8")).doFinal(base64);
			String signature = Hex.encodeHexString(hash);
			FormBody formBody = getCancelFormBody(orderId);
			
			HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
			urlBuilder.addPathSegments("order/delete");
			Request.Builder cancelRequestBuilder = new Request.Builder().url(urlBuilder.build().url().toString());
			
			cancelRequestBuilder.addHeader("X-MAX-ACCESSKEY", exchangeConfig.getApiKey());
			cancelRequestBuilder.addHeader("X-MAX-PAYLOAD", base64String);
			cancelRequestBuilder.addHeader("X-MAX-SIGNATURE", signature);
			Request request = cancelRequestBuilder.post(formBody).build();
			Response response = okHttpClient.newCall(request).execute();
			JsonNode node = objectMapper.readTree(response.body().string());
			if(node.has("id")) {
				mapClientIdToMAXId.remove(orderId);
				mapClientIdToStrategy.remove(orderId);
				mapMAXIdToClientId.remove(node.get("id").asLong());
				strategy.onCancelAck(orderId);
			} else if(node.has("error")) {
				JsonNode errorNode = node.get("error");
				log.error("Cancel reject, {}", errorNode.get("message").asText());
				strategy.onCancelRej(orderId);
			} else {
				log.error("Unknow status when cancel order. {}", node.toString());
				strategy.onCancelRej(orderId);
			}
		} catch (Exception e) {
			log.error("Cancel order failed.", e);
		}
	}
	
	private String getCancelPayload(int orderId) {
		ObjectNode node = objectMapper.createObjectNode();
		long nonce = getCurrentMillis();
		node.put("path", "/api/v2/order/delete");
		node.put("nonce", nonce);
		node.put("id", mapClientIdToMAXId.get(orderId));
		return node.toString();
	}
	
	private FormBody getCancelFormBody(int orderId) {
		FormBody.Builder builder = new FormBody.Builder();
		builder.addEncoded("id", String.valueOf(mapClientIdToMAXId.get(orderId)));
		return builder.build();
	}

	@Override
	public Map<String, Double> queryBalance() {
		Map<String, Double> mapBalance = new HashMap<>();
		try {
			String payload = getQueryBalancePayload();
			byte[] base64 = Base64.encodeBase64(payload.getBytes());
			String base64String = Base64.encodeBase64String(payload.getBytes());
			byte[] hash = HmacUtils.getInitializedMac(HmacAlgorithms.HMAC_SHA_256, exchangeConfig.getSecret().getBytes("UTF-8")).doFinal(base64);
			String signature = Hex.encodeHexString(hash);
			
			HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
			urlBuilder.addPathSegments("members/me");
			Request.Builder balanceRequestBuilder = new Request.Builder().url(urlBuilder.build().url().toString());
			
			balanceRequestBuilder.addHeader("X-MAX-ACCESSKEY", exchangeConfig.getApiKey());
			balanceRequestBuilder.addHeader("X-MAX-PAYLOAD", base64String);
			balanceRequestBuilder.addHeader("X-MAX-SIGNATURE", signature);
			Request request = balanceRequestBuilder.build();
			Response response = okHttpClient.newCall(request).execute();
			JsonNode node = objectMapper.readTree(response.body().string());
			JsonNode balanceNode = node.get("accounts");
			for(JsonNode data : balanceNode) {
				String currency = data.get("currency").asText();
				double balance = data.get("balance").asDouble();
				mapBalance.put(currency, balance);
			}
		} catch (Exception e) {
			log.error("Query balance failed.", e);
		}
		return mapBalance;
	}
	
	private String getQueryBalancePayload() {
		ObjectNode node = objectMapper.createObjectNode();
		long nonce = getCurrentMillis();
		node.put("path", "/api/v2/members/me");
		node.put("nonce", nonce);
		return node.toString();
	}
	
	private WebSocket createWebSocket(String url, WebSocketListener listener) {
		Request request = new Request.Builder().url(url).build();
		return okHttpClient.newWebSocket(request, listener);
	}
	
	private String sendSyncHttpGet(String url) throws IOException {
		return okHttpClient.newCall(new Request.Builder().url(url).build()).execute().body().string();
	}
	
	private long getCurrentMillis() {
		return System.currentTimeMillis() + timeDiff;
	}
	
	private void strategyOnBook(String productString) {
		MarketBook marketBook = mapBooks.get(productString);
		Product product = productMapper.getProduct(productString, getExchangeName());
		for(Strategy strategy : mapStrategies.get(productString)) {
			strategy.onBook(getExchangeName(), product, marketBook.topPrice(Side.buy, 1)[0], marketBook.topQty(Side.buy, 1)[0], marketBook.topPrice(Side.sell, 1)[0], marketBook.topQty(Side.sell, 1)[0]);
		}
	}
	
	private void processBook(JsonNode node) {
		String action = node.get("action").asText();
		String productString = node.get("market").asText();
		Side side = Side.valueOf(node.get("side").asText());
		double price = node.get("price").asDouble();
		double qty = node.get("volume").asDouble();
		MarketBook marketBook = mapBooks.get(productString);
		switch(action) {
		case "add":
			marketBook.addQty(side, price, qty);
			break;
		case "remove":
			marketBook.subQty(side, price, qty);
			break;
		case "update":
			log.info("update book:", node.toString());
			break;
		}
		
		strategyOnBook(productString);
	}
	
	public class MAXChannelMessageHandler extends ChannelMessageHandler {
		private String challenge;
		
		public MAXChannelMessageHandler() {
		}
		
		@Override
		public void processMessage(JsonNode jsonNode) {
			String info = jsonNode.get("info").asText();
			if(info.equals("orderbook")) {
				onBook(jsonNode);
			} else if(info.equals("account")) {
				String reason = jsonNode.get("reason").asText();
				if(reason.equals("trade")) {
					onFill(jsonNode);
				}
			} else if(info.equals("subscribed")) {
				String channel = jsonNode.get("channel").asText();
				if(channel.equals("orderbook")) {
					System.out.println(jsonNode.toString());
					String productString = jsonNode.get("market").asText();
					afterSubscribeSocket(productString);
				}
			} else if(info.equals("challenge")) {
				onChallenge(jsonNode);
			}
		}
		
		private void onBook(JsonNode node) {
			String productString = node.get("market").asText();
			if(setAfterSnapshot.contains(productString)) {
				processBook(node);
			} else {
				cacheBook(node);
			}
		}
		
		private void cacheBook(JsonNode node) {
			String productString = node.get("market").asText();
			if(!mapCacheBook.containsKey(productString)) {
				mapCacheBook.put(productString, new ArrayList<>());
			}
			mapCacheBook.get(productString).add(node);
		}
		
		private void onFill(JsonNode node) {
			java.awt.Toolkit.getDefaultToolkit().beep();
			JsonNode tradeNode = node.get("trade");
			JsonNode fillNode = null;
			if(tradeNode.has("ask")) {
				fillNode = tradeNode.get("ask");
			} else if(tradeNode.has("bid")) {
				fillNode = tradeNode.get("bid");
			}
			
			long orderId = fillNode.get("id").asLong();
			log.info("Fill id:{}", orderId);
			
			if(mapMAXIdToClientId.containsKey(orderId)) {
				int clientId = mapMAXIdToClientId.get(orderId);
				Strategy strategy = mapClientIdToStrategy.get(clientId);
				double price = fillNode.get("price").asDouble();
				double qty = fillNode.get("executed_volume").asDouble();
				
				if(fillNode.get("remaining_volume").asDouble() == 0) {
					mapMAXIdToClientId.remove(orderId);
					mapClientIdToStrategy.remove(clientId);
					mapClientIdToMAXId.remove(clientId);
				}
				
				strategy.onFill(clientId, price, qty);
			} else {
				log.warn("Unknow fill. {}", node.toString());
			}
		}
		
		private void onChallenge(JsonNode node) {
			String key = node.get("msg").asText();
			log.info("Challenge Key:{}", key);
			this.challenge = key;
			subPrivateMsg(key);
		}
		
		public String getChallengeKey() {
			return challenge;
		}

		@Override
		public void reconnect() {
			webSocket = createWebSocket(exchangeConfig.getWebSocketAddr(), websocketListener);
		}
	}
}
