package name.qd.arbitrage_digital_currencies.exchanges;

import name.qd.arbitrage_digital_currencies.Constants.ExchangeName;

public interface ExchangeConfigLoader {
	public ExchangeConfig getExchangeConfig(ExchangeName exchangeName);
}
