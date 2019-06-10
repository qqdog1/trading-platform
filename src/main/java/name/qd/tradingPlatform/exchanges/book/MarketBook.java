package name.qd.tradingPlatform.exchanges.book;

import name.qd.tradingPlatform.Constants.Side;

public class MarketBook {
	private SideBook bidBook;
	private SideBook askBook;

	public MarketBook() {
		bidBook = new SideBook(Side.buy);
		askBook = new SideBook(Side.sell);
	}
	
	public void addQty(Side side, double price, double qty) {
		switch(side) {
		case buy:
			bidBook.add(price, qty);
			break;
		case sell:
			askBook.add(price, qty);
			break;
		}
	}
	
	public void addBidQuote(double price, double qty) {
		bidBook.add(price, qty);
	}

	public void addAskQuote(double price, double qty) {
		askBook.add(price, qty);
	}
	
	public void subQty(Side side, double price, double qty) {
		switch(side) {
		case buy:
			bidBook.sub(price, qty);
			break;
		case sell:
			askBook.sub(price, qty);
			break;
		}
	}
	
	public void subBidQuote(double price, double qty) {
		bidBook.sub(price, qty);
	}

	public void subAskQuote(double price, double qty) {
		askBook.sub(price, qty);
	}

	public void upsertQuote(Side side, double price, double qty) {
		switch (side) {
		case buy:
			bidBook.upsert(price, qty);
			break;
		case sell:
			askBook.upsert(price, qty);
			break;
		}
	}

	public void removeQuote(Side side, double price) {
		switch (side) {
		case buy:
			bidBook.remove(price);
			break;
		case sell:
			askBook.remove(price);
			break;
		}
	}

	private void addQuote(SideBook book, double price, double qty) {
		book.upsert(price, qty);
	}

	public Double[] topPrice(Side side, int n) {
		SideBook book = (side == Side.buy) ? bidBook : askBook;
		return book.topPrice(n);
	}

	public Double[] topQty(Side side, int n) {
		SideBook book = (side == Side.buy) ? bidBook : askBook;
		return book.topQty(n);
	}
}
