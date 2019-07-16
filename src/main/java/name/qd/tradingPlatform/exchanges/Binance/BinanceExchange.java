package name.qd.tradingPlatform.exchanges.Binance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class BinanceExchange implements Exchange {
	private Logger log = LoggerFactory.getLogger(BinanceExchange.class);
	private ObjectMapper objectMapper = JsonUtils.getObjectMapper();
	private final ExchangeConfig exchangeConfig;
	private Map<String, List<Strategy>> mapStrategies = new HashMap<>();
	private final OkHttpClient okHttpClient = new OkHttpClient.Builder().pingInterval(10, TimeUnit.SECONDS).build();
	private final ProductMapper productMapper;
	private ChannelMessageHandler channelMessageHandler;
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private HttpUrl httpUrl;
	private Request.Builder orderRequestBuilder;
	private long timeDiff;
	private Map<String, Double> mapBalance = new HashMap<>();
	
	public BinanceExchange(ExchangeConfig exchangeConfig, ProductMapper productMapper) {
		this.exchangeConfig = exchangeConfig;
		this.productMapper = productMapper;
		channelMessageHandler = new BinanceChannelMessageHandler(mapStrategies, productMapper, getExchangeName());
		executor.execute(channelMessageHandler);
		httpUrl = HttpUrl.parse(exchangeConfig.getRESTAddr());
		initRequestBuilder();
		syncTime();
	}
	
	private void initRequestBuilder() {
		HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
		urlBuilder.addPathSegments("api/v3/order");
		orderRequestBuilder = new Request.Builder().url(urlBuilder.build().url().toString());
	}
	
	private void syncTime() {
		try {
			HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
			urlBuilder.addPathSegments("/api/v1/time");
			String result = sendSyncHttpGet(urlBuilder.build().url().toString());
			JsonNode node = objectMapper.readTree(result);
			long serverTime = node.get("serverTime").asLong();
			long currentTime = System.currentTimeMillis();
			timeDiff = serverTime - currentTime;
		} catch (IOException e) {
			log.error("Sync time with server error.", e);
		}
	}
	
	@Override
	public ExchangeName getExchangeName() {
		return ExchangeName.Binance;
	}
	
	@Override
	public List<String> getProducts() {
		List<String> lst = new ArrayList<>();
		try {
			HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
			urlBuilder.addPathSegments("/api/v1/exchangeInfo");
			String result = sendSyncHttpGet(urlBuilder.build().url().toString());
			JsonNode node = objectMapper.readTree(result);
			JsonNode nodeProducts = node.get("symbols");
			for(JsonNode nodeProduct : nodeProducts) {
				lst.add(nodeProduct.get("symbol").asText());
			}
		} catch (IOException e) {
			log.error("get exchangeinfo failed.", e);
		}
		return lst;
	}
	
	@Override
	public Map<String, List<Double>> getInstantPrice() {
		Map<String, List<Double>> map = new HashMap<>();
		try {
			HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
			urlBuilder.addPathSegments("/api/v3/ticker/bookTicker");
			String result = sendSyncHttpGet(urlBuilder.build().url().toString());
			JsonNode node = objectMapper.readTree(result);
			for(JsonNode symbolNode : node) {
				List<Double> lst = new ArrayList<>();
				lst.add(symbolNode.get("bidPrice").asDouble());
				lst.add(symbolNode.get("askPrice").asDouble());
				String product = symbolNode.get("symbol").asText();
				map.put(product, lst);
			}
		} catch (IOException e) {
			log.error("get all product instant price failed.", e);
		}
		return map;
	}
	
	@Override
	public List<Double> getInstantPrice(String product) {
		List<Double> lst = new ArrayList<>();
		try {
			HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
			urlBuilder.addPathSegments("/api/v3/ticker/bookTicker");
			urlBuilder.addEncodedQueryParameter("symbol", product);
			String result = sendSyncHttpGet(urlBuilder.build().url().toString());
			JsonNode node = objectMapper.readTree(result);
			lst.add(node.get("bidPrice").asDouble());
			lst.add(node.get("askPrice").asDouble());
		} catch (IOException e) {
			log.error("get instant price failed. {}", product, e);
		}
		return lst;
	}
	
	@Override
	public void subscribe(Product[] products, Strategy strategy) {
		List<String> lstProducts = new ArrayList<>();
		for(Product product : products) {
			String productString = productMapper.getExchangeProductString(product, getExchangeName());
			if(!mapStrategies.containsKey(productString)) {
				List<Strategy> lst = new ArrayList<>();
				lstProducts.add(productString);
				mapStrategies.put(productString, lst);
			}
			mapStrategies.get(productString).add(strategy);
		}
		subscribeProduct(lstProducts);
	}
	
	private void subscribeProduct(List<String> lst) {
		StringBuilder sb = new StringBuilder();
		sb.append("stream?streams=");
		for(String product : lst) {
			sb.append(product.toLowerCase()).append("@depth5.b10/");
		}
		createWebSocket(exchangeConfig.getWebSocketAddr() + sb.toString(), new ExchangeWebSocketListener(channelMessageHandler));
	}
	
	@Override
	public void sendOrder(int orderId, Product product, Side side, double price, double qty, Strategy strategy) {
		log.info("send Order : {}, {}, {}, {}", product, side, price, qty);
		
		String signature = null;
		String productString = productMapper.getExchangeProductString(product, getExchangeName());
		productString = productString.toUpperCase();
		long timestamp = getCurrentMillis();
		try {
			byte[] hmac_key = exchangeConfig.getSecret().getBytes("UTF-8");
			byte[] hash = HmacUtils.getInitializedMac(HmacAlgorithms.HMAC_SHA_256, hmac_key).doFinal(getOrderQueryString(orderId, productString, side, price, qty, timestamp).getBytes());
			signature = Hex.encodeHexString(hash);
			
			FormBody body = getOrderPostBody(orderId, productString, side, price, qty, timestamp, signature);
			orderRequestBuilder.addHeader("X-MBX-APIKEY", exchangeConfig.getApiKey());
			Request request = orderRequestBuilder.post(body).build();
			
			Callback callback = new BinanceHttpCallback(strategy);
			okHttpClient.newCall(request).enqueue(callback);
		} catch (Exception e) {
			log.error("encrypt query string failed", e);
		}
	}
	
	private String getOrderQueryString(int orderId, String product, Side side, double price, double qty, long timestamp) {
		HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
		urlBuilder.addEncodedQueryParameter("symbol", product);
		urlBuilder.addEncodedQueryParameter("side", side.name());
		urlBuilder.addEncodedQueryParameter("type", "LIMIT");
		urlBuilder.addEncodedQueryParameter("timeInForce", "IOC");
		urlBuilder.addEncodedQueryParameter("quantity", Double.toString(qty));
		urlBuilder.addEncodedQueryParameter("price", Double.toString(price));
		urlBuilder.addEncodedQueryParameter("newClientOrderId", Integer.toString(orderId));
		urlBuilder.addEncodedQueryParameter("timestamp", Long.toString(timestamp));
		return urlBuilder.build().encodedQuery();
	}
	
	private FormBody getOrderPostBody(int orderId, String product, Side side, double price, double qty, long timestamp, String signature) {
		FormBody.Builder builder = new FormBody.Builder();
		builder.addEncoded("symbol", product);
		builder.addEncoded("side", side.name());
		builder.addEncoded("type", "LIMIT");
		builder.addEncoded("timeInForce", "IOC");
		builder.addEncoded("quantity", Double.toString(qty));
		builder.addEncoded("price", Double.toString(price));
		builder.addEncoded("newClientOrderId", Integer.toString(orderId));
		builder.addEncoded("timestamp", Long.toString(timestamp));
		builder.addEncoded("signature", signature);
		return builder.build();
	}
	
	@Override
	public void sendCancel(int orderId, Strategy strategy) {
		
	}
	
	@Override
	public Map<String, Double> queryBalance() {
		mapBalance.clear();
		
		HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
		urlBuilder.addPathSegments("/api/v3/account");
		String signature = null;
		long timestamp = getCurrentMillis();
		
		try {
			byte[] hmac_key = exchangeConfig.getSecret().getBytes("UTF-8");
			byte[] hash = HmacUtils.getInitializedMac(HmacAlgorithms.HMAC_SHA_256, hmac_key).doFinal(getAccountInfoQueryString(timestamp).getBytes());
			signature = Hex.encodeHexString(hash);
			
			urlBuilder.addEncodedQueryParameter("timestamp", Long.toString(timestamp));
			urlBuilder.addEncodedQueryParameter("signature", signature);
			Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.build().url().toString());
			requestBuilder.addHeader("X-MBX-APIKEY", exchangeConfig.getApiKey());
			Request request = requestBuilder.get().build();
			Response response = okHttpClient.newCall(request).execute();
			parseBalance(response);
			return mapBalance;
		} catch (Exception e) {
			log.error("encrypt query string failed", e);
		}
		return null;
	}
	
	private void parseBalance(Response response) {
		try {
			JsonNode root = objectMapper.readTree(response.body().string());
			JsonNode balanceNode = root.get("balances");
			for(JsonNode node : balanceNode) {
				String currency = node.get("asset").asText();
				double qty = node.get("free").asDouble();
				if(qty > 0) {
					mapBalance.put(currency, qty);
				}
			}
		} catch (IOException e) {
			log.error("parse balance failed.", e);
		}
	}
	
	private String getAccountInfoQueryString(long timestamp) {
		HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
		urlBuilder.addEncodedQueryParameter("timestamp", String.valueOf(timestamp));
		return urlBuilder.build().encodedQuery();
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

	@Override
	public Map<String, Double> getTickSize() {
		return null;
	}

	@Override
	public void unsubscribe(Strategy strategy) {
		
	}
}
