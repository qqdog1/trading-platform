package name.qd.tradingPlatform.utils;

import java.util.Properties;

public class LazyLogSetting {

	public static void setTestLog() {
		Properties prop = System.getProperties();
		prop.setProperty("log4j.configurationFile", "./config/testlog4j2.xml");
	}
}
