package name.qd.tradingPlatform.exchanges.book;

import java.util.Comparator;
import java.util.TreeMap;

import name.qd.tradingPlatform.Constants.Side;

public class SideBook {
	private final TreeMap<Double, Double> bookMap;
	
	public SideBook(Side side) {
		switch(side) {
		case buy:
			this.bookMap= new TreeMap<>(Comparator.reverseOrder());
			break;
		case sell:
			this.bookMap= new TreeMap<>();
			break;
		default:
			throw new RuntimeException("not supported side");
		}
	}
	
	public void add(double price, double qty) {
		double currentQty = 0;
		if(bookMap.containsKey(price)) {
			currentQty = bookMap.get(price);
		}
		currentQty += qty;
		bookMap.put(price, currentQty);
	}
	
	public void sub(double price, double qty) {
		if(!bookMap.containsKey(price)) {
			return;
		}
		double currentQty = bookMap.get(price);
		currentQty -= qty;
		if(currentQty == 0) {
			bookMap.remove(price);
		} else {
			bookMap.put(price, currentQty);
		}
	}
	
    public void upsert(double price, double qty) {
    	Double oldQty = bookMap.computeIfAbsent(price, k -> qty);
    	if(oldQty != null)
    		oldQty = qty;
    }
    
    public void remove(double price) {
        bookMap.remove(price);
	}
    
    public Double[] topPrice(int n) {
		return bookMap.keySet().stream().limit(n).toArray(m -> new Double[m]);
	}
    
    public Double[] topQty(int n) {
		return bookMap.values().stream().limit(n).toArray(m -> new Double[m]);
	}
    
    public Double[] getAllPrice() {
    	return topPrice(bookMap.size());
    }
    
    public Double[] getAllQty() {
    	return topQty(bookMap.size());
    }
    
    public int getSize() {
    	return bookMap.size();
    }
}
