package name.qd.tradingPlatform.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import name.qd.tradingPlatform.exchanges.Exchange;
import name.qd.tradingPlatform.exchanges.ExchangeManager;
import name.qd.tradingPlatform.Constants.ExchangeName;
import name.qd.tradingPlatform.Constants.Product;

public class ProductMapperScanner {
	
	private ProductMapperScanner() {
		long timestamp = System.currentTimeMillis();
		for(ExchangeName exchangeName : ExchangeName.values()) {
			List<String> lst = getExchangeProducts(exchangeName);
			if(lst == null) continue;
			List<String> lstResult = parseProducts(exchangeName, lst);
			writeFile(exchangeName, lstResult);
		}
		System.out.println(System.currentTimeMillis() - timestamp);
		System.exit(1);
	}
	
	private List<String> getExchangeProducts(ExchangeName exchangeName) {
		Exchange exchange = ExchangeManager.getInstance().getExchange(exchangeName);
		return (exchange == null) ? null : exchange.getProducts();
	}
	
	private List<String> parseProducts(ExchangeName exchangeName, List<String> lst) {
		List<String> lstResult = new ArrayList<>();
		for(Product product : Product.values()) {
			for(String symbol : lst) {
				if(isSame(exchangeName, product, symbol)) {
					lstResult.add(product.name() + "=" + symbol);
				}
			}
		}
		return lstResult;
	}
	
	private boolean isSame(ExchangeName exchangeName, Product product, String symbol) {
		String productString = product.name().replace("_", "").toLowerCase();
		symbol = ProductUtils.getProductString(exchangeName, symbol);
		return productString.equals(symbol);
	}
	
	private void writeFile(ExchangeName exchangeName, List<String> lst) {
		File file = new File(ProductUtils.getProductFilePath(exchangeName));
		Path path = file.toPath();
		if(Files.exists(path)) {
			try {
				Files.deleteIfExists(path);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			Files.createFile(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			Files.write(path, lst);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new ProductMapperScanner();
	}
}
