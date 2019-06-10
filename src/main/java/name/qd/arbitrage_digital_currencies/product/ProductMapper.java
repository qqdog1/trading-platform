package name.qd.arbitrage_digital_currencies.product;

import java.util.List;

import name.qd.arbitrage_digital_currencies.Constants.ExchangeName;
import name.qd.arbitrage_digital_currencies.Constants.Product;

public interface ProductMapper {
	public String getExchangeProductString(Product product, ExchangeName exchangeName);
	public Product getProduct(String product, ExchangeName exchangeName);
	public List<Product> getProducts(ExchangeName exchangeName);
}
