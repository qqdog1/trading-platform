package name.qd.tradingPlatform.exchanges;

import name.qd.tradingPlatform.Constants.ExchangeName;

public interface ExchangeConfigLoader {
	public ExchangeConfig getExchangeConfig(ExchangeName exchangeName);
}
