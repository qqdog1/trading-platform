package name.qd.arbitrage_digital_currencies.utils;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaScriptRunner {
	private static Logger log = LoggerFactory.getLogger(JavaScriptRunner.class);
	private final ScriptEngineManager scriptEngineManager;
	private final ScriptEngine scriptEngine;
	
	public JavaScriptRunner() {
		scriptEngineManager = new ScriptEngineManager();
		scriptEngine = scriptEngineManager.getEngineByName("JavaScript");
	}
	
	public String runFunction(String function, String funName) {
		String result = null;
		try {
			scriptEngine.eval(function);
			Invocable inv = (Invocable)scriptEngine;
			Object obj = inv.invokeFunction(funName, "qq");
			result = obj.toString();
		} catch (ScriptException | NoSuchMethodException e) {
			log.error("run javascript function failed.", e);
		}
		return result;
	}
	
	public String[] parseCloudFlareFunc(String script) {
		String[] s = script.split("\n");
		String[] result = new String[3];
		
		String jschl_vc = null;
		String pass = null;
		for(int i = 0; i < s.length; i++) {
			System.out.println(s[i]);
			if(s[i].contains("jschl_vc")) {
				result[0] = s[i].substring(48, s[i].length() - 3);
			} else if(s[i].contains("pass")) {
				result[1] = s[i].substring(44, s[i].length() - 3);
			}
		}
		System.out.println(jschl_vc);
		System.out.println(pass);
		
		StringBuilder sb = new StringBuilder();
		sb.append("function qq(){")
		.append(s[29])
		.append(s[36].replace("a.value", "a").replace("t.length", "17"))
		.append("\r\nreturn a;}");
		String answer = runFunction(sb.toString(), "qq");
		System.out.println(answer);
		int ans = (int)Double.parseDouble(answer.toString());
		System.out.println(ans);
		result[2] = String.valueOf(ans);
		return result;
	}
	
	public static void main(String[] s) {
		new JavaScriptRunner();
	}
}
