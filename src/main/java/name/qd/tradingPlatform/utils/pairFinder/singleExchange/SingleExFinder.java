package name.qd.tradingPlatform.utils.pairFinder.singleExchange;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.tradingPlatform.exchanges.Exchange;

public abstract class SingleExFinder {
	private Logger log = LoggerFactory.getLogger(SingleExFinder.class);
	protected Exchange exchange;
	protected double threshold = 1;

	public SingleExFinder() {
	}
	
	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}
	
	protected void findPair(String[] products) {
		String product1;
		String product2;
		String product3;
		
		for(int i = 0 ; i < products.length -2 ; i++) {
			product1 = products[i];
			for(int j = i+1 ; j < products.length -1 ; j++) {
				product2 = products[j];
				for(int k = j+1 ; k < products.length ; k++) {
					product3 = products[k];
					List<String> lst = getCycleList(product1, product2, product3);
					if(lst != null && lst.size() > 0) {
						calcRate(lst);
					}
				}
			}
		}
	}
	
	public List<String> getCycleList(String p1, String p2, String p3) {
		List<String> lst = new ArrayList<>();
		String[] s1 = getPair(p1);
		String[] s2 = getPair(p2);
		String[] s3 = getPair(p3);
		
		if(s1 == null || s2 == null || s3 == null) {
//			log.error("unknow product. {}:{}:{}", p1, p2, p3);
			return null;
		}
		
		if(s1[0].equals(s2[0])) {
			if(s1[1].equals(s3[0]) && s2[1].equals(s3[1])) {
				lst.add(p1);
				lst.add(p3);
				lst.add(p2);
			} else if(s2[1].equals(s3[0]) && s1[1].equals(s3[1])) {
				lst.add(p2);
				lst.add(p3);
				lst.add(p1);
			}
		} else if(s1[0].equals(s3[0])) {
			if(s1[1].equals(s2[0]) && s2[1].equals(s3[1])) {
				lst.add(p1);
				lst.add(p2);
				lst.add(p3);
			} else if(s2[0].equals(s3[1]) && s1[1].equals(s2[1])) {
				lst.add(p3);
				lst.add(p2);
				lst.add(p1);
			}
		} else if(s2[0].equals(s3[0])) {
			if(s1[0].equals(s2[1]) && s1[1].equals(s3[1])) {
				lst.add(p2);
				lst.add(p1);
				lst.add(p3);
			} else if(s1[0].equals(s3[1]) && s1[1].equals(s2[1])) {
				lst.add(p3);
				lst.add(p1);
				lst.add(p2);
			}
		}
		
		// return lst, a/b b/c a/c
		return lst;
	}
	
	public abstract String[] getPair(String product);
	public abstract List<Double> getPrice(String product);
	
	private void calcRate(List<String> lst) {
		List<Double> price3 = getPrice(lst.get(2));
		List<Double> price2 = getPrice(lst.get(1));
		List<Double> price1 = getPrice(lst.get(0));
		
		try {
			// A/C sell price / B/C buy price
			double rate = price3.get(1) / price2.get(0);
			double benefit = price1.get(0) / rate;
			if(benefit > threshold) {
				log.info("s:{}@{}, s:{}@{}, b:{}@{} - {}", lst.get(0), price1.get(0), lst.get(1), price2.get(0), lst.get(2), price3.get(1), benefit);
			}
			
			rate = price3.get(0) / price2.get(1);
			benefit = rate / price1.get(1);
			if(benefit > threshold) {
				log.info("b:{}@{}, b:{}@{}, s:{}@{} - {}", lst.get(0), price1.get(1), lst.get(1), price2.get(1), lst.get(2), price3.get(0), benefit);
			}
		} catch(IndexOutOfBoundsException e) {
			
		}
	}
}
