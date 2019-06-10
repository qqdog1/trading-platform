package name.qd.tradingPlatform.exchanges;

public class ExchangeConfig {
	private String webSocketAddr;
	private String RESTAddr;
	
	private String apiKey;
	private String secret;

	public String getRESTAddr() {
		return RESTAddr;
	}
	
	public void setRESTAddr(String RESTAddr) {
		this.RESTAddr = RESTAddr;
	}
	
	public String getWebSocketAddr() {
		return webSocketAddr;
	}

	public void setWebSocketAddr(String webSocketAddr) {
		this.webSocketAddr = webSocketAddr;
	}
	
	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}
}
