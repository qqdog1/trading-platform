package name.qd.tradingPlatform.exchanges.Bittrex;

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

public class BittrexExchange extends Exchange {
	private Logger log = LoggerFactory.getLogger(BittrexExchange.class);
	private ObjectMapper objectMapper = JsonUtils.getObjectMapper();
	private final OkHttpClient okHttpClient = new OkHttpClient.Builder().pingInterval(10, TimeUnit.SECONDS).build();
	private ExchangeConfig exchangeConfig;
	private FileProductMapperManager productMapper;
	private HttpUrl httpUrl;
	
	public BittrexExchange(ExchangeConfig exchangeConfig, FileProductMapperManager productMapper) {
		this.exchangeConfig = exchangeConfig;
		this.productMapper = productMapper;
		httpUrl = HttpUrl.parse(exchangeConfig.getRESTAddr());
	}
	
	@Override
	public ExchangeName getExchangeName() {
		return ExchangeName.Bittrex;
	}

	@Override
	public List<String> getProducts() {
		List<String> lst = new ArrayList<>();
		try {
			HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
			urlBuilder.addPathSegments("public/getmarkets");
			String result = sendSyncHttpGet(urlBuilder.build().url().toString());
			JsonNode node = objectMapper.readTree(result);
			JsonNode dataNode = node.get("result");
			for(JsonNode nodeProduct : dataNode) {
				lst.add(nodeProduct.get("MarketName").asText());
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
			urlBuilder.addPathSegments("public/getorderbook");
			urlBuilder.addEncodedQueryParameter("market", product);
			urlBuilder.addEncodedQueryParameter("type", "both");
			result = sendSyncHttpGet(urlBuilder.build().url().toString());
			JsonNode node = objectMapper.readTree(result);
			JsonNode dataNode = node.get("result");
			JsonNode bidNode = dataNode.get("buy").get(0);
			JsonNode askNode = dataNode.get("sell").get(0);
			lst.add(bidNode.get("Rate").asDouble());
			lst.add(askNode.get("Rate").asDouble());
		} catch (IOException e) {
			log.error("get instant price failed.", e);
		} catch (NullPointerException e) {
			log.error("unknow message {}", result);
			return lst;
		}
		return lst;
	}

	@Override
	public void subscribe(Product[] products, Strategy strategy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendOrder(int orderId, Product product, Side side, double price, double qty, Strategy strategy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendCancel(int orderId, Strategy strategy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, Double> queryBalance() {
		// TODO Auto-generated method stub
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
