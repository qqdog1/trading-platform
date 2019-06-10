package name.qd.arbitrage_digital_currencies.strategies.frame;

import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

public class SingleMessageFrame {
	private JFrame frame;
	private JPanel panel;
	private JTextArea textArea;
	
	public SingleMessageFrame() {
		frame = new JFrame("GOGO");
		frame.setSize(1200, 768);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		panel = new JPanel();
		
		frame.add(panel);
		textArea = new JTextArea();
		textArea.setEditable(false);
		Font font = new Font("Consolas", 0, 20);
		textArea.setFont(font);
		panel.add(textArea);
	}
	
	public void append(String msg) {
		textArea.append(msg);
		textArea.append("\n");
	}
	
	public void clear() {
		textArea.setText("");
	}
}
