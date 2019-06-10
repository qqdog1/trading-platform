package name.qd.tradingPlatform.strategies;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.tradingPlatform.utils.StrategyUtils;

public class ControllPanel {
	private static final Logger log = LoggerFactory.getLogger(ControllPanel.class);
	private JFrame frame;
	private GridBagConstraints gridBagConstraints = new GridBagConstraints();
	private JPanel panel = new JPanel();
	private List<Class> lstClass = new ArrayList<>();
	private List<JLabel> lstLabel = new ArrayList<>();
	private List<JButton> lstStartButton = new ArrayList<>();
	private List<JButton> lstStopButton = new ArrayList<>();
	private Map<String, Strategy> mapStrategies = new HashMap<>();
	
	public ControllPanel() {
		frame = new JFrame("ControllPanel");
		frame.setSize(800, 600);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		frame.add(panel);
		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
		panel.setLayout(new GridBagLayout());
	}
	
	public void addStrategy(Class clazz) {
		lstClass.add(clazz);
		
		JLabel lbl = new JLabel(clazz.getSimpleName());
		addToSelectPanel(lbl, 0, lstLabel.size());
		lstLabel.add(lbl);
		JButton btnStart = new JButton("Start");
		addToSelectPanel(btnStart, 1, lstStartButton.size());
		lstStartButton.add(btnStart);
		JButton btnStop = new JButton("Stop");
		addToSelectPanel(btnStop, 2, lstStopButton.size());
		lstStopButton.add(btnStop);
		
		frame.revalidate();
		
		btnStart.addActionListener(new StartListener(clazz));
		btnStop.addActionListener(new StopListener(clazz));
	}
	
	private void addToSelectPanel(Component comp, int x, int y) {
		gridBagConstraints.gridx = x;
		gridBagConstraints.gridy = y;
		panel.add(comp, gridBagConstraints);
	}
	
	private class StartListener implements ActionListener {
		private Class clazz;
		public StartListener(Class clazz) {
			this.clazz = clazz;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			if(mapStrategies.containsKey(clazz.getName())) {
				log.warn("Strategy already started.");
				return;
			}
			
			Strategy strategy;
			try {
				strategy = (Strategy) Class.forName(clazz.getName()).newInstance();
				StrategyUtils.start(strategy);
				mapStrategies.put(clazz.getName(), strategy);
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
				log.error("Load {} strategy failed.", clazz.getName(), ex);
			}
		}
	}
	
	private class StopListener implements ActionListener {
		private Class clazz;
		public StopListener(Class clazz) {
			this.clazz = clazz;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			if(!mapStrategies.containsKey(clazz.getName())) {
				log.warn("Strategy not active.");
				return;
			}
			
			Strategy strategy = mapStrategies.get(clazz.getName());
			StrategyUtils.stop(strategy);
			if(strategy.stop()) {
				mapStrategies.remove(clazz.getName());
			}
		}
	}
}
