package name.qd.tradingPlatform.exchanges;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.tradingPlatform.Constants.ExchangeName;

public class FileExchangeConfigLoader implements ExchangeConfigLoader {
	private Logger log = LoggerFactory.getLogger(FileExchangeConfigLoader.class);

	@Override
	public ExchangeConfig getExchangeConfig(ExchangeName exchangeName) {
		StringBuilder sb = new StringBuilder();
		sb.append("/config/").append(exchangeName.name()).append(".txt");
		try (FileInputStream fIn = new FileInputStream(sb.toString())) {
			Properties properties = new Properties();
			properties.load(fIn);
			
			ExchangeConfig exchangeConfig = new ExchangeConfig();
			exchangeConfig.setRESTAddr(properties.getProperty("rest"));
			exchangeConfig.setWebSocketAddr(properties.getProperty("ws"));
			exchangeConfig.setApiKey(properties.getProperty("key"));
			exchangeConfig.setSecret(properties.getProperty("secret"));
			return exchangeConfig;
		} catch (FileNotFoundException e) {
			log.error("{} exchange config not exist.", exchangeName.name(), e);
		} catch (IOException e) {
			log.error("Get {} exchange config fail.", exchangeName.name(), e);
		}
		return null;
	}

}
