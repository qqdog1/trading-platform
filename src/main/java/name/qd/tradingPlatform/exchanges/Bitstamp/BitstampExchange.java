package name.qd.tradingPlatform.exchanges.Bitstamp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

public class BitstampExchange extends Exchange {
	private Logger log = LoggerFactory.getLogger(BitstampExchange.class);
	private ObjectMapper objectMapper = JsonUtils.getObjectMapper();
	private final OkHttpClient okHttpClient = new OkHttpClient.Builder().pingInterval(10, TimeUnit.SECONDS).build();
	private ExchangeConfig exchangeConfig;
	private FileProductMapperManager productMapper;
	private HttpUrl httpUrl;
	
	public BitstampExchange(ExchangeConfig exchangeConfig, FileProductMapperManager productMapper) {
		this.exchangeConfig = exchangeConfig;
		this.productMapper = productMapper;
		httpUrl = HttpUrl.parse(exchangeConfig.getRESTAddr());
	}

	@Override
	public ExchangeName getExchangeName() {
		return ExchangeName.Bitstamp;
	}

	@Override
	public List<String> getProducts() {
		List<String> lst = new ArrayList<>();
		try {
			HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
			urlBuilder.addPathSegments("v2/trading-pairs-info");
			String result = sendSyncHttpGet(urlBuilder.build().url().toString());
			JsonNode node = objectMapper.readTree(result);
			for(JsonNode dataNode : node) {
				lst.add(dataNode.get("url_symbol").asText());
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
		try {
			HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
			urlBuilder.addPathSegments("v2/order_book");
			urlBuilder.addPathSegments(product);
			String result = sendSyncHttpGet(urlBuilder.build().url().toString());
			JsonNode node = objectMapper.readTree(result);
			JsonNode bidNode = node.get("bids").get(0);
			JsonNode askNode = node.get("asks").get(0);
			lst.add(bidNode.get(0).asDouble());
			lst.add(askNode.get(0).asDouble());
		} catch (IOException e) {
			log.error("get instant price failed.", e);
		}
		return lst;
	}

	@Override
	public void subscribe(Product[] products, Strategy strategy) {
		
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
