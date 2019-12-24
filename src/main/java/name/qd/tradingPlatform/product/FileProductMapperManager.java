package name.qd.tradingPlatform.product;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;

public class FileProductMapperManager {
	private Map<ExchangeName, ProductMapper> mapProductMapper = new HashMap<>();
	
	public FileProductMapperManager() {
		for(ExchangeName exchangeName : ExchangeName.values()) {
			NullFileProductMapper mapper = new NullFileProductMapper(exchangeName, this);
			mapProductMapper.put(exchangeName, mapper);
		}
	}
	
	public void initProductMapper(ExchangeName exchangeName) {
		FileProductMapper fileProductMapper = new FileProductMapper(exchangeName);
		mapProductMapper.put(exchangeName, fileProductMapper);
	}

	public String getExchangeProductString(Product product, ExchangeName exchangeName) {
		return mapProductMapper.get(exchangeName).getExchangeProductString(product);
	}

	public Product getProduct(String product, ExchangeName exchangeName) {
		return mapProductMapper.get(exchangeName).getProduct(product);
	}

	public List<Product> getProducts(ExchangeName exchangeName) {
		return mapProductMapper.get(exchangeName).getProducts();
	}
	
	public String initByGetExchangeProductString(Product product, ExchangeName exchangeName) {
		initProductMapper(exchangeName);
		return getExchangeProductString(product, exchangeName);
	}
	
	public Product initByGetProduct(String product, ExchangeName exchangeName) {
		initProductMapper(exchangeName);
		return getProduct(product, exchangeName);
	}
	
	public List<Product> initByGetProducts(ExchangeName exchangeName) {
		initProductMapper(exchangeName);
		return getProducts(exchangeName);
	}
}
