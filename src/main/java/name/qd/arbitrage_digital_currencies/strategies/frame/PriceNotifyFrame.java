package name.qd.arbitrage_digital_currencies.strategies.frame;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import name.qd.arbitrage_digital_currencies.Constants.Product;
import name.qd.arbitrage_digital_currencies.Constants.Side;
import name.qd.arbitrage_digital_currencies.strategies.order.SidePrice;
import name.qd.arbitrage_digital_currencies.strategies.order.TradeCycle;

public class PriceNotifyFrame {
	private JFrame frame;
	private GridBagConstraints gridBagConstraints = new GridBagConstraints();
	private JPanel panel = new JPanel();
	private JScrollPane scrollPane = new JScrollPane(panel);
	private List<JLabel[]> lstlbl = new ArrayList<>();
	private List<JTextField[]> lstTf = new ArrayList<>();
	private List<TradeCycle> lstTradeCycle = new ArrayList<>();
	private DecimalFormat df = new DecimalFormat("#.########");
	
	private int dataLine = 0;
	private int data_X = 0;

	public PriceNotifyFrame() {
		frame = new JFrame("Notifier");
		frame.setSize(1200, 768);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		frame.add(scrollPane);
		
		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
		panel.setLayout(new GridBagLayout());
	}
	
	public void setTradeCycle(TradeCycle tradeCycle) {
		lstTradeCycle.add(tradeCycle);
		
		JLabel[] lbls = new JLabel[4];
		JTextField[] tfs = new JTextField[4];
		lbls[0] = new JLabel(tradeCycle.getAB().getProduct()+"_"+tradeCycle.getAB().getSide());
		lbls[1] = new JLabel(tradeCycle.getAC().getProduct()+"_"+tradeCycle.getAC().getSide());
		lbls[2] = new JLabel(tradeCycle.getBC().getProduct()+"_"+tradeCycle.getBC().getSide());
		lbls[3] = new JLabel("PNL:");
		tfs[0] = new JTextField(df.format(tradeCycle.getAB().getPrice()));
		tfs[1] = new JTextField(df.format(tradeCycle.getAC().getPrice()));
		tfs[2] = new JTextField(df.format(tradeCycle.getBC().getPrice()));
		tfs[3] = new JTextField("");
		lstlbl.add(lbls);
		lstTf.add(tfs);
		
		if(data_X == 2) {
			dataLine++;
			data_X = 0;
		}
		
		addToSelectPanel(lbls[0], (data_X*8)+0, dataLine);
		addToSelectPanel(tfs[0], (data_X*8)+1, dataLine);
		addToSelectPanel(lbls[1], (data_X*8)+2, dataLine);
		addToSelectPanel(tfs[1], (data_X*8)+3, dataLine);
		addToSelectPanel(lbls[2], (data_X*8)+4, dataLine);
		addToSelectPanel(tfs[2], (data_X*8)+5, dataLine);
		addToSelectPanel(lbls[3], (data_X*8)+6, dataLine);
		addToSelectPanel(tfs[3], (data_X*8)+7, dataLine);
		
		data_X++;
	}
	
	public void update() {
		for(int i = 0 ; i < lstTradeCycle.size() ; i++) {
			TradeCycle tradeCycle = lstTradeCycle.get(i);
			JTextField[] tfs = lstTf.get(i);
			tfs[0].setText(df.format(tradeCycle.getAB().getPrice()));
			tfs[1].setText(df.format(tradeCycle.getAC().getPrice()));
			tfs[2].setText(df.format(tradeCycle.getBC().getPrice()));
			tfs[3].setText(df.format(tradeCycle.getPnl()));
		}
	}
	
	private void addToSelectPanel(Component comp, int x, int y) {
		gridBagConstraints.gridx = x;
		gridBagConstraints.gridy = y;
		panel.add(comp, gridBagConstraints);
	}
	
	public static void main(String[] s) {
		PriceNotifyFrame frame = new PriceNotifyFrame();
		Random random = new Random();
		SidePrice sp1 = new SidePrice();
		sp1.setProduct(Product.BCD_TWD);
		sp1.setSide(Side.buy);
		sp1.setPrice(random.nextInt(987654));
		sp1.setQty(random.nextInt(100));
		SidePrice sp2 = new SidePrice();
		sp2.setProduct(Product.BCD_TWD);
		sp2.setSide(Side.sell);
		sp2.setPrice(random.nextInt(987654));
		sp2.setQty(random.nextInt(100));
		SidePrice sp3 = new SidePrice();
		sp3.setProduct(Product.BCD_TWD);
		sp3.setSide(Side.sell);
		sp3.setPrice(random.nextInt(987654));
		sp3.setQty(random.nextInt(100));
		TradeCycle tc = new TradeCycle(sp1, sp2, sp3);
		tc.setPnl(1);
		
		frame.setTradeCycle(tc);
		frame.setTradeCycle(tc);

		while(true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			sp1.setPrice(random.nextInt(987654));
			sp1.setQty(random.nextInt(100));
			sp2.setPrice(random.nextInt(987654));
			sp2.setQty(random.nextInt(100));
			sp3.setPrice(random.nextInt(987654));
			sp3.setQty(random.nextInt(100));
			frame.update();
		}
	}
}
