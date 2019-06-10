package name.qd.tradingPlatform.exchanges.Binance;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import name.qd.tradingPlatform.strategies.Strategy;
import name.qd.tradingPlatform.utils.JsonUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class BinanceHttpCallback implements Callback {
	private Logger log = LoggerFactory.getLogger(BinanceHttpCallback.class);
	private Strategy strategy;
	private ObjectMapper objectMapper = JsonUtils.getObjectMapper();

	public BinanceHttpCallback(Strategy strategy) {
		this.strategy = strategy;
	}
	
	@Override
	public void onFailure(Call call, IOException e) {
		log.error("order onFailure", e);
	}

	@Override
	public void onResponse(Call call, Response response) throws IOException {
		JsonNode node = objectMapper.readTree(response.body().string());
		if(node.has("status")) {
			String status = node.get("status").asText();
			int orderId = node.get("clientOrderId").asInt();
			switch(status) {
			case "FILLED":
				double qty = node.get("executedQty").asDouble();
				double price = node.get("price").asDouble();
				strategy.onFill(orderId, price, qty);
				break;
			case "EXPIRED":
				strategy.onCancelAck(orderId);
				break;
			default:
				log.error("unknow order response status. {}", node.toString());
				break;
			}
		} else {
			// TODO send order rej
			log.error("Order failed. {}", node.toString());
		}
	}
}
