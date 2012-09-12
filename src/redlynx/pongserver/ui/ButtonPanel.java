package redlynx.pongserver.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

public class ButtonPanel extends JPanel implements ActionListener {

	
	private PongVisualizer listener;
	private JButton button;
	
	public void setTitle(String title) {
		
		button.setText(title);
		button.setActionCommand(title);
		button.setPreferredSize(button.getMinimumSize());
	}
	
	public ButtonPanel(String title, PongVisualizer listener) {
		this(new String[]{title}, listener);
	}
	public ButtonPanel(String[] titles, PongVisualizer listener) {
		this.listener = listener;
		setBackground(Color.BLACK);
		for (int i = 0; i< titles.length; i++  ) {
			button = new JButton(titles[i]);
			button.setPreferredSize(button.getMinimumSize());
			button.addActionListener(this);
			add(button);
		}
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		listener.valueChanged(e.getActionCommand(), 1);
		
	}
}
