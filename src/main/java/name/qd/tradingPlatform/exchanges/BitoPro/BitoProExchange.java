package name.qd.tradingPlatform.exchanges.BitoPro;

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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

public class BitoProExchange implements Exchange {
	private Logger log = LoggerFactory.getLogger(BitoProExchange.class);
	private ObjectMapper objectMapper = JsonUtils.getObjectMapper();
	private final ExchangeConfig exchangeConfig;
	private final ProductMapper productMapper;
	private Map<String, List<Strategy>> mapStrategies = new HashMap<>();
	private Map<String, ChannelMessageHandler> mapMessageHandler = new HashMap<>();
	private List<OkHttpClient> lstOkHttpClient = new ArrayList<>();
	private int CLIENT_MAX_SOCKET_SIZE = 5;
	private int currentSocketSize = 0;
	private int productSize = 27;
	private ExecutorService executor = Executors.newFixedThreadPool(productSize);
	private Map<String, WebSocket> mapProductWS = new HashMap<>();

	public BitoProExchange(ExchangeConfig exchangeConfig, ProductMapper productMapper) {
		this.exchangeConfig = exchangeConfig;
		this.productMapper = productMapper;
	}
	
	@Override
	public ExchangeName getExchangeName() {
		return ExchangeName.BitoPro;
	}

	@Override
	public List<String> getProducts() {
		return null;
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
				if(currentSocketSize == 0) {
					lstOkHttpClient.add(new OkHttpClient.Builder().pingInterval(10, TimeUnit.SECONDS).build());
				}
				mapStrategies.put(productString, new ArrayList<Strategy>());
				currentSocketSize++;
				if(currentSocketSize == CLIENT_MAX_SOCKET_SIZE) {
					currentSocketSize = 0;
				}
				createWebSocket(product, productString);
			}
			mapStrategies.get(productString).add(strategy);
		}
	}
	
	private void createWebSocket(Product product, String productString) {
		String url = exchangeConfig.getWebSocketAddr() + productString + "/trade_anonymous";
		Request request = new Request.Builder().url(url).build();
		OkHttpClient httpClient = lstOkHttpClient.get(lstOkHttpClient.size()-1);
		ChannelMessageHandler channelMessageHandler = new BitoChannelMessageHandler(mapStrategies.get(productString), product, productString, httpClient);
		mapMessageHandler.put(productString, channelMessageHandler);
		executor.execute(channelMessageHandler);
		WebSocket ws = httpClient.newWebSocket(request, new ExchangeWebSocketListener(channelMessageHandler));
		mapProductWS.put(productString, ws);
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
	public Map<String, Double> getTickSize() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void unsubscribe(Strategy strategy) {
		// TODO Auto-generated method stub
		
	}
	
	public class BitoChannelMessageHandler extends ChannelMessageHandler {
		private ExchangeName exchangeName = ExchangeName.BitoPro;
		private final List<Strategy> lstStrategies;
		private double POWER = 100000000;
		private Product product;
		private String productString;
		private OkHttpClient httpClient;

		public BitoChannelMessageHandler(List<Strategy> lstStrategies, Product product, String productString, OkHttpClient httpClient) {
			this.lstStrategies = lstStrategies;
			this.product = product;
			this.productString = productString;
			this.httpClient = httpClient;
		}
		
		@Override
		public void processMessage(JsonNode jsonNode) {
			int type = jsonNode.get(0).asInt();
			if(type == 51) {
				JsonNode dataNode = jsonNode.get(3);
				JsonNode bidNode = dataNode.get(0);
				JsonNode askNode = dataNode.get(1);
				double bidPrice = 0d;
				double bidQty = 0d;
				if(bidNode.has(0)) {
					bidPrice = bidNode.get(0).get(0).asDouble();
					bidPrice /= POWER;
					bidQty = bidNode.get(0).get(1).asDouble();
					bidQty /= POWER;
				}
				double askPrice = 0d;
				double askQty = 0d;
				if(askNode.has(0)) {
					askPrice = askNode.get(0).get(0).asDouble();
					askPrice /= POWER;
					askQty = askNode.get(0).get(1).asDouble();
					askQty /= POWER;
				}
				for(Strategy strategy : lstStrategies) {
					strategy.onBook(exchangeName, product, bidPrice, bidQty, askPrice, askQty);
				}
			}
		}

		@Override
		public void reconnect() {
			log.warn("{} reconnect", productString);
			String url = exchangeConfig.getWebSocketAddr() + productString + "/trade_anonymous";
			Request request = new Request.Builder().url(url).build();
			mapProductWS.put(productString, httpClient.newWebSocket(request, new ExchangeWebSocketListener(this)));
		}
	}
}
