package name.qd.tradingPlatform.product;

import java.util.List;

import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;

public class NullFileProductMapper implements ProductMapper {
	private FileProductMapperManager manager;
	private ExchangeName exchangeName;
	
	public NullFileProductMapper(ExchangeName exchangeName, FileProductMapperManager manager) {
		this.manager = manager;
		this.exchangeName = exchangeName;
	}

	@Override
	public String getExchangeProductString(Product product) {
		return manager.initByGetExchangeProductString(product, exchangeName);
	}

	@Override
	public Product getProduct(String product) {
		return manager.initByGetProduct(product, exchangeName);
	}

	@Override
	public List<Product> getProducts() {
		return manager.initByGetProducts(exchangeName);
	}
}
