package redlynx.pong.server.ui;

import java.awt.Color;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.StringTokenizer;

import javax.swing.JFrame;
import javax.swing.JPanel;

import redlynx.pong.server.GameState;
import redlynx.pong.server.PongServer;
import redlynx.pong.ui.ButtonPanel;
import redlynx.pong.ui.PongVisualizer;
import redlynx.pong.ui.SliderPanel;
import redlynx.pong.ui.ValueChangeListener;

public class PongServerFrame extends JFrame implements WindowListener, ValueChangeListener {
	private static final long serialVersionUID = 1L;
	
	
	private SliderPanel[] dataSliders;
	private ButtonPanel pauseResumeButton;
	private PongServer server;
	GameState model;
	
	String ballSpeed = "Ball Speed";
	String ballRadius = "Ball Radius";
	String paddleHeight = "Paddle Height";
	String paddleWidth = "Paddle Width";
	String paddleSpeed = "Paddle Speed";
	//String deflectionMode = "Deflection Mode";
	//String deflectionValue1 = "Deflection Value 1";
	//String deflectionValue2 = "Deflection Value 2";
	
	String missileSpeed = "Missile Speed";
	String missileStartPos = "Missile Start Pos";
	String missileFreq = "Missile Frequency";
	
	
	String screenX = "Screen X";
	String screenY = "Screen Y";
	
	String inputLag = "Input Lag";
	String outputLag = "Output Lag";
	String messageInterval = "Message Inteval";
	String tickInterval = "Tick Inteval??";
	
	String resetButton = "Reset";
	
	String pauseButton = "Pause";
	String resumeButton = "Resume";
	
	String kick1Button = "Kick Player 1";
	String kick2Button = "Kick Player 2";
	
	String saveButton1 = "Save1";
	String loadButton1 = "Load1";
	String saveButton2 = "Save2";
	String loadButton2 = "Load2";
	String saveButton3 = "Save3";
	String loadButton3 = "Load3";
	
	
	String saveFile = "settings";
	String saveFileSuffix = ".txt";
	
	private void saveSettings(int saveId) {
		try {
			File file = new File(saveFile+saveId+saveFileSuffix);
			PrintStream out = new PrintStream(new FileOutputStream(file));
			for (int i = 0; i < dataSliders.length; i++) {
				out.println(dataSliders[i].getTitle()+":"+dataSliders[i].getValue());
			}
			out.flush();
			out.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	private void loadSettings(int saveId) {
		try {
			File file = new File(saveFile+saveId+saveFileSuffix);
			int fileLenght = (int)file.length();
			byte[] data = new byte[fileLenght];
			FileInputStream stream = new FileInputStream(file);
			stream.read(data);
			StringTokenizer st = new StringTokenizer(new String(data), "\r\n");
			while (st.hasMoreTokens()) {
				StringTokenizer pair = new StringTokenizer(st.nextToken(),":");
				String title = "";
				int value = 0;
				if(pair.hasMoreTokens()) { 
					title = pair.nextToken();
					if(pair.hasMoreTokens()) {
						String valueToken = pair.nextToken();
						try {
							value = Integer.parseInt(valueToken);
						}
						catch(NumberFormatException e) {
							System.err.println("Invalid value for "+title+": "+valueToken);
						}
					}
				}
				
				
				for (int i = 0; i < dataSliders.length; i++) {
					if (title.equals(dataSliders[i].getTitle())) {
						dataSliders[i].setValue(value);
					}
				}
				
			}
			stream.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	

	public void valueChanged(String title, int value) {
		
		System.out.println(""+title+" : "+value);
		
		if (ballSpeed.equals(title)) {
			model.setBallSpeed(value/10.0f);
		}
		else if (ballRadius.equals(title)) {
			model.setBallRadius(value);
		}
		else if (paddleHeight.equals(title)) {
			model.setPaddleHeight(value);
		}
		else if (paddleWidth.equals(title)) {
			model.setPaddleWidth(value);
		}
		else if (paddleSpeed.equals(title)) {
			model.setPaddleSpeed(value/10.0f);
		}
		/*
		else if (deflectionMode.equals(title)) {
			model.setDeflectionMode(value);
		}
		else if (deflectionValue1.equals(title)) {
			model.setDeflectionValue(0, value);
		}
		else if (deflectionValue2.equals(title)) {
			model.setDeflectionValue(1, value);
		}
		*/
		else if (missileFreq.equals(title)) {
			server.setMissileInterval(value);
		}
		else if (missileSpeed.equals(title)) {
			model.setMissileSpeed( value / 10.0 );
		}
		else if (missileStartPos.equals(title)) {
			model.setMissileStartPosition(value);
		}
		
		else if (screenX.equals(title)) {
			model.setScreenSize(value, model.screenHeight);
		}
		else if (screenY.equals(title)) {
			model.setScreenSize( model.screenWidth, value);
			
		}
		else if (inputLag.equals(title)) {
			server.setInputLag(value);
		}
		else if (outputLag.equals(title)) {
			server.setOutputLag(value);
		}
		else if (messageInterval.equals(title)) {
			server.setMessageInterval(value);
		}	
		else if (tickInterval.equals(title)) {
			model.setTickInterval(value);
		}
		else if (resetButton.equals(title)) {
			server.resetGame();
		}
		else if (pauseButton.equals(title)) {
			pauseResumeButton.setTitle(resumeButton);
			server.setPaused(true);
		}
		else if (resumeButton.equals(title)) {
			pauseResumeButton.setTitle(pauseButton);
			server.setPaused(false);
		}
		else if (kick1Button.equals(title)) {
			server.kickPlayer(0);
		}
		else if (kick2Button.equals(title)) {
			server.kickPlayer(1);
		}
		else if (saveButton1.equals(title)) {
			saveSettings(1);
		}
		else if (loadButton1.equals(title)) {
			loadSettings(1);
		}
		else if (saveButton2.equals(title)) {
			saveSettings(2);
		}
		else if (loadButton2.equals(title)) {
			loadSettings(2);
		}
		else if (saveButton3.equals(title)) {
			saveSettings(3);
		}
		else if (loadButton3.equals(title)) {
			loadSettings(3);
		}
	}
	
	
	@SuppressWarnings("deprecation")
	public PongServerFrame(PongVisualizer pongGameArea, GameState model, PongServer server) {
		super("Pong Server");
		this.model = model;
		this.server = server;
		
	    Container content = getContentPane();
	    content.setBackground(Color.black);
	    addWindowListener(this);

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
	    
	    dataSliders = new SliderPanel[] {
	    		new SliderPanel(paddleSpeed,1, 200, (int)(model.paddleConfig.maxSpeed*10), this),
	    		//new SliderPanel(deflectionMode, 0, 0, model.deflectionMode, this),
	    	    //new SliderPanel(deflectionValue1, 0, 100, model.deflectionValue[0], this),
	    	    //new SliderPanel(deflectionValue2, 0, 100, model.deflectionValue[1], this),
	    	    
	    		new SliderPanel(missileFreq, 10, 999, server.missileInterval, this),
	    		new SliderPanel(missileStartPos, 5, 95, model.missileStartPos, this),
	    		new SliderPanel(missileSpeed, 1, 200, (int)(model.missileSpeed*10), this),
	    	    	    	    
	    		new SliderPanel(paddleHeight, 1, 300, model.paddleConfig.height, this),
	    	    new SliderPanel(paddleWidth, 1, 300, model.paddleConfig.width, this),
	    	    new SliderPanel(ballRadius, 1, 50, model.ball.conf.radius, this),
	    	    new SliderPanel(ballSpeed, 1, 200, (int)(model.ball.conf.speed*10), this),
	    	    
	    	    
	    	    new SliderPanel(screenX, 10, 1000, model.screenWidth, this),
	    	    new SliderPanel(screenY, 10, 1000, model.screenHeight, this),
	    	    
	    	    new SliderPanel(inputLag, 0, 500, 0, this),
	    	    new SliderPanel(outputLag, 0, 500, 0, this),
	    	    
	    	    new SliderPanel(messageInterval, 0, 100, 0, this),
	    	    new SliderPanel(tickInterval, 1, 250, model.tickInterval, this)
	    };
	    
	    
	    
	    JPanel empty = new JPanel();   empty.setBackground(Color.BLACK);   controlArea.add(empty);
	    empty = new JPanel();    empty.setBackground(Color.BLACK);    controlArea.add(empty);
	    
	    controlArea.add(new ButtonPanel(resetButton, this));
	    pauseResumeButton = new ButtonPanel(pauseButton, this);
	    controlArea.add(pauseResumeButton);
	    
	    controlArea.add(new ButtonPanel(kick1Button, this));
	    controlArea.add(new ButtonPanel(kick2Button, this));
	    
	    //controlArea.add(dataSliders[0]);
	    //empty = new JPanel();    empty.setBackground(Color.BLACK);    controlArea.add(empty);
	    for (int i = 0; i < dataSliders.length; i++) {
	    	controlArea.add(dataSliders[i]);
	    }
 	    controlArea.add(new ButtonPanel(new String[] {saveButton1,saveButton2,saveButton3}, this));
	    controlArea.add(new ButtonPanel(new String[] {loadButton1,loadButton2, loadButton3}, this));
	    
	    
	    
	    
	    
		
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
	
}
