package name.qd.tradingPlatform.exchanges.Kraken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import name.qd.tradingPlatform.exchanges.Exchange;
import name.qd.tradingPlatform.exchanges.ExchangeConfig;
import name.qd.tradingPlatform.product.FileProductMapperManager;
import name.qd.tradingPlatform.strategies.Strategy;
import name.qd.tradingPlatform.utils.JsonUtils;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class KrakenExchange implements Exchange {
	private Logger log = LoggerFactory.getLogger(KrakenExchange.class);
	private ObjectMapper objectMapper = JsonUtils.getObjectMapper();
	private final OkHttpClient okHttpClient = new OkHttpClient.Builder().pingInterval(10, TimeUnit.SECONDS).build();
	private ExchangeConfig exchangeConfig;
	private FileProductMapperManager productMapper;
	private HttpUrl httpUrl;
	private Set<String> subscribeProducts = new HashSet<>();
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private KrakenMarketGrabber marketGrabber;
	private Map<String, List<Strategy>> mapStrategies = new HashMap<>();
	
	public KrakenExchange(ExchangeConfig exchangeConfig, FileProductMapperManager productMapper) {
		this.exchangeConfig = exchangeConfig;
		this.productMapper = productMapper;
		httpUrl = HttpUrl.parse(exchangeConfig.getRESTAddr());
		marketGrabber = new KrakenMarketGrabber(this, subscribeProducts, mapStrategies, productMapper);
		executor.execute(marketGrabber);
	}
	
	@Override
	public ExchangeName getExchangeName() {
		return ExchangeName.Kraken;
	}
	
	@Override
	public List<String> getProducts() {
		List<String> lst = new ArrayList<>();
		try {
			HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
			urlBuilder.addPathSegments("public/AssetPairs");
			String result = sendSyncHttpGet(urlBuilder.build().url().toString());
			JsonNode node = objectMapper.readTree(result);
			JsonNode dataNode = node.get("result");
			Iterator<JsonNode> it = dataNode.elements();
			while(it.hasNext()) {
				JsonNode symbolNode = (JsonNode)it.next();
				lst.add(symbolNode.get("altname").asText());
			}
		} catch (IOException e) {
			log.error("get symbols failed.");
			return lst;
		} catch (Exception e) {
			log.error("get symbols failed.");
			return lst;
		}
		return lst;
	}
	
	public List<Double> getMarketData(String product) {
		List<Double> lst = new ArrayList<>();
		String result = null;
		try {
			HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
			urlBuilder.addPathSegments("public/Depth");
			urlBuilder.addEncodedQueryParameter("pair", product);
			urlBuilder.addEncodedQueryParameter("count", "1");
			result = sendSyncHttpGet(urlBuilder.build().url().toString());
			JsonNode node = objectMapper.readTree(result);
			JsonNode resultNode = node.get("result");
			JsonNode symbolNode = resultNode.elements().next();
			JsonNode bidNode = symbolNode.get("bids").get(0);
			JsonNode askNode = symbolNode.get("asks").get(0);
			lst.add(bidNode.get(0).asDouble());
			lst.add(bidNode.get(1).asDouble());
			lst.add(askNode.get(0).asDouble());
			lst.add(askNode.get(1).asDouble());
		} catch (Exception e) {
			log.error("get market data failed. {}", result);
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
		List<Double> lstMarketData = getMarketData(product);
		lst.add(lstMarketData.get(0));
		lst.add(lstMarketData.get(2));
		return lst;
	}
	
	@Override
	public void subscribe(Product[] products, Strategy strategy) {
		for(Product product : products) {
			String productString = productMapper.getExchangeProductString(product, getExchangeName());
			if(!mapStrategies.containsKey(productString)) {
				mapStrategies.put(productString, new ArrayList<>());
			}
			mapStrategies.get(productString).add(strategy);
			subscribeProducts.add(productString);
		}
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
