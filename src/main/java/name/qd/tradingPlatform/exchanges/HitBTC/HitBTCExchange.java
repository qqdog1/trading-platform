package name.qd.tradingPlatform.exchanges.HitBTC;

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
import name.qd.tradingPlatform.utils.JsonUtils;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class HitBTCExchange extends Exchange {
	private Logger log = LoggerFactory.getLogger(HitBTCExchange.class);
	private ObjectMapper objectMapper = JsonUtils.getObjectMapper();
	private final OkHttpClient okHttpClient = new OkHttpClient.Builder().pingInterval(10, TimeUnit.SECONDS).build();
	private ExchangeConfig exchangeConfig;
	private FileProductMapperManager productMapper;
	private HttpUrl httpUrl;
	private WebSocket webSocket;
	private ChannelMessageHandler channelMessageHandler;
	private Map<String, List<Strategy>> mapStrategies = new HashMap<>();
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private Object balanceLock = new Object();
	private Map<String, Double> mapBalance = new HashMap<>();
	private Map<Integer, Strategy> mapOrderIdToStrategy = new HashMap<>();
	
	public HitBTCExchange(ExchangeConfig exchangeConfig, FileProductMapperManager productMapper) {
		this.exchangeConfig = exchangeConfig;
		this.productMapper = productMapper;
		httpUrl = HttpUrl.parse(exchangeConfig.getRESTAddr());
		channelMessageHandler = new HitBTCChannelMessageHandler(this, mapStrategies, getExchangeName(), productMapper, mapOrderIdToStrategy);
		webSocket = createWebSocket(exchangeConfig.getWebSocketAddr(), new ExchangeWebSocketListener(channelMessageHandler));
		executor.execute(channelMessageHandler);
		webSocketLogin();
	}
	
	@Override
	public ExchangeName getExchangeName() {
		return ExchangeName.HitBTC;
	}
	
	private void webSocketLogin() {
		String ts = String.valueOf(System.currentTimeMillis());
		byte[] hash = HmacUtils.getInitializedMac(HmacAlgorithms.HMAC_SHA_256, exchangeConfig.getSecret().getBytes()).doFinal(ts.getBytes());
		String signature = Hex.encodeHexString(hash);
		
		ObjectNode node = objectMapper.createObjectNode();
		node.put("method", "login");
		node.put("id", HitBTCConstants.CHANNEL_ID_LOGIN);
		ObjectNode nodeParams = node.putObject("params");
		nodeParams.put("algo", "HS256");
		nodeParams.put("pKey", exchangeConfig.getApiKey());
		nodeParams.put("nonce", ts);
		nodeParams.put("signature", signature);
		webSocket.send(node.toString());
	}

	@Override
	public List<String> getProducts() {
		List<String> lst = new ArrayList<>();
		try {
			HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
			urlBuilder.addPathSegments("api/2/public/symbol");
			String result = sendSyncHttpGet(urlBuilder.build().url().toString());
			JsonNode node = objectMapper.readTree(result);
			for(JsonNode nodeProduct : node) {
				lst.add(nodeProduct.get("id").asText());
			}
		} catch (IOException e) {
			log.error("get symbols failed.");
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
		String result = null;
		try {
			HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
			urlBuilder.addPathSegments("api/2/public/orderbook");
			urlBuilder.addPathSegments(product);
			urlBuilder.addEncodedQueryParameter("limit", "1");
			result = sendSyncHttpGet(urlBuilder.build().url().toString());
			JsonNode node = objectMapper.readTree(result);
			JsonNode bidNode = node.get("bid").get(0);
			JsonNode askNode = node.get("ask").get(0);
			lst.add(bidNode.get("price").asDouble());
			lst.add(askNode.get("price").asDouble());
		} catch (NullPointerException e1) {
			log.error("get instant price failed. {} {}", product, result);
		} catch (IOException e) {
			log.error("get instant price failed. {} {}", product, result);
			return getInstantPrice(product);
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
	
	private void subscribeProduct(String productString) {
		webSocket.send(getSubscribeString(productString));
	}
	
	private String getSubscribeString(String product) {
		ObjectNode node = objectMapper.createObjectNode();
		node.put("method", "subscribeOrderbook");
		node.put("id", HitBTCConstants.CHANNEL_ID_BOOK);
		ObjectNode symbolNode = node.putObject("params");
		symbolNode.put("symbol", product);
		return node.toString();
	}

	@Override
	public void sendOrder(int orderId, Product product, Side side, double price, double qty, Strategy strategy) {
		mapOrderIdToStrategy.put(orderId, strategy);
		
		String productString = productMapper.getExchangeProductString(product, getExchangeName());
		ObjectNode node = objectMapper.createObjectNode();
		node.put("method", "newOrder");
		node.put("id", HitBTCConstants.CHANNEL_ID_ORDER);
		ObjectNode symbolNode = node.putObject("params");
		symbolNode.put("clientOrderId", orderId);
		symbolNode.put("symbol", productString);
		symbolNode.put("side", side.name());
		symbolNode.put("timeInForce", "IOC");
		symbolNode.put("price", price);
		symbolNode.put("quantity", qty);
		webSocket.send(node.toString());
	}

	@Override
	public void sendCancel(int orderId, Strategy strategy) {
		
	}

	@Override
	public Map<String, Double> queryBalance() {
		ObjectNode node = objectMapper.createObjectNode();
		node.put("method", "getTradingBalance");
		node.putObject("params");
		node.put("id", HitBTCConstants.CHANNEL_ID_BALANCE);
		webSocket.send(node.toString());
		synchronized(balanceLock) {
			try {
				balanceLock.wait(5000);
			} catch (Exception e) {
				log.error("", e);
			}
		}
		try {
			restBalance();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return mapBalance;
	}
	
	private void restBalance() throws IOException {
		HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
		urlBuilder.addPathSegments("api/2/trading/balance");
		String credential = Credentials.basic(exchangeConfig.getApiKey(), exchangeConfig.getSecret());
		Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.build().url().toString());
		requestBuilder.addHeader("Authorization", credential);
		Request request = requestBuilder.get().build();
		Response response = okHttpClient.newCall(request).execute();
		System.out.println(exchangeConfig.getApiKey() + ":" + exchangeConfig.getSecret() + ":" + credential);
		System.out.println("QQ " + response.body().string());
	}
	
	public void afterLogin() {
		subscribeReport();
	}
	
	private void subscribeReport() {
		ObjectNode node = objectMapper.createObjectNode();
		node.put("method", "subscribeReports");
		node.putObject("params");
		webSocket.send(node.toString());
	}
	
	public void setBalance(JsonNode node) {
		mapBalance.clear();
		
		JsonNode resultNode = node.get("result");
		for(JsonNode balanceNode : resultNode) {
			String currency = balanceNode.get("currency").asText();
			double qty = balanceNode.get("available").asDouble();
			if(qty > 0) {
				mapBalance.put(currency, qty);
			}
		}
		synchronized(balanceLock) {
			balanceLock.notifyAll();
		}
	}
	
	private String sendSyncHttpGet(String url) throws IOException {
		return okHttpClient.newCall(new Request.Builder().url(url).build()).execute().body().string();
	}
	
	private WebSocket createWebSocket(String url, WebSocketListener listener) {
		Request request = new Request.Builder().url(url).build();
		return okHttpClient.newWebSocket(request, listener);
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
