package name.qd.arbitrage_digital_currencies.strategies.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BalanceChecker {
	private static Logger log = LoggerFactory.getLogger(BalanceChecker.class);
	private Map<String, Double> mapBalance = new HashMap<>();
	private Map<String, Double> mapTolerable = new HashMap<>();
	
	public boolean isBalanceDecrease(Map<String, Double> newBalance) {
		Set<String> set = getUnionSet(mapBalance.keySet(), newBalance.keySet());
		boolean isDecrease = false;
		for(String currency : set) {
			double tolerable = getTolerable(currency);
			if(mapBalance.containsKey(currency)) {
				double oldValue = mapBalance.get(currency);
				if(newBalance.containsKey(currency)) {
					double newValue = newBalance.get(currency);
					if(oldValue - newValue > tolerable) {
						isDecrease = true;
						log.info("Balance Decrease !! [{}]:{}->{}, {}", currency, oldValue, newValue, newValue-oldValue);
					} else if(newValue - oldValue > 0) {
						log.info("Balance Increase ! [{}]:{}->{}, {}", currency, oldValue, newValue, newValue-oldValue);
					}
				} else {
					if(mapBalance.get(currency) > 0) {
						isDecrease = true;
						log.info("Balance Decrease !! [{}]:{}->{}, {}", currency, oldValue, 0, -oldValue);
					}
				}
			} else if(newBalance.containsKey(currency)){
				double newValue = newBalance.get(currency);
				if(newValue > 0) {
					log.info("Balance Increase ! [{}]:{}->{}, {}", currency, 0, newValue, newValue);
				}
			}
		}
//		setNewBalance(newBalance);
		return isDecrease;
	}
	
	public void setNewBalance(Map<String, Double> map) {
		this.mapBalance = map;
	}
	
	public Map<String, Double> getCurrentBalance() {
		return mapBalance;
	}
	
	public void setTolerable(String currency, double value) {
		mapTolerable.put(currency, value);
	}
	
	public void setTolerable(Map<String, Double> map) {
		for(String currency : map.keySet()) {
			mapTolerable.put(currency, map.get(currency));
		}
	}
	
	private double getTolerable(String currency) {
		double tolerable = 0;
		if(mapTolerable.containsKey(currency)) {
			tolerable = mapTolerable.get(currency);
		}
		return tolerable;
	}
	
	private Set<String> getUnionSet(Set<String> set1, Set<String> set2) {
		Set<String> set = new HashSet<>();
		set.addAll(set1);
		set.addAll(set2);
		return set;
	}
	
	public static void main(String[] s) {
		BalanceChecker bc = new BalanceChecker();
		
		Map<String, Double> m1 = new HashMap<>();
		Map<String, Double> m2 = new HashMap<>();
		
		m1.put("A", 100d);
		m1.put("B", 30d);
		m2.put("B", 28d);
		m2.put("C", 40d);
		
		bc.setTolerable("B", 5d);
		
		bc.isBalanceDecrease(m1);
		bc.isBalanceDecrease(m2);
	}
}
