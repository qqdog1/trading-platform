package name.qd.tradingPlatform.utils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import name.qd.tradingPlatform.Constants.FiatMoney;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class FiatMoneyRateUtil {
	private static FiatMoneyRateUtil instance = new FiatMoneyRateUtil();
	private static Logger log = LoggerFactory.getLogger(FiatMoneyRateUtil.class);
	private final OkHttpClient okHttpClient = new OkHttpClient.Builder().pingInterval(10, TimeUnit.SECONDS).build();
	private HttpUrl httpUrl;
	private ObjectMapper objectMapper = JsonUtils.getObjectMapper();
	private JsonNode node;
	
	public static FiatMoneyRateUtil getInstance() {
		return instance;
	}
	
	private FiatMoneyRateUtil() {
		httpUrl = HttpUrl.parse("https://tw.rter.info/capi.php");
		try {
			HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
			String result = sendSyncHttpGet(urlBuilder.build().url().toString());
			node = objectMapper.readTree(result);
		} catch (IOException e) {
			log.error("get exchangeinfo failed.", e);
		}
	}
	
	public double getRate(FiatMoney quoteCurrency, FiatMoney baseCurrency) {
		String name = quoteCurrency.name() + baseCurrency.name();
		JsonNode currencyNode = node.get(name);
		return currencyNode.get("Exrate").asDouble();
	}
	
	private String sendSyncHttpGet(String url) throws IOException {
		return okHttpClient.newCall(new Request.Builder().url(url).build()).execute().body().string();
	}
	
	public static void main(String[] s) {
		FiatMoneyRateUtil bank = new FiatMoneyRateUtil();
		double rate = bank.getRate(FiatMoney.USD, FiatMoney.TWD);
		System.out.println(rate);
	}
}
