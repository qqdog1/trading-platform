package name.qd.tradingPlatform.product;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.tradingPlatform.utils.ProductUtils;
import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;

public class FileProductMapper implements ProductMapper {
	private Logger log = LoggerFactory.getLogger(FileProductMapper.class);
	private Map<Product, String> mapProductToString = new HashMap<>();
	private Map<String, Product> mapStringToProduct = new HashMap<>();

	public FileProductMapper(ExchangeName exchangeName) {
		try (FileInputStream fIn = new FileInputStream(ProductUtils.getProductFilePath(exchangeName))) {
			Properties properties = new Properties();
			properties.load(fIn);
			for (Product product : Product.values()) {
				String productString = properties.getProperty(product.name());
				if (productString != null) {
					mapProductToString.put(product, productString);
					mapStringToProduct.put(productString, product);
				}
			}
		} catch (FileNotFoundException e) {
			log.error("{} exchange product file not found.", exchangeName.name(), e);
		} catch (IOException e) {
			log.error("Read {} exchange product file failed.", exchangeName.name(), e);
		}
	}

	@Override
	public String getExchangeProductString(Product product) {
		return mapProductToString.get(product);
	}

	@Override
	public Product getProduct(String product) {
		return mapStringToProduct.get(product);
	}

	@Override
	public List<Product> getProducts() {
		List<Product> lst = new ArrayList<>();
		for (Product product : mapStringToProduct.values()) {
			lst.add(product);
		}
		return lst;
	}
}
