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

import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;
import name.qd.tradingPlatform.Constants.Side;
import name.qd.tradingPlatform.exchanges.ChannelMessageHandler;
import name.qd.tradingPlatform.exchanges.Exchange;
import name.qd.tradingPlatform.exchanges.ExchangeConfig;
import name.qd.tradingPlatform.product.FileProductMapperManager;
import name.qd.tradingPlatform.strategies.Strategy;
import name.qd.tradingPlatform.utils.JsonUtils;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class BTSEExchange implements Exchange {
	private Logger log = LoggerFactory.getLogger(BTSEExchange.class);
	private ObjectMapper objectMapper = JsonUtils.getObjectMapper();
	private final ExchangeConfig exchangeConfig;
	private final FileProductMapperManager productMapper;
	private ChannelMessageHandler channelMessageHandler;
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private HttpUrl httpUrl;
	private Map<String, List<Strategy>> mapStrategies = new HashMap<>();
	private final OkHttpClient okHttpClient = new OkHttpClient.Builder().pingInterval(10, TimeUnit.SECONDS).build();
	
	private static final String SUB_PREFIX = "orderBookApi:";
	
	public BTSEExchange(ExchangeConfig exchangeConfig, FileProductMapperManager productMapper) {
		this.exchangeConfig = exchangeConfig;
		this.productMapper = productMapper;
		httpUrl = HttpUrl.parse(exchangeConfig.getRESTAddr());
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
			log.error("get exchangeinfo failed.", e);
		}
		return lst;
	}

	@Override
	public Map<String, Double> getTickSize() {
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
		List<String> lstProducts = new ArrayList<>();
		for(Product product : products) {
			
		}
		
		
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
	
	private String sendSyncHttpGet(String url) throws IOException {
		return okHttpClient.newCall(new Request.Builder().url(url).build()).execute().body().string();
	}
}
