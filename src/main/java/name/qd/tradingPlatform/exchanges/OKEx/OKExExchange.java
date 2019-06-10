package name.qd.tradingPlatform.exchanges.OKEx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import name.qd.tradingPlatform.exchanges.ChannelMessageHandler;
import name.qd.tradingPlatform.exchanges.Exchange;
import name.qd.tradingPlatform.exchanges.ExchangeConfig;
import name.qd.tradingPlatform.exchanges.ExchangeWebSocketListener;
import name.qd.tradingPlatform.product.ProductMapper;
import name.qd.tradingPlatform.strategies.Strategy;
import name.qd.tradingPlatform.utils.JsonUtils;
import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;
import name.qd.tradingPlatform.Constants.Side;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class OKExExchange implements Exchange {
	private static Logger log = LoggerFactory.getLogger(OKExExchange.class);
	private ObjectMapper objectMapper = JsonUtils.getObjectMapper();
	private final ExchangeConfig exchangeConfig;
	private Map<String, List<Strategy>> mapStrategies = new HashMap<>();
	private final OkHttpClient okHttpClient = new OkHttpClient.Builder().pingInterval(10, TimeUnit.SECONDS).build();
	private WebSocket webSocket;
	private ChannelMessageHandler channelMessageHandler;
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private final ProductMapper productMapper;
	private HttpUrl httpUrl;
	private Request.Builder orderRequestBuilder;
	private Request.Builder cancelRequestBuilder;
	private Request.Builder balanceRequestBuilder;
	private Map<Long, Integer> mapExOrderIdToOrderId = new HashMap<>();
	private Map<Integer, Pair<Long, String>> mapOrderIdToExOrderId = new HashMap<>();
	private Map<Long, Strategy> mapExOrderIdToStrategy = new HashMap<>();
	private Map<String, Double> mapBalance = new HashMap<>();
	
	public OKExExchange(ExchangeConfig exchangeConfig, ProductMapper productMapper) {
		this.exchangeConfig = exchangeConfig;
		this.productMapper = productMapper;
		channelMessageHandler = new OKExChannelMessageHandler(this, mapStrategies, productMapper, mapExOrderIdToOrderId, mapExOrderIdToStrategy);
		webSocket = createWebSocket(exchangeConfig.getWebSocketAddr(), new ExchangeWebSocketListener(channelMessageHandler));
		httpUrl = HttpUrl.parse(exchangeConfig.getRESTAddr());
		executor.execute(channelMessageHandler);
		webSocketLogin();
		initRequestBuilder();
	}
	
	private void initRequestBuilder() {
		HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
		urlBuilder.addPathSegments("api/vi/trade.do");
		orderRequestBuilder = new Request.Builder().url(urlBuilder.build().url().toString());
		
		urlBuilder = httpUrl.newBuilder();
		urlBuilder.addPathSegments("api/vi/cancel_order.do");
		cancelRequestBuilder = new Request.Builder().url(urlBuilder.build().url().toString());
		
		urlBuilder = httpUrl.newBuilder();
		urlBuilder.addPathSegments("api/vi/userinfo.do");
		balanceRequestBuilder = new Request.Builder().url(urlBuilder.build().url().toString());
	}
	
	@Override
	public ExchangeName getExchangeName() {
		return ExchangeName.OKEx;
	}
	
	@Override
	public List<String> getProducts() {
		List<String> lst = new ArrayList<>();
		try {
			HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
			urlBuilder.addPathSegments("v2/markets/products");
			String result = sendSyncHttpGet(urlBuilder.build().url().toString());
			JsonNode node = objectMapper.readTree(result);
			JsonNode dataNode = node.get("data");
			for(JsonNode data : dataNode) {
				lst.add(data.get("symbol").asText());
			}
		} catch (IOException e) {
			log.error("get products failed.", e);
		}
		return lst;
	}
	
	@Override
	public Map<String, List<Double>> getInstantPrice() {
		return null;
	}
	
	@Override
	public List<Double> getInstantPrice(String product) {
		List<Double> lst = new ArrayList<>();
		try {
			HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
			urlBuilder.addPathSegments("api/v1/depth.do");
			urlBuilder.addEncodedQueryParameter("symbol", product);
			urlBuilder.addEncodedQueryParameter("size", "1");
			String result = sendSyncHttpGet(urlBuilder.build().url().toString());
			JsonNode node = objectMapper.readTree(result);
			JsonNode bidNode = node.get("bids").get(0);
			JsonNode askNode = node.get("asks").get(0);
			lst.add(bidNode.get(0).asDouble());
			lst.add(askNode.get(0).asDouble());
		} catch (IOException e) {
			log.error("get products failed.", e);
		}
		return lst;
	}

	@Override
	public void subscribe(Product[] products, Strategy strategy) {
		for(Product product : products) {
			String productString = productMapper.getExchangeProductString(product, getExchangeName());
			if(!mapStrategies.containsKey(productString)) {
				subscribeProduct(productString);
				mapStrategies.put(productString, new ArrayList<Strategy>());
			}
			mapStrategies.get(productString).add(strategy);
		}
	}
	
	private void webSocketLogin() {
		byte[] hash = DigestUtils.md5(getLoginQueryString());
		String md5 = Hex.encodeHexString(hash).toUpperCase();
		
		ObjectNode node = objectMapper.createObjectNode();
		node.put("event", "login");
		ObjectNode nodeParameters = node.putObject("parameters");
		nodeParameters.put("api_key", exchangeConfig.getApiKey());
		nodeParameters.put("sign", md5);
		webSocket.send(node.toString());
	}
	
	private String getLoginQueryString() {
		HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
		urlBuilder.addEncodedQueryParameter("api_key", exchangeConfig.getApiKey());
		urlBuilder.addEncodedQueryParameter("secret_key", exchangeConfig.getSecret());
		return urlBuilder.build().encodedQuery();
	}
	
	private String getBalanceQueryString() {
		HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
		urlBuilder.addEncodedQueryParameter("api_key", exchangeConfig.getApiKey());
		urlBuilder.addEncodedQueryParameter("secret_key", exchangeConfig.getSecret());
		return urlBuilder.build().encodedQuery();
	}
	
	private void subscribeProduct(String productString) {
		webSocket.send(getSubscribeString(productString));
	}
	
	private String getSubscribeString(String product) {
		ObjectNode node = objectMapper.createObjectNode();
		node.put("event", "addChannel");
		node.put("channel", "ok_sub_spot_" + product + "_depth");
		return node.toString();
	}
	
	public void subscribeTradeInfo() {
		ObjectNode node = objectMapper.createObjectNode();
		node.put("event", "addChannel");
		node.put("channel", "ok_sub_spot_X_order");
		webSocket.send(node.toString());
	}

	@Override
	public void sendOrder(int orderId, Product product, Side side, double price, double qty, Strategy strategy) {
		log.info("send Order : {}, {}, {}, {}, {}", orderId, product, side, price, qty);
		
		String productString = productMapper.getExchangeProductString(product, getExchangeName());
		byte[] hash = DigestUtils.md5(getOrderQueryString(productString, side, price, qty));
		String md5 = Hex.encodeHexString(hash).toUpperCase();
		
		FormBody body = getOrderPostBody(productString, side, price, qty, md5);
		Request request = orderRequestBuilder.post(body).build();
		
		try {
			String response = okHttpClient.newCall(request).execute().body().string();
			processOrderAck(orderId, productString, response, strategy);
		} catch (IOException e) {
			log.error("send order to exchange failed.", e);
		}
	}
	
	@Override
	public void sendCancel(int orderId, Strategy strategy) {
		log.info("cancel order : {}", orderId);
		
		Pair<Long, String> pair = mapOrderIdToExOrderId.get(orderId);
		byte[] hash = DigestUtils.md5(getCancelQueryString(pair.getKey(), pair.getValue()));
		String md5 = Hex.encodeHexString(hash).toUpperCase();
		
		FormBody body = getCancelPostBody(pair.getKey(), pair.getValue(), md5);
		Request request = cancelRequestBuilder.post(body).build();
		
		try {
			String response = okHttpClient.newCall(request).execute().body().string();
			processCancel(orderId, response, strategy);
		} catch (IOException e) {
			log.error("send cancel to exchange failed.", e);
		}
	}
	
	private void processOrderAck(int orderId, String product, String response, Strategy strategy) {
		try {
			JsonNode node = objectMapper.readTree(response);
			if(node.has("order_id")) {
				long okExOrderId = node.get("order_id").asLong();
				mapExOrderIdToOrderId.put(okExOrderId, orderId);
				mapOrderIdToExOrderId.put(orderId, Pair.of(okExOrderId, product));
				mapExOrderIdToStrategy.put(okExOrderId, strategy);
				strategy.onOrderAck(orderId);
			} else {
				strategy.onOrderRej(orderId);
			}
		} catch (IOException e) {
			log.error("parse json object failed. {}", response);
		}
	}
	
	private void processCancel(int orderId, String response, Strategy strategy) {
		try {
			JsonNode node = objectMapper.readTree(response);
			boolean isSuccess = node.get("result").asBoolean();
			if(isSuccess) {
				strategy.onCancelAck(orderId);
				Pair<Long, String> pair = mapOrderIdToExOrderId.remove(orderId);
				mapExOrderIdToOrderId.remove(pair.getLeft());
				mapExOrderIdToStrategy.remove(pair.getLeft());
			} else {
				strategy.onCancelRej(orderId);
			}
		} catch (IOException e) {
			log.error("parse json object failed. {}", response);
		}
	}
	
	private String getOrderQueryString(String product, Side side, double price, double qty) {
		HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
		urlBuilder.addEncodedQueryParameter("amount", Double.toString(qty));
		urlBuilder.addEncodedQueryParameter("api_key", exchangeConfig.getApiKey());
		urlBuilder.addEncodedQueryParameter("price", Double.toString(price));
		urlBuilder.addEncodedQueryParameter("symbol", product);
		urlBuilder.addEncodedQueryParameter("type", side.name());
		urlBuilder.addEncodedQueryParameter("secret_key", exchangeConfig.getSecret());
		return urlBuilder.build().encodedQuery();
	}
	
	private FormBody getOrderPostBody(String product, Side side, double price, double qty, String sign) {
		FormBody.Builder builder = new FormBody.Builder();
		builder.add("amount", Double.toString(qty));
		builder.add("api_key", exchangeConfig.getApiKey());
		builder.add("price", Double.toString(price));
		builder.add("symbol", product);
		builder.add("type", side.name());
		builder.add("sign", sign);
		return builder.build();
	}
	
	private String getCancelQueryString(long exOrderId, String product) {
		HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
		urlBuilder.addEncodedQueryParameter("api_key", exchangeConfig.getApiKey());
		urlBuilder.addEncodedQueryParameter("order_id", Long.toString(exOrderId));
		urlBuilder.addEncodedQueryParameter("symbol", product);
		urlBuilder.addEncodedQueryParameter("secret_key", exchangeConfig.getSecret());
		return urlBuilder.build().encodedQuery();
	}
	
	private FormBody getCancelPostBody(long exOrderId, String product, String sign) {
		FormBody.Builder builder = new FormBody.Builder();
		builder.add("api_key", exchangeConfig.getApiKey());
		builder.add("order_id", Long.toString(exOrderId));
		builder.add("symbol", product);
		builder.add("sign", sign);
		return builder.build();
	}
	
	private FormBody getBalancePostBody(String sign) {
		FormBody.Builder builder = new FormBody.Builder();
		builder.add("api_key", exchangeConfig.getApiKey());
		builder.add("sign", sign);
		return builder.build();
	}

	@Override
	public Map<String, Double> queryBalance() {
		mapBalance.clear();
		
		byte[] hash = DigestUtils.md5(getBalanceQueryString());
		String md5 = Hex.encodeHexString(hash).toUpperCase();
		
		FormBody body = getBalancePostBody(md5);
		Request request = balanceRequestBuilder.post(body).build();
		String response = null;
		try {
			response = okHttpClient.newCall(request).execute().body().string();
		} catch (IOException e) {
			log.error("query balance failed.", e);
			return null;
		}
		
		try {
			JsonNode node = objectMapper.readTree(response);
			JsonNode balanceNode = node.get("info").get("funds").get("free");
			Iterator<String> it = balanceNode.fieldNames();
			while(it.hasNext()) {
				String currency = it.next();
				double value = balanceNode.get(currency).asDouble();
				if(value > 0) {
					mapBalance.put(currency, value);
				}
			}
		} catch (IOException e) {
			log.error("parse query balance json string failed.", e);
			return null;
		}
		
		return mapBalance;
	}
	
	private WebSocket createWebSocket(String url, WebSocketListener listener) {
		Request request = new Request.Builder().url(url).build();
		return okHttpClient.newWebSocket(request, listener);
	}
	
	private String sendSyncHttpGet(String url) throws IOException {
		return okHttpClient.newCall(new Request.Builder().url(url).build()).execute().body().string();
	}

	@Override
	public Map<String, Double> getTickSize() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void unsubscribe(Strategy strategy) {
		// TODO Auto-generated method stub
		
	}
}
