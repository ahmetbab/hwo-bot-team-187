package redlynx.pong.client.ui;

import java.awt.Color;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JPanel;

import redlynx.pong.client.network.Communicator;
import redlynx.pong.ui.GameStateAccessorInterface;
import redlynx.pong.ui.PongVisualizer;

public class PongClientFrame extends JFrame implements WindowListener, KeyListener {
	
	PongVisualizer pongGameArea;
	GameStateAccessorInterface accessor;
	Communicator comm;
	
	public PongClientFrame(String title, PongVisualizer pongGameArea, GameStateAccessorInterface accessor, Communicator comm) {
		super(title);
		this.pongGameArea = pongGameArea;
		this.accessor = accessor;
		this.comm = comm;
		
	    Container content = getContentPane();
	    content.setBackground(Color.black);
	    
		addWindowListener(this);
		addKeyListener(this);

	    GridBagLayout gb = new GridBagLayout();
	    content.setLayout(gb);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 1;
	    c.weighty = 1;
	    c.fill = GridBagConstraints.BOTH;
	    c.anchor = GridBagConstraints.NORTHWEST;
	    content.add(pongGameArea, c);
	    JPanel controlArea = new JPanel(new GridLayout(12, 2));
	    controlArea.setBackground(Color.black);
	    c.gridx++;
	    c.weightx = 0;
	    c.fill = GridBagConstraints.REMAINDER;
	    content.add(controlArea, c);
		pack();
		pongGameArea.render();
		show();
	}

	
	
	@Override
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
		System.exit(-1);
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		System.exit(-1);
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_UP:
			comm.sendUpdate(-1);
			break;
		case KeyEvent.VK_DOWN:
			comm.sendUpdate(1);
			break;
        case KeyEvent.VK_S:
            comm.sendUpdate(0);
            break;
		}
	}



	@Override
	public void keyReleased(KeyEvent e) {
	}



	@Override
	public void keyTyped(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
}
