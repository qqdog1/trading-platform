package name.qd.tradingPlatform.product;

import java.util.List;

import name.qd.tradingPlatform.Constants.Product;

public interface ProductMapper {
	public String getExchangeProductString(Product product);
	public Product getProduct(String product);
	public List<Product> getProducts();
}
