package name.qd.tradingPlatform.exchanges.BTSE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
import name.qd.tradingPlatform.utils.JsonUtils;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class BTSEExchange extends Exchange {
	private Logger log = LoggerFactory.getLogger(BTSEExchange.class);
	private ObjectMapper objectMapper = JsonUtils.getObjectMapper();
	private final ExchangeConfig exchangeConfig;
	private final FileProductMapperManager productMapper;
	private ChannelMessageHandler channelMessageHandler;
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private HttpUrl httpUrl;
	private Map<String, List<Strategy>> mapStrategies = new HashMap<>();
	private final OkHttpClient okHttpClient = new OkHttpClient.Builder().pingInterval(10, TimeUnit.SECONDS).build();
	private WebSocket webSocket;
	
	public BTSEExchange(ExchangeConfig exchangeConfig, FileProductMapperManager productMapper) {
		this.exchangeConfig = exchangeConfig;
		this.productMapper = productMapper;
		httpUrl = HttpUrl.parse(exchangeConfig.getRESTAddr());
		channelMessageHandler = new BTSEChannelMessageHandler(this, mapStrategies, productMapper);
		executor.execute(channelMessageHandler);
		webSocket = createWebSocket(exchangeConfig.getWebSocketAddr(), new ExchangeWebSocketListener(channelMessageHandler));
	}
	
	@Override
	public ExchangeName getExchangeName() {
		return ExchangeName.BTSE;
	}

	@Override
	public List<String> getProducts() {
		List<String> lst = new ArrayList<>();
		try {
			HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
			urlBuilder.addPathSegments("spot/v2/markets");
			String result = sendSyncHttpGet(urlBuilder.build().url().toString());
			JsonNode node = objectMapper.readTree(result);
			for(JsonNode nodeProduct : node) {
				lst.add(nodeProduct.get("symbol").asText());
			}
		} catch (IOException e) {
			log.error("get {} products failed.", getExchangeName().name(), e);
		}
		return lst;
	}

	@Override
	public Map<String, Double> getTickSize() {
		List<String> lstProducts = getProducts();
		Map<String, Double> map = new HashMap<>();
		for(String product : lstProducts) {
			try {
				HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
				urlBuilder.addPathSegments("spot/api/v3/market_summary");
				urlBuilder.addEncodedQueryParameter("symbol", product);
				String result = sendSyncHttpGet(urlBuilder.build().url().toString());
				JsonNode node = objectMapper.readTree(result);
				for(JsonNode nodeProduct : node) {
					map.put(product, nodeProduct.get("size").asDouble());
				}
			} catch (IOException e) {
				log.error("get {} {} ticksize failed.", getExchangeName().name(), product, e);
			}
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
		ObjectNode node = objectMapper.createObjectNode();
		node.put("op", "subscribe");
		ArrayNode arrayNode = node.putArray("args");
		for(String product : lst) {
			arrayNode.add("orderBook:" + product + "_0");
		}
		webSocket.send(node.toString());
	}

	@Override
	public void unsubscribe(Strategy strategy) {
		
	}

	@Override
	public void sendOrder(int orderId, Product product, Side side, double price, double qty, Strategy strategy) {
		
	}

	@Override
	public void sendCancel(int orderId, Strategy strategy) {
		
	}

	@Override
	public Map<String, Double> queryBalance() {
		return null;
	}
	
	@Override
	public Object customizeAction(Object ... objects) {
		BTSECustomizeAction customizeAction = BTSECustomizeAction.valueOf((String) objects[0]);
		switch(customizeAction) {
		case QUERY_CONVERT_RATE:
			try {
				return queryConvertRate((String) objects[1], (String) objects[2]);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	private Double queryConvertRate(String from , String to) throws IOException {
		// https://www.btse.com/api/rateConvert?scrCurrency=BTC&targetCurrency=ETH&amount=1
		HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
		urlBuilder.addPathSegments("/rateConvert");
		urlBuilder.addEncodedQueryParameter("scrCurrency", from);
		urlBuilder.addEncodedQueryParameter("targetCurrency", to);
		urlBuilder.addEncodedQueryParameter("amount", "1");
		Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.build());
		// TODO 改成custom config 
		requestBuilder.addHeader("token", exchangeConfig.getApiKey());
		requestBuilder.addHeader("X-XSRF-TOKEN", exchangeConfig.getSecret());
		
		String response = okHttpClient.newCall(requestBuilder.build()).execute().body().string();
		
		JsonNode node = objectMapper.readTree(response);
		
		return Double.valueOf(node.get("data").asDouble());
	}
	
	private String sendSyncHttpGet(String url) throws IOException {
		return okHttpClient.newCall(new Request.Builder().url(url).build()).execute().body().string();
	}
	
	private WebSocket createWebSocket(String url, WebSocketListener listener) {
		Request request = new Request.Builder().url(url).build();
		return okHttpClient.newWebSocket(request, listener);
	}
}
