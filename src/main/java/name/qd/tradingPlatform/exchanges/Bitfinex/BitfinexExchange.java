package name.qd.tradingPlatform.exchanges.Bitfinex;

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

public class BitfinexExchange implements Exchange {
	private static Logger log = LoggerFactory.getLogger(BitfinexExchange.class);
	private ObjectMapper objectMapper = JsonUtils.getObjectMapper();
	private final ExchangeConfig exchangeConfig;
	private Map<String, List<Strategy>> mapStrategies = new HashMap<>();
	private final OkHttpClient okHttpClient = new OkHttpClient.Builder().pingInterval(10, TimeUnit.SECONDS).build();
	private final FileProductMapperManager productMapper;
	private WebSocket webSocket;
	private ChannelMessageHandler channelMessageHandler;
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private HttpUrl httpUrl;
	
	public BitfinexExchange(ExchangeConfig exchangeConfig, FileProductMapperManager productMapper) {
		this.exchangeConfig = exchangeConfig;
		this.productMapper = productMapper;
		channelMessageHandler = new BitfinexChannelMessageHandler(mapStrategies, productMapper, getExchangeName());
		webSocket = createWebSocket(exchangeConfig.getWebSocketAddr(), new ExchangeWebSocketListener(channelMessageHandler));
		httpUrl = HttpUrl.parse(exchangeConfig.getRESTAddr());
		executor.execute(channelMessageHandler);
		initRequestBuilder();
	}
	
	private void initRequestBuilder() {
		
	}
	
	@Override
	public ExchangeName getExchangeName() {
		return ExchangeName.Bitfinex;
	}
	
	@Override
	public List<String> getProducts() {
		List<String> lst = new ArrayList<>();
		try {
			HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
			urlBuilder.addPathSegments("symbols");
			String result = sendSyncHttpGet(urlBuilder.build().url().toString());
			JsonNode node = objectMapper.readTree(result);
			for(JsonNode productNode : node) {
				lst.add(productNode.asText());
			}
		} catch (IOException e) {
			log.error("get symbols failed.", e);
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
			urlBuilder.addPathSegments("book");
			urlBuilder.addPathSegments(product);
			urlBuilder.addEncodedQueryParameter("limit_bids", "1");
			urlBuilder.addEncodedQueryParameter("limit_asks", "1");
			result = sendSyncHttpGet(urlBuilder.build().url().toString());
			JsonNode node = objectMapper.readTree(result);
			JsonNode bidNode = node.get("bids").get(0);
			JsonNode askNode = node.get("asks").get(0);
			lst.add(bidNode.get("price").asDouble());
			lst.add(askNode.get("price").asDouble());
		} catch (IOException e) {
			log.error("get instant price failed. {}", result);
		} catch (NullPointerException e) {
			log.error("{} {}", result, product);
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e1) {
				log.error(e1.getMessage(), e1);
			}
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
		webSocket.send(createSubscribeString(productString));
	}
	
	private String createSubscribeString(String product) {
		ObjectNode node = objectMapper.createObjectNode();
		node.put("event", "subscribe");
		node.put("channel", "book");
		node.put("symbol", product);
		return node.toString();
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
