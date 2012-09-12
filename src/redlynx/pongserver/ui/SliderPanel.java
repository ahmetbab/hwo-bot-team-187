package redlynx.pongserver.ui;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class SliderPanel extends JPanel implements ChangeListener {
	private PongVisualizer listener;
	private JTextField text;
	private JSlider slider;
	private String title;
	
	public String getTitle() {
		return title;
	}
	public int getValue() {
		return slider.getValue();
	}
	public void setValue(int value) {
		slider.setValue(value);
	}
	
	SliderPanel(String title, int min, int max, int defaultValue, PongVisualizer listener) {
		this.listener = listener;
		this.title = title;
		
		setBackground(Color.BLACK);
		
		TitledBorder border =BorderFactory.createTitledBorder( title);
		border.setTitleColor(Color.WHITE);
		setBorder(border);
		text = new JTextField(2); 
		text.setEnabled(false);
		text.setText(""+defaultValue);
		text.setDisabledTextColor(Color.black);
		 
		slider = new JSlider();
		slider.setBackground(Color.BLACK);
		slider.setMaximum(max);
		slider.setMinimum(min);
		slider.setValue(defaultValue);
		
		slider.addChangeListener(this);
		add(text);
		add(slider);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		 JSlider source = (JSlider)e.getSource();  // get the slider
		 int value = source.getValue();
		 text.setText(""+value); 
	     if (!source.getValueIsAdjusting()) {
	        listener.valueChanged(title, value);
	     }
	}
}