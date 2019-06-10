package name.qd.tradingPlatform.product;

import java.util.List;

import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;

public interface ProductMapper {
	public String getExchangeProductString(Product product, ExchangeName exchangeName);
	public Product getProduct(String product, ExchangeName exchangeName);
	public List<Product> getProducts(ExchangeName exchangeName);
}
