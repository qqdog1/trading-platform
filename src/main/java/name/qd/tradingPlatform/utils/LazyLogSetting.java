package name.qd.tradingPlatform.utils;

import java.util.Properties;

public class LazyLogSetting {
	public static Properties prop = System.getProperties();
	
	public static void setDefaultLog() {
		prop.setProperty("log4j.configurationFile", "./config/log4j2.xml");
	}

	public static void setTestLog() {
		prop.setProperty("log4j.configurationFile", "./config/testlog4j2.xml");
	}
}
