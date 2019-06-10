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
	private Map<ExchangeName, Map<Product, String>> mapProductToString = new HashMap<>();
	private Map<ExchangeName, Map<String, Product>> mapStringToProduct = new HashMap<>();
	
	public FileProductMapper() {
		for(ExchangeName exchangeName : ExchangeName.values()) {
			mapProductToString.put(exchangeName, new HashMap<>());
			mapStringToProduct.put(exchangeName, new HashMap<>());
			try (FileInputStream fIn = new FileInputStream(ProductUtils.getProductFilePath(exchangeName))) {
				Properties properties = new Properties();
				properties.load(fIn);
				for(Product product : Product.values()) {
					String productString = properties.getProperty(product.name());
					if(productString != null) {
						mapProductToString.get(exchangeName).put(product, productString);
						mapStringToProduct.get(exchangeName).put(productString, product);
					}
				}
			} catch (FileNotFoundException e) {
				log.error("{} exchange product file not found.", exchangeName.name(), e);
			} catch (IOException e) {
				log.error("Read {} exchange product file failed.", exchangeName.name(), e);
			}
		}
	}

	@Override
	public String getExchangeProductString(Product product, ExchangeName exchangeName) {
		return mapProductToString.get(exchangeName).get(product);
	}

	@Override
	public Product getProduct(String product, ExchangeName exchangeName) {
		return mapStringToProduct.get(exchangeName).get(product);
	}

	@Override
	public List<Product> getProducts(ExchangeName exchangeName) {
		List<Product> lst = new ArrayList<>();
		for(Product product : mapStringToProduct.get(exchangeName).values()) {
			lst.add(product);
		}
		return lst;
	}
}
